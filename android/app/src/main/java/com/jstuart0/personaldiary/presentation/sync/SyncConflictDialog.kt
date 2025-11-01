package com.jstuart0.personaldiary.presentation.sync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jstuart0.personaldiary.domain.model.Entry
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for resolving sync conflicts between local and server versions
 */
@Composable
fun SyncConflictDialog(
    localEntry: Entry,
    serverEntry: Entry,
    onKeepLocal: () -> Unit,
    onKeepServer: () -> Unit,
    onMerge: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Warning, contentDescription = null)
        },
        title = {
            Text("Sync Conflict")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "This entry has been modified both locally and on the server. Which version would you like to keep?",
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider()

                // Local version
                ConflictVersionCard(
                    title = "Local Version",
                    entry = localEntry,
                    icon = Icons.Default.PhoneAndroid
                )

                HorizontalDivider()

                // Server version
                ConflictVersionCard(
                    title = "Server Version",
                    entry = serverEntry,
                    icon = Icons.Default.Cloud
                )
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onKeepLocal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Keep Local")
                }

                Button(
                    onClick = onKeepServer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Keep Server")
                }

                OutlinedButton(
                    onClick = onMerge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.MergeType, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Merge Both")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ConflictVersionCard(
    title: String,
    entry: Entry,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (entry.title != null) {
                Text(
                    text = "Title: ${entry.title}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = entry.content.take(200) + if (entry.content.length > 200) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Modified: ${formatTimestamp(entry.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (entry.tags.isNotEmpty()) {
                    Text(
                        text = "${entry.tags.size} tags",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Sync status indicator
 */
@Composable
fun SyncStatusIndicator(
    isSyncing: Boolean,
    lastSyncTime: Long?,
    onSyncNow: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSyncing) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (isSyncing) Icons.Default.Sync else Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = if (isSyncing) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = if (isSyncing) "Syncing..." else "Synced",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (lastSyncTime != null && !isSyncing) {
                    Text(
                        text = "Last synced: ${formatTimestamp(lastSyncTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isSyncing) {
                FilledTonalButton(onClick = onSyncNow) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sync Now")
                }
            }
        }
    }
}

/**
 * Pending changes indicator
 */
@Composable
fun PendingChangesIndicator(
    pendingCount: Int,
    onViewPending: () -> Unit
) {
    if (pendingCount > 0) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "$pendingCount pending ${if (pendingCount == 1) "change" else "changes"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onViewPending) {
                    Text("View")
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} minutes ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        diff < 604800_000 -> "${diff / 86400_000} days ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
