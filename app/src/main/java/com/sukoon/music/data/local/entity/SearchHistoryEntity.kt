package com.sukoon.music.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing search history.
 * Keeps track of the last 10 search queries with timestamps.
 *
 * Design:
 * - query: The search query text (unique to prevent duplicates)
 * - timestamp: When the search was performed (for sorting and cleanup)
 * - Index on timestamp for efficient ORDER BY queries
 */
@Entity(
    tableName = "search_history",
    indices = [Index(value = ["timestamp"], name = "index_search_history_timestamp")]
)
data class SearchHistoryEntity(
    /**
     * The search query text. Acts as primary key to prevent duplicate queries.
     * When user re-searches same query, we update timestamp instead of creating new row.
     */
    @PrimaryKey
    val query: String,

    /**
     * Timestamp when this query was last searched (milliseconds since epoch).
     * Used for sorting (most recent first) and cleanup (delete oldest).
     */
    val timestamp: Long = System.currentTimeMillis()
)
