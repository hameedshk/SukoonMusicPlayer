package com.sukoon.music.integration

import com.sukoon.music.data.local.dao.SongDao
import com.sukoon.music.data.local.entity.SongEntity
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.data.repository.SongRepositoryImpl
import com.sukoon.music.domain.model.UserPreferences
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration tests verifying the flow between UserPreferences and SongRepository.
 * Covers filtering and folder exclusion business logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FolderIntegrationTest {

    private lateinit var songDao: SongDao
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var repository: SongRepositoryImpl
    private val preferencesFlow = MutableStateFlow(UserPreferences())

    @Before
    fun setup() {
        songDao = mockk()
        preferencesManager = mockk()
        
        // Mock preferences behavior
        coEvery { preferencesManager.userPreferencesFlow } returns preferencesFlow

        val testDispatcher = UnconfinedTestDispatcher()
        val testScope = TestScope(testDispatcher)

        repository = SongRepositoryImpl(
            songDao = songDao,
            recentlyPlayedDao = mockk(relaxed = true),
            recentlyPlayedArtistDao = mockk(relaxed = true),
            recentlyPlayedAlbumDao = mockk(relaxed = true),
            playlistDao = mockk(relaxed = true),
            genreCoverDao = mockk(relaxed = true),
            mediaStoreScanner = mockk(relaxed = true),
            preferencesManager = preferencesManager,
            scope = testScope
        )
    }

    /**
     * Verifies that updating the minimum duration preference correctly filters
     * songs in the repository layer.
     */
    @Test
    fun `filtering flow - minimum duration affects folder contents`() = runTest {
        // 1. Setup songs with durations: 10s, 30s, 60s
        val songs = listOf(
            createSong(1, "10s", "/Music", 10000L),
            createSong(2, "30s", "/Music", 30000L),
            createSong(3, "60s", "/Music", 60000L)
        )
        coEvery { songDao.getAllSongs() } returns MutableStateFlow(songs)

        // 2. Set minimum to 30s via the preferences flow
        preferencesFlow.value = UserPreferences(minimumAudioDuration = 30)

        // 3. Verify only >=30s appear in the processed folders
        val folders = repository.getAllFolders().first()
        val musicFolder = folders.find { it.name == "Music" }
        assertNotNull(musicFolder)
        assertEquals(2, musicFolder?.songCount)
    }

    /**
     * Verifies that adding and removing folder paths from the exclusion list
     * correctly toggles their visibility in the library.
     */
    @Test
    fun `exclusion flow - adding and removing exclusions toggles folder visibility`() = runTest {
        // 1. Setup songs in folders A, B, C
        val songs = listOf(
            createSong(1, "A1", "/A"),
            createSong(2, "B1", "/B"),
            createSong(3, "C1", "/C")
        )
        coEvery { songDao.getAllSongs() } returns MutableStateFlow(songs)

        // 2. Exclude folder B
        preferencesFlow.value = UserPreferences(excludedFolderPaths = setOf("/B"))

        // 3. Verify getAllFolders returns A, C only
        var folders = repository.getAllFolders().first()
        assertEquals(2, folders.size)
        assertTrue(folders.any { it.name == "A" })
        assertTrue(folders.any { it.name == "C" })
        assertTrue(folders.none { it.name == "B" })

        // 4. Remove exclusion for folder B
        preferencesFlow.value = UserPreferences(excludedFolderPaths = emptySet())

        // 5. Verify all folders appear again
        folders = repository.getAllFolders().first()
        assertEquals(3, folders.size)
        assertTrue(folders.any { it.name == "B" })
    }

    private fun createSong(id: Long, title: String, path: String, duration: Long = 180000L): SongEntity {
        return SongEntity(
            id = id,
            title = title,
            artist = "Artist",
            album = "Album",
            duration = duration,
            uri = "uri/$id",
            albumArtUri = null,
            dateAdded = 0L,
            isLiked = false,
            folderPath = path,
            genre = "Unknown Genre",
            year = 0,
            size = 0,
            playCount = 0
        )
    }
}
