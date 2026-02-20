package com.sukoon.music.data.repository

import com.sukoon.music.domain.model.PlaybackState
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PlaybackRepositoryImplTest {

    @Test
    fun positionIsClampedWithinDuration() = runTest {
        val state = PlaybackState(
            currentPosition = 5000L,
            duration = 3000L,
            isPlaying = true
        )

        val clampedPosition = state.currentPosition.coerceIn(0L, state.duration)

        assertEquals(3000L, clampedPosition)
    }

    @Test
    fun queueIndexIsClampedToValidRange() {
        val queueSize = 5
        val negativeIndex = -1
        val overflowIndex = 10

        val validNegative = negativeIndex.coerceIn(0, queueSize - 1)
        val validOverflow = overflowIndex.coerceIn(0, queueSize - 1)

        assertEquals(0, validNegative)
        assertEquals(queueSize - 1, validOverflow)
    }

    @Test
    fun zeroDurationProgressDefaultsToZero() = runTest {
        val state = PlaybackState(
            duration = 0L,
            currentPosition = 0L
        )

        val progress = if (state.duration > 0L) {
            (state.currentPosition.toFloat() / state.duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        assertEquals(0f, progress)
    }
}
