package com.sukoon.music.repository

import com.sukoon.music.data.local.dao.RecentlyPlayedDao
import com.sukoon.music.data.local.dao.SongDao
import com.sukoon.music.data.local.entity.SongEntity
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.data.repository.SongRepositoryImpl
import com.sukoon.music.data.source.MediaStoreScanner
import com.sukoon.music.domain.model.FolderSortMode
import com.sukoon.music.domain.model.UserPreferences
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for folder functionality, filtering, and sorting in SongRepositoryImpl.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SongRepositoryFolderTest {

    private lateinit var songDao: SongDao
    private lateinit var recentlyPlayedDao: RecentlyPlayedDao
    private lateinit var mediaStoreScanner: MediaStoreScanner
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var repository: SongRepositoryImpl
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        songDao = mockk(relaxed = true)
        recentlyPlayedDao = mockk(relaxed = true)
        mediaStoreScanner = mockk(relaxed = true)
        preferencesManager = mockk(relaxed = true)

        val testDispatcher = UnconfinedTestDispatcher()
        testScope = TestScope(testDispatcher)

        repository = SongRepositoryImpl(
            songDao = songDao,
            recentlyPlayedDao = recentlyPlayedDao,
            recentlyPlayedArtistDao = mockk(relaxed = true),
            recentlyPlayedAlbumDao = mockk(relaxed = true),
            playlistDao = mockk(relaxed = true),
            genreCoverDao = mockk(relaxed = true),
            mediaStoreScanner = mediaStoreScanner,
            preferencesManager = preferencesManager,
            scope = testScope
        )

        // Default preferences
        coEvery { preferencesManager.userPreferencesFlow } returns flowOf(UserPreferences())
    }

    @Test
    fun `getAllFolders groups songs by folder path correctly`() = runTest {
        val songs = listOf(
            createSongEntity(1, "Song 1", "/storage/music/Rock"),
            createSongEntity(2, "Song 2", "/storage/music/Rock"),
            createSongEntity(3, "Song 3", "/storage/music/Jazz"),
            createSongEntity(4, "Song 4", "/storage/music/Pop")
        )
        coEvery { songDao.getAllSongs() } returns flowOf(songs)

        val folders = repository.getAllFolders().first()

        assertEquals(3, folders.size)
        assertTrue(folders.any { it.name == "Rock" && it.songCount == 2 })
    }

    @Test
    fun `getAllFolders filters songs by minimum duration`() = runTest {
        // Given - Minimum duration set to 60s (60000ms)
        coEvery { preferencesManager.userPreferencesFlow } returns flowOf(
            UserPreferences(minimumAudioDuration = 60)
        )

        val songs = listOf(
            createSongEntity(1, "Long Song", "/storage/music/Rock", duration = 120000L), // 120s
            createSongEntity(2, "Short Song", "/storage/music/Rock", duration = 30000L)   // 30s
        )
        coEvery { songDao.getAllSongs() } returns flowOf(songs)

        // When
        val folders = repository.getAllFolders().first()

        // Then
        val rockFolder = folders.find { it.name == "Rock" }
        assertNotNull(rockFolder)
        assertEquals(1L, rockFolder?.songCount?.toLong())
    }

    @Test
    fun `getAllFolders excludes folders in exclusion list`() = runTest {
        // Given
        val excludedPath = "/storage/music/Excluded"
        coEvery { preferencesManager.userPreferencesFlow } returns flowOf(
            UserPreferences(excludedFolderPaths = setOf(excludedPath))
        )

        val songs = listOf(
            createSongEntity(1, "Normal Song", "/storage/music/Normal"),
            createSongEntity(2, "Hidden Song", excludedPath)
        )
        coEvery { songDao.getAllSongs() } returns flowOf(songs)

        // When
        val folders = repository.getAllFolders().first()

        // Then
        assertEquals(1, folders.size)
        assertEquals("Normal", folders[0].name)
        assertTrue(folders.none { it.path == excludedPath })
    }

    @Test
    fun `getAllFolders sorts by NAME_DESC correctly`() = runTest {
        coEvery { preferencesManager.userPreferencesFlow } returns flowOf(
            UserPreferences(folderSortMode = FolderSortMode.NAME_DESC)
        )

        val songs = listOf(
            createSongEntity(1, "A", "/A"),
            createSongEntity(2, "B", "/B"),
            createSongEntity(3, "C", "/C")
        )
        coEvery { songDao.getAllSongs() } returns flowOf(songs)

        val folders = repository.getAllFolders().first()

        assertEquals("C", folders[0].name)
        assertEquals("B", folders[1].name)
        assertEquals("A", folders[2].name)
    }

    @Test
    fun `getAllFolders sorts by TRACK_COUNT correctly`() = runTest {
        coEvery { preferencesManager.userPreferencesFlow } returns flowOf(
            UserPreferences(folderSortMode = FolderSortMode.TRACK_COUNT)
        )

        val songs = listOf(
            createSongEntity(1, "S1", "/Many"),
            createSongEntity(2, "S2", "/Many"),
            createSongEntity(3, "S3", "/Few")
        )
        coEvery { songDao.getAllSongs() } returns flowOf(songs)

        val folders = repository.getAllFolders().first()

        assertEquals("Many", folders[0].name)
        assertEquals(2, folders[0].songCount)
        assertEquals("Few", folders[1].name)
    }

    @Test
    fun `getAllFolders sorts by DURATION correctly`() = runTest {
        coEvery { preferencesManager.userPreferencesFlow } returns flowOf(
            UserPreferences(folderSortMode = FolderSortMode.DURATION)
        )

        val songs = listOf(
            createSongEntity(1, "S1", "/Long", duration = 1000000L),
            createSongEntity(2, "S2", "/Short", duration = 10000L)
        )
        coEvery { songDao.getAllSongs() } returns flowOf(songs)

        val folders = repository.getAllFolders().first()

        assertEquals("Long", folders[0].name)
        assertEquals("Short", folders[1].name)
    }

    private fun createSongEntity(
        id: Long,
        title: String,
        folderPath: String?,
        duration: Long = 180000L,
        dateAdded: Long = System.currentTimeMillis()
    ): SongEntity {
        return SongEntity(
            id = id,
            title = title,
            artist = "Artist",
            album = "Album",
            duration = duration,
            uri = "uri/$id",
            albumArtUri = null,
            dateAdded = dateAdded,
            isLiked = false,
            folderPath = folderPath,
            genre = "Unknown Genre",
            year = 0,
            size = 0,
            playCount = 0
        )
    }
}
