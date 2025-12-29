package com.sukoon.music.domain.repository

import com.sukoon.music.domain.model.SearchHistory
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for search history operations.
 * Follows the architecture pattern established by SongRepository.
 */
interface SearchHistoryRepository {

    /**
     * Observe all search history ordered by most recent first.
     * Limited to 10 most recent queries.
     *
     * @return Flow that emits updated history when changes occur
     */
    fun getSearchHistory(): Flow<List<SearchHistory>>

    /**
     * Save a search query to history.
     * If query already exists, updates timestamp.
     * Automatically maintains 10-item limit by deleting oldest.
     *
     * @param query The search query to save (trimmed, non-blank)
     */
    suspend fun saveSearch(query: String)

    /**
     * Delete a specific search query from history.
     *
     * @param query The search query to remove
     */
    suspend fun deleteSearch(query: String)

    /**
     * Clear all search history.
     */
    suspend fun clearAllHistory()
}
