package com.jstuart0.personaldiary.domain.model

/**
 * Media (photo/video) domain model
 */
data class Media(
    val mediaId: String,
    val entryId: String,
    val localPath: String, // Local encrypted file path
    val serverUrl: String? = null, // URL on server
    val mimeType: String,
    val fileSize: Long,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null, // For videos, in seconds
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long
)
