package com.sukoon.music.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sukoon.music.R
import com.sukoon.music.domain.model.LyricsState
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.RepeatMode
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.model.SongAudioSettings
import com.sukoon.music.domain.model.UserPreferences
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.ui.components.MiniPlayer
import com.sukoon.music.ui.viewmodel.HomeViewModel
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackUiSynchronizationE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var fakePlaybackRepository: FakePlaybackRepository
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var playlistViewModel: PlaylistViewModel
    private lateinit var lyricsState: MutableStateFlow<LyricsState>
    private lateinit var lyricsEvents: MutableSharedFlow<String>
    private lateinit var sleepTimerState: MutableStateFlow<Boolean>

    @Before
    fun setup() {
        fakePlaybackRepository = FakePlaybackRepository(
            initialState = playbackStateFor(
                songId = 1L,
                title = "Sync Song A",
                isPlaying = false,
                positionMs = 12_000L
            )
        )
        lyricsState = MutableStateFlow(LyricsState.NotFound)
        lyricsEvents = MutableSharedFlow(extraBufferCapacity = 1)
        sleepTimerState = MutableStateFlow(false)

        homeViewModel = mockk(relaxed = true)
        every { homeViewModel.playbackRepository } returns fakePlaybackRepository
        every { homeViewModel.playbackState } returns fakePlaybackRepository.playbackState
        every { homeViewModel.lyricsState } returns lyricsState
        every { homeViewModel.lyricsActionEvents } returns lyricsEvents
        every { homeViewModel.isSleepTimerActive } returns sleepTimerState
        every { homeViewModel.fetchLyrics(any()) } just Runs
        every { homeViewModel.importLyricsFromUri(any(), any()) } just Runs
        every { homeViewModel.clearLyrics(any()) } just Runs
        every { homeViewModel.saveManualLyrics(any(), any(), any()) } just Runs
        every { homeViewModel.clearManualLyrics(any()) } just Runs
        every { homeViewModel.updateLyricsSyncOffset(any(), any()) } just Runs
        every { homeViewModel.toggleLike(any(), any()) } just Runs
        every { homeViewModel.toggleShuffle() } just Runs
        every { homeViewModel.toggleRepeat() } just Runs
        every { homeViewModel.seekTo(any()) } just Runs
        every { homeViewModel.seekToNext() } just Runs
        every { homeViewModel.seekToPrevious() } just Runs
        every { homeViewModel.playPause() } answers {
            runBlocking { fakePlaybackRepository.playPause() }
        }

        playlistViewModel = mockk(relaxed = true)
        every { playlistViewModel.playlists } returns MutableStateFlow(emptyList())
        every { playlistViewModel.addSongToPlaylist(any(), any()) } just Runs
    }

    @OptIn(UnstableApi::class)
    @Test
    fun playbackState_fromRepository_staysSynced_betweenMiniPlayer_andNowPlaying() {
        setSynchronizedContent()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val playDescription = context.getString(R.string.common_play)
        val pauseDescription = context.getString(R.string.now_playing_pause)

        composeRule.onAllNodesWithContentDescription(playDescription).assertCountEquals(2)

        composeRule.runOnIdle {
            fakePlaybackRepository.emit(
                playbackStateFor(
                    songId = 2L,
                    title = "Sync Song B",
                    isPlaying = true,
                    positionMs = 45_000L
                )
            )
        }
        composeRule.waitForIdle()

        composeRule.onAllNodesWithContentDescription(pauseDescription).assertCountEquals(2)

        val syncedSongNodes = composeRule.onAllNodesWithText("Sync Song B").fetchSemanticsNodes()
        assertTrue(
            "Expected synced song title to appear on both MiniPlayer and NowPlaying surfaces",
            syncedSongNodes.size >= 2
        )
    }

    @OptIn(UnstableApi::class)
    @Test
    fun playPause_actions_keepMiniPlayer_andNowPlaying_inLockstep() {
        setSynchronizedContent()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val playDescription = context.getString(R.string.common_play)
        val pauseDescription = context.getString(R.string.now_playing_pause)

        composeRule.onAllNodesWithContentDescription(playDescription).assertCountEquals(2)

        composeRule.onAllNodesWithContentDescription(playDescription)[0].performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithContentDescription(pauseDescription).assertCountEquals(2)

        composeRule.onAllNodesWithContentDescription(pauseDescription)[0].performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithContentDescription(playDescription).assertCountEquals(2)

        assertTrue(
            "Expected repository playPause() to be invoked from UI controls",
            fakePlaybackRepository.playPauseCallCount >= 2
        )
    }

    @OptIn(UnstableApi::class)
    private fun setSynchronizedContent() {
        composeRule.setContent {
            MaterialTheme {
                val playbackState by homeViewModel.playbackState.collectAsStateWithLifecycle()
                Column {
                    MiniPlayer(
                        playbackState = playbackState,
                        onPlayPauseClick = { homeViewModel.playPause() },
                        onNextClick = { homeViewModel.seekToNext() },
                        onClick = {},
                        onSeek = { homeViewModel.seekTo(it) },
                        userPreferences = UserPreferences(),
                        modifier = Modifier.testTag("mini_player_surface")
                    )

                    NowPlayingScreen(
                        onBackClick = {},
                        viewModel = homeViewModel,
                        playlistViewModel = playlistViewModel
                    )
                }
            }
        }
    }

    private fun playbackStateFor(
        songId: Long,
        title: String,
        isPlaying: Boolean,
        positionMs: Long
    ): PlaybackState {
        return PlaybackState(
            isPlaying = isPlaying,
            currentPosition = positionMs,
            duration = 180_000L,
            repeatMode = RepeatMode.OFF,
            currentSong = Song(
                id = songId,
                title = title,
                artist = "Artist $songId",
                album = "Album $songId",
                duration = 180_000L,
                uri = "content://songs/$songId",
                albumArtUri = null,
                dateAdded = 0L,
                isLiked = false
            )
        )
    }

    private class FakePlaybackRepository(initialState: PlaybackState) : PlaybackRepository {
        private val mutablePlaybackState = MutableStateFlow(initialState)
        override val playbackState: StateFlow<PlaybackState> = mutablePlaybackState

        var playPauseCallCount: Int = 0
            private set

        fun emit(state: PlaybackState) {
            mutablePlaybackState.value = state
        }

        override suspend fun play() {
            mutablePlaybackState.value = mutablePlaybackState.value.copy(isPlaying = true)
        }

        override suspend fun pause() {
            mutablePlaybackState.value = mutablePlaybackState.value.copy(isPlaying = false)
        }

        override suspend fun playPause() {
            playPauseCallCount += 1
            val current = mutablePlaybackState.value
            mutablePlaybackState.value = current.copy(isPlaying = !current.isPlaying)
        }

        override suspend fun seekTo(positionMs: Long) {
            val current = mutablePlaybackState.value
            val duration = current.duration.coerceAtLeast(0L)
            val bounded = if (duration > 0L) {
                positionMs.coerceIn(0L, duration)
            } else {
                positionMs.coerceAtLeast(0L)
            }
            mutablePlaybackState.value = current.copy(currentPosition = bounded)
        }

        override suspend fun seekToNext() = Unit
        override suspend fun seekToPrevious() = Unit
        override suspend fun playSong(song: Song, queueName: String?) = Unit
        override suspend fun playQueue(songs: List<Song>, startIndex: Int, queueName: String?) = Unit
        override suspend fun shuffleAndPlayQueue(songs: List<Song>, queueName: String?) = Unit
        override suspend fun addToQueue(song: Song) = Unit
        override suspend fun addToQueue(songs: List<Song>) = Unit
        override suspend fun playNext(song: Song) = Unit
        override suspend fun playNext(songs: List<Song>) = Unit
        override suspend fun addToQueueNext(songs: List<Song>) = Unit
        override suspend fun removeFromQueue(index: Int) = Unit
        override suspend fun seekToQueueIndex(index: Int) = Unit
        override suspend fun setRepeatMode(mode: RepeatMode) = Unit
        override suspend fun setShuffleEnabled(enabled: Boolean) = Unit
        override suspend fun setPlaybackSpeed(speed: Float) = Unit
        override suspend fun refreshCurrentSong() = Unit
        override fun refreshPlaybackState(forcePositionResync: Boolean) = Unit
        override suspend fun savePlaybackState() = Unit
        override suspend fun reapplyCurrentSongSettings(songId: Long) = Unit
        override suspend fun previewCurrentSongSettings(songId: Long, settings: SongAudioSettings) = Unit
        override suspend fun applyCurrentSongSettingsImmediately(songId: Long, settings: SongAudioSettings) = Unit
        override suspend fun connect() = Unit
        override fun disconnect() = Unit
    }
}
