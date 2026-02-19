package com.sukoon.music.data.usecase

import com.sukoon.music.domain.model.PlaybackSessionState
import com.sukoon.music.domain.usecase.SessionController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session controller implementation.
 *
 * Session lifecycle:
 * 1. User toggles "Private Session" ON in Settings → startPrivateSession()
 * 2. Session is ACTIVE, inactivity timer starts
 * 3. Each playback event → refreshInactivityTimer() (resets expiry counter)
 * 4. If no playback for 5 minutes → auto-expiry → session becomes INACTIVE
 * 5. User toggles "Private Session" OFF → stopPrivateSession()
 * 6. Session is INACTIVE (no history will be logged)
 *
 * CRITICAL: Session is in-memory only. Killing app = session lost.
 * CRITICAL: History checks use isSessionPrivate() before logging.
 */
@Singleton
class SessionControllerImpl @Inject constructor() : SessionController {
    companion object {
        private const val SESSION_EXPIRY_MS = 300_000L // 5 minutes
    }

    private val _sessionState = MutableStateFlow(PlaybackSessionState())
    private val scope = CoroutineScope(Dispatchers.Main)
    private var expiryJob: Job? = null

    override fun sessionState(): Flow<PlaybackSessionState> = _sessionState.asStateFlow()

    override suspend fun startPrivateSession() {
        val current = _sessionState.value
        if (current.isActive) return // Already active, no-op

        // Start new session with current timestamp
        _sessionState.value = PlaybackSessionState(
            isActive = true,
            startedAtMs = System.currentTimeMillis(),
            expiryTimeMs = SESSION_EXPIRY_MS
        )

        // Schedule auto-expiry
        scheduleAutoExpiry()
    }

    override suspend fun stopPrivateSession() {
        expiryJob?.cancel()
        _sessionState.value = PlaybackSessionState() // Reset to inactive
    }

    override suspend fun refreshInactivityTimer() {
        val current = _sessionState.value
        if (!current.isActive || current.hasExpired()) return // No-op if not active or already expired

        // Reset the expiry timer by restarting the session with current timestamp
        _sessionState.value = PlaybackSessionState(
            isActive = true,
            startedAtMs = System.currentTimeMillis(),
            expiryTimeMs = SESSION_EXPIRY_MS
        )

        // Reschedule auto-expiry
        scheduleAutoExpiry()
    }

    override suspend fun isSessionPrivate(): Boolean {
        val current = _sessionState.value
        if (!current.isActive) return false

        // Check if expired
        if (current.hasExpired()) {
            // Session expired, auto-deactivate it
            stopPrivateSession()
            return false
        }

        return true
    }

    /**
     * Schedule auto-expiry after inactivity.
     * Cancels any previous job and creates a new one.
     */
    private fun scheduleAutoExpiry() {
        expiryJob?.cancel()
        val scheduledStartTime = _sessionState.value.startedAtMs

        expiryJob = scope.launch {
            delay(SESSION_EXPIRY_MS)
            // Auto-expire only if the same session is still active.
            val current = _sessionState.value
            if (current.isActive && current.startedAtMs == scheduledStartTime) {
                _sessionState.value = PlaybackSessionState()
            }
        }
    }
}
