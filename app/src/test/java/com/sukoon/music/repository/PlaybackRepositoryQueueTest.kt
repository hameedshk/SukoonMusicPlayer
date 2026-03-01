package com.sukoon.music.repository

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import com.sukoon.music.data.local.dao.SongAudioSettingsDao
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.data.repository.PlaybackRepositoryImpl
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.SongRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackRepositoryQueueTest {

    private lateinit var context: Context
    private lateinit var songRepository: SongRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var songAudioSettingsDao: SongAudioSettingsDao
    private lateinit var repository: PlaybackRepositoryImpl
    private lateinit var mediaController: MediaController
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        songRepository = mockk(relaxed = true)
        preferencesManager = mockk(relaxed = true)
        songAudioSettingsDao = mockk(relaxed = true)
        mediaController = mockk(relaxed = true)
        coEvery { songAudioSettingsDao.getSettings(any()) } returns null
        
        val testDispatcher = UnconfinedTestDispatcher()
        testScope = TestScope(testDispatcher)

        repository = PlaybackRepositoryImpl(
            context = context,
            scope = testScope,
            songRepository = songRepository,
            preferencesManager = preferencesManager,
            queueRepository = mockk(relaxed = true),
            listeningStatsRepository = mockk(relaxed = true),
            songAudioSettingsDao = songAudioSettingsDao,
            sessionController = mockk(relaxed = true)
        )

        // Inject mock mediaController using reflection since it's private
        val field: Field = PlaybackRepositoryImpl::class.java.getDeclaredField("mediaController")
        field.isAccessible = true
        field.set(repository, mediaController)
    }

    @Test
    fun `playNext inserts single song after current index`() = runTest {
        // Given
        every { mediaController.currentMediaItemIndex } returns 5
        every { mediaController.mediaItemCount } returns 10
        val song = createTestSong(100)

        // When
        repository.playNext(song)

        // Then
        verify { mediaController.addMediaItem(6, any<MediaItem>()) }
    }

    @Test
    fun `playNext inserts multiple songs after current index`() = runTest {
        // Given
        every { mediaController.currentMediaItemIndex } returns 2
        every { mediaController.mediaItemCount } returns 10
        val songs = listOf(createTestSong(101), createTestSong(102))

        // When
        repository.playNext(songs)

        // Then
        verify { mediaController.addMediaItems(3, any<List<MediaItem>>()) }
    }

    @Test
    fun `playNext handles empty queue (currentIndex -1)`() = runTest {
        // Given
        every { mediaController.currentMediaItemIndex } returns -1
        val song = createTestSong(103)

        // When
        repository.playNext(song)

        // Then
        verify { mediaController.addMediaItem(0, any<MediaItem>()) }
    }

    @Test
    fun `addToQueue appends multiple songs to the end`() = runTest {
        // Given
        val songs = listOf(createTestSong(201), createTestSong(202))

        // When
        repository.addToQueue(songs)

        // Then
        verify { mediaController.addMediaItems(any<List<MediaItem>>()) }
    }

    private fun createTestSong(id: Long): Song {
        return Song(
            id = id,
            title = "Song $id",
            artist = "Artist",
            album = "Album",
            duration = 180000L,
            uri = "content://media/$id",
            albumArtUri = null,
            dateAdded = 0L,
            isLiked = false
        )
    }
}
