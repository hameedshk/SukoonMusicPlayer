package com.sukoon.music.domain.usecase

import com.sukoon.music.domain.model.PlaybackSessionState
import kotlinx.coroutines.flow.Flow

/**
 * Session controller: Single source of truth for private session state.
 *
 * Responsibilities:
 * - Manage session lifecycle (start/stop)
 * - Enforce session-scoped expiry on inactivity
 * - Expose immutable session state as a reactive Flow
 * - Prevent history/stats logging when session is active
 *
 * CRITICAL: This is read-only to consumers. Only Settings can call start/stop.
 */
interface SessionController {
    /**
     * Observable session state.
     * Emits updated state whenever session starts/stops/expires.
     */
    fun sessionState(): Flow<PlaybackSessionState>

    /**
     * Start a new private session.
     * If session is already active, this is a no-op.
     * Session will auto-expire after inactivity (default 5 minutes).
     */
    suspend fun startPrivateSession()

    /**
     * Stop the current private session.
     * If no session is active, this is a no-op.
     */
    suspend fun stopPrivateSession()

    /**
     * Reset the inactivity timer. Called on playback event.
     * If session has expired, this is a no-op.
     */
    suspend fun refreshInactivityTimer()

    /**
     * Check if history logging should be blocked.
     * Returns true if session is active AND not expired.
     */
    suspend fun isSessionPrivate(): Boolean
}
