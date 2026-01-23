package com.sukoon.music.data.audio

import androidx.media3.exoplayer.ExoPlayer
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CrossfadeManagerTest {

    @MockK
    lateinit var mockPlayer: ExoPlayer

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `applyCrossfade with 0ms should set volume to 1f`() = runTest(testDispatcher) {
        val manager = CrossfadeManager(mockPlayer, this)
        every { mockPlayer.volume = any() } just runs

        manager.applyCrossfade(0)

        verify { mockPlayer.volume = 1.0f }
    }

    @Test
    fun `applyCrossfade should animate volume from start to end`() = runTest(testDispatcher) {
        val manager = CrossfadeManager(mockPlayer, this)
        every { mockPlayer.volume = any() } just runs

        manager.applyCrossfade(160) // 160ms = ~10 steps at 16ms each

        advanceTimeBy(160)

        // Verify volume was animated (multiple calls to set volume)
        verify(atLeast = 2) { mockPlayer.volume = any() }
    }

    @Test
    fun `cancel should restore volume to 1f`() = runTest(testDispatcher) {
        val manager = CrossfadeManager(mockPlayer, this)
        every { mockPlayer.volume = any() } just runs

        manager.applyCrossfade(500)
        manager.cancel()

        verify { mockPlayer.volume = 1.0f }
    }

    @Test
    fun `applyCrossfade called twice should cancel first animation`() = runTest(testDispatcher) {
        val manager = CrossfadeManager(mockPlayer, this)
        every { mockPlayer.volume = any() } just runs

        manager.applyCrossfade(500)
        manager.applyCrossfade(300) // Should cancel first

        advanceTimeBy(300)

        // Should complete second animation successfully
        verify { mockPlayer.volume = 1.0f }
    }
}
