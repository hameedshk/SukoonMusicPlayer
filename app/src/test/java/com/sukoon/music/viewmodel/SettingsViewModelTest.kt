package com.sukoon.music.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.sukoon.music.data.billing.BillingState
import com.sukoon.music.data.premium.PremiumManager
import com.sukoon.music.domain.model.PlaybackSessionState
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.ScanState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.model.UserPreferences
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SettingsRepository
import com.sukoon.music.domain.repository.SongRepository
import com.sukoon.music.domain.repository.StorageStats
import com.sukoon.music.domain.usecase.SessionController
import com.sukoon.music.ui.viewmodel.EndOfTrackResult
import com.sukoon.music.ui.viewmodel.SettingsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var songRepository: SongRepository
    private lateinit var playbackRepository: PlaybackRepository
    private lateinit var sessionController: SessionController
    private lateinit var premiumManager: PremiumManager

    private val playbackStateFlow = MutableStateFlow(PlaybackState())
    private var nowMs: Long = 1_000L

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        settingsRepository = mockk(relaxed = true)
        songRepository = mockk(relaxed = true)
        playbackRepository = mockk(relaxed = true)
        sessionController = mockk(relaxed = true)
        premiumManager = mockk(relaxed = true)

        every { settingsRepository.userPreferences } returns flowOf(UserPreferences())
        every { settingsRepository.appLanguageTagFlow() } returns flowOf(null)
        every { settingsRepository.premiumBannerDismissedFlow() } returns flowOf(false)
        every { settingsRepository.shouldShowRatingBannerFlow() } returns flowOf(false)
        coEvery { settingsRepository.getStorageStats() } returns StorageStats(
            databaseSizeBytes = 0L,
            cacheSizeBytes = 0L,
            audioLibrarySizeBytes = 0L
        )

        every { songRepository.scanState } returns MutableStateFlow(ScanState.Idle)
        every { songRepository.isScanning } returns MutableStateFlow(false)

        every { sessionController.sessionState() } returns flowOf(PlaybackSessionState())
        every { premiumManager.billingState } returns flowOf(BillingState.Idle)
        every { playbackRepository.playbackState } returns playbackStateFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setSleepTimer with positive minutes persists target time`() = runTest {
        val viewModel = createViewModel()
        viewModel.setSleepTimer(10)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            settingsRepository.setSleepTimerTargetTime(601_000L)
        }
    }

    @Test
    fun `setSleepTimer with zero minutes clears timer`() = runTest {
        val viewModel = createViewModel()
        viewModel.setSleepTimer(0)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            settingsRepository.setSleepTimerTargetTime(0L)
        }
    }

    @Test
    fun `setSleepTimer with negative minutes clears timer`() = runTest {
        val viewModel = createViewModel()
        viewModel.setSleepTimer(-5)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            settingsRepository.setSleepTimerTargetTime(0L)
        }
    }

    @Test
    fun `setSleepTimerEndOfTrack persists target when playback has remaining duration`() = runTest {
        val viewModel = createViewModel()
        playbackStateFlow.value = PlaybackState(
            currentSong = testSong(),
            duration = 240_000L,
            currentPosition = 40_000L
        )

        val result = viewModel.setSleepTimerEndOfTrack()
        advanceUntilIdle()

        assertEquals(EndOfTrackResult.Success, result)
        coVerify(exactly = 1) {
            settingsRepository.setSleepTimerTargetTime(201_000L)
        }
    }

    @Test
    fun `setSleepTimerEndOfTrack returns NoActiveTrack when no song is active`() = runTest {
        val viewModel = createViewModel()
        playbackStateFlow.value = PlaybackState(
            currentSong = null,
            duration = 240_000L,
            currentPosition = 40_000L
        )

        val result = viewModel.setSleepTimerEndOfTrack()
        advanceUntilIdle()

        assertEquals(EndOfTrackResult.NoActiveTrack, result)
        coVerify(exactly = 0) {
            settingsRepository.setSleepTimerTargetTime(any())
        }
    }

    @Test
    fun `setSleepTimerEndOfTrack returns TrackEndNotAvailable when duration is invalid`() = runTest {
        val viewModel = createViewModel()
        playbackStateFlow.value = PlaybackState(
            currentSong = testSong(),
            duration = 0L,
            currentPosition = 0L
        )

        val result = viewModel.setSleepTimerEndOfTrack()
        advanceUntilIdle()

        assertEquals(EndOfTrackResult.TrackEndNotAvailable, result)
        coVerify(exactly = 0) {
            settingsRepository.setSleepTimerTargetTime(any())
        }
    }

    @Test
    fun `new timer selection overwrites previous timer target`() = runTest {
        val viewModel = createViewModel()

        nowMs = 1_000L
        viewModel.setSleepTimer(5)

        nowMs = 2_000L
        viewModel.setSleepTimer(10)

        advanceUntilIdle()

        coVerify(exactly = 1) {
            settingsRepository.setSleepTimerTargetTime(301_000L)
        }
        coVerify(exactly = 1) {
            settingsRepository.setSleepTimerTargetTime(602_000L)
        }
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            settingsRepository = settingsRepository,
            songRepository = songRepository,
            playbackRepository = playbackRepository,
            sessionController = sessionController,
            premiumManager = premiumManager,
            savedStateHandle = SavedStateHandle()
        ).also { viewModel ->
            viewModel.currentTimeProvider = { nowMs }
            testDispatcher.scheduler.advanceUntilIdle()
        }
    }

    private fun testSong(): Song {
        return Song(
            id = 1L,
            title = "Sleep Song",
            artist = "Artist",
            album = "Album",
            duration = 240_000L,
            uri = "content://test/song/1"
        )
    }
}
