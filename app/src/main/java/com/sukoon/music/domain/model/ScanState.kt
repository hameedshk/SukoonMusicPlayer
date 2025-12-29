package com.sukoon.music.domain.model

/**
 * Represents the current state of MediaStore scanning.
 * This is exposed via StateFlow to allow UI to observe scanning progress.
 */
sealed class ScanState {
    /**
     * No scanning operation in progress.
     */
    data object Idle : ScanState()

    /**
     * Scanning is currently in progress.
     * @param scannedCount Number of songs scanned so far
     * @param message Optional progress message (e.g., "Scanning: Artist - Title")
     */
    data class Scanning(
        val scannedCount: Int = 0,
        val message: String? = null
    ) : ScanState()

    /**
     * Scanning completed successfully.
     * @param totalSongs Total number of songs found and indexed
     * @param message Success message
     */
    data class Success(
        val totalSongs: Int,
        val message: String = "Found $totalSongs songs"
    ) : ScanState()

    /**
     * Scanning failed due to an error.
     * @param error Error message describing what went wrong
     */
    data class Error(
        val error: String
    ) : ScanState()
}
