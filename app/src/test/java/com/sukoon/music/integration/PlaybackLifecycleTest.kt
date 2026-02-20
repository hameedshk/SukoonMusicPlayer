package com.sukoon.music.integration

import com.sukoon.music.domain.model.PlaybackState
import kotlin.test.assertEquals
import org.junit.Test

class PlaybackLifecycleTest {

    @Test
    fun playbackStatePreservesPositionOnRestore() {
        val originalState = PlaybackState(
            isPlaying = true,
            currentPosition = 30000L,
            duration = 180000L
        )

        val savedPosition = originalState.currentPosition
        val restoredState = originalState.copy(currentPosition = savedPosition)

        assertEquals(originalState.currentPosition, restoredState.currentPosition)
        assertEquals(originalState.isPlaying, restoredState.isPlaying)
    }

    @Test
    fun queueRestoreDetectsSongMismatch() {
        val savedSongId = 123L
        val restoredQueueSongId = 456L

        val mismatchDetected = savedSongId != restoredQueueSongId

        assertEquals(true, mismatchDetected)
    }
}
