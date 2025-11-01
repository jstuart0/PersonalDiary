package com.jstuart0.personaldiary.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jstuart0.personaldiary.BuildConfig
import com.jstuart0.personaldiary.domain.model.SocialProvider

/**
 * Settings screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val socialAccounts by viewModel.socialAccounts.collectAsStateWithLifecycle()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val lockTimeout by viewModel.lockTimeout.collectAsStateWithLifecycle()
    val isAutoLockEnabled by viewModel.isAutoLockEnabled.collectAsStateWithLifecycle()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLockTimeoutDialog by remember { mutableStateOf(false) }

    // Handle logout
    LaunchedEffect(uiState) {
        if (uiState is SettingsUiState.LoggedOut) {
            onNavigateToLogin()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Account Section
            SettingsSection(title = "Account") {
                if (currentUser != null) {
                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = "Email",
                        subtitle = currentUser!!.email
                    )

                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "Encryption Tier",
                        subtitle = currentUser!!.encryptionTier.name
                    )
                }

                SettingsItem(
                    icon = Icons.Default.Logout,
                    title = "Logout",
                    subtitle = "Sign out of your account",
                    onClick = { showLogoutDialog = true }
                )
            }

            HorizontalDivider()

            // Security Section
            SettingsSection(title = "Security") {
                SettingsSwitchItem(
                    icon = Icons.Default.Lock,
                    title = "App Lock",
                    subtitle = "Require authentication to open app",
                    checked = isAppLockEnabled,
                    onCheckedChange = { viewModel.setAppLockEnabled(it) }
                )

                if (isAppLockEnabled) {
                    SettingsSwitchItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Authentication",
                        subtitle = if (viewModel.biometricAvailability.isAvailable()) {
                            "Use fingerprint or face to unlock"
                        } else {
                            viewModel.biometricAvailability.getMessage()
                        },
                        checked = isBiometricEnabled,
                        enabled = viewModel.biometricAvailability.isAvailable(),
                        onCheckedChange = { viewModel.setBiometricEnabled(it) }
                    )

                    SettingsItem(
                        icon = Icons.Default.Timer,
                        title = "Auto-lock timeout",
                        subtitle = viewModel.getLockTimeoutDisplayName(lockTimeout),
                        onClick = { showLockTimeoutDialog = true }
                    )

                    SettingsSwitchItem(
                        icon = Icons.Default.LockClock,
                        title = "Auto-lock",
                        subtitle = "Automatically lock app after timeout",
                        checked = isAutoLockEnabled,
                        onCheckedChange = { viewModel.setAutoLockEnabled(it) }
                    )
                }
            }

            HorizontalDivider()

            // Social Accounts Section
            SettingsSection(title = "Social Accounts") {
                if (socialAccounts.isEmpty()) {
                    Text(
                        text = "No connected accounts",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    socialAccounts.forEach { account ->
                        SocialAccountItem(
                            provider = account.provider,
                            displayName = account.displayName ?: account.email ?: "Unknown",
                            onDisconnect = { viewModel.disconnectSocialAccount(account.accountId) }
                        )
                    }
                }

                // TODO: Add connect Facebook button
            }

            HorizontalDivider()

            // About Section
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = BuildConfig.VERSION_NAME
                )

                SettingsItem(
                    icon = Icons.Default.Description,
                    title = "Privacy Policy",
                    onClick = { /* Navigate to privacy policy */ }
                )

                SettingsItem(
                    icon = Icons.Default.Gavel,
                    title = "Terms of Service",
                    onClick = { /* Navigate to terms */ }
                )

                SettingsItem(
                    icon = Icons.Default.Code,
                    title = "Open Source Licenses",
                    onClick = { /* Navigate to licenses */ }
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        // Logout confirmation dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                title = { Text("Logout") },
                text = { Text("Are you sure you want to logout?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            viewModel.logout()
                        }
                    ) {
                        Text("Logout")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Lock timeout selection dialog
        if (showLockTimeoutDialog) {
            AlertDialog(
                onDismissRequest = { showLockTimeoutDialog = false },
                title = { Text("Auto-lock timeout") },
                text = {
                    Column {
                        viewModel.getLockTimeoutOptions().forEach { (timeout, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setLockTimeout(timeout)
                                        showLockTimeoutDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = lockTimeout == timeout,
                                    onClick = {
                                        viewModel.setLockTimeout(timeout)
                                        showLockTimeoutDialog = false
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(name)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLockTimeoutDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Error snackbar
        if (uiState is SettingsUiState.Error) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text((uiState as SettingsUiState.Error).message)
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (onClick != null) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            }
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    }
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun SocialAccountItem(
    provider: SocialProvider,
    displayName: String,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            when (provider) {
                SocialProvider.FACEBOOK -> Icons.Default.Facebook
                else -> Icons.Default.Link
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = provider.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onDisconnect) {
            Text("Disconnect")
        }
    }
}
