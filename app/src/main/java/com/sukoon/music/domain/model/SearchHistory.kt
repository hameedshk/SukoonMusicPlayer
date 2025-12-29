package com.sukoon.music.domain.model

/**
 * Domain model representing a search history entry.
 * Separated from SearchHistoryEntity (data layer) following Clean Architecture.
 *
 * @property query The search query text
 * @property timestamp When the search was performed (milliseconds since epoch)
 */
data class SearchHistory(
    val query: String,
    val timestamp: Long
)
