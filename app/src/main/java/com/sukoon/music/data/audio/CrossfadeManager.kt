package com.sukoon.music.data.audio

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.sukoon.music.util.DevLogger

/**
 * Manages audio crossfade transitions between tracks.
 * Animates volume down on outgoing track and up on incoming track.
 */
class CrossfadeManager(
    private val player: ExoPlayer,
    private val scope: CoroutineScope
) {
    private var crossfadeJob: Job? = null

    /**
     * Apply crossfade effect with specified duration.
     * Fades out current volume and fades in new volume over the duration.
     *
     * @param durationMs Duration in milliseconds (0 = disabled)
     */
    fun applyCrossfade(durationMs: Int) {
        // Cancel any pending crossfade
        crossfadeJob?.cancel()

        if (durationMs <= 0) {
            // Crossfade disabled - ensure volume is at normal level
            player.volume = 1.0f
            DevLogger.d("CrossfadeManager", "Crossfade disabled")
            return
        }

        DevLogger.d("CrossfadeManager", "Starting crossfade: ${durationMs}ms, current volume: ${player.volume}")

        crossfadeJob = scope.launch {
            try {
                // Save current volume state
                val startVolume = player.volume
                val endVolume = 1.0f

                DevLogger.d("CrossfadeManager", "Fade out phase starting: $startVolume → 0.0f")

                // Fade out phase (first half)
                val fadeOutDuration = durationMs / 2L
                animateVolume(startVolume, 0.0f, fadeOutDuration)

                DevLogger.d("CrossfadeManager", "Fade out complete, volume now: ${player.volume}")

                // New track starts playing here (volume is at 0)
                // Fade in phase (second half)
                val fadeInDuration = durationMs - fadeOutDuration

                DevLogger.d("CrossfadeManager", "Fade in phase starting: 0.0f → $endVolume")
                animateVolume(0.0f, endVolume, fadeInDuration)

                DevLogger.d("CrossfadeManager", "Crossfade completed: ${durationMs}ms, final volume: ${player.volume}")
            } catch (e: Exception) {
                DevLogger.e("CrossfadeManager", "Crossfade interrupted", e)
                // Restore normal volume on error
                player.volume = 1.0f
            }
        }
    }

    /**
     * Animate volume from start to end over duration.
     * Uses linear interpolation with 16ms steps for smooth animation.
     */
    private suspend fun animateVolume(startVolume: Float, endVolume: Float, durationMs: Long) {
        if (durationMs <= 0) {
            player.volume = endVolume
            return
        }

        val stepDurationMs = 16L // ~60fps
        val steps = (durationMs / stepDurationMs).toInt().coerceAtLeast(1)
        val volumeDelta = (endVolume - startVolume) / steps

        for (step in 0..steps) {
            val progress = step.toFloat() / steps
            player.volume = startVolume + (endVolume - startVolume) * progress

            if (step < steps) {
                delay(stepDurationMs)
            }
        }

        // Ensure we end at exact target volume
        player.volume = endVolume
    }

    /**
     * Cancel any ongoing crossfade animation.
     * Restores volume to normal immediately.
     */
    fun cancel() {
        crossfadeJob?.cancel()
        crossfadeJob = null
        player.volume = 1.0f
        DevLogger.d("CrossfadeManager", "Crossfade cancelled")
    }

    /**
     * Release resources (called on service destroy).
     */
    fun release() {
        cancel()
    }
}
