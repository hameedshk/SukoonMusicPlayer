package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Album Detail screen.
 */
@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val songRepository: SongRepository,
    val playbackRepository: PlaybackRepository,
    val adMobManager: com.sukoon.music.data.ads.AdMobManager
) : ViewModel() {

    private val _albumId = MutableStateFlow<Long>(-1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val album: StateFlow<Album?> = _albumId
        .flatMapLatest { id ->
            if (id == -1L) flowOf(null)
            else songRepository.getAlbumById(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val albumSongs: StateFlow<List<Song>> = _albumId
        .flatMapLatest { id ->
            if (id == -1L) flowOf(emptyList())
            else songRepository.getSongsByAlbumId(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    fun loadAlbum(albumId: Long) {
        _albumId.value = albumId
    }

    fun playPause() {
        viewModelScope.launch {
            playbackRepository.playPause()
        }
    }

    fun seekToNext() {
        viewModelScope.launch {
            playbackRepository.seekToNext()
        }
    }

    /**
     * Play all songs in the album from the beginning.
     */
    fun playAlbum(albumSongs: List<Song>) {
        if (albumSongs.isEmpty()) return
        viewModelScope.launch {
            playbackRepository.playQueue(albumSongs, startIndex = 0)
        }
    }

    /**
     * Shuffle and play all songs in the album.
     */
    fun shuffleAlbum(albumSongs: List<Song>) {
        if (albumSongs.isEmpty()) return
        viewModelScope.launch {
            playbackRepository.setShuffleEnabled(true)
            playbackRepository.playQueue(albumSongs, startIndex = 0)
        }
    }

    /**
     * Play a specific song from the album.
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
     */
    fun toggleLike(songId: Long, isLiked: Boolean) {
        viewModelScope.launch {
            songRepository.updateLikeStatus(songId, !isLiked)
        }
    }
}
