package com.jstuart0.personaldiary.data.repository

import com.jstuart0.personaldiary.data.encryption.EncryptionService
import com.jstuart0.personaldiary.data.local.dao.EntryDao
import com.jstuart0.personaldiary.data.local.dao.EntryFtsDao
import com.jstuart0.personaldiary.data.local.dao.MediaDao
import com.jstuart0.personaldiary.data.local.entity.EntryEntity
import com.jstuart0.personaldiary.data.local.entity.EntryFtsEntity
import com.jstuart0.personaldiary.data.remote.api.PersonalDiaryApi
import com.jstuart0.personaldiary.data.remote.model.*
import com.jstuart0.personaldiary.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for diary entries
 * Implements offline-first pattern
 */
@Singleton
class EntryRepository @Inject constructor(
    private val api: PersonalDiaryApi,
    private val entryDao: EntryDao,
    private val entryFtsDao: EntryFtsDao,
    private val mediaDao: MediaDao,
    private val encryptionService: EncryptionService
) {

    /**
     * Get entries as Flow for reactive updates
     */
    fun getEntriesFlow(userId: String): Flow<List<Entry>> {
        return entryDao.getEntriesFlow(userId).map { entities ->
            entities.map { entity ->
                decryptEntry(entity)
            }
        }
    }

    /**
     * Get single entry by ID
     */
    suspend fun getEntry(entryId: String): Entry? {
        val entity = entryDao.getEntry(entryId) ?: return null
        return decryptEntry(entity)
    }

    /**
     * Get entries by date range
     */
    fun getEntriesByDateRange(userId: String, startTime: Long, endTime: Long): Flow<List<Entry>> {
        return entryDao.getEntriesByDateRange(userId, startTime, endTime).map { entities ->
            entities.map { decryptEntry(it) }
        }
    }

    /**
     * Get entries by source (DIARY, FACEBOOK, etc.)
     */
    fun getEntriesBySource(userId: String, source: EntrySource): Flow<List<Entry>> {
        return entryDao.getEntriesBySource(userId, source.name).map { entities ->
            entities.map { decryptEntry(it) }
        }
    }

    /**
     * Create new entry
     * Saves locally first, then syncs to server
     */
    suspend fun createEntry(
        userId: String,
        title: String?,
        content: String,
        tags: List<String> = emptyList(),
        mediaIds: List<String> = emptyList()
    ): Result<Entry> {
        return try {
            // Encrypt content
            val encryptedContent = encryptionService.encrypt(content).getOrThrow()
            val contentHash = encryptionService.generateContentHash(content)

            // Create entry
            val entryId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val entry = Entry(
                entryId = entryId,
                userId = userId,
                title = title,
                content = content,
                contentHash = contentHash,
                source = EntrySource.DIARY,
                tags = tags,
                mediaIds = mediaIds,
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )

            // Save to local database
            val entity = EntryEntity.fromDomain(entry, encryptedContent)
            entryDao.insertEntryWithTags(entity, tags)

            // Save to FTS for E2E tier
            if (encryptionService.getEncryptionTier() == EncryptionTier.E2E) {
                val ftsEntity = EntryFtsEntity(
                    rowid = 0, // Auto-generated
                    entryId = entryId,
                    title = title,
                    content = content,
                    tags = tags.joinToString(",")
                )
                entryFtsDao.insert(ftsEntity)
            }

            // Sync to server in background (handled by WorkManager)
            // Mark for sync
            entryDao.updateSyncStatus(entryId, SyncStatus.PENDING.name)

            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update existing entry
     */
    suspend fun updateEntry(
        entryId: String,
        title: String?,
        content: String,
        tags: List<String> = emptyList(),
        mediaIds: List<String> = emptyList()
    ): Result<Entry> {
        return try {
            // Get existing entry
            val existingEntity = entryDao.getEntry(entryId)
                ?: return Result.failure(Exception("Entry not found"))

            // Encrypt content
            val encryptedContent = encryptionService.encrypt(content).getOrThrow()
            val contentHash = encryptionService.generateContentHash(content)

            // Update entry
            val updatedEntity = existingEntity.copy(
                title = title,
                encryptedContent = encryptedContent,
                contentHash = contentHash,
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING.name
            )

            // Save to local database
            entryDao.updateEntryWithTags(updatedEntity, tags)

            // Update FTS for E2E tier
            if (encryptionService.getEncryptionTier() == EncryptionTier.E2E) {
                val ftsEntity = EntryFtsEntity(
                    rowid = 0,
                    entryId = entryId,
                    title = title,
                    content = content,
                    tags = tags.joinToString(",")
                )
                entryFtsDao.update(ftsEntity)
            }

            // Decrypt and return
            val entry = decryptEntry(updatedEntity)
            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete entry
     */
    suspend fun deleteEntry(entryId: String): Result<Unit> {
        return try {
            val entity = entryDao.getEntry(entryId)
                ?: return Result.failure(Exception("Entry not found"))

            // Delete from local database
            entryDao.delete(entity)

            // Delete from FTS
            entryFtsDao.delete(entryId)

            // Delete associated media
            mediaDao.deleteForEntry(entryId)

            // Mark for deletion sync
            // TODO: Track deletions for sync

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync entries with server
     */
    suspend fun syncEntries(userId: String, lastSyncAt: Long?): Result<Unit> {
        return try {
            // Get local entries pending sync
            val pendingEntries = entryDao.getEntriesByStatus(userId, SyncStatus.PENDING.name)

            // Convert to DTOs
            val localEntryDtos = pendingEntries.map { entity ->
                val tags = entryDao.getTagsForEntry(entity.entryId)
                val mediaIds = mediaDao.getMediaForEntry(entity.entryId).map { it.mediaId }

                EntryDto(
                    entryId = entity.entryId,
                    userId = entity.userId,
                    title = entity.title,
                    encryptedContent = entity.encryptedContent,
                    contentHash = entity.contentHash,
                    source = entity.source,
                    tags = tags,
                    mediaIds = mediaIds,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    externalPostId = entity.externalPostId,
                    externalPostUrl = entity.externalPostUrl
                )
            }

            // Call sync API
            val request = SyncEntriesRequest(
                lastSyncAt = lastSyncAt,
                localEntries = localEntryDtos
            )
            val response = api.syncEntries(request)

            if (response.isSuccessful && response.body() != null) {
                val syncResponse = response.body()!!

                // Save server entries locally
                syncResponse.serverEntries.forEach { dto ->
                    val entity = EntryEntity(
                        entryId = dto.entryId,
                        userId = dto.userId,
                        title = dto.title,
                        encryptedContent = dto.encryptedContent,
                        contentHash = dto.contentHash,
                        source = dto.source,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                        syncStatus = SyncStatus.SYNCED.name,
                        externalPostId = dto.externalPostId,
                        externalPostUrl = dto.externalPostUrl
                    )
                    entryDao.insertEntryWithTags(entity, dto.tags)

                    // Update FTS if E2E tier
                    if (encryptionService.getEncryptionTier() == EncryptionTier.E2E) {
                        val decryptedContent = encryptionService.decrypt(dto.encryptedContent).getOrNull()
                        if (decryptedContent != null) {
                            val ftsEntity = EntryFtsEntity(
                                rowid = 0,
                                entryId = dto.entryId,
                                title = dto.title,
                                content = decryptedContent,
                                tags = dto.tags.joinToString(",")
                            )
                            entryFtsDao.insert(ftsEntity)
                        }
                    }
                }

                // Mark local entries as synced
                pendingEntries.forEach { entity ->
                    entryDao.updateSyncStatus(entity.entryId, SyncStatus.SYNCED.name)
                }

                // Handle conflicts (simplified - take server version)
                syncResponse.conflicts.forEach { conflict ->
                    val entity = EntryEntity(
                        entryId = conflict.serverVersion.entryId,
                        userId = conflict.serverVersion.userId,
                        title = conflict.serverVersion.title,
                        encryptedContent = conflict.serverVersion.encryptedContent,
                        contentHash = conflict.serverVersion.contentHash,
                        source = conflict.serverVersion.source,
                        createdAt = conflict.serverVersion.createdAt,
                        updatedAt = conflict.serverVersion.updatedAt,
                        syncStatus = SyncStatus.CONFLICT.name,
                        externalPostId = conflict.serverVersion.externalPostId,
                        externalPostUrl = conflict.serverVersion.externalPostUrl
                    )
                    entryDao.insert(entity)
                }

                Result.success(Unit)
            } else {
                Result.failure(Exception("Sync failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Helper to decrypt entry and load tags/media
     */
    private suspend fun decryptEntry(entity: EntryEntity): Entry {
        val decryptedContent = encryptionService.decrypt(entity.encryptedContent)
            .getOrElse { "" }

        val tags = entryDao.getTagsForEntry(entity.entryId)
        val mediaIds = mediaDao.getMediaForEntry(entity.entryId).map { it.mediaId }

        return entity.toDomain(decryptedContent, tags, mediaIds)
    }
}
