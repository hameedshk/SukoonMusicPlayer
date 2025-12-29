package com.sukoon.music.domain.model

/**
 * Source of lyrics for tracking where they came from.
 * Used for displaying source indicator in UI and debugging sync issues.
 */
enum class LyricsSource {
    /**
     * Lyrics found in a local .lrc file in the same directory as the audio file.
     * Highest priority - most accurate for local music collections.
     */
    LOCAL_FILE,

    /**
     * Lyrics embedded in the audio file's ID3 tags (USLT/SYLT frames).
     * Second priority - guaranteed to match the exact audio file.
     */
    EMBEDDED,

    /**
     * Lyrics fetched from LRCLIB.net API.
     * Fallback when offline sources are not available.
     */
    ONLINE,

    /**
     * Lyrics manually imported/pasted by the user.
     * Allows custom lyrics or corrections.
     */
    MANUAL,

    /**
     * Unknown or legacy source (for existing cached lyrics).
     */
    UNKNOWN
}
