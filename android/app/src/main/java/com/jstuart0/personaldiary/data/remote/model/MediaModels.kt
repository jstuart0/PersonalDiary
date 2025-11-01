package com.jstuart0.personaldiary.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * API Request/Response models for media files
 */

data class MediaDto(
    @SerializedName("media_id")
    val mediaId: String,
    @SerializedName("entry_id")
    val entryId: String,
    @SerializedName("server_url")
    val serverUrl: String,
    @SerializedName("mime_type")
    val mimeType: String,
    @SerializedName("file_size")
    val fileSize: Long,
    @SerializedName("width")
    val width: Int? = null,
    @SerializedName("height")
    val height: Int? = null,
    @SerializedName("duration")
    val duration: Int? = null,
    @SerializedName("created_at")
    val createdAt: Long
)

data class MediaUploadResponse(
    @SerializedName("media_id")
    val mediaId: String,
    @SerializedName("upload_url")
    val uploadUrl: String,
    @SerializedName("server_url")
    val serverUrl: String
)
