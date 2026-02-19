package com.sukoon.music.data.browsertree

import com.sukoon.music.domain.repository.PlaylistRepository
import com.sukoon.music.domain.repository.SongRepository
import com.sukoon.music.domain.repository.QueueRepository
import com.sukoon.music.data.preferences.PreferencesManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class BrowseTreeTest {

    private val playlistRepository = mockk<PlaylistRepository>()
    private val songRepository = mockk<SongRepository>()
    private val queueRepository = mockk<QueueRepository>()
    private val preferencesManager = mockk<PreferencesManager>()

    private val browseTree = BrowseTree(
        playlistRepository,
        songRepository,
        queueRepository,
        preferencesManager
    )

    @Test
    fun `getRootChildren includes Queue, Playlists, Albums, Artists when not in private session`() = runTest {
        coEvery { preferencesManager.isPrivateSession() } returns false

        val children = browseTree.getChildren(BrowseTree.ROOT_ID)

        assertEquals(5, children.size, "Should have 5 root categories when not in private session")
        assertEquals("Queue", children[0].mediaMetadata?.title)
        assertEquals("Playlists", children[1].mediaMetadata?.title)
        assertEquals("Albums", children[2].mediaMetadata?.title)
        assertEquals("Artists", children[3].mediaMetadata?.title)
        assertEquals("Recently Played", children[4].mediaMetadata?.title)
    }

    @Test
    fun `getRootChildren excludes Recently Played in private session`() = runTest {
        coEvery { preferencesManager.isPrivateSession() } returns true

        val children = browseTree.getChildren(BrowseTree.ROOT_ID)

        assertEquals(4, children.size, "Should have 4 root categories in private session")
        val titles = children.map { it.mediaMetadata?.title?.toString() }
        assertEquals(listOf("Queue", "Playlists", "Albums", "Artists"), titles)
    }

    @Test
    fun `getRootChildren returns empty list on error`() = runTest {
        coEvery { preferencesManager.isPrivateSession() } throws Exception("Database error")

        val children = browseTree.getChildren(BrowseTree.ROOT_ID)

        assertEquals(0, children.size, "Should return empty list on error")
    }

    @Test
    fun `getQueueChildren returns empty list when queue is null`() = runTest {
        coEvery { queueRepository.getCurrentQueueWithSongs() } returns null

        val children = browseTree.getChildren(BrowseTree.QUEUE_ID)

        assertEquals(0, children.size, "Should return empty list when queue is null")
    }

    @Test
    fun `getPlaylistsChildren returns empty list on error`() = runTest {
        coEvery { playlistRepository.getAllPlaylists() } throws Exception("Database error")

        val children = browseTree.getChildren(BrowseTree.PLAYLISTS_ID)

        assertEquals(0, children.size, "Should return empty list on error")
    }

    @Test
    fun `getAlbumsChildren returns empty list on error`() = runTest {
        coEvery { songRepository.getAllAlbums() } throws Exception("Database error")

        val children = browseTree.getChildren(BrowseTree.ALBUMS_ID)

        assertEquals(0, children.size, "Should return empty list on error")
    }

    @Test
    fun `getArtistsChildren returns empty list on error`() = runTest {
        coEvery { songRepository.getAllArtists() } throws Exception("Database error")

        val children = browseTree.getChildren(BrowseTree.ARTISTS_ID)

        assertEquals(0, children.size, "Should return empty list on error")
    }

    @Test
    fun `getRecentlyPlayedChildren returns empty list on error`() = runTest {
        coEvery { songRepository.getRecentlyPlayed() } throws Exception("Database error")

        val children = browseTree.getChildren(BrowseTree.RECENTLY_PLAYED_ID)

        assertEquals(0, children.size, "Should return empty list on error")
    }

    @Test
    fun `getChildren returns empty list for invalid parentId`() = runTest {
        val children = browseTree.getChildren("invalid_parent_id")

        assertEquals(0, children.size, "Should return empty list for invalid parent ID")
    }

    @Test
    fun `getPlaylistSongsChildren returns empty list on invalid playlist ID format`() = runTest {
        val children = browseTree.getChildren("playlist_invalid")

        assertEquals(0, children.size, "Should return empty list on invalid ID format")
    }

    @Test
    fun `getAlbumSongsChildren returns empty list on invalid album ID format`() = runTest {
        val children = browseTree.getChildren("album_invalid")

        assertEquals(0, children.size, "Should return empty list on invalid ID format")
    }

    @Test
    fun `getArtistSongsChildren returns empty list on invalid artist ID format`() = runTest {
        val children = browseTree.getChildren("artist_invalid")

        assertEquals(0, children.size, "Should return empty list on invalid ID format")
    }
}
