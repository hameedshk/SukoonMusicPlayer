package com.sukoon.music.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.PlaybackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.sukoon.music.ui.theme.*

/**
 * Handler for song context menu actions.
 * Centralizes all song-related actions to avoid passing multiple callbacks.
 */
class SongMenuHandler(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val playbackRepository: PlaybackRepository,
    private val onNavigateToAlbum: (Long) -> Unit = {},
    private val onNavigateToAlbumBySong: ((Song) -> Unit)? = null,
    private val onNavigateToArtist: (Long) -> Unit = {},
    private val onNavigateToArtistBySong: ((Song) -> Unit)? = null,
    private val onShowPlaylistSelector: (Song) -> Unit = {},
    private val onShowDeleteConfirmation: (Song) -> Unit = {},
    private val onShowEditTags: (Song) -> Unit = {},
    private val onShowChangeCover: (Song) -> Unit = {},
    private val onShowSetRingtone: (Song) -> Unit = {},
    private val onShowEditAudio: (Song) -> Unit = {},
    private val onShowUpdateLyrics: (Song) -> Unit = {},
    private val onShowSongInfo: (Song) -> Unit = {},
    private val onToggleLike: (Long, Boolean) -> Unit = { _, _ -> },
    private val onShare: (Song) -> Unit = {}
) {

    fun handleSetAsRingtone(song: Song) {
        // TODO: Implement ringtone setting
        // Requires WRITE_SETTINGS permission and RingtoneManager
        onShowSetRingtone(song)
    }

    fun handleChangeCover(song: Song) {
        // TODO: Implement cover art selection
        // Requires file picker and metadata writing
        onShowChangeCover(song)
    }

    fun handleEditTags(song: Song) {
        // TODO: Implement tag editing dialog/screen
        onShowEditTags(song)
    }

    fun handlePlayNext(song: Song) {
        coroutineScope.launch {
            playbackRepository.playNext(song)
        }
    }

    fun handleAddToQueue(song: Song) {
        coroutineScope.launch {
            playbackRepository.addToQueue(song)
        }
    }

    fun handleAddToPlaylist(song: Song) {
        // TODO: Implement playlist selection dialog
        onShowPlaylistSelector(song)
    }

    fun handleGoToAlbum(song: Song) {
        onNavigateToAlbumBySong?.let { navigateBySong ->
            navigateBySong(song)
            return
        }
        val albumId = song.album.hashCode().toLong()
        onNavigateToAlbum(albumId)
    }

    fun handleGoToArtist(song: Song) {
        onNavigateToArtistBySong?.let { navigateBySong ->
            navigateBySong(song)
            return
        }
        val artistId = song.artist.hashCode().toLong()
        onNavigateToArtist(artistId)
    }

    fun handleToggleLike(song: Song, isLiked: Boolean) {
        onToggleLike(song.id, isLiked)
    }

    fun handleShare(song: Song) {
        onShare(song)
    }

    fun handleEditAudio(song: Song) {
        // TODO: Determine what "Edit audio" should do
        // Possibly opens equalizer or audio effects
        onShowEditAudio(song)
    }

    fun handleUpdateLyrics(song: Song) {
        // TODO: Implement lyrics update dialog
        onShowUpdateLyrics(song)
    }

    fun handleShowSongInfo(song: Song) {
        onShowSongInfo(song)
    }
    fun handleDeleteFromDevice(song: Song) {
        // Show confirmation dialog before deleting
        onShowDeleteConfirmation(song)
    }

    /**
     * Actually perform the deletion after user confirms.
     */
    fun performDelete(song: Song): DeleteHelper.DeleteResult {
        return DeleteHelper.deleteSongs(context, listOf(song))
    }
}

/**
 * Composable to remember a SongMenuHandler instance.
 */
@Composable
fun rememberSongMenuHandler(
    playbackRepository: PlaybackRepository,
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToAlbumBySong: ((Song) -> Unit)? = null,
    onNavigateToArtist: (Long) -> Unit = {},
    onNavigateToArtistBySong: ((Song) -> Unit)? = null,
    onShowPlaylistSelector: (Song) -> Unit = {},
    onShowDeleteConfirmation: (Song) -> Unit = {},
    onShowEditTags: (Song) -> Unit = {},
    onShowChangeCover: (Song) -> Unit = {},
    onShowSetRingtone: (Song) -> Unit = {},
    onShowEditAudio: (Song) -> Unit = {},
    onShowUpdateLyrics: (Song) -> Unit = {},
    onShowSongInfo: (Song) -> Unit = {},
    onToggleLike: (Long, Boolean) -> Unit = { _, _ -> },
    onShare: (Song) -> Unit = {}
): SongMenuHandler {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    return remember(
        playbackRepository,
        onNavigateToAlbum,
        onNavigateToAlbumBySong,
        onNavigateToArtist,
        onNavigateToArtistBySong,
        onShowPlaylistSelector,
        onShowDeleteConfirmation,
        onShowSongInfo,
        onToggleLike,
        onShare
    ) {
        SongMenuHandler(
            context = context,
            coroutineScope = coroutineScope,
            playbackRepository = playbackRepository,
            onNavigateToAlbum = onNavigateToAlbum,
            onNavigateToAlbumBySong = onNavigateToAlbumBySong,
            onNavigateToArtist = onNavigateToArtist,
            onNavigateToArtistBySong = onNavigateToArtistBySong,
            onShowPlaylistSelector = onShowPlaylistSelector,
            onShowDeleteConfirmation = onShowDeleteConfirmation,
            onShowEditTags = onShowEditTags,
            onShowChangeCover = onShowChangeCover,
            onShowSetRingtone = onShowSetRingtone,
            onShowEditAudio = onShowEditAudio,
            onShowUpdateLyrics = onShowUpdateLyrics,
            onShowSongInfo = onShowSongInfo,
            onToggleLike = onToggleLike,
            onShare = onShare
        )
    }
}
