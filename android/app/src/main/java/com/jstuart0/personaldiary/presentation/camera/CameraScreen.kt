package com.jstuart0.personaldiary.presentation.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Camera capture screen with photo and video support
 */
@Composable
fun CameraScreen(
    entryId: String? = null,
    onMediaCaptured: (List<CapturedMedia>) -> Unit,
    onClose: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraSelector by viewModel.cameraSelector.collectAsStateWithLifecycle()
    val flashMode by viewModel.flashMode.collectAsStateWithLifecycle()
    val capturedMedia by viewModel.capturedMedia.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: false
    }

    // Request permissions if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    if (!hasCameraPermission || !hasAudioPermission) {
        PermissionDeniedScreen(
            onRequestPermissions = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            },
            onClose = onClose
        )
        return
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0L) }

    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = Executors.newSingleThreadExecutor()

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setFlashMode(flashMode)
                        .build()

                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build()

                    videoCapture = VideoCapture.withOutput(recorder)

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture,
                            videoCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, executor)

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onClose,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Flash mode toggle
                IconButton(
                    onClick = { viewModel.toggleFlashMode() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        when (flashMode) {
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                            ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                            else -> Icons.Default.FlashOff
                        },
                        contentDescription = "Flash mode"
                    )
                }

                // Camera flip
                IconButton(
                    onClick = { viewModel.toggleCamera() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Flip camera")
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recording duration
            if (isRecording) {
                Surface(
                    color = Color.Red.copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = formatDuration(recordingDuration),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Capture buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo capture button
                FloatingActionButton(
                    onClick = {
                        val photoFile = viewModel.createPhotoFile()
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        imageCapture?.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    viewModel.onPhotoCaptured(photoFile, entryId)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    viewModel.onPhotoCaptureError(exception)
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .border(3.dp, Color.White, CircleShape),
                    containerColor = Color.White.copy(alpha = 0.9f)
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Take photo",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Video capture button
                FloatingActionButton(
                    onClick = {
                        if (!isRecording) {
                            // Start recording
                            val videoFile = viewModel.createVideoFile()
                            val outputOptions = FileOutputOptions.Builder(videoFile).build()

                            recording = videoCapture?.output
                                ?.prepareRecording(context, outputOptions)
                                ?.apply {
                                    if (hasAudioPermission) {
                                        withAudioEnabled()
                                    }
                                }
                                ?.start(ContextCompat.getMainExecutor(context)) { event ->
                                    when (event) {
                                        is VideoRecordEvent.Start -> {
                                            isRecording = true
                                            scope.launch {
                                                while (isRecording) {
                                                    delay(1000)
                                                    recordingDuration++
                                                }
                                            }
                                        }
                                        is VideoRecordEvent.Finalize -> {
                                            isRecording = false
                                            if (!event.hasError()) {
                                                viewModel.onVideoCaptured(
                                                    videoFile,
                                                    recordingDuration * 1000,
                                                    entryId
                                                )
                                            }
                                            recordingDuration = 0
                                        }
                                    }
                                }
                        } else {
                            // Stop recording
                            recording?.stop()
                            recording = null
                        }
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .border(3.dp, if (isRecording) Color.Red else Color.White, CircleShape),
                    containerColor = if (isRecording) Color.Red.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f)
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                        contentDescription = if (isRecording) "Stop recording" else "Start recording",
                        tint = if (isRecording) Color.White else Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Done button (if media captured)
            if (capturedMedia.isNotEmpty()) {
                Button(
                    onClick = {
                        onMediaCaptured(capturedMedia)
                        viewModel.clearCapturedMedia()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Done (${capturedMedia.size})")
                }
            }
        }

        // Error snackbar
        if (uiState is CameraUiState.Error) {
            Snackbar(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                action = {
                    TextButton(onClick = { viewModel.resetState() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text((uiState as CameraUiState.Error).message)
            }
        }
    }
}

@Composable
fun PermissionDeniedScreen(
    onRequestPermissions: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "To capture photos and videos, please grant camera and microphone permissions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        Button(onClick = onRequestPermissions) {
            Text("Grant Permissions")
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onClose) {
            Text("Cancel")
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}
