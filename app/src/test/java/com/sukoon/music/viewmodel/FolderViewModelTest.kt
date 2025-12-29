package com.sukoon.music.viewmodel

import com.sukoon.music.domain.model.Folder
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import com.sukoon.music.ui.viewmodel.FolderViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for FolderViewModel.
 *
 * Tests the following functionality:
 * - Folder list exposure
 * - Playback state observation
 * - Play folder functionality
 * - Shuffle folder functionality
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FolderViewModelTest {

    private lateinit var songRepository: SongRepository
    private lateinit var playbackRepository: PlaybackRepository
    private lateinit var viewModel: FolderViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        songRepository = mockk(relaxed = true)
        playbackRepository = mockk(relaxed = true)

        // Setup default mocks
        coEvery { songRepository.getAllFolders() } returns flowOf(emptyList())
        coEvery { songRepository.getSongsByFolderId(any()) } returns flowOf(emptyList())
        coEvery { playbackRepository.playbackState } returns flowOf(PlaybackState())

        viewModel = FolderViewModel(
            songRepository = songRepository,
            playbackRepository = playbackRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `folders StateFlow emits folder list from repository`() = runTest {
        // Given
        val testFolders = listOf(
            createFolder(1, "Rock", "/storage/music/Rock", 10),
            createFolder(2, "Jazz", "/storage/music/Jazz", 5)
        )

        coEvery { songRepository.getAllFolders() } returns flowOf(testFolders)

        // Recreate ViewModel with new mock
        viewModel = FolderViewModel(songRepository, playbackRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(testFolders, viewModel.folders.value)
    }

    @Test
    fun `playFolder calls playbackRepository with folder songs`() = runTest {
        // Given
        val folderId = 123L
        val folderSongs = listOf(
            createSong(1, "Song 1"),
            createSong(2, "Song 2"),
            createSong(3, "Song 3")
        )

        coEvery { songRepository.getSongsByFolderId(folderId) } returns flowOf(folderSongs)

        // When
        viewModel.playFolder(folderId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 1) {
            playbackRepository.playQueue(folderSongs, startIndex = 0)
        }
    }

    @Test
    fun `playFolder does not call playbackRepository when folder is empty`() = runTest {
        // Given
        val folderId = 123L
        coEvery { songRepository.getSongsByFolderId(folderId) } returns flowOf(emptyList())

        // When
        viewModel.playFolder(folderId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 0) {
            playbackRepository.playQueue(any(), any())
        }
    }

    @Test
    fun `shuffleFolder calls playbackRepository with shuffled songs`() = runTest {
        // Given
        val folderId = 123L
        val folderSongs = listOf(
            createSong(1, "Song 1"),
            createSong(2, "Song 2"),
            createSong(3, "Song 3")
        )

        coEvery { songRepository.getSongsByFolderId(folderId) } returns flowOf(folderSongs)

        // When
        viewModel.shuffleFolder(folderId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 1) {
            playbackRepository.playQueue(
                songs = match { songs ->
                    // Verify it's a shuffled version of the same songs
                    songs.size == folderSongs.size &&
                    songs.containsAll(folderSongs)
                },
                startIndex = 0
            )
        }
    }

    @Test
    fun `shuffleFolder does not call playbackRepository when folder is empty`() = runTest {
        // Given
        val folderId = 123L
        coEvery { songRepository.getSongsByFolderId(folderId) } returns flowOf(emptyList())

        // When
        viewModel.shuffleFolder(folderId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 0) {
            playbackRepository.playQueue(any(), any())
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

        coEvery { playbackRepository.playbackState } returns flowOf(testPlaybackState)

        // Recreate ViewModel with new mock
        viewModel = FolderViewModel(songRepository, playbackRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(testPlaybackState, viewModel.playbackState.value)
    }

    @Test
    fun `onFolderClick does nothing but is available for future use`() {
        // Given
        val folderId = 123L

        // When - Should not throw
        viewModel.onFolderClick(folderId)

        // Then - Just verify it doesn't crash
        // This method is a placeholder for future functionality
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
