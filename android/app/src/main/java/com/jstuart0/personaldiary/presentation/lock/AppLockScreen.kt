package com.jstuart0.personaldiary.presentation.lock

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.jstuart0.personaldiary.service.BiometricAuthManager
import com.jstuart0.personaldiary.service.BiometricAvailability

/**
 * App lock screen with biometric authentication
 */
@Composable
fun AppLockScreen(
    biometricManager: BiometricAuthManager,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }

    val biometricAvailability = remember {
        biometricManager.isBiometricAvailable()
    }

    LaunchedEffect(Unit) {
        if (biometricAvailability.isAvailable() && activity != null) {
            isAuthenticating = true
            biometricManager.showBiometricPrompt(
                activity = activity,
                title = "Unlock Personal Diary",
                subtitle = "Verify your identity to access your entries",
                onSuccess = {
                    isAuthenticating = false
                    onUnlocked()
                },
                onError = { error ->
                    isAuthenticating = false
                    errorMessage = error
                },
                onFailed = {
                    isAuthenticating = false
                    errorMessage = "Authentication failed"
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Lock icon
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(120.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Title
            Text(
                text = "Personal Diary Locked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // Subtitle
            Text(
                text = if (biometricAvailability.isAvailable()) {
                    "Authenticate to access your private entries"
                } else {
                    biometricAvailability.getMessage()
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Error message
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Retry button
            if (biometricAvailability.isAvailable() && activity != null) {
                Button(
                    onClick = {
                        isAuthenticating = true
                        errorMessage = null
                        biometricManager.showBiometricPrompt(
                            activity = activity,
                            title = "Unlock Personal Diary",
                            subtitle = "Verify your identity to access your entries",
                            onSuccess = {
                                isAuthenticating = false
                                onUnlocked()
                            },
                            onError = { error ->
                                isAuthenticating = false
                                errorMessage = error
                            },
                            onFailed = {
                                isAuthenticating = false
                                errorMessage = "Authentication failed"
                            }
                        )
                    },
                    enabled = !isAuthenticating
                ) {
                    if (isAuthenticating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isAuthenticating) "Authenticating..." else "Unlock")
                }
            } else if (!biometricAvailability.isAvailable()) {
                // Show error state
                OutlinedButton(
                    onClick = { /* Navigate to settings */ }
                ) {
                    Text("Open Device Settings")
                }
            }
        }
    }
}

/**
 * App lock wrapper that conditionally shows lock screen
 */
@Composable
fun AppLockWrapper(
    biometricManager: BiometricAuthManager,
    shouldLock: Boolean,
    onUnlocked: () -> Unit,
    content: @Composable () -> Unit
) {
    var isLocked by remember { mutableStateOf(shouldLock) }

    if (isLocked) {
        AppLockScreen(
            biometricManager = biometricManager,
            onUnlocked = {
                isLocked = false
                onUnlocked()
            }
        )
    } else {
        content()
    }
}
