package com.jstuart0.personaldiary.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jstuart0.personaldiary.data.repository.SearchRepository
import com.jstuart0.personaldiary.domain.model.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for search screen
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val allTags: StateFlow<List<String>> = searchRepository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateQuery(query: String) {
        _query.value = query
    }

    fun search() {
        val query = _query.value.trim()
        if (query.isEmpty()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        viewModelScope.launch {
            _uiState.value = SearchUiState.Searching

            val result = searchRepository.search(query)
            _uiState.value = result.fold(
                onSuccess = { results ->
                    if (results.isEmpty()) {
                        SearchUiState.NoResults(query)
                    } else {
                        SearchUiState.Success(results)
                    }
                },
                onFailure = { error ->
                    SearchUiState.Error(error.message ?: "Search failed")
                }
            )
        }
    }

    fun searchByTag(tag: String) {
        _query.value = "#$tag"
        search()
    }

    fun clear() {
        _query.value = ""
        _uiState.value = SearchUiState.Idle
    }
}

/**
 * UI state for search
 */
sealed class SearchUiState {
    object Idle : SearchUiState()
    object Searching : SearchUiState()
    data class Success(val results: List<SearchResult>) : SearchUiState()
    data class NoResults(val query: String) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}
