package com.sukoon.music.ui.animation

import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackMotionMapperTest {

    private val song = Song(
        id = 1L,
        title = "Song",
        artist = "Artist",
        album = "Album",
        duration = 180_000L,
        uri = "file://song.mp3"
    )

    @Test
    fun `maps to RUNNING when actively playing and visible`() {
        val state = PlaybackState(
            isPlaying = true,
            isLoading = false,
            error = null,
            currentSong = song
        )

        val motion = state.toMotionDirective(isVisible = true)

        assertEquals(MotionPlayState.RUNNING, motion.state)
    }

    @Test
    fun `maps to HOLD when paused with active song`() {
        val state = PlaybackState(
            isPlaying = false,
            currentSong = song
        )

        val motion = state.toMotionDirective(isVisible = true)

        assertEquals(MotionPlayState.HOLD, motion.state)
    }

    @Test
    fun `maps to HOLD when loading with active song`() {
        val state = PlaybackState(
            isPlaying = true,
            isLoading = true,
            currentSong = song
        )

        val motion = state.toMotionDirective(isVisible = true)

        assertEquals(MotionPlayState.HOLD, motion.state)
    }

    @Test
    fun `maps to HOLD when error with active song`() {
        val state = PlaybackState(
            isPlaying = true,
            error = "playback error",
            currentSong = song
        )

        val motion = state.toMotionDirective(isVisible = true)

        assertEquals(MotionPlayState.HOLD, motion.state)
    }

    @Test
    fun `maps to REST when no song`() {
        val state = PlaybackState(isPlaying = true, currentSong = null)

        val motion = state.toMotionDirective(isVisible = true)

        assertEquals(MotionPlayState.REST, motion.state)
    }

    @Test
    fun `maps to REST when hidden even if playing`() {
        val state = PlaybackState(
            isPlaying = true,
            currentSong = song
        )

        val motion = state.toMotionDirective(isVisible = false)

        assertEquals(MotionPlayState.REST, motion.state)
    }
}
