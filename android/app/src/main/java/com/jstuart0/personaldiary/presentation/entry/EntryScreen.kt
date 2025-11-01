package com.jstuart0.personaldiary.presentation.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Entry screen for creating and editing diary entries
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    viewModel: EntryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onEntrySaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val title by viewModel.title.collectAsState()
    val content by viewModel.content.collectAsState()
    val tags by viewModel.tags.collectAsState()

    var showTagDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is EntryUiState.Saved) {
            onEntrySaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (uiState) {
                            is EntryUiState.Creating -> "New Entry"
                            is EntryUiState.Editing -> "Edit Entry"
                            else -> "Entry"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveEntry() },
                        enabled = content.isNotBlank() && uiState !is EntryUiState.Saving
                    ) {
                        Icon(Icons.Default.Save, "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is EntryUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                    )
                }
            }
            is EntryUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.clearError() }) {
                        Text("Retry")
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title field
                    OutlinedTextField(
                        value = title,
                        onValueChange = { viewModel.updateTitle(it) },
                        label = { Text("Title (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        enabled = state !is EntryUiState.Saving
                    )

                    // Content field
                    OutlinedTextField(
                        value = content,
                        onValueChange = { viewModel.updateContent(it) },
                        label = { Text("How was your day?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Default
                        ),
                        enabled = state !is EntryUiState.Saving,
                        minLines = 10
                    )

                    // Tags section
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tags",
                                style = MaterialTheme.typography.titleSmall
                            )
                            TextButton(onClick = { showTagDialog = true }) {
                                Text("Add Tag")
                            }
                        }

                        if (tags.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                tags.forEach { tag ->
                                    InputChip(
                                        selected = false,
                                        onClick = { },
                                        label = { Text(tag) },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { viewModel.removeTag(tag) },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (state is EntryUiState.Saving) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showTagDialog) {
        AddTagDialog(
            onDismiss = { showTagDialog = false },
            onAddTag = { tag ->
                viewModel.addTag(tag)
                showTagDialog = false
            }
        )
    }
}

@Composable
fun AddTagDialog(
    onDismiss: () -> Unit,
    onAddTag: (String) -> Unit
) {
    var tagText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Tag") },
        text = {
            OutlinedTextField(
                value = tagText,
                onValueChange = { tagText = it },
                label = { Text("Tag name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (tagText.isNotBlank()) {
                        onAddTag(tagText.trim())
                    }
                },
                enabled = tagText.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
