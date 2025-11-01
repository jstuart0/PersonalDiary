package com.jstuart0.personaldiary.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jstuart0.personaldiary.domain.model.Media
import com.jstuart0.personaldiary.domain.model.SyncStatus

/**
 * Room entity for encrypted media files
 */
@Entity(
    tableName = "media",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entryId"), Index("syncStatus")]
)
data class MediaEntity(
    @PrimaryKey
    val mediaId: String,
    val entryId: String,
    val localPath: String, // Path to encrypted file
    val serverUrl: String? = null,
    val mimeType: String,
    val fileSize: Long,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null,
    val syncStatus: String,
    val createdAt: Long
) {
    fun toDomain(): Media {
        return Media(
            mediaId = mediaId,
            entryId = entryId,
            localPath = localPath,
            serverUrl = serverUrl,
            mimeType = mimeType,
            fileSize = fileSize,
            width = width,
            height = height,
            duration = duration,
            syncStatus = SyncStatus.valueOf(syncStatus),
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(media: Media): MediaEntity {
            return MediaEntity(
                mediaId = media.mediaId,
                entryId = media.entryId,
                localPath = media.localPath,
                serverUrl = media.serverUrl,
                mimeType = media.mimeType,
                fileSize = media.fileSize,
                width = media.width,
                height = media.height,
                duration = media.duration,
                syncStatus = media.syncStatus.name,
                createdAt = media.createdAt
            )
        }
    }
}
