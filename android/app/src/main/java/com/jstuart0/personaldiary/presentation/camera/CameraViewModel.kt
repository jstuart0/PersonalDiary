package com.jstuart0.personaldiary.presentation.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jstuart0.personaldiary.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for camera capture functionality
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Ready)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector: StateFlow<CameraSelector> = _cameraSelector.asStateFlow()

    private val _capturedMedia = MutableStateFlow<List<CapturedMedia>>(emptyList())
    val capturedMedia: StateFlow<List<CapturedMedia>> = _capturedMedia.asStateFlow()

    private val _flashMode = MutableStateFlow(ImageCapture.FLASH_MODE_AUTO)
    val flashMode: StateFlow<Int> = _flashMode.asStateFlow()

    /**
     * Toggle between front and back camera
     */
    fun toggleCamera() {
        _cameraSelector.value = if (_cameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    /**
     * Cycle through flash modes
     */
    fun toggleFlashMode() {
        _flashMode.value = when (_flashMode.value) {
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }
    }

    /**
     * Create output file for photo
     */
    fun createPhotoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val fileName = "PHOTO_${timestamp}.jpg"
        val outputDirectory = getOutputDirectory()
        return File(outputDirectory, fileName)
    }

    /**
     * Create output file for video
     */
    fun createVideoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val fileName = "VIDEO_${timestamp}.mp4"
        val outputDirectory = getOutputDirectory()
        return File(outputDirectory, fileName)
    }

    /**
     * Get output directory for media files
     */
    private fun getOutputDirectory(): File {
        val mediaDir = context.externalCacheDirs.firstOrNull()?.let {
            File(it, "PersonalDiary").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
    }

    /**
     * Handle photo captured
     */
    fun onPhotoCaptured(file: File, entryId: String? = null) {
        viewModelScope.launch {
            _uiState.value = CameraUiState.Processing

            try {
                // Get image dimensions
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
                val width = options.outWidth
                val height = options.outHeight

                val capturedMedia = CapturedMedia(
                    file = file,
                    mimeType = "image/jpeg",
                    width = width,
                    height = height,
                    duration = null
                )

                _capturedMedia.value = _capturedMedia.value + capturedMedia

                // If entryId is provided, save immediately
                if (entryId != null) {
                    val result = mediaRepository.saveMedia(
                        entryId = entryId,
                        sourceFile = file,
                        mimeType = "image/jpeg",
                        width = width,
                        height = height
                    )

                    result.onFailure { error ->
                        _uiState.value = CameraUiState.Error(error.message ?: "Failed to save photo")
                        return@launch
                    }
                }

                _uiState.value = CameraUiState.Captured(capturedMedia)
            } catch (e: Exception) {
                _uiState.value = CameraUiState.Error(e.message ?: "Failed to process photo")
            }
        }
    }

    /**
     * Handle photo capture error
     */
    fun onPhotoCaptureError(exception: ImageCaptureException) {
        _uiState.value = CameraUiState.Error(
            "Photo capture failed: ${exception.message}"
        )
    }

    /**
     * Handle video captured
     */
    fun onVideoCaptured(file: File, duration: Long, entryId: String? = null) {
        viewModelScope.launch {
            _uiState.value = CameraUiState.Processing

            try {
                // Get video dimensions using MediaMetadataRetriever
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)

                val width = retriever.extractMetadata(
                    android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                )?.toIntOrNull()

                val height = retriever.extractMetadata(
                    android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                )?.toIntOrNull()

                retriever.release()

                val capturedMedia = CapturedMedia(
                    file = file,
                    mimeType = "video/mp4",
                    width = width,
                    height = height,
                    duration = duration.toInt()
                )

                _capturedMedia.value = _capturedMedia.value + capturedMedia

                // If entryId is provided, save immediately
                if (entryId != null) {
                    val result = mediaRepository.saveMedia(
                        entryId = entryId,
                        sourceFile = file,
                        mimeType = "video/mp4",
                        width = width,
                        height = height,
                        duration = duration.toInt()
                    )

                    result.onFailure { error ->
                        _uiState.value = CameraUiState.Error(error.message ?: "Failed to save video")
                        return@launch
                    }
                }

                _uiState.value = CameraUiState.Captured(capturedMedia)
            } catch (e: Exception) {
                _uiState.value = CameraUiState.Error(e.message ?: "Failed to process video")
            }
        }
    }

    /**
     * Remove captured media
     */
    fun removeCapturedMedia(media: CapturedMedia) {
        media.file.delete()
        _capturedMedia.value = _capturedMedia.value.filter { it != media }
        _uiState.value = CameraUiState.Ready
    }

    /**
     * Clear all captured media
     */
    fun clearCapturedMedia() {
        _capturedMedia.value.forEach { it.file.delete() }
        _capturedMedia.value = emptyList()
        _uiState.value = CameraUiState.Ready
    }

    /**
     * Reset to ready state
     */
    fun resetState() {
        _uiState.value = CameraUiState.Ready
    }
}

/**
 * UI state for camera screen
 */
sealed class CameraUiState {
    object Ready : CameraUiState()
    object Processing : CameraUiState()
    data class Captured(val media: CapturedMedia) : CameraUiState()
    data class Error(val message: String) : CameraUiState()
}

/**
 * Captured media data
 */
data class CapturedMedia(
    val file: File,
    val mimeType: String,
    val width: Int?,
    val height: Int?,
    val duration: Int?
)
