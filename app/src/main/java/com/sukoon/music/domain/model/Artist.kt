package com.sukoon.music.domain.model

/**
 * Represents an artist in the music library.
 *
 * Artists are derived by grouping songs by artist name.
 * Contains metadata and references to songs and albums by this artist.
 */
data class Artist(
    /**
     * Unique identifier for the artist.
     * Generated from artist name hash for consistency.
     */
    val id: Long,

    /**
     * Artist name.
     */
    val name: String,

    /**
     * Number of songs by this artist.
     */
    val songCount: Int,

    /**
     * Number of albums by this artist.
     */
    val albumCount: Int,

    /**
     * Total duration of all songs by this artist (milliseconds).
     */
    val totalDuration: Long,

    /**
     * Artist artwork URI (from first song by this artist).
     * Typically uses album art from the first song.
     */
    val artworkUri: String?,

    /**
     * List of song IDs by this artist.
     * Used for detail screen and playback.
     */
    val songIds: List<Long> = emptyList(),

    /**
     * List of album IDs by this artist.
     * Used for displaying artist's discography.
     */
    val albumIds: List<Long> = emptyList()
) {
    /**
     * Format total duration as readable string (e.g., "2:45:32").
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

    /**
     * Format song count as readable string (e.g., "42 songs").
     */
    fun formattedSongCount(): String {
        return if (songCount == 1) "1 song" else "$songCount songs"
    }

    /**
     * Format album count as readable string (e.g., "5 albums").
     */
    fun formattedAlbumCount(): String {
        return if (albumCount == 1) "1 album" else "$albumCount albums"
    }
}
