package com.sukoon.music.domain.model

/**
 * Immutable session state for private playback.
 *
 * @param isActive Whether a private session is currently active
 * @param startedAtMs Timestamp when session started (0 if not active)
 * @param expiryTimeMs Auto-expiry time in milliseconds since session start (300_000 = 5 min)
 */
data class PlaybackSessionState(
    val isActive: Boolean = false,
    val startedAtMs: Long = 0,
    val expiryTimeMs: Long = 300_000 // 5 minutes default
) {
    /** Check if session has expired due to inactivity. */
    fun hasExpired(): Boolean {
        if (!isActive) return false
        val elapsedMs = System.currentTimeMillis() - startedAtMs
        return elapsedMs >= expiryTimeMs
    }

    /** Get remaining time before auto-expiry in milliseconds. */
    fun getTimeRemainingMs(): Long {
        if (!isActive) return 0
        val elapsedMs = System.currentTimeMillis() - startedAtMs
        return (expiryTimeMs - elapsedMs).coerceAtLeast(0)
    }
}
