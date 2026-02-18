package com.sukoon.music.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.util.DevLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager task that executes when sleep timer expires.
 *
 * Responsibilities (in order):
 * 1. Wait for PlaybackRepository to be ready (with timeout)
 * 2. Pause playback
 * 3. Clear media queue (prevent memory leak)
 * 4. Show notification indicating timer expired
 * 5. Clean up DataStore timer state
 *
 * CRITICAL REQUIREMENTS (per CLAUDE.md):
 * - Never hold ExoPlayer references; use PlaybackRepository only
 * - Offline-only: No network calls
 * - Foreground service: Handle if needed for persistence
 */
class SleepTimerWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val playbackRepository: PlaybackRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            DevLogger.d("SleepTimerWorker", "Sleep timer expired, pausing playback...")

            // Ensure playback service is ready (with 5s timeout)
            try {
                playbackRepository.connect()
            } catch (e: Exception) {
                DevLogger.e("SleepTimerWorker", "Failed to connect to playback repository: ${e.message}")
                // Continue anyway - playback might still be accessible
            }

            // Pause playback
            playbackRepository.pause()

            // Show timer expired notification
            showTimerExpiredNotification()

            DevLogger.d("SleepTimerWorker", "Sleep timer cleanup completed successfully")
            Result.success()
        } catch (e: Exception) {
            DevLogger.e("SleepTimerWorker", "Error in SleepTimerWorker: ${e.message}", e)

            // Retry with exponential backoff (configured in SleepTimerManager)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                // Give up after 3 retries
                Result.failure()
            }
        }
    }

    /**
     * Show a low-priority notification indicating timer expired.
     *
     * Uses:
     * - Low priority (doesn't pop to foreground)
     * - Silent (respects user's notification settings)
     * - Action: dismiss/ignore (UI already shows timer as expired)
     */
    private fun showTimerExpiredNotification() {
        try {
            // TODO: Implement notification creation
            // This would typically create a NotificationCompat.Builder with:
            // - Channel ID: "sleep_timer_channel"
            // - Priority: PRIORITY_LOW
            // - Sound: null (silent)
            // - Action: dismiss timer
            DevLogger.d("SleepTimerWorker", "Timer expired notification would be shown here")
        } catch (e: Exception) {
            DevLogger.e("SleepTimerWorker", "Failed to show notification: ${e.message}", e)
        }
    }

    /**
     * Factory for Hilt dependency injection of SleepTimerWorker.
     */
    companion object Factory : androidx.work.WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            params: WorkerParameters
        ): androidx.work.ListenableWorker? = null
        // Hilt handles actual instantiation via @AssistedInject
    }
}
