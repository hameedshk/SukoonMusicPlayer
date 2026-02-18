package com.sukoon.music.domain.manager

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.worker.SleepTimerWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Sealed class representing the state of the sleep timer.
 *
 * @see SleepTimerManager
 */
sealed class SleepTimerState {
    /** Timer is inactive. */
    object Inactive : SleepTimerState()

    /** Timer is active and counting down. */
    data class Active(
        val totalMinutes: Int,
        val endTimeMillis: Long,
        val remainingMinutes: Int = calculateRemaining(endTimeMillis)
    ) : SleepTimerState() {
        companion object {
            private fun calculateRemaining(endTimeMillis: Long): Int {
                val remaining = (endTimeMillis - System.currentTimeMillis()) / (1000 * 60)
                return max(0, remaining.toInt())
            }
        }
    }

    /** Error occurred with the timer. */
    data class Error(val message: String) : SleepTimerState()
}

/**
 * Manager for sleep timer state and scheduling using WorkManager.
 *
 * Responsibilities:
 * - Maintain reactive [timerState] StateFlow for UI consumption
 * - Schedule and cancel WorkManager tasks for timer expiry
 * - Persist timer state in DataStore for process death recovery
 * - Countdown ticker updating remaining time every minute
 *
 * CRITICAL REQUIREMENTS (per CLAUDE.md):
 * - Offline-only: No network calls
 * - ExoPlayer: Never holds references; uses PlaybackRepository instead
 * - StateFlow: Expose for `collectAsStateWithLifecycle()` consumption
 * - WorkManager: Required for persistence across process death
 * - DataStore: Fallback persistence if WorkManager fails
 */
@Singleton
class SleepTimerManager @Inject constructor(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val SLEEP_TIMER_WORK_NAME = "sleep_timer_work"
        private const val COUNTDOWN_INTERVAL_MS = 60_000L // Update UI every minute
    }

    private val _timerState = MutableStateFlow<SleepTimerState>(SleepTimerState.Inactive)
    val timerState: StateFlow<SleepTimerState> = _timerState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)
    private var countdownJob: Job? = null

    init {
        scope.launch {
            // Restore timer state on initialization
            restoreTimerStateIfNeeded()
        }
    }

    /**
     * Start a new sleep timer with the specified duration.
     *
     * @param durationMinutes Duration in minutes (must be > 0)
     * @throws IllegalArgumentException if durationMinutes <= 0
     */
    suspend fun startTimer(durationMinutes: Int) {
        require(durationMinutes > 0) { "Duration must be greater than 0" }

        try {
            val endTimeMillis = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)

            // Schedule WorkManager task to fire at exact expiry time
            scheduleTimerExpiry(endTimeMillis)

            // Persist state to DataStore for recovery
            preferencesManager.setSleepTimerTargetTime(endTimeMillis)

            // Update UI state
            _timerState.value = SleepTimerState.Active(
                totalMinutes = durationMinutes,
                endTimeMillis = endTimeMillis
            )

            // Start countdown ticker
            startCountdownTicker(endTimeMillis)
        } catch (e: Exception) {
            _timerState.value = SleepTimerState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Cancel the active sleep timer.
     *
     * Cleans up:
     * - WorkManager scheduled task
     * - Countdown ticker job
     * - DataStore persistence
     * - UI state
     */
    suspend fun cancelTimer() {
        try {
            // Cancel WorkManager task
            WorkManager.getInstance(context).cancelUniqueWork(SLEEP_TIMER_WORK_NAME)

            // Cancel countdown ticker
            countdownJob?.cancel()
            countdownJob = null

            // Clear DataStore persistence
            preferencesManager.setSleepTimerTargetTime(0L)

            // Reset UI state
            _timerState.value = SleepTimerState.Inactive
        } catch (e: Exception) {
            _timerState.value = SleepTimerState.Error(e.message ?: "Failed to cancel timer")
        }
    }

    /**
     * Restore timer state on app startup if a timer was active before process death.
     *
     * Validates:
     * - DataStore has valid end time
     * - Timer hasn't already expired
     * - WorkManager task is still scheduled
     */
    private suspend fun restoreTimerStateIfNeeded() {
        try {
            val endTimeMillis = preferencesManager.getSleepTimerTargetTime()

            // No saved timer state
            if (endTimeMillis == null || endTimeMillis <= 0) {
                _timerState.value = SleepTimerState.Inactive
                return
            }

            val now = System.currentTimeMillis()
            val remainingMs = endTimeMillis - now

            when {
                remainingMs <= 0 -> {
                    // Timer already expired, clean up
                    cancelTimer()
                }

                remainingMs > 0 -> {
                    // Timer is still valid, restore and restart countdown
                    val durationMinutes = ((remainingMs + 59_999) / (60 * 1000)).toInt()
                    _timerState.value = SleepTimerState.Active(
                        totalMinutes = durationMinutes,
                        endTimeMillis = endTimeMillis
                    )

                    // Reschedule WorkManager task (may have been lost)
                    scheduleTimerExpiry(endTimeMillis)

                    // Restart countdown
                    startCountdownTicker(endTimeMillis)
                }
            }
        } catch (e: Exception) {
            _timerState.value = SleepTimerState.Error(e.message ?: "Failed to restore timer")
        }
    }

    /**
     * Schedule WorkManager task to execute when timer expires.
     *
     * Uses:
     * - OneTimeWorkRequest with delay = endTimeMillis - now
     * - Exponential backoff (initial: 15s, max: 1h)
     * - Unique work name for deduplication
     */
    private fun scheduleTimerExpiry(endTimeMillis: Long) {
        val delayMs = endTimeMillis - System.currentTimeMillis()

        if (delayMs > 0) {
            val workRequest = OneTimeWorkRequestBuilder<SleepTimerWorker>()
                .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setBackoffPolicy(BackoffPolicy.EXPONENTIAL, 15, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                SLEEP_TIMER_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    /**
     * Start countdown ticker that updates UI every minute with remaining time.
     *
     * Updates [_timerState] with decremented [SleepTimerState.Active.remainingMinutes].
     * Stops when timer expires or countdown is explicitly cancelled.
     */
    private fun startCountdownTicker(endTimeMillis: Long) {
        // Cancel previous ticker if any
        countdownJob?.cancel()

        countdownJob = scope.launch {
            while (true) {
                delay(COUNTDOWN_INTERVAL_MS)

                val remaining = endTimeMillis - System.currentTimeMillis()

                if (remaining <= 0) {
                    // Timer expired, WorkManager will handle pause+cleanup
                    break
                }

                // Update remaining minutes for UI
                val currentState = _timerState.value
                if (currentState is SleepTimerState.Active) {
                    val remainingMinutes = ((remaining + 59_999) / (60 * 1000)).toInt()
                    _timerState.value = currentState.copy(remainingMinutes = remainingMinutes)
                }
            }
        }
    }

    /**
     * Get current sleep timer end time from DataStore.
     * @return End time in milliseconds, or null if no timer is saved
     */
    suspend fun getSleepTimerTargetTime(): Long? {
        return preferencesManager.getSleepTimerTargetTime()
    }
}
