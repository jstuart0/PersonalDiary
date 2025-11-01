package com.jstuart0.personaldiary.data.repository

import com.jstuart0.personaldiary.data.encryption.EncryptionService
import com.jstuart0.personaldiary.data.local.dao.EntryDao
import com.jstuart0.personaldiary.data.local.dao.EntryFtsDao
import com.jstuart0.personaldiary.data.remote.api.PersonalDiaryApi
import com.jstuart0.personaldiary.data.remote.model.SearchRequest
import com.jstuart0.personaldiary.domain.model.EncryptionTier
import com.jstuart0.personaldiary.domain.model.Entry
import com.jstuart0.personaldiary.domain.model.SearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for search operations
 * Uses FTS for E2E tier (local search)
 * Uses API search for UCE tier (server-side search)
 */
@Singleton
class SearchRepository @Inject constructor(
    private val api: PersonalDiaryApi,
    private val entryDao: EntryDao,
    private val entryFtsDao: EntryFtsDao,
    private val encryptionService: EncryptionService
) {

    /**
     * Search entries based on encryption tier
     */
    suspend fun search(query: String, limit: Int = 50): Result<List<SearchResult>> {
        return when (encryptionService.getEncryptionTier()) {
            EncryptionTier.E2E -> searchLocal(query, limit)
            EncryptionTier.UCE -> searchRemote(query, limit)
        }
    }

    /**
     * Local search using FTS (E2E tier)
     */
    private suspend fun searchLocal(query: String, limit: Int): Result<List<SearchResult>> {
        return try {
            val ftsResults = entryFtsDao.searchWithLimit(query, limit)

            val results = ftsResults.map { ftsEntity ->
                val entry = entryDao.getEntry(ftsEntity.entryId)
                val tags = entryDao.getTagsForEntry(ftsEntity.entryId)

                SearchResult(
                    entryId = ftsEntity.entryId,
                    title = ftsEntity.title,
                    snippet = ftsEntity.content.take(200), // First 200 chars
                    tags = tags,
                    createdAt = entry?.createdAt ?: 0L,
                    score = 1.0f // FTS doesn't provide score
                )
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remote search using API (UCE tier)
     */
    private suspend fun searchRemote(query: String, limit: Int): Result<List<SearchResult>> {
        return try {
            val request = SearchRequest(query = query, limit = limit)
            val response = api.search(request)

            if (response.isSuccessful && response.body() != null) {
                val searchResponse = response.body()!!

                val results = searchResponse.results.map { dto ->
                    SearchResult(
                        entryId = dto.entryId,
                        title = dto.title,
                        snippet = dto.snippet,
                        tags = emptyList(), // Would need to fetch separately
                        createdAt = dto.createdAt,
                        score = dto.score
                    )
                }

                Result.success(results)
            } else {
                Result.failure(Exception("Search failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all unique tags
     */
    fun getAllTags(): Flow<List<String>> {
        return entryDao.getAllTags()
    }

    /**
     * Get user-created tags (excluding auto-generated)
     */
    fun getUserTags(): Flow<List<String>> {
        return entryDao.getUserTags()
    }
}
