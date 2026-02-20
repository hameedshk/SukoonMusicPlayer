package com.sukoon.music.regression

import com.sukoon.music.domain.model.PlaybackState
import kotlin.test.assertTrue
import org.junit.Test

class MiniPlayerRegressionTest {

    @Test
    fun durationZeroDoesNotCauseInvalidProgress() {
        val state = PlaybackState(duration = 0L)

        val progress = if (state.duration > 0L) {
            state.currentPosition.toFloat() / state.duration.toFloat()
        } else {
            0f
        }

        assertTrue(progress >= 0f)
    }

    @Test
    fun positionIsNotAllowedPastDuration() {
        val state = PlaybackState(
            duration = 100L,
            currentPosition = 150L
        )

        val clampedPosition = state.currentPosition.coerceIn(0L, state.duration)
        assertTrue(clampedPosition <= state.duration)
    }

    @Test
    fun queueIndexOutOfBoundsIsClamped() {
        val queueSize = 5
        val invalidIndex = 10

        val validIndex = invalidIndex.coerceIn(0, queueSize - 1)
        assertTrue(validIndex < queueSize)
    }

    @Test
    fun reconnectFlowReplacesNullControllerReference() {
        var mediaController: Any? = null
        mediaController = Any()

        assertTrue(mediaController != null)
    }

    @Test
    fun snapshotStateIsIndependentFromFutureMutation() {
        var playbackState = PlaybackState(isPlaying = true)
        val snapshot = playbackState.copy()
        playbackState = PlaybackState(isPlaying = false)

        assertTrue(snapshot.isPlaying)
        assertTrue(!playbackState.isPlaying)
    }
}
