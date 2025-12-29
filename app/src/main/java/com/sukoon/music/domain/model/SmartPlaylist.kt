package com.sukoon.music.domain.model

/**
 * Represents smart playlists that are automatically generated based on user behavior.
 * These are not stored in the database but computed dynamically.
 */
enum class SmartPlaylistType {
    MY_FAVOURITE,      // Liked/favorited songs
    LAST_ADDED,        // Recently added songs
    RECENTLY_PLAYED,   // Songs played recently
    MOST_PLAYED;       // Songs with highest play count

    companion object {
        // Negative IDs to distinguish from real playlist IDs
        const val MY_FAVOURITE_ID = -1L
        const val LAST_ADDED_ID = -2L
        const val RECENTLY_PLAYED_ID = -3L
        const val MOST_PLAYED_ID = -4L

        fun fromId(id: Long): SmartPlaylistType? = when (id) {
            MY_FAVOURITE_ID -> MY_FAVOURITE
            LAST_ADDED_ID -> LAST_ADDED
            RECENTLY_PLAYED_ID -> RECENTLY_PLAYED
            MOST_PLAYED_ID -> MOST_PLAYED
            else -> null
        }

        fun toId(type: SmartPlaylistType): Long = when (type) {
            MY_FAVOURITE -> MY_FAVOURITE_ID
            LAST_ADDED -> LAST_ADDED_ID
            RECENTLY_PLAYED -> RECENTLY_PLAYED_ID
            MOST_PLAYED -> MOST_PLAYED_ID
        }
    }
}

/**
 * Data class representing a smart playlist with its metadata.
 */
data class SmartPlaylist(
    val type: SmartPlaylistType,
    val title: String,
    val songCount: Int,
    val iconRes: Int? = null  // Optional icon resource ID
) {
    val id: Long
        get() = SmartPlaylistType.toId(type)

    companion object {
        fun getDisplayName(type: SmartPlaylistType): String = when (type) {
            SmartPlaylistType.MY_FAVOURITE -> "My favourite"
            SmartPlaylistType.LAST_ADDED -> "Last added"
            SmartPlaylistType.RECENTLY_PLAYED -> "Recently played"
            SmartPlaylistType.MOST_PLAYED -> "Most played"
        }
    }
}
