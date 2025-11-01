package com.jstuart0.personaldiary.data.repository

import android.content.Context
import com.jstuart0.personaldiary.data.encryption.EncryptionService
import com.jstuart0.personaldiary.data.local.dao.MediaDao
import com.jstuart0.personaldiary.data.local.entity.MediaEntity
import com.jstuart0.personaldiary.data.remote.api.PersonalDiaryApi
import com.jstuart0.personaldiary.domain.model.Media
import com.jstuart0.personaldiary.domain.model.SyncStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for media files (photos, videos)
 * Handles encryption, upload, and download
 */
@Singleton
class MediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: PersonalDiaryApi,
    private val mediaDao: MediaDao,
    private val encryptionService: EncryptionService
) {

    /**
     * Get media for entry
     */
    fun getMediaForEntry(entryId: String): Flow<List<Media>> {
        return mediaDao.getMediaForEntryFlow(entryId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Save media file locally (encrypted)
     */
    suspend fun saveMedia(
        entryId: String,
        sourceFile: File,
        mimeType: String,
        width: Int? = null,
        height: Int? = null,
        duration: Int? = null
    ): Result<Media> {
        return try {
            val mediaId = UUID.randomUUID().toString()
            val encryptedFile = File(context.filesDir, "media/$mediaId.enc")
            encryptedFile.parentFile?.mkdirs()

            // Encrypt file
            val fileContent = sourceFile.readBytes()
            val encryptedContent = encryptionService.encrypt(String(fileContent)).getOrThrow()
            encryptedFile.writeText(encryptedContent)

            // Create media entity
            val media = Media(
                mediaId = mediaId,
                entryId = entryId,
                localPath = encryptedFile.absolutePath,
                serverUrl = null,
                mimeType = mimeType,
                fileSize = encryptedFile.length(),
                width = width,
                height = height,
                duration = duration,
                syncStatus = SyncStatus.PENDING,
                createdAt = System.currentTimeMillis()
            )

            // Save to database
            mediaDao.insert(MediaEntity.fromDomain(media))

            Result.success(media)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload media to server
     */
    suspend fun uploadMedia(mediaId: String): Result<String> {
        return try {
            val media = mediaDao.getMedia(mediaId)
                ?: return Result.failure(Exception("Media not found"))

            // Get upload URL from server
            val uploadUrlResponse = api.getUploadUrl(
                entryId = media.entryId,
                mimeType = media.mimeType,
                fileSize = media.fileSize
            )

            if (!uploadUrlResponse.isSuccessful || uploadUrlResponse.body() == null) {
                return Result.failure(Exception("Failed to get upload URL"))
            }

            val uploadData = uploadUrlResponse.body()!!

            // Read encrypted file
            val file = File(media.localPath)
            val requestBody = file.asRequestBody(media.mimeType.toMediaTypeOrNull())
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, requestBody)
                .build()

            // Upload file
            val uploadResponse = api.uploadMedia(uploadData.uploadUrl, multipartBody)

            if (uploadResponse.isSuccessful) {
                // Update media with server URL
                mediaDao.updateServerUrl(
                    mediaId = mediaId,
                    url = uploadData.serverUrl,
                    status = SyncStatus.SYNCED.name
                )
                Result.success(uploadData.serverUrl)
            } else {
                Result.failure(Exception("Upload failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download media from server
     */
    suspend fun downloadMedia(mediaId: String): Result<File> {
        return try {
            val media = mediaDao.getMedia(mediaId)
                ?: return Result.failure(Exception("Media not found"))

            // Check if already downloaded
            val localFile = File(media.localPath)
            if (localFile.exists()) {
                return Result.success(localFile)
            }

            // Download from server
            val response = api.downloadMedia(mediaId)
            if (!response.isSuccessful || response.body() == null) {
                return Result.failure(Exception("Download failed"))
            }

            // Save to local file
            localFile.parentFile?.mkdirs()
            response.body()!!.byteStream().use { input ->
                localFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Result.success(localFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete media
     */
    suspend fun deleteMedia(mediaId: String): Result<Unit> {
        return try {
            val media = mediaDao.getMedia(mediaId)
                ?: return Result.failure(Exception("Media not found"))

            // Delete local file
            val file = File(media.localPath)
            if (file.exists()) {
                file.delete()
            }

            // Delete from server if uploaded
            if (media.serverUrl != null) {
                try {
                    api.deleteMedia(mediaId)
                } catch (e: Exception) {
                    // Continue even if server deletion fails
                }
            }

            // Delete from database
            mediaDao.delete(MediaEntity.fromDomain(media.toDomain()))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get total storage used
     */
    suspend fun getTotalStorageUsed(): Long {
        return mediaDao.getTotalStorageUsed() ?: 0L
    }
}
