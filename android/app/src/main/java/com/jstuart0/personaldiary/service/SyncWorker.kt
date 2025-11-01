package com.jstuart0.personaldiary.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.jstuart0.personaldiary.data.local.dao.EntryDao
import com.jstuart0.personaldiary.data.local.dao.MediaDao
import com.jstuart0.personaldiary.data.remote.api.PersonalDiaryApi
import com.jstuart0.personaldiary.data.remote.model.CreateEntryRequest
import com.jstuart0.personaldiary.data.remote.model.UpdateEntryRequest
import com.jstuart0.personaldiary.data.repository.AuthRepository
import com.jstuart0.personaldiary.data.repository.MediaRepository
import com.jstuart0.personaldiary.domain.model.SyncStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Background sync worker for syncing local changes to server
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val authRepository: AuthRepository,
    private val entryDao: EntryDao,
    private val mediaDao: MediaDao,
    private val mediaRepository: MediaRepository,
    private val api: PersonalDiaryApi
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Check if user is logged in
            val user = authRepository.getCurrentUser()
            if (user == null) {
                Log.d(TAG, "User not logged in, skipping sync")
                return Result.success()
            }

            Log.d(TAG, "Starting background sync")

            // Sync entries
            val entrySyncResult = syncEntries()
            if (!entrySyncResult) {
                return Result.retry()
            }

            // Sync media
            val mediaSyncResult = syncMedia()
            if (!mediaSyncResult) {
                return Result.retry()
            }

            Log.d(TAG, "Background sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * Sync entries with server
     */
    private suspend fun syncEntries(): Boolean {
        try {
            // Get current user
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                Log.e(TAG, "No current user found")
                return false
            }

            // Get pending entries
            val pendingEntries = entryDao.getEntriesByStatus(currentUser.userId, SyncStatus.PENDING.name)

            Log.d(TAG, "Found ${pendingEntries.size} pending entries to sync")

            for (entry in pendingEntries) {
                try {
                    if (entry.externalPostId == null) {
                        // Get tags for entry
                        val tags = entryDao.getTagsForEntry(entry.entryId)

                        // Create new entry on server
                        val request = CreateEntryRequest(
                            title = entry.title,
                            encryptedContent = entry.encryptedContent,
                            contentHash = entry.contentHash,
                            source = entry.source,
                            tags = tags,
                            mediaIds = emptyList()
                        )

                        val response = api.createEntry(request)
                        if (response.isSuccessful && response.body() != null) {
                            val serverEntry = response.body()!!
                            entryDao.updateServerEntryId(
                                entryId = entry.entryId,
                                serverEntryId = serverEntry.entryId
                            )
                            entryDao.updateSyncStatus(entry.entryId, SyncStatus.SYNCED.name)
                            Log.d(TAG, "Created entry ${entry.entryId} on server")
                        } else {
                            Log.e(TAG, "Failed to create entry ${entry.entryId}")
                            return false
                        }
                    } else {
                        // Get tags for entry
                        val tags = entryDao.getTagsForEntry(entry.entryId)

                        // Update existing entry on server
                        val request = UpdateEntryRequest(
                            title = entry.title,
                            encryptedContent = entry.encryptedContent,
                            contentHash = entry.contentHash,
                            tags = tags,
                            mediaIds = emptyList()
                        )

                        val response = api.updateEntry(entry.externalPostId!!, request)
                        if (response.isSuccessful) {
                            entryDao.updateSyncStatus(entry.entryId, SyncStatus.SYNCED.name)
                            Log.d(TAG, "Updated entry ${entry.entryId} on server")
                        } else {
                            Log.e(TAG, "Failed to update entry ${entry.entryId}")
                            return false
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync entry ${entry.entryId}", e)
                    // Continue with other entries
                }
            }

            // Download new entries from server
            try {
                // For now, get all entries (later can implement lastSync timestamp)
                val response = api.getEntries(
                    limit = 100,
                    offset = 0
                )

                if (response.isSuccessful && response.body() != null) {
                    val entries = response.body()!!
                    Log.d(TAG, "Downloaded ${entries.size} new entries from server")

                    for (serverEntry in entries) {
                        // Check if entry already exists locally
                        val existingEntry = entryDao.getEntryByServerId(serverEntry.entryId)
                        if (existingEntry == null) {
                            // Insert new entry
                            val localEntryId = java.util.UUID.randomUUID().toString()
                            val localEntry = com.jstuart0.personaldiary.data.local.entity.EntryEntity(
                                entryId = localEntryId,
                                userId = serverEntry.userId,
                                title = serverEntry.title,
                                encryptedContent = serverEntry.encryptedContent,
                                contentHash = serverEntry.contentHash,
                                source = serverEntry.source,
                                createdAt = serverEntry.createdAt,
                                updatedAt = serverEntry.updatedAt,
                                syncStatus = SyncStatus.SYNCED.name,
                                externalPostId = serverEntry.entryId,
                                externalPostUrl = null
                            )
                            entryDao.insert(localEntry)

                            // Insert tags for new entry
                            if (serverEntry.tags.isNotEmpty()) {
                                val tagEntities = serverEntry.tags.map { tagName ->
                                    com.jstuart0.personaldiary.data.local.entity.EntryTagEntity(
                                        entryId = localEntryId,
                                        tagName = tagName,
                                        autoGenerated = false
                                    )
                                }
                                entryDao.insertTags(tagEntities)
                            }
                        } else if (serverEntry.updatedAt > existingEntry.updatedAt) {
                            // Update existing entry if server version is newer
                            val updatedEntry = existingEntry.copy(
                                title = serverEntry.title,
                                encryptedContent = serverEntry.encryptedContent,
                                contentHash = serverEntry.contentHash,
                                updatedAt = serverEntry.updatedAt,
                                syncStatus = SyncStatus.SYNCED.name
                            )
                            entryDao.update(updatedEntry)

                            // Update tags separately if they exist
                            if (serverEntry.tags.isNotEmpty()) {
                                entryDao.deleteTagsForEntry(existingEntry.entryId)
                                val tagEntities = serverEntry.tags.map { tagName ->
                                    com.jstuart0.personaldiary.data.local.entity.EntryTagEntity(
                                        entryId = existingEntry.entryId,
                                        tagName = tagName,
                                        autoGenerated = false
                                    )
                                }
                                entryDao.insertTags(tagEntities)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download entries from server", e)
                // Don't fail sync if download fails
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Entry sync failed", e)
            return false
        }
    }

    /**
     * Sync media with server
     */
    private suspend fun syncMedia(): Boolean {
        try {
            // Get pending media
            val pendingMedia = mediaDao.getMediaByStatus(SyncStatus.PENDING.name)

            Log.d(TAG, "Found ${pendingMedia.size} pending media files to sync")

            for (media in pendingMedia) {
                try {
                    val result = mediaRepository.uploadMedia(media.mediaId)
                    if (result.isSuccess) {
                        Log.d(TAG, "Uploaded media ${media.mediaId}")
                    } else {
                        Log.e(TAG, "Failed to upload media ${media.mediaId}")
                        // Continue with other media files
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload media ${media.mediaId}", e)
                    // Continue with other media files
                }
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Media sync failed", e)
            return false
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "PersonalDiarySync"

        /**
         * Schedule periodic sync work
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncWork
            )

            Log.d(TAG, "Scheduled periodic sync work")
        }

        /**
         * Trigger immediate sync
         */
        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWork = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_immediate",
                ExistingWorkPolicy.REPLACE,
                syncWork
            )

            Log.d(TAG, "Triggered immediate sync")
        }

        /**
         * Cancel all sync work
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled sync work")
        }
    }
}
