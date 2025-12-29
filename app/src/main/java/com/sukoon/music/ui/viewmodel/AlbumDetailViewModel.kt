package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Album Detail screen.
 *
 * Responsibilities:
 * - Load specific album by ID
 * - Load songs in the album
 * - Provide playback actions (play, shuffle, play song)
 * - Handle like/unlike songs
 */
@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playbackRepository: PlaybackRepository
) : ViewModel() {

    private var currentAlbumId: Long = -1

    /**
     * Current album being displayed.
     * Updated when loadAlbum() is called.
     */
    private var _album: StateFlow<Album?> = songRepository.getAlbumById(-1)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    val album: StateFlow<Album?> get() = _album

    /**
     * Songs in the current album.
     * Updated when loadAlbum() is called.
     */
    private var _albumSongs: StateFlow<List<Song>> = songRepository.getSongsByAlbumId(-1)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val albumSongs: StateFlow<List<Song>> get() = _albumSongs

    /**
     * Current playback state for UI feedback.
     */
    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    /**
     * Load album and its songs by ID.
     * Call this from LaunchedEffect when screen opens.
     *
     * @param albumId The unique ID of the album
     */
    fun loadAlbum(albumId: Long) {
        if (currentAlbumId == albumId) return // Already loaded

        currentAlbumId = albumId

        _album = songRepository.getAlbumById(albumId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

        _albumSongs = songRepository.getSongsByAlbumId(albumId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    /**
     * Play all songs in the album from the beginning.
     *
     * @param albumSongs List of songs to play
     */
    fun playAlbum(albumSongs: List<Song>) {
        if (albumSongs.isEmpty()) return
        viewModelScope.launch {
            playbackRepository.playQueue(albumSongs, startIndex = 0)
        }
    }

    /**
     * Shuffle and play all songs in the album.
     *
     * @param albumSongs List of songs to shuffle and play
     */
    fun shuffleAlbum(albumSongs: List<Song>) {
        if (albumSongs.isEmpty()) return
        viewModelScope.launch {
            val shuffledSongs = albumSongs.shuffled()
            playbackRepository.playQueue(shuffledSongs, startIndex = 0)
        }
    }

    /**
     * Play a specific song from the album.
     * Sets the queue to all album songs.
     *
     * @param song The song to play
     * @param albumSongs The full album song list for queue context
     */
    fun playSong(song: Song, albumSongs: List<Song>) {
        viewModelScope.launch {
            val index = albumSongs.indexOf(song)
            if (index >= 0) {
                playbackRepository.playQueue(albumSongs, startIndex = index)
            }
        }
    }

    /**
     * Toggle like status of a song.
     *
     * @param songId The unique ID of the song
     * @param isLiked Current like status (will be toggled)
     */
    fun toggleLike(songId: Long, isLiked: Boolean) {
        viewModelScope.launch {
            songRepository.updateLikeStatus(songId, !isLiked)
        }
    }
}
