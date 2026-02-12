package com.sukoon.music.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: String,
    val path: String = "",
    val playCount: Int = 0,
    val year: Int = 0,
    val size: Long = 0,
    val albumArtUri: String? = null,
    val dateAdded: Long = 0,
    val isLiked: Boolean = false,
    val genre: String = "Unknown Genre"
) : Parcelable {
    /**
     * Format duration as readable string (e.g., "3:45" or "1:23:45").
     */
    fun durationFormatted(): String {
        val totalSeconds = duration / 1000
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
