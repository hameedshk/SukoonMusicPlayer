package com.sukoon.music.viewmodel

import android.content.Context
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.data.analytics.AnalyticsTracker
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.domain.model.PlaybackSessionState
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.ScanState
import com.sukoon.music.domain.model.UserPreferences
import com.sukoon.music.domain.repository.ListeningStatsRepository
import com.sukoon.music.domain.repository.LyricsRepository
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SettingsRepository
import com.sukoon.music.domain.repository.SongRepository
import com.sukoon.music.domain.usecase.SessionController
import com.sukoon.music.ui.model.HomeTabKey
import com.sukoon.music.ui.viewmodel.HomeViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var songRepository: SongRepository
    private lateinit var playbackRepository: PlaybackRepository
    private lateinit var lyricsRepository: LyricsRepository
    private lateinit var listeningStatsRepository: ListeningStatsRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var adMobManager: AdMobManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var sessionController: SessionController
    private lateinit var analyticsTracker: AnalyticsTracker
    private lateinit var context: Context

    private val scanStateFlow = MutableStateFlow<ScanState>(ScanState.Idle)
    private val playbackStateFlow = MutableStateFlow(PlaybackState())
    private val sessionStateFlow = MutableStateFlow(PlaybackSessionState())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        songRepository = mockk(relaxed = true)
        playbackRepository = mockk(relaxed = true)
        lyricsRepository = mockk(relaxed = true)
        listeningStatsRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        adMobManager = mockk(relaxed = true)
        preferencesManager = mockk(relaxed = true)
        sessionController = mockk(relaxed = true)
        analyticsTracker = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { songRepository.scanState } returns scanStateFlow
        every { songRepository.getAllSongs() } returns flowOf(emptyList())
        every { songRepository.getRecentlyPlayed() } returns flowOf(emptyList())
        every { songRepository.getRediscoverAlbums() } returns flowOf(emptyList())
        coEvery { songRepository.scanLocalMusic() } returns true

        every { playbackRepository.playbackState } returns playbackStateFlow
        every { sessionController.sessionState() } returns sessionStateFlow

        every { preferencesManager.getSelectedHomeTabFlow() } returns flowOf(HomeTabKey.HOME.storageToken)
        every { preferencesManager.userPreferencesFlow } returns flowOf(UserPreferences(scanOnStartup = false))

        every { settingsRepository.userPreferences } returns flowOf(UserPreferences())
        every { settingsRepository.shouldShowRatingBannerFlow() } returns flowOf(false)
        coEvery { settingsRepository.getLastScanTime() } returns System.currentTimeMillis()

        coEvery { listeningStatsRepository.cleanupOldStats() } returns Unit
        coEvery { listeningStatsRepository.getTotalListeningTime7Days() } returns 0
        coEvery { listeningStatsRepository.getTopArtist7Days() } returns null
        coEvery { listeningStatsRepository.getPeakTimeOfDay7Days() } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startup scan does not run when scanOnStartup is disabled`() = runTest {
        every { preferencesManager.userPreferencesFlow } returns flowOf(UserPreferences(scanOnStartup = false))

        createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 0) { songRepository.scanLocalMusic() }
    }

    @Test
    fun `startup scan runs when scanOnStartup is enabled and last scan is stale`() = runTest {
        every { preferencesManager.userPreferencesFlow } returns flowOf(UserPreferences(scanOnStartup = true))
        coEvery { settingsRepository.getLastScanTime() } returns 0L
        coEvery { songRepository.scanLocalMusic() } returns true

        createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 1) { songRepository.scanLocalMusic() }
    }

    @Test
    fun `manual scan emits success result event`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        coEvery { songRepository.scanLocalMusic() } coAnswers {
            scanStateFlow.value = ScanState.Success(totalSongs = 42)
            true
        }

        val event = async { viewModel.manualScanResults.first() }
        viewModel.scanLocalMusic()
        advanceUntilIdle()

        assertEquals(HomeViewModel.ManualScanResult.Success(totalSongs = 42), event.await())
    }

    @Test
    fun `manual scan emits error event when repository rejects concurrent scan`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        coEvery { songRepository.scanLocalMusic() } coAnswers {
            scanStateFlow.value = ScanState.Error("Scan already in progress.")
            false
        }

        val event = async { viewModel.manualScanResults.first() }
        viewModel.scanLocalMusic()
        advanceUntilIdle()

        val result = event.await()
        assertTrue(result is HomeViewModel.ManualScanResult.Error)
        assertEquals("Scan already in progress.", (result as HomeViewModel.ManualScanResult.Error).message)
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            songRepository = songRepository,
            playbackRepository = playbackRepository,
            lyricsRepository = lyricsRepository,
            listeningStatsRepository = listeningStatsRepository,
            settingsRepository = settingsRepository,
            adMobManager = adMobManager,
            preferencesManager = preferencesManager,
            sessionController = sessionController,
            analyticsTracker = analyticsTracker,
            context = context
        )
    }
}
