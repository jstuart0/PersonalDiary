package com.jstuart0.personaldiary.domain.model

/**
 * Search result model
 */
data class SearchResult(
    val entryId: String,
    val title: String? = null,
    val snippet: String,
    val tags: List<String> = emptyList(),
    val createdAt: Long,
    val score: Float = 0f
)
