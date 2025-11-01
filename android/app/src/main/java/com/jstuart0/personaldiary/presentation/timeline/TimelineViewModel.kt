package com.jstuart0.personaldiary.presentation.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jstuart0.personaldiary.data.repository.AuthRepository
import com.jstuart0.personaldiary.data.repository.EntryRepository
import com.jstuart0.personaldiary.domain.model.Entry
import com.jstuart0.personaldiary.domain.model.EntrySource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for timeline screen
 * Displays list of diary entries
 */
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val entryRepository: EntryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TimelineUiState>(TimelineUiState.Loading)
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    private val _selectedSource = MutableStateFlow<EntrySource?>(null)
    val selectedSource: StateFlow<EntrySource?> = _selectedSource.asStateFlow()

    init {
        loadEntries()
    }

    private fun loadEntries() {
        viewModelScope.launch {
            authRepository.getCurrentUserFlow()
                .filterNotNull()
                .flatMapLatest { user ->
                    val source = _selectedSource.value
                    if (source != null) {
                        entryRepository.getEntriesBySource(user.userId, source)
                    } else {
                        entryRepository.getEntriesFlow(user.userId)
                    }
                }
                .catch { error ->
                    _uiState.value = TimelineUiState.Error(error.message ?: "Failed to load entries")
                }
                .collect { entries ->
                    _uiState.value = if (entries.isEmpty()) {
                        TimelineUiState.Empty
                    } else {
                        TimelineUiState.Success(entries)
                    }
                }
        }
    }

    fun filterBySource(source: EntrySource?) {
        _selectedSource.value = source
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            entryRepository.deleteEntry(entryId)
        }
    }

    fun syncEntries() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch

            _uiState.value = TimelineUiState.Syncing((_uiState.value as? TimelineUiState.Success)?.entries ?: emptyList())

            val result = entryRepository.syncEntries(user.userId, null)
            result.onFailure { error ->
                _uiState.value = TimelineUiState.Error(error.message ?: "Sync failed")
            }
            // Success is handled by the Flow observer
        }
    }

    fun retry() {
        loadEntries()
    }
}

/**
 * UI state for timeline
 */
sealed class TimelineUiState {
    object Loading : TimelineUiState()
    object Empty : TimelineUiState()
    data class Success(val entries: List<Entry>) : TimelineUiState()
    data class Syncing(val entries: List<Entry>) : TimelineUiState()
    data class Error(val message: String) : TimelineUiState()
}
