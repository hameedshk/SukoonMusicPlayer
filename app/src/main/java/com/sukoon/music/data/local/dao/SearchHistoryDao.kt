package com.sukoon.music.data.local.dao

import androidx.room.*
import com.sukoon.music.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing search history.
 * Manages the last 10 search queries with automatic cleanup.
 */
@Dao
interface SearchHistoryDao {

    /**
     * Get all search history ordered by most recent first.
     * Returns Flow for reactive UI updates.
     * Limited to 10 most recent queries.
     */
    @Query("""
        SELECT * FROM search_history
        ORDER BY timestamp DESC
        LIMIT 10
    """)
    fun getAllHistory(): Flow<List<SearchHistoryEntity>>

    /**
     * Insert or update a search query.
     * If query already exists, updates timestamp (due to @PrimaryKey on query).
     * After insert, triggers cleanup to maintain max 10 items.
     *
     * @param searchHistory The search query to save
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(searchHistory: SearchHistoryEntity)

    /**
     * Delete a specific search query from history.
     * Used when user taps "X" on individual history chip.
     *
     * @param query The search query to remove
     */
    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearch(query: String)

    /**
     * Clear all search history.
     * Used when user taps "Clear all history" button.
     */
    @Query("DELETE FROM search_history")
    suspend fun clearAll()

    /**
     * Delete oldest search queries if count exceeds 10.
     * This is called after each insert to maintain the limit.
     * Keeps only the 10 most recent queries.
     */
    @Query("""
        DELETE FROM search_history
        WHERE query NOT IN (
            SELECT query FROM search_history
            ORDER BY timestamp DESC
            LIMIT 10
        )
    """)
    suspend fun deleteOldest()

    /**
     * Get current count of search history items.
     * Used for UI display ("10 recent searches") and testing.
     */
    @Query("SELECT COUNT(*) FROM search_history")
    suspend fun getCount(): Int
}
