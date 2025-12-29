package com.sukoon.music.domain.model

/**
 * Domain model for song lyrics.
 *
 * Supports both synced (timestamped) and plain lyrics from multiple sources.
 */
data class Lyrics(
    val trackId: Long,
    val syncedLyrics: String?,  // LRC format with timestamps
    val plainLyrics: String?,   // Plain text without timestamps
    val syncOffset: Long = 0L,  // Manual offset correction in milliseconds (±500ms tolerance)
    val source: LyricsSource = LyricsSource.UNKNOWN,  // Where the lyrics came from
    val lastFetched: Long = System.currentTimeMillis()
)

/**
 * Parsed lyric line with timestamp for synced playback.
 */
data class LyricLine(
    val timestamp: Long,  // Milliseconds from start
    val text: String
)

/**
 * State for lyrics fetching and display.
 */
sealed class LyricsState {
    data object Loading : LyricsState()
    data class Success(
        val lyrics: Lyrics,
        val parsedLines: List<LyricLine>
    ) : LyricsState()
    data class Error(val message: String) : LyricsState()
    data object NotFound : LyricsState()
}
