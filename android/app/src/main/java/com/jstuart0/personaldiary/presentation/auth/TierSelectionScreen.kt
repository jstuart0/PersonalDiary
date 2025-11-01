package com.jstuart0.personaldiary.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jstuart0.personaldiary.domain.model.EncryptionTier

/**
 * Screen for selecting encryption tier (E2E or UCE)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TierSelectionScreen(
    onTierSelected: (EncryptionTier) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Security Level") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "How do you want to secure your diary?",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Text(
                text = "This choice affects encryption and search capabilities",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // E2E Encryption Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onTierSelected(EncryptionTier.E2E) }
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Maximum Security (E2E)",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Recommended for privacy",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = "End-to-End Encryption",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "✓ Your keys are stored only on your device\n" +
                                "✓ Server cannot read your entries\n" +
                                "✓ Maximum privacy protection\n" +
                                "✓ Local search on device\n" +
                                "⚠ Requires recovery codes backup",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // UCE Encryption Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onTierSelected(EncryptionTier.UCE) }
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column {
                            Text(
                                text = "Balanced Security (UCE)",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Easy to use",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Text(
                        text = "User-Controlled Encryption",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "✓ Password-based encryption\n" +
                                "✓ Server-side search available\n" +
                                "✓ Easier password recovery\n" +
                                "✓ Still highly secure\n" +
                                "⚠ Server can potentially decrypt",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "You cannot change this choice after signup",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}
