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

enum class AlbumSongSortMode {
    TRACK_NUMBER,
    TITLE,
    ARTIST,
    DURATION
}

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
    private val _sortMode = MutableStateFlow(AlbumSongSortMode.TRACK_NUMBER)
    val sortMode: StateFlow<AlbumSongSortMode> = _sortMode.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSongIds: StateFlow<Set<Long>> = _selectedSongIds.asStateFlow()

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
    val albumSongs: StateFlow<List<Song>> = combine(
        _albumId,
        _sortMode
    ) { id, sortMode ->
        if (id == -1L) emptyList()
        else {
            val songs = songRepository.getSongsByAlbumId(id).firstOrNull() ?: emptyList()
            when (sortMode) {
                AlbumSongSortMode.TRACK_NUMBER -> songs // Keep original order (usually track number)
                AlbumSongSortMode.TITLE -> songs.sortedBy { it.title.lowercase() }
                AlbumSongSortMode.ARTIST -> songs.sortedBy { it.artist.lowercase() }
                AlbumSongSortMode.DURATION -> songs.sortedByDescending { it.duration }
            }
        }
    }.stateIn(
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
            playbackRepository.shuffleAndPlayQueue(albumSongs)
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

    fun setSortMode(mode: AlbumSongSortMode) {
        _sortMode.value = mode
    }

    fun toggleSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) {
            _selectedSongIds.value = emptySet()
        }
    }

    fun toggleSongSelection(songId: Long) {
        val current = _selectedSongIds.value
        _selectedSongIds.value = if (current.contains(songId)) {
            current - songId
        } else {
            current + songId
        }
    }

    fun selectAllSongs(songs: List<Song>) {
        _selectedSongIds.value = songs.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedSongIds.value = emptySet()
    }

    fun playSelectedSongs(allSongs: List<Song>) {
        val ids = _selectedSongIds.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            val selectedSongs = allSongs.filter { ids.contains(it.id) }
            if (selectedSongs.isNotEmpty()) {
                playbackRepository.playQueue(selectedSongs, startIndex = 0)
                toggleSelectionMode(false)
            }
        }
    }

    fun addSelectedToQueue(allSongs: List<Song>) {
        val ids = _selectedSongIds.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            val selectedSongs = allSongs.filter { ids.contains(it.id) }
            selectedSongs.forEach { song ->
                playbackRepository.addToQueue(song)
            }
            toggleSelectionMode(false)
        }
    }

    fun deleteSelectedSongs() {
        val ids = _selectedSongIds.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            // TODO: Implement delete functionality
            toggleSelectionMode(false)
        }
    }
}
