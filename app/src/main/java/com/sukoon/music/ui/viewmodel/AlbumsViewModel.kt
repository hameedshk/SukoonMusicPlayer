package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AlbumSortMode {
    ALBUM_NAME,
    ARTIST_NAME,
    SONG_COUNT,
    YEAR,
    RANDOM
}

/**
 * ViewModel for Albums screen.
 */
@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playbackRepository: PlaybackRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow(AlbumSortMode.ALBUM_NAME)
    val sortMode: StateFlow<AlbumSortMode> = _sortMode.asStateFlow()

    private val _isAscending = MutableStateFlow(true)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    private val _selectedAlbumIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedAlbumIds: StateFlow<Set<Long>> = _selectedAlbumIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    /**
     * All albums grouped from song library, filtered and sorted.
     */
    val albums: StateFlow<List<Album>> = combine(
        songRepository.getAllAlbums(),
        _searchQuery,
        _sortMode,
        _isAscending
    ) { allAlbums, query, sort, ascending ->
        var filtered = if (query.isBlank()) {
            allAlbums
        } else {
            allAlbums.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.artist.contains(query, ignoreCase = true) 
            }
        }

        filtered = when (sort) {
            AlbumSortMode.ALBUM_NAME -> if (ascending) filtered.sortedBy { it.title.lowercase() } else filtered.sortedByDescending { it.title.lowercase() }
            AlbumSortMode.ARTIST_NAME -> if (ascending) filtered.sortedBy { it.artist.lowercase() } else filtered.sortedByDescending { it.artist.lowercase() }
            AlbumSortMode.SONG_COUNT -> if (ascending) filtered.sortedBy { it.songCount } else filtered.sortedByDescending { it.songCount }
            AlbumSortMode.YEAR -> if (ascending) filtered.sortedBy { it.year ?: 0 } else filtered.sortedByDescending { it.year ?: 0 }
            AlbumSortMode.RANDOM -> filtered.shuffled()
        }

        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Recently played albums - for now using a subset of all albums as placeholder
     * In a real app, this would come from a dedicated "recent" repository.
     */
    val recentlyPlayedAlbums: StateFlow<List<Album>> = songRepository.getAllAlbums()
        .map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortMode(mode: AlbumSortMode) {
        _sortMode.value = mode
    }

    fun setAscending(ascending: Boolean) {
        _isAscending.value = ascending
    }

    fun toggleSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) {
            _selectedAlbumIds.value = emptySet()
        }
    }

    fun toggleAlbumSelection(albumId: Long) {
        val current = _selectedAlbumIds.value
        _selectedAlbumIds.value = if (current.contains(albumId)) {
            current - albumId
        } else {
            current + albumId
        }
    }

    fun selectAllAlbums() {
        _selectedAlbumIds.value = albums.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedAlbumIds.value = emptySet()
    }

    fun playAlbum(albumId: Long) {
        viewModelScope.launch {
            songRepository.getSongsByAlbumId(albumId)
                .firstOrNull()?.let { albumSongs ->
                    playbackRepository.setShuffleEnabled(false)
                    playbackRepository.playQueue(albumSongs, startIndex = 0)
                }
        }
    }

    fun shuffleAlbum(albumId: Long) {
        viewModelScope.launch {
            songRepository.getSongsByAlbumId(albumId)
                .firstOrNull()?.let { albumSongs ->
                    playbackRepository.setShuffleEnabled(true)
                    playbackRepository.playQueue(albumSongs, startIndex = 0)
                }
        }
    }

    fun playSelectedAlbums() {
        val ids = _selectedAlbumIds.value
        if (ids.isEmpty()) return
        
        viewModelScope.launch {
            val allSelectedSongs = ids.flatMap { id ->
                songRepository.getSongsByAlbumId(id).firstOrNull() ?: emptyList()
            }
            if (allSelectedSongs.isNotEmpty()) {
                playbackRepository.setShuffleEnabled(false)
                playbackRepository.playQueue(allSelectedSongs, startIndex = 0)
                toggleSelectionMode(false)
            }
        }
    }
}
