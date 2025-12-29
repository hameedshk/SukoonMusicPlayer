package com.sukoon.music.domain.model

/**
 * Represents an album in the music library.
 *
 * Albums are derived by grouping songs by album name.
 * Contains metadata and list of songs belonging to this album.
 */
data class Album(
    /**
     * Unique identifier for the album.
     * Generated from album name hash for consistency.
     */
    val id: Long,

    /**
     * Album title.
     */
    val title: String,

    /**
     * Album artist (first artist from songs in album).
     */
    val artist: String,

    /**
     * Number of songs in this album.
     */
    val songCount: Int,

    /**
     * Total duration of all songs in album (milliseconds).
     */
    val totalDuration: Long,

    /**
     * Album art URI (from first song in album).
     */
    val albumArtUri: String?,

    /**
     * Year of release (if available from MediaStore).
     */
    val year: Int? = null,

    /**
     * List of song IDs in this album.
     * Used for detail screen and playback.
     */
    val songIds: List<Long> = emptyList()
) {
    /**
     * Format total duration as readable string (e.g., "45:32").
     */
    fun formattedDuration(): String {
        val totalSeconds = totalDuration / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}
