package com.sukoon.music.domain.model

data class Queue(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isCurrent: Boolean = false,
    val songCount: Int = 0,
    val totalDuration: Long = 0
) {
    /**
     * Format total duration as readable string (e.g., "3:45" or "1:23:45").
     */
    fun durationFormatted(): String {
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
