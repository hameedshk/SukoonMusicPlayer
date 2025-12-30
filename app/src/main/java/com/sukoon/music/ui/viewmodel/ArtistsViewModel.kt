package com.sukoon.music.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ArtistSortMode {
    ARTIST_NAME,
    ALBUM_COUNT,
    SONG_COUNT,
    RANDOM
}

/**
 * ViewModel for Artists screen.
 */
@HiltViewModel
class ArtistsViewModel @Inject constructor(
    val songRepository: SongRepository,
    private val playbackRepository: PlaybackRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow(ArtistSortMode.ARTIST_NAME)
    val sortMode: StateFlow<ArtistSortMode> = _sortMode.asStateFlow()

    private val _isAscending = MutableStateFlow(true)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    private val _selectedArtistIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedArtistIds: StateFlow<Set<Long>> = _selectedArtistIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _showAddToPlaylistArtistId = MutableStateFlow<Long?>(null)
    val showAddToPlaylistArtistId: StateFlow<Long?> = _showAddToPlaylistArtistId.asStateFlow()

    private val _deleteResult = MutableStateFlow<DeleteHelper.DeleteResult?>(null)
    val deleteResult: StateFlow<DeleteHelper.DeleteResult?> = _deleteResult.asStateFlow()

    /**
     * All artists filtered and sorted.
     */
    val artists: StateFlow<List<Artist>> = combine(
        songRepository.getAllArtists(),
        _searchQuery,
        _sortMode,
        _isAscending
    ) { allArtists, query, sort, ascending ->
        var filtered = if (query.isBlank()) {
            allArtists
        } else {
            allArtists.filter { it.name.contains(query, ignoreCase = true) }
        }

        filtered = when (sort) {
            ArtistSortMode.ARTIST_NAME -> if (ascending) filtered.sortedBy { it.name.lowercase() } else filtered.sortedByDescending { it.name.lowercase() }
            ArtistSortMode.ALBUM_COUNT -> if (ascending) filtered.sortedBy { it.albumCount } else filtered.sortedByDescending { it.albumCount }
            ArtistSortMode.SONG_COUNT -> if (ascending) filtered.sortedBy { it.songCount } else filtered.sortedByDescending { it.songCount }
            ArtistSortMode.RANDOM -> filtered.shuffled()
        }

        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Recently played artists from repository.
     */
    val recentlyPlayedArtists: StateFlow<List<Artist>> = songRepository.getRecentlyPlayedArtists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortMode(mode: ArtistSortMode) {
        _sortMode.value = mode
    }

    fun setAscending(ascending: Boolean) {
        _isAscending.value = ascending
    }

    fun toggleSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) {
            _selectedArtistIds.value = emptySet()
        }
    }

    fun toggleArtistSelection(artistId: Long) {
        val current = _selectedArtistIds.value
        _selectedArtistIds.value = if (current.contains(artistId)) {
            current - artistId
        } else {
            current + artistId
        }
    }

    fun selectAllArtists() {
        _selectedArtistIds.value = artists.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedArtistIds.value = emptySet()
    }

    fun playArtist(artistId: Long) {
        viewModelScope.launch {
            songRepository.getSongsByArtistId(artistId)
                .firstOrNull()?.let { artistSongs ->
                    playbackRepository.playQueue(artistSongs, startIndex = 0)
                    logArtistInteraction(artistId)
                }
        }
    }

    fun playArtistNext(artistId: Long) {
        viewModelScope.launch {
            songRepository.getSongsByArtistId(artistId)
                .firstOrNull()?.let { artistSongs ->
                    playbackRepository.playNext(artistSongs)
                }
        }
    }

    fun addArtistToQueue(artistId: Long) {
        viewModelScope.launch {
            songRepository.getSongsByArtistId(artistId)
                .firstOrNull()?.let { artistSongs ->
                    playbackRepository.addToQueue(artistSongs)
                }
        }
    }

    fun showAddToPlaylistDialog(artistId: Long?) {
        _showAddToPlaylistArtistId.value = artistId
    }

    fun logArtistInteraction(artistId: Long) {
        val artist = artists.value.find { it.id == artistId }
        artist?.let {
            viewModelScope.launch {
                songRepository.logArtistPlay(it.name)
            }
        }
    }

    fun shuffleArtist(artistId: Long) {
        viewModelScope.launch {
            songRepository.getSongsByArtistId(artistId)
                .firstOrNull()?.let { artistSongs ->
                    playbackRepository.setShuffleEnabled(true)
                    playbackRepository.playQueue(artistSongs, startIndex = 0)
                    logArtistInteraction(artistId)
                }
        }
    }

    fun playSelectedArtists() {
        val ids = _selectedArtistIds.value
        if (ids.isEmpty()) return
        
        viewModelScope.launch {
            val allSelectedSongs = ids.flatMap { id ->
                songRepository.getSongsByArtistId(id).firstOrNull() ?: emptyList()
            }
            if (allSelectedSongs.isNotEmpty()) {
                playbackRepository.playQueue(allSelectedSongs, startIndex = 0)
                toggleSelectionMode(false)
            }
        }
    }

    fun deleteArtist(artistId: Long) {
        viewModelScope.launch {
            val songs = songRepository.getSongsByArtistId(artistId)
                .firstOrNull() ?: emptyList()

            if (songs.isEmpty()) {
                _deleteResult.value = DeleteHelper.DeleteResult.Success
                return@launch
            }

            val result = DeleteHelper.deleteSongs(context, songs)
            _deleteResult.value = result
        }
    }

    fun clearDeleteResult() {
        _deleteResult.value = null
    }
}
