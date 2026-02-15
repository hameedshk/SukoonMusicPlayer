package com.sukoon.music.viewmodel

import android.content.Context
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.domain.model.Folder
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import com.sukoon.music.ui.viewmodel.FolderViewModel
import com.sukoon.music.domain.model.UserPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FolderViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FolderViewModelTest {

    private lateinit var songRepository: SongRepository
    private lateinit var playbackRepository: PlaybackRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var adMobManager: AdMobManager
    private lateinit var context: Context
    private lateinit var viewModel: FolderViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val mockPlaybackState = MutableStateFlow(PlaybackState())
    private val mockUserPreferences = MutableStateFlow(UserPreferences())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        songRepository = mockk(relaxed = true)
        playbackRepository = mockk(relaxed = true)
        preferencesManager = mockk(relaxed = true)
        adMobManager = mockk(relaxed = true)
        context = mockk(relaxed = true)

        // Setup default mocks
        every { songRepository.getAllFolders() } returns flowOf(emptyList())
        every { songRepository.getAllSongs() } returns flowOf(emptyList())
        every { playbackRepository.playbackState } returns mockPlaybackState
        every { preferencesManager.userPreferencesFlow } returns mockUserPreferences

        viewModel = FolderViewModel(
            songRepository = songRepository,
            playbackRepository = playbackRepository,
            preferencesManager = preferencesManager,
            adMobManager = adMobManager,
            context = context
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `folders StateFlow emits sorted folder list from repository`() = runTest {
        // Given
        val testFolders = listOf(
            createFolder(2, "Jazz", "/storage/emulated/0/Music/Jazz", 5),
            createFolder(1, "Rock", "/storage/emulated/0/Music/Rock", 10)
        )

        every { songRepository.getAllFolders() } returns flowOf(testFolders)

        // Recreate ViewModel to collect from updated flow
        viewModel = FolderViewModel(
            songRepository,
            playbackRepository,
            preferencesManager,
            adMobManager,
            context
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - NAME_ASC is default
        assertEquals(2, viewModel.folders.value.size)
        assertEquals("Jazz", viewModel.folders.value[0].name)
        assertEquals("Rock", viewModel.folders.value[1].name)
    }

    @Test
    fun `playFolder calls playbackRepository with folder songs`() = runTest {
        // Given
        val path = "/storage/emulated/0/Music/Rock"
        val songs = listOf(
            createSong(1, "Song 1"),
            createSong(2, "Song 2")
        )
        val folders = listOf(
            createFolder(1, "Rock", path, 2).copy(songIds = listOf(1, 2))
        )

        every { songRepository.getAllFolders() } returns flowOf(folders)
        every { songRepository.getAllSongs() } returns flowOf(songs)

        // Recreate to pick up new flows
        viewModel = FolderViewModel(
            songRepository,
            playbackRepository,
            preferencesManager,
            adMobManager,
            context
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.playFolder(path)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 1) {
            playbackRepository.playQueue(match { it.size == 2 }, startIndex = 0)
        }
    }

    @Test
    fun `playbackState exposes playback repository state`() = runTest {
        // Given
        val testSong = createSong(1, "Test Song")
        val testPlaybackState = PlaybackState(
            currentSong = testSong,
            isPlaying = true,
            currentPosition = 5000L
        )

        // Update the mock StateFlow
        mockPlaybackState.value = testPlaybackState
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(testPlaybackState, viewModel.playbackState.value)
    }

    // Helper functions
    private fun createFolder(
        id: Long,
        name: String,
        path: String,
        songCount: Int
    ): Folder {
        return Folder(
            id = id,
            path = path,
            name = name,
            songCount = songCount,
            totalDuration = 600000L,
            albumArtUri = null,
            songIds = List(songCount) { it.toLong() }
        )
    }

    private fun createSong(
        id: Long,
        title: String
    ): Song {
        return Song(
            id = id,
            title = title,
            artist = "Test Artist",
            album = "Test Album",
            duration = 180000L,
            uri = "content://media/external/audio/media/$id",
            albumArtUri = null,
            dateAdded = System.currentTimeMillis(),
            isLiked = false
        )
    }
}
