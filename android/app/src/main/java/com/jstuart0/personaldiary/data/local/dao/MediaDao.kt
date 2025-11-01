package com.jstuart0.personaldiary.data.local.dao

import androidx.room.*
import com.jstuart0.personaldiary.data.local.entity.MediaEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Media operations
 */
@Dao
interface MediaDao {

    @Query("SELECT * FROM media WHERE entryId = :entryId ORDER BY createdAt ASC")
    suspend fun getMediaForEntry(entryId: String): List<MediaEntity>

    @Query("SELECT * FROM media WHERE entryId = :entryId ORDER BY createdAt ASC")
    fun getMediaForEntryFlow(entryId: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE mediaId = :mediaId")
    suspend fun getMedia(mediaId: String): MediaEntity?

    @Query("SELECT * FROM media WHERE syncStatus = :status")
    suspend fun getMediaByStatus(status: String): List<MediaEntity>

    @Query("SELECT * FROM media WHERE serverUrl IS NULL")
    suspend fun getLocalOnlyMedia(): List<MediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: MediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(media: List<MediaEntity>)

    @Update
    suspend fun update(media: MediaEntity)

    @Delete
    suspend fun delete(media: MediaEntity)

    @Query("DELETE FROM media WHERE entryId = :entryId")
    suspend fun deleteForEntry(entryId: String)

    @Query("UPDATE media SET syncStatus = :status WHERE mediaId = :mediaId")
    suspend fun updateSyncStatus(mediaId: String, status: String)

    @Query("UPDATE media SET serverUrl = :url, syncStatus = :status WHERE mediaId = :mediaId")
    suspend fun updateServerUrl(mediaId: String, url: String, status: String)

    @Query("SELECT COUNT(*) FROM media WHERE entryId = :entryId")
    suspend fun getMediaCount(entryId: String): Int

    @Query("SELECT SUM(fileSize) FROM media")
    suspend fun getTotalStorageUsed(): Long?
}
