package com.jstuart0.personaldiary.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Social media integration API models
 */

// ==================== Social Account DTOs ====================

data class SocialAccountDto(
    @SerializedName("account_id")
    val accountId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("platform")
    val platform: String,
    @SerializedName("platform_user_id")
    val platformUserId: String,
    @SerializedName("platform_username")
    val platformUsername: String,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("last_sync_at")
    val lastSyncAt: Long?,
    @SerializedName("created_at")
    val createdAt: Long
)

// ==================== OAuth Exchange ====================

data class SocialTokenResponse(
    @SerializedName("account_id")
    val accountId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("provider_user_id")
    val providerUserId: String,
    @SerializedName("display_name")
    val displayName: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("profile_picture_url")
    val profilePictureUrl: String?,
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String?,
    @SerializedName("expires_in")
    val expiresIn: Long
)

// ==================== Social Sharing ====================

data class SocialShareResponse(
    @SerializedName("post_id")
    val postId: String,
    @SerializedName("post_url")
    val postUrl: String?,
    @SerializedName("success")
    val success: Boolean
)

// ==================== Social Import ====================

data class SocialImportResponse(
    @SerializedName("posts")
    val posts: List<ImportedPostDto>,
    @SerializedName("has_more")
    val hasMore: Boolean,
    @SerializedName("next_cursor")
    val nextCursor: String?
)

data class ImportedPostDto(
    @SerializedName("post_id")
    val postId: String,
    @SerializedName("message")
    val message: String?,
    @SerializedName("created_time")
    val createdTime: Long,
    @SerializedName("permalink")
    val permalink: String?,
    @SerializedName("attachments")
    val attachments: List<String>? = null
)

// ==================== Requests ====================

data class ConnectSocialAccountRequest(
    @SerializedName("platform")
    val platform: String,
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("platform_user_id")
    val platformUserId: String,
    @SerializedName("platform_username")
    val platformUsername: String
)