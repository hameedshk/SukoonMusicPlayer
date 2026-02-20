package com.sukoon.music.ui.animation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import kotlin.math.PI

internal const val DEFAULT_PLAYBACK_MOTION_SPEED = 0.55f

internal fun advancePhase(
    phase: Float,
    deltaSeconds: Float,
    motionState: MotionPlayState,
    intensity: Float,
    speed: Float = DEFAULT_PLAYBACK_MOTION_SPEED
): Float {
    return when (motionState) {
        MotionPlayState.RUNNING -> {
            val next = phase + (deltaSeconds * speed * intensity)
            val maxPhase = (PI * 2).toFloat()
            if (next > maxPhase) next - maxPhase else next
        }
        MotionPlayState.HOLD -> phase
        MotionPlayState.REST -> 0f
    }
}

@Composable
fun rememberPlaybackMotionClock(
    motion: MotionDirective,
    speed: Float = DEFAULT_PLAYBACK_MOTION_SPEED
): State<Float> {
    return produceState(initialValue = 0f, motion.state, motion.songId, motion.intensity) {
        if (motion.state == MotionPlayState.REST) {
            value = 0f
            return@produceState
        }

        var phase = value
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = now
                    return@withFrameNanos
                }
                val dt = (now - lastFrameNanos).coerceAtLeast(0L) / 1_000_000_000f
                lastFrameNanos = now
                phase = advancePhase(
                    phase = phase,
                    deltaSeconds = dt,
                    motionState = motion.state,
                    intensity = motion.intensity,
                    speed = speed
                )
                value = phase
            }
        }
    }
}
