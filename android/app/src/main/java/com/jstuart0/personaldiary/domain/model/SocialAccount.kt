package com.jstuart0.personaldiary.domain.model

/**
 * Connected social media account
 */
data class SocialAccount(
    val accountId: String,
    val platform: SocialPlatform,
    val platformUserId: String,
    val platformUsername: String,
    val isActive: Boolean,
    val lastSyncAt: Long?,
    val createdAt: Long
)

/**
 * Supported social media platforms
 */
enum class SocialPlatform {
    FACEBOOK,
    INSTAGRAM, // Future
    TWITTER    // Future
}

/**
 * Imported post from social media
 */
data class ImportedPost(
    val postId: String,
    val message: String?,
    val createdTime: Long,
    val permalink: String?,
    val attachments: List<String>? = null
)
