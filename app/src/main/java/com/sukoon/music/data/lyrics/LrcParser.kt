package com.sukoon.music.data.lyrics

import android.util.Log
import com.sukoon.music.domain.model.LyricLine

/**
 * Parser for LRC (Lyric) format with timestamps.
 *
 * LRC Format Examples:
 * [00:12.00]Line of lyrics
 * [01:15.30]Another line
 * [mm:ss.xx]Text
 *
 * Supports:
 * - Multiple timestamps per line
 * - Metadata tags ([ar:Artist], [ti:Title], [al:Album])
 * - Offset correction
 */
object LrcParser {

    private val LRC_LINE_REGEX = Regex("""^\[(\d{2}):(\d{2})\.(\d{2})\](.*)$""")
    private val LRC_METADATA_REGEX = Regex("""^\[([a-z]+):(.+)\]$""")

    private const val TAG = "LRCParser"

    /**
     * Parse LRC format lyrics into timestamped lines.
     *
     * @param lrcContent Raw LRC format string
     * @param offsetMs Manual offset correction in milliseconds (±500ms tolerance)
     * @return List of LyricLine sorted by timestamp
     */
    fun parse(lrcContent: String?, offsetMs: Long = 0): List<LyricLine> {
        if (lrcContent.isNullOrBlank()) return emptyList()
        Log.d(TAG, "=== LYRICS FETCH START ===")
        val lines = mutableListOf<LyricLine>()

        lrcContent.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach

            // Skip metadata lines ([ar:], [ti:], [al:], etc.)
            if (LRC_METADATA_REGEX.matches(trimmed)) return@forEach

            // Parse timestamp and text
            val match = LRC_LINE_REGEX.matchEntire(trimmed)
            if (match != null) {
                val (minutes, seconds, centiseconds, text) = match.destructured

                val timestampMs = (minutes.toLong() * 60 * 1000) +
                        (seconds.toLong() * 1000) +
                        (centiseconds.toLong() * 10)

                // Apply offset correction
                val correctedTimestamp = (timestampMs + offsetMs).coerceAtLeast(0)

                lines.add(
                    LyricLine(
                        timestamp = correctedTimestamp,
                        text = text.trim()
                    )
                )
            }
        }

        // Sort by timestamp
        return lines.sortedBy { it.timestamp }
    }

    /**
     * Find the active lyric line for the current playback position.
     * Uses binary search for O(log n) performance instead of O(n) linear scan.
     *
     * @param lines Parsed lyric lines (must be sorted by timestamp)
     * @param currentPositionMs Current playback position in milliseconds
     * @param tolerance Sync tolerance in milliseconds (default: 500ms)
     * @return Index of the current line, or -1 if not found
     */
    fun findActiveLine(
        lines: List<LyricLine>,
        currentPositionMs: Long,
        tolerance: Long = 500
    ): Int {
        if (lines.isEmpty()) return -1

        val targetPosition = currentPositionMs + tolerance

        // Binary search to find the last line whose timestamp <= targetPosition
        var left = 0
        var right = lines.size - 1
        var activeIndex = -1

        while (left <= right) {
            val mid = left + (right - left) / 2

            if (lines[mid].timestamp <= targetPosition) {
                // This line is a candidate, search right half for later lines
                activeIndex = mid
                left = mid + 1
            } else {
                // This line is too late, search left half
                right = mid - 1
            }
        }

        return activeIndex
    }

    /**
     * Check if lyrics are in sync with playback.
     *
     * Returns false if drift exceeds tolerance, indicating need for fallback to plain lyrics.
     */
    fun isInSync(
        lines: List<LyricLine>,
        currentPositionMs: Long,
        duration: Long,
        tolerance: Long = 500
    ): Boolean {
        if (lines.isEmpty()) return false

        // Check if last line timestamp is reasonable compared to track duration
        val lastLine = lines.lastOrNull() ?: return false
        val drift = kotlin.math.abs(lastLine.timestamp - duration)

        return drift <= tolerance
    }
}
