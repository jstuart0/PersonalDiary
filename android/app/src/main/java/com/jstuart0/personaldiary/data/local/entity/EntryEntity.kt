package com.jstuart0.personaldiary.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jstuart0.personaldiary.domain.model.Entry
import com.jstuart0.personaldiary.domain.model.EntrySource
import com.jstuart0.personaldiary.domain.model.SyncStatus

/**
 * Room entity for encrypted diary entries
 */
@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index("createdAt"),
        Index("syncStatus")
    ]
)
data class EntryEntity(
    @PrimaryKey
    val entryId: String,
    val userId: String,
    val title: String? = null,
    val encryptedContent: String, // Encrypted content
    val contentHash: String, // SHA-256 hash
    val source: String, // DIARY, FACEBOOK, etc.
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String, // SYNCED, PENDING, FAILED, CONFLICT
    val externalPostId: String? = null,
    val externalPostUrl: String? = null
) {
    fun toDomain(decryptedContent: String, tags: List<String>, mediaIds: List<String>): Entry {
        return Entry(
            entryId = entryId,
            userId = userId,
            title = title,
            content = decryptedContent,
            contentHash = contentHash,
            source = EntrySource.valueOf(source),
            tags = tags,
            mediaIds = mediaIds,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncStatus = SyncStatus.valueOf(syncStatus),
            externalPostId = externalPostId,
            externalPostUrl = externalPostUrl
        )
    }

    companion object {
        fun fromDomain(entry: Entry, encryptedContent: String): EntryEntity {
            return EntryEntity(
                entryId = entry.entryId,
                userId = entry.userId,
                title = entry.title,
                encryptedContent = encryptedContent,
                contentHash = entry.contentHash,
                source = entry.source.name,
                createdAt = entry.createdAt,
                updatedAt = entry.updatedAt,
                syncStatus = entry.syncStatus.name,
                externalPostId = entry.externalPostId,
                externalPostUrl = entry.externalPostUrl
            )
        }
    }
}
