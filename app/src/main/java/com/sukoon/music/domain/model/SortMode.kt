package com.sukoon.music.domain.model

/**
 * Enum representing available sort modes for search results.
 */
enum class SortMode {
    /**
     * Sort by search relevance (default).
     * Prioritizes title matches > artist matches > album matches.
     * Exact matches rank higher than partial matches.
     */
    RELEVANCE,

    /**
     * Sort alphabetically by song title (A-Z).
     */
    TITLE,

    /**
     * Sort alphabetically by artist name (A-Z).
     */
    ARTIST,

    /**
     * Sort by date added (newest first).
     * Uses Song.dateAdded field from MediaStore.
     */
    DATE_ADDED
}
