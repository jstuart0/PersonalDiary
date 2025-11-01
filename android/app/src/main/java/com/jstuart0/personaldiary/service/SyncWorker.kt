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
            // Get pending entries
            val pendingEntries = entryDao.getEntriesByStatus(SyncStatus.PENDING.name)

            Log.d(TAG, "Found ${pendingEntries.size} pending entries to sync")

            for (entry in pendingEntries) {
                try {
                    if (entry.serverEntryId == null) {
                        // Create new entry on server
                        val request = CreateEntryRequest(
                            title = entry.title,
                            content = entry.content,
                            tags = entry.tags,
                            mediaIds = emptyList(),
                            timestamp = entry.timestamp
                        )

                        val response = api.createEntry(request)
                        if (response.isSuccessful && response.body() != null) {
                            val serverEntry = response.body()!!
                            entryDao.updateServerEntryId(
                                localId = entry.entryId,
                                serverId = serverEntry.entryId,
                                status = SyncStatus.SYNCED.name
                            )
                            Log.d(TAG, "Created entry ${entry.entryId} on server")
                        } else {
                            Log.e(TAG, "Failed to create entry ${entry.entryId}")
                            return false
                        }
                    } else {
                        // Update existing entry on server
                        val request = UpdateEntryRequest(
                            title = entry.title,
                            content = entry.content,
                            tags = entry.tags,
                            mediaIds = emptyList()
                        )

                        val response = api.updateEntry(entry.serverEntryId, request)
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
                val lastSync = entryDao.getLastSyncTimestamp() ?: 0L
                val response = api.getEntries(
                    since = lastSync,
                    limit = 100
                )

                if (response.isSuccessful && response.body() != null) {
                    val entries = response.body()!!.entries
                    Log.d(TAG, "Downloaded ${entries.size} new entries from server")

                    for (serverEntry in entries) {
                        // Check if entry already exists locally
                        val existingEntry = entryDao.getEntryByServerId(serverEntry.entryId)
                        if (existingEntry == null) {
                            // Insert new entry
                            val localEntry = com.jstuart0.personaldiary.data.local.entity.EntryEntity(
                                entryId = java.util.UUID.randomUUID().toString(),
                                serverEntryId = serverEntry.entryId,
                                userId = serverEntry.userId,
                                title = serverEntry.title,
                                content = serverEntry.content,
                                tags = serverEntry.tags,
                                timestamp = serverEntry.timestamp,
                                createdAt = serverEntry.createdAt,
                                updatedAt = serverEntry.updatedAt,
                                syncStatus = SyncStatus.SYNCED.name,
                                lastSyncAt = System.currentTimeMillis()
                            )
                            entryDao.insert(localEntry)
                        } else if (serverEntry.updatedAt > existingEntry.updatedAt) {
                            // Update existing entry if server version is newer
                            val updatedEntry = existingEntry.copy(
                                title = serverEntry.title,
                                content = serverEntry.content,
                                tags = serverEntry.tags,
                                updatedAt = serverEntry.updatedAt,
                                syncStatus = SyncStatus.SYNCED.name,
                                lastSyncAt = System.currentTimeMillis()
                            )
                            entryDao.update(updatedEntry)
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
