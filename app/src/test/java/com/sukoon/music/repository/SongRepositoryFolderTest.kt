package com.sukoon.music.repository

import com.sukoon.music.data.local.dao.RecentlyPlayedDao
import com.sukoon.music.data.local.dao.SongDao
import com.sukoon.music.data.local.entity.SongEntity
import com.sukoon.music.data.repository.SongRepositoryImpl
import com.sukoon.music.data.source.MediaStoreScanner
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for folder functionality in SongRepositoryImpl.
 *
 * Tests the following methods:
 * - getAllFolders()
 * - getFolderById()
 * - getSongsByFolderId()
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SongRepositoryFolderTest {

    private lateinit var songDao: SongDao
    private lateinit var recentlyPlayedDao: RecentlyPlayedDao
    private lateinit var mediaStoreScanner: MediaStoreScanner
    private lateinit var repository: SongRepositoryImpl
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        songDao = mockk(relaxed = true)
        recentlyPlayedDao = mockk(relaxed = true)
        mediaStoreScanner = mockk(relaxed = true)

        val testDispatcher = UnconfinedTestDispatcher()
        testScope = TestScope(testDispatcher)

        repository = SongRepositoryImpl(
            songDao = songDao,
            recentlyPlayedDao = recentlyPlayedDao,
            mediaStoreScanner = mediaStoreScanner,
            scope = testScope
        )
    }

    @Test
    fun `getAllFolders groups songs by folder path correctly`() = runTest {
        // Given - Songs in different folders
        val songs = listOf(
            createSongEntity(1, "Song 1", "/storage/music/Rock"),
            createSongEntity(2, "Song 2", "/storage/music/Rock"),
            createSongEntity(3, "Song 3", "/storage/music/Jazz"),
            createSongEntity(4, "Song 4", "/storage/music/Pop"),
            createSongEntity(5, "Song 5", null) // Song without folder path
        )

        coEvery { songDao.getAllSongs() } returns flowOf(songs)

        // When
        val folders = repository.getAllFolders().first()

        // Then
        assertEquals(3, folders.size, "Should have 3 folders (excluding songs without folder path)")

        // Verify folder details
        val rockFolder = folders.find { it.name == "Rock" }
        assertNotNull(rockFolder, "Rock folder should exist")
        assertEquals(2, rockFolder.songCount, "Rock folder should have 2 songs")
        assertEquals("/storage/music/Rock", rockFolder.path)

        val jazzFolder = folders.find { it.name == "Jazz" }
        assertNotNull(jazzFolder, "Jazz folder should exist")
        assertEquals(1, jazzFolder.songCount, "Jazz folder should have 1 song")

        val popFolder = folders.find { it.name == "Pop" }
        assertNotNull(popFolder, "Pop folder should exist")
        assertEquals(1, popFolder.songCount, "Pop folder should have 1 song")
    }

    @Test
    fun `getAllFolders filters out songs without folder paths`() = runTest {
        // Given - Songs all without folder paths
        val songs = listOf(
            createSongEntity(1, "Song 1", null),
            createSongEntity(2, "Song 2", null),
            createSongEntity(3, "Song 3", "")
        )

        coEvery { songDao.getAllSongs() } returns flowOf(songs)

        // When
        val folders = repository.getAllFolders().first()

        // Then
        assertEquals(0, folders.size, "Should have no folders when all songs lack folder paths")
    }

    @Test
    fun `getAllFolders calculates total duration correctly`() = runTest {
        // Given - Songs with different durations
        val songs = listOf(
            createSongEntity(1, "Song 1", "/storage/music/Rock", duration = 180000L), // 3 minutes
            createSongEntity(2, "Song 2", "/storage/music/Rock", duration = 240000L), // 4 minutes
            createSongEntity(3, "Song 3", "/storage/music/Jazz", duration = 300000L)  // 5 minutes
        )

        coEvery { songDao.getAllSongs() } returns flowOf(songs)

        // When
        val folders = repository.getAllFolders().first()

        // Then
        val rockFolder = folders.find { it.name == "Rock" }
        assertNotNull(rockFolder)
        assertEquals(420000L, rockFolder.totalDuration, "Rock folder total duration should be 7 minutes")

        val jazzFolder = folders.find { it.name == "Jazz" }
        assertNotNull(jazzFolder)
        assertEquals(300000L, jazzFolder.totalDuration, "Jazz folder total duration should be 5 minutes")
    }

    @Test
    fun `getFolderById returns correct folder`() = runTest {
        // Given
        val songs = listOf(
            createSongEntity(1, "Song 1", "/storage/music/Rock"),
            createSongEntity(2, "Song 2", "/storage/music/Rock"),
            createSongEntity(3, "Song 3", "/storage/music/Jazz")
        )

        coEvery { songDao.getAllSongs() } returns flowOf(songs)

        // When - Get all folders first to find the ID
        val allFolders = repository.getAllFolders().first()
        val rockFolder = allFolders.find { it.name == "Rock" }!!

        // Then - Get folder by ID
        val retrievedFolder = repository.getFolderById(rockFolder.id).first()

        assertNotNull(retrievedFolder, "Should find folder by ID")
        assertEquals(rockFolder.id, retrievedFolder.id)
        assertEquals("Rock", retrievedFolder.name)
        assertEquals(2, retrievedFolder.songCount)
    }

    @Test
    fun `getFolderById returns null for non-existent folder`() = runTest {
        // Given
        val songs = listOf(
            createSongEntity(1, "Song 1", "/storage/music/Rock")
        )

        coEvery { songDao.getAllSongs() } returns flowOf(songs)

        // When
        val folder = repository.getFolderById(999999L).first()

        // Then
        assertNull(folder, "Should return null for non-existent folder ID")
    }

    @Test
    fun `getSongsByFolderId returns correct songs`() = runTest {
        // Given
        val songs = listOf(
            createSongEntity(1, "Rock Song 1", "/storage/music/Rock"),
            createSongEntity(2, "Rock Song 2", "/storage/music/Rock"),
            createSongEntity(3, "Jazz Song", "/storage/music/Jazz")
        )

        coEvery { songDao.getAllSongs() } returns flowOf(songs)

        // When - Get folder ID first
        val allFolders = repository.getAllFolders().first()
        val rockFolder = allFolders.find { it.name == "Rock" }!!
        val rockSongs = repository.getSongsByFolderId(rockFolder.id).first()

        // Then
        assertEquals(2, rockSongs.size, "Should return 2 songs in Rock folder")
        assertEquals("Rock Song 1", rockSongs[0].title)
        assertEquals("Rock Song 2", rockSongs[1].title)
    }

    @Test
    fun `getSongsByFolderId returns empty list for non-existent folder`() = runTest {
        // Given
        val songs = listOf(
            createSongEntity(1, "Song 1", "/storage/music/Rock")
        )

        coEvery { songDao.getAllSongs() } returns flowOf(songs)

        // When
        val folderSongs = repository.getSongsByFolderId(999999L).first()

        // Then
        assertEquals(0, folderSongs.size, "Should return empty list for non-existent folder")
    }

    @Test
    fun `folder name is extracted correctly from path`() = runTest {
        // Given - Songs with various path depths
        val songs = listOf(
            createSongEntity(1, "Song 1", "/storage/emulated/0/Music/Rock"),
            createSongEntity(2, "Song 2", "/Music"),
            createSongEntity(3, "Song 3", "/storage/music/deep/nested/folder/Jazz")
        )

        coEvery { songDao.getAllSongs() } returns flowOf(songs)

        // When
        val folders = repository.getAllFolders().first()

        // Then
        assertEquals(3, folders.size)

        val rockFolder = folders.find { it.path == "/storage/emulated/0/Music/Rock" }
        assertEquals("Rock", rockFolder?.name)

        val musicFolder = folders.find { it.path == "/Music" }
        assertEquals("Music", musicFolder?.name)

        val jazzFolder = folders.find { it.path == "/storage/music/deep/nested/folder/Jazz" }
        assertEquals("Jazz", jazzFolder?.name)
    }

    @Test
    fun `folder ID is consistent for same path`() = runTest {
        // Given
        val songs = listOf(
            createSongEntity(1, "Song 1", "/storage/music/Rock"),
            createSongEntity(2, "Song 2", "/storage/music/Rock")
        )

        coEvery { songDao.getAllSongs() } returns flowOf(songs)

        // When - Get folders twice
        val folders1 = repository.getAllFolders().first()
        val folders2 = repository.getAllFolders().first()

        // Then - IDs should be the same (based on path hash)
        val rockFolder1 = folders1.find { it.name == "Rock" }!!
        val rockFolder2 = folders2.find { it.name == "Rock" }!!

        assertEquals(rockFolder1.id, rockFolder2.id, "Folder ID should be consistent across calls")
    }

    // Helper function to create test song entities
    private fun createSongEntity(
        id: Long,
        title: String,
        folderPath: String?,
        duration: Long = 180000L
    ): SongEntity {
        return SongEntity(
            id = id,
            title = title,
            artist = "Test Artist",
            album = "Test Album",
            duration = duration,
            uri = "content://media/external/audio/media/$id",
            albumArtUri = null,
            dateAdded = System.currentTimeMillis(),
            isLiked = false,
            folderPath = folderPath
        )
    }
}
