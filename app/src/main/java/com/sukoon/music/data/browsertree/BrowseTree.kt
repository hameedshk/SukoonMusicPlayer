package com.sukoon.music.data.browsertree

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.sukoon.music.domain.repository.PlaylistRepository
import com.sukoon.music.domain.repository.SongRepository
import com.sukoon.music.domain.repository.QueueRepository
import com.sukoon.music.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import com.sukoon.music.util.DevLogger

class BrowseTree @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
    private val queueRepository: QueueRepository,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        const val ROOT_ID = "root"
        const val QUEUE_ID = "queue"
        const val NOW_PLAYING_ID = "now_playing"
        const val PLAYLISTS_ID = "playlists"
        const val ALBUMS_ID = "albums"
        const val ARTISTS_ID = "artists"
        const val RECENTLY_PLAYED_ID = "recently_played"
    }

    suspend fun getChildren(parentId: String): List<MediaItem> {
        return try {
            when (parentId) {
                ROOT_ID -> getRootChildren()
                QUEUE_ID -> getQueueChildren()
                PLAYLISTS_ID -> getPlaylistsChildren()
                ALBUMS_ID -> getAlbumsChildren()
                ARTISTS_ID -> getArtistsChildren()
                RECENTLY_PLAYED_ID -> getRecentlyPlayedChildren()
                else -> {
                    if (parentId.startsWith("playlist_")) {
                        getPlaylistSongsChildren(parentId)
                    } else if (parentId.startsWith("album_")) {
                        getAlbumSongsChildren(parentId)
                    } else if (parentId.startsWith("artist_")) {
                        getArtistSongsChildren(parentId)
                    } else {
                        emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            DevLogger.e("BrowseTree", "Browse error for parentId=$parentId", e)
            emptyList()
        }
    }

    private suspend fun getRootChildren(): List<MediaItem> {
        val children = mutableListOf<MediaItem>()

        // Add browsable categories
        children.add(
            MediaItem.Builder()
                .setMediaId(QUEUE_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Queue")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        )

        children.add(
            MediaItem.Builder()
                .setMediaId(PLAYLISTS_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Playlists")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        )

        children.add(
            MediaItem.Builder()
                .setMediaId(ALBUMS_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Albums")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        )

        children.add(
            MediaItem.Builder()
                .setMediaId(ARTISTS_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Artists")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        )

        // Only show Recently Played if NOT in private session
        val userPrefs = preferencesManager.userPreferencesFlow.first()
        if (!userPrefs.isPrivateSessionEnabled) {
            children.add(
                MediaItem.Builder()
                    .setMediaId(RECENTLY_PLAYED_ID)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("Recently Played")
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()
            )
        }

        return children
    }

    private suspend fun getQueueChildren(): List<MediaItem> {
        return try {
            val queueWithSongs = queueRepository.getCurrentQueueWithSongs()
            queueWithSongs?.songs?.map { it.toMediaItem() } ?: emptyList()
        } catch (e: Exception) {
            DevLogger.e("BrowseTree", "Failed to load queue", e)
            emptyList()
        }
    }

    private suspend fun getPlaylistsChildren(): List<MediaItem> {
        return try {
            playlistRepository.getAllPlaylists()
                .first()
                .map { it.toMediaItem() }
        } catch (e: Exception) {
            DevLogger.e("BrowseTree", "Failed to load playlists", e)
            emptyList()
        }
    }

    private suspend fun getAlbumsChildren(): List<MediaItem> {
        return try {
            songRepository.getAllAlbums()
                .first()
                .map { it.toMediaItem() }
        } catch (e: Exception) {
            DevLogger.e("BrowseTree", "Failed to load albums", e)
            emptyList()
        }
    }

    private suspend fun getArtistsChildren(): List<MediaItem> {
        return try {
            songRepository.getAllArtists()
                .first()
                .map { it.toMediaItem() }
        } catch (e: Exception) {
            DevLogger.e("BrowseTree", "Failed to load artists", e)
            emptyList()
        }
    }

    private suspend fun getRecentlyPlayedChildren(): List<MediaItem> {
        return try {
            songRepository.getRecentlyPlayed()
                .first()
                .map { it.toMediaItem() }
        } catch (e: Exception) {
            DevLogger.e("BrowseTree", "Failed to load recently played", e)
            emptyList()
        }
    }

    private suspend fun getPlaylistSongsChildren(playlistId: String): List<MediaItem> {
        return try {
            val id = playlistId.removePrefix("playlist_").toLongOrNull()
            if (id == null) {
                DevLogger.e("BrowseTree", "Invalid playlist ID format: $playlistId")
                return emptyList()
            }
            playlistRepository.getSongsInPlaylist(id)
                .first()
                .map { it.toMediaItem() }
        } catch (e: Exception) {
            DevLogger.e("BrowseTree", "Failed to load playlist songs for $playlistId", e)
            emptyList()
        }
    }

    private suspend fun getAlbumSongsChildren(albumId: String): List<MediaItem> {
        return try {
            val id = albumId.removePrefix("album_").toLongOrNull()
            if (id == null) {
                DevLogger.e("BrowseTree", "Invalid album ID format: $albumId")
                return emptyList()
            }
            songRepository.getSongsByAlbumId(id)
                .first()
                .map { it.toMediaItem() }
        } catch (e: Exception) {
            DevLogger.e("BrowseTree", "Failed to load album songs for $albumId", e)
            emptyList()
        }
    }

    private suspend fun getArtistSongsChildren(artistId: String): List<MediaItem> {
        return try {
            val id = artistId.removePrefix("artist_").toLongOrNull()
            if (id == null) {
                DevLogger.e("BrowseTree", "Invalid artist ID format: $artistId")
                return emptyList()
            }
            songRepository.getSongsByArtistId(id)
                .first()
                .map { it.toMediaItem() }
        } catch (e: Exception) {
            DevLogger.e("BrowseTree", "Failed to load artist songs for $artistId", e)
            emptyList()
        }
    }
}
