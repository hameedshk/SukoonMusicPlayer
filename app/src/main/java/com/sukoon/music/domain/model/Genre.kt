package com.sukoon.music.domain.model

import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Represents a genre in the music library.
 *
 * Genres are derived by grouping songs by genre name.
 * Contains metadata and references to songs by this genre.
 */
data class Genre(
    /**
     * Unique identifier for the genre.
     * Generated from genre name hash for consistency.
     */
    val id: Long,

    /**
     * Genre name.
     */
    val name: String,

    /**
     * Number of songs in this genre.
     */
    val songCount: Int,

    /**
     * Total duration of all songs in this genre (milliseconds).
     */
    val totalDuration: Long,

    /**
     * Genre artwork URI (from first song in this genre).
     * Typically uses album art from the first song.
     */
    val artworkUri: String?,

    /**
     * List of song IDs in this genre.
     * Used for detail screen and playback.
     */
    val songIds: List<Long> = emptyList()
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

    companion object {
        /**
         * Generate stable ID from genre name using MD5 hash.
         * Prevents hash collisions and ensures consistency across restarts.
         */
        fun generateId(name: String): Long {
            val md5 = MessageDigest.getInstance("MD5")
            val hash = md5.digest(name.lowercase().toByteArray())
            return ByteBuffer.wrap(hash).getLong()
        }
    }
}
