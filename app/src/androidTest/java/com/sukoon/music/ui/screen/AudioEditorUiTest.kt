package com.sukoon.music.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.model.UserPreferences
import com.sukoon.music.shouldShowGlobalMiniPlayer
import com.sukoon.music.ui.components.MiniPlayer
import com.sukoon.music.ui.navigation.Routes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioEditorUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleSong = Song(
        id = 1L,
        title = "Test Song",
        artist = "Artist",
        album = "Album",
        duration = 180_000L,
        uri = "content://songs/1"
    )

    @Test
    fun miniPlayerHiddenOnAudioEditorRoute() {
        val playbackState = PlaybackState(
            currentSong = sampleSong,
            isPlaying = true,
            duration = sampleSong.duration,
            currentPosition = 15_000L
        )

        composeRule.setContent {
            MaterialTheme {
                if (playbackState.currentSong != null && shouldShowGlobalMiniPlayer(Routes.AudioEditor.route)) {
                    MiniPlayer(
                        playbackState = playbackState,
                        onPlayPauseClick = {},
                        onNextClick = {},
                        onSeek = {},
                        onClick = {},
                        userPreferences = UserPreferences(),
                        modifier = Modifier.testTag("global_mini_player")
                    )
                }
            }
        }

        composeRule.onNodeWithTag("global_mini_player").assertDoesNotExist()
    }

    @Test
    fun miniPlayerShownOnRegularRoute() {
        val playbackState = PlaybackState(
            currentSong = sampleSong,
            isPlaying = true,
            duration = sampleSong.duration,
            currentPosition = 15_000L
        )

        composeRule.setContent {
            MaterialTheme {
                if (playbackState.currentSong != null && shouldShowGlobalMiniPlayer(Routes.Songs.route)) {
                    MiniPlayer(
                        playbackState = playbackState,
                        onPlayPauseClick = {},
                        onNextClick = {},
                        onSeek = {},
                        onClick = {},
                        userPreferences = UserPreferences(),
                        modifier = Modifier.testTag("global_mini_player")
                    )
                }
            }
        }

        composeRule.onNodeWithTag("global_mini_player").assertExists()
    }

    @Test
    fun audioEditorPreviewControlsAreVisibleAndEnabledForEditedSong() {
        val playbackState = PlaybackState(
            currentSong = sampleSong,
            isPlaying = false,
            duration = sampleSong.duration,
            currentPosition = 0L
        )

        composeRule.setContent {
            MaterialTheme {
                AudioPreviewCard(
                    song = sampleSong,
                    playbackState = playbackState,
                    onTogglePlayback = {},
                    onSeek = {}
                )
            }
        }

        composeRule.onNodeWithTag("audio_editor_preview_card").assertExists()
        composeRule.onNodeWithTag("audio_editor_preview_play_pause").assertExists().assertIsEnabled()
        composeRule.onNodeWithTag("audio_editor_preview_seek").assertExists().assertIsEnabled()
    }

    @Test
    fun audioEditorPreviewShowsUnavailableStateWhenSongMissing() {
        composeRule.setContent {
            MaterialTheme {
                AudioPreviewCard(
                    song = null,
                    playbackState = PlaybackState(),
                    onTogglePlayback = {},
                    onSeek = {}
                )
            }
        }

        composeRule.onNodeWithText("Song unavailable for preview").assertExists()
        composeRule.onNodeWithTag("audio_editor_preview_play_pause").assertExists().assertIsNotEnabled()
        composeRule.onNodeWithTag("audio_editor_preview_seek").assertExists().assertIsNotEnabled()
    }
}
