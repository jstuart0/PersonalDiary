package com.jstuart0.personaldiary.presentation.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jstuart0.personaldiary.data.repository.AuthRepository
import com.jstuart0.personaldiary.data.repository.EntryRepository
import com.jstuart0.personaldiary.data.repository.MediaRepository
import com.jstuart0.personaldiary.domain.model.Entry
import com.jstuart0.personaldiary.domain.model.Media
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for entry create/edit screen
 */
@HiltViewModel
class EntryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val entryRepository: EntryRepository,
    private val mediaRepository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val entryId: String? = savedStateHandle["entryId"]

    private val _uiState = MutableStateFlow<EntryUiState>(EntryUiState.Loading)
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _media = MutableStateFlow<List<Media>>(emptyList())
    val media: StateFlow<List<Media>> = _media.asStateFlow()

    init {
        loadEntry()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            if (entryId != null) {
                // Load existing entry
                val entry = entryRepository.getEntry(entryId)
                if (entry != null) {
                    _title.value = entry.title ?: ""
                    _content.value = entry.content
                    _tags.value = entry.tags

                    // Load media
                    mediaRepository.getMediaForEntry(entryId)
                        .collect { mediaList ->
                            _media.value = mediaList
                        }

                    _uiState.value = EntryUiState.Editing(entry)
                } else {
                    _uiState.value = EntryUiState.Error("Entry not found")
                }
            } else {
                // New entry
                _uiState.value = EntryUiState.Creating
            }
        }
    }

    fun updateTitle(title: String) {
        _title.value = title
    }

    fun updateContent(content: String) {
        _content.value = content
    }

    fun addTag(tag: String) {
        if (tag.isNotBlank() && !_tags.value.contains(tag)) {
            _tags.value = _tags.value + tag
        }
    }

    fun removeTag(tag: String) {
        _tags.value = _tags.value.filter { it != tag }
    }

    fun addMedia(file: File, mimeType: String, width: Int? = null, height: Int? = null) {
        viewModelScope.launch {
            val currentEntry = (uiState.value as? EntryUiState.Editing)?.entry
            val targetEntryId = currentEntry?.entryId ?: return@launch

            val result = mediaRepository.saveMedia(
                entryId = targetEntryId,
                sourceFile = file,
                mimeType = mimeType,
                width = width,
                height = height
            )

            result.onSuccess { newMedia ->
                _media.value = _media.value + newMedia
            }
        }
    }

    fun removeMedia(mediaId: String) {
        viewModelScope.launch {
            mediaRepository.deleteMedia(mediaId)
            _media.value = _media.value.filter { it.mediaId != mediaId }
        }
    }

    fun saveEntry() {
        viewModelScope.launch {
            _uiState.value = EntryUiState.Saving

            val user = authRepository.getCurrentUser()
            if (user == null) {
                _uiState.value = EntryUiState.Error("User not logged in")
                return@launch
            }

            val titleValue = _title.value.takeIf { it.isNotBlank() }
            val contentValue = _content.value
            val tagsValue = _tags.value
            val mediaIds = _media.value.map { it.mediaId }

            val result = if (entryId != null) {
                // Update existing
                entryRepository.updateEntry(entryId, titleValue, contentValue, tagsValue, mediaIds)
            } else {
                // Create new
                entryRepository.createEntry(user.userId, titleValue, contentValue, tagsValue, mediaIds)
            }

            _uiState.value = result.fold(
                onSuccess = { entry -> EntryUiState.Saved(entry) },
                onFailure = { error -> EntryUiState.Error(error.message ?: "Failed to save entry") }
            )
        }
    }

    fun clearError() {
        if (_uiState.value is EntryUiState.Error) {
            if (entryId != null) {
                loadEntry()
            } else {
                _uiState.value = EntryUiState.Creating
            }
        }
    }
}

/**
 * UI state for entry screen
 */
sealed class EntryUiState {
    object Loading : EntryUiState()
    object Creating : EntryUiState()
    data class Editing(val entry: Entry) : EntryUiState()
    object Saving : EntryUiState()
    data class Saved(val entry: Entry) : EntryUiState()
    data class Error(val message: String) : EntryUiState()
}
