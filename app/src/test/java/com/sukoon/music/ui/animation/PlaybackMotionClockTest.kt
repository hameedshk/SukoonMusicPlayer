package com.sukoon.music.ui.animation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackMotionClockTest {

    @Test
    fun `advancePhase advances in RUNNING`() {
        val next = advancePhase(
            phase = 0.2f,
            deltaSeconds = 0.5f,
            motionState = MotionPlayState.RUNNING,
            intensity = 1f
        )

        assertTrue(next > 0.2f)
    }

    @Test
    fun `advancePhase holds in HOLD`() {
        val next = advancePhase(
            phase = 1.3f,
            deltaSeconds = 1.0f,
            motionState = MotionPlayState.HOLD,
            intensity = 1f
        )

        assertEquals(1.3f, next, 0.0001f)
    }

    @Test
    fun `advancePhase resets in REST`() {
        val next = advancePhase(
            phase = 2.4f,
            deltaSeconds = 1.0f,
            motionState = MotionPlayState.REST,
            intensity = 1f
        )

        assertEquals(0f, next, 0.0001f)
    }
}
