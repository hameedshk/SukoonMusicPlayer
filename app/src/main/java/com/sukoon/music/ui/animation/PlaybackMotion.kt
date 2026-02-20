package com.sukoon.music.ui.animation

import androidx.compose.runtime.Immutable
import com.sukoon.music.domain.model.PlaybackState

enum class MotionPlayState {
    RUNNING,
    HOLD,
    REST
}

@Immutable
data class MotionDirective(
    val state: MotionPlayState,
    val songId: Long?,
    val intensity: Float
)

fun PlaybackState.toMotionDirective(isVisible: Boolean): MotionDirective {
    val hasSong = currentSong != null
    val state = when {
        isVisible && hasSong && isPlaying && !isLoading && error == null -> MotionPlayState.RUNNING
        isVisible && hasSong -> MotionPlayState.HOLD
        else -> MotionPlayState.REST
    }
    val intensity = if (state == MotionPlayState.RUNNING) 1f else 0f
    return MotionDirective(
        state = state,
        songId = currentSong?.id,
        intensity = intensity
    )
}
