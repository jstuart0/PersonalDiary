package com.jstuart0.personaldiary.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * API Request/Response models for diary entries
 */

data class EntryDto(
    @SerializedName("entry_id")
    val entryId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("encrypted_content")
    val encryptedContent: String,
    @SerializedName("content_hash")
    val contentHash: String,
    @SerializedName("source")
    val source: String,
    @SerializedName("tags")
    val tags: List<String>,
    @SerializedName("media_ids")
    val mediaIds: List<String>,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("updated_at")
    val updatedAt: Long,
    @SerializedName("external_post_id")
    val externalPostId: String? = null,
    @SerializedName("external_post_url")
    val externalPostUrl: String? = null
)

data class CreateEntryRequest(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("encrypted_content")
    val encryptedContent: String,
    @SerializedName("content_hash")
    val contentHash: String,
    @SerializedName("source")
    val source: String = "DIARY",
    @SerializedName("tags")
    val tags: List<String> = emptyList(),
    @SerializedName("media_ids")
    val mediaIds: List<String> = emptyList()
)

data class UpdateEntryRequest(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("encrypted_content")
    val encryptedContent: String,
    @SerializedName("content_hash")
    val contentHash: String,
    @SerializedName("tags")
    val tags: List<String> = emptyList(),
    @SerializedName("media_ids")
    val mediaIds: List<String> = emptyList()
)

data class SyncEntriesRequest(
    @SerializedName("last_sync_at")
    val lastSyncAt: Long?,
    @SerializedName("local_entries")
    val localEntries: List<EntryDto>
)

data class SyncEntriesResponse(
    @SerializedName("server_entries")
    val serverEntries: List<EntryDto>,
    @SerializedName("conflicts")
    val conflicts: List<ConflictDto>,
    @SerializedName("sync_timestamp")
    val syncTimestamp: Long
)

data class ConflictDto(
    @SerializedName("entry_id")
    val entryId: String,
    @SerializedName("server_version")
    val serverVersion: EntryDto,
    @SerializedName("client_version")
    val clientVersion: EntryDto
)

data class SearchRequest(
    @SerializedName("query")
    val query: String,
    @SerializedName("limit")
    val limit: Int = 50,
    @SerializedName("offset")
    val offset: Int = 0
)

data class SearchResponse(
    @SerializedName("results")
    val results: List<SearchResultDto>,
    @SerializedName("total_count")
    val totalCount: Int
)

data class SearchResultDto(
    @SerializedName("entry_id")
    val entryId: String,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("snippet")
    val snippet: String,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("score")
    val score: Float
)
