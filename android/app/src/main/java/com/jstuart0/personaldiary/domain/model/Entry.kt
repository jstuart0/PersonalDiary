package com.jstuart0.personaldiary.domain.model

/**
 * Diary entry domain model
 */
data class Entry(
    val entryId: String,
    val userId: String,
    val title: String? = null,
    val content: String, // Decrypted content
    val contentHash: String, // SHA-256 hash of plaintext
    val source: EntrySource,
    val tags: List<String> = emptyList(),
    val mediaIds: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val externalPostId: String? = null, // For social media posts
    val externalPostUrl: String? = null
)

/**
 * Source of the diary entry
 */
enum class EntrySource {
    DIARY,
    FACEBOOK,
    INSTAGRAM,
    TWITTER
}

/**
 * Sync status for offline-first architecture
 */
enum class SyncStatus {
    SYNCED,      // Successfully synced with server
    PENDING,     // Waiting to be uploaded
    FAILED,      // Upload failed
    CONFLICT     // Server has newer version
}
