package com.sukoon.music.domain.model

/**
 * Represents a folder in the music library.
 *
 * Folders are derived by grouping songs by their file system directory path.
 * Contains metadata and list of songs belonging to this folder.
 */
data class Folder(
    /**
     * Unique identifier for the folder.
     * Generated from folder path hash for consistency.
     */
    val id: Long,

    /**
     * Full file system path to the folder.
     * Example: /storage/emulated/0/Music/Rock
     */
    val path: String,

    /**
     * Folder name (last segment of path).
     * Example: "Rock" from "/storage/emulated/0/Music/Rock"
     */
    val name: String,

    /**
     * Number of songs in this folder.
     */
    val songCount: Int,

    /**
     * Total duration of all songs in folder (milliseconds).
     */
    val totalDuration: Long,

    /**
     * Album art URI (from first song in folder).
     */
    val albumArtUri: String?,

    /**
     * List of song IDs in this folder.
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
