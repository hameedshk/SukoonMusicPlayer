package com.sukoon.music.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Genre
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GenreSortMode {
    NAME,
    SONG_COUNT,
    RANDOM
}

/**
 * ViewModel for Genres screen.
 */
@HiltViewModel
class GenresViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playbackRepository: PlaybackRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenreIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedGenreIds: StateFlow<Set<Long>> = _selectedGenreIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _deleteResult = MutableStateFlow<DeleteHelper.DeleteResult?>(null)
    val deleteResult: StateFlow<DeleteHelper.DeleteResult?> = _deleteResult.asStateFlow()
    
    private val _showAddToPlaylistDialog = MutableStateFlow<List<Long>?>(null)
    val showAddToPlaylistDialog: StateFlow<List<Long>?> = _showAddToPlaylistDialog.asStateFlow()

    private val _sortMode = MutableStateFlow(GenreSortMode.NAME)
    val sortMode: StateFlow<GenreSortMode> = _sortMode.asStateFlow()

    private val _isAscending = MutableStateFlow(true)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    /**
     * Filter state for genres (e.g., "All", "Popular", etc. - currently just "All")
     */
    private val _genreFilter = MutableStateFlow<String>("All")
    val genreFilter: StateFlow<String> = _genreFilter.asStateFlow()

    fun setGenreFilter(filter: String) {
        _genreFilter.value = filter
    }

    // --- State for Inline Genre Songs ---
    private val _selectedGenreId = MutableStateFlow<Long?>(null)
    val selectedGenreId: StateFlow<Long?> = _selectedGenreId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val genreSongs: StateFlow<List<Song>> = _selectedGenreId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else songRepository.getSongsByGenreId(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * All genres from song library, filtered and sorted.
     */
    val genres: StateFlow<List<Genre>> = combine(
        songRepository.getAllGenres(),
        _searchQuery,
        _genreFilter,
        _sortMode,
        _isAscending
    ) { allGenres, query, filter, sortMode, isAscending ->
        var filtered = if (query.isBlank()) {
            allGenres
        } else {
            allGenres.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }

        // Apply additional filtering based on genreFilter if needed
        // For now, "All" doesn't filter further.

        // Apply sorting
        val sorted = when (sortMode) {
            GenreSortMode.NAME -> filtered.sortedBy { it.name.lowercase() }
            GenreSortMode.SONG_COUNT -> filtered.sortedBy { it.songCount }
            GenreSortMode.RANDOM -> filtered.shuffled()
        }

        if (sortMode != GenreSortMode.RANDOM) {
            if (isAscending) sorted else sorted.reversed()
        } else {
            sorted
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortMode(mode: GenreSortMode) {
        _sortMode.value = mode
    }

    fun setAscending(ascending: Boolean) {
        _isAscending.value = ascending
    }

    fun toggleSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) {
            _selectedGenreIds.value = emptySet()
        }
    }

    fun toggleGenreSelection(genreId: Long) {
        val current = _selectedGenreIds.value
        _selectedGenreIds.value = if (current.contains(genreId)) {
            current - genreId
        } else {
            current + genreId
        }
    }

    fun selectAllGenres() {
        _selectedGenreIds.value = genres.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedGenreIds.value = emptySet()
    }

    fun selectGenre(id: Long?) {
        _selectedGenreId.value = id
    }

    fun playGenre(genreId: Long) {
        viewModelScope.launch {
            songRepository.getSongsByGenreId(genreId)
                .firstOrNull()?.let { genreSongs ->
                    playbackRepository.playQueue(genreSongs, startIndex = 0)
                }
        }
    }

    fun playGenreSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        viewModelScope.launch {
            playbackRepository.playQueue(songs, startIndex = 0)
        }
    }

    fun shuffleGenreSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        viewModelScope.launch {
            playbackRepository.shuffleAndPlayQueue(songs)
        }
    }

    fun playSongInGenre(song: Song, songs: List<Song>) {
        viewModelScope.launch {
            val index = songs.indexOf(song)
            if (index != -1) {
                playbackRepository.playQueue(songs, index)
            }
        }
    }

    fun shuffleGenre(genreId: Long) {
        viewModelScope.launch {
            songRepository.getSongsByGenreId(genreId)
                .firstOrNull()?.let { genreSongs ->
                    playbackRepository.playQueue(genreSongs.shuffled(), startIndex = 0)
                }
        }
    }

    fun playGenreNext(genreId: Long) {
        viewModelScope.launch {
            songRepository.getSongsByGenreId(genreId).firstOrNull()?.let { songs ->
                playbackRepository.playNext(songs)
            }
        }
    }

    fun addGenreToQueue(genreId: Long) {
        viewModelScope.launch {
            songRepository.getSongsByGenreId(genreId).firstOrNull()?.let { songs ->
                playbackRepository.addToQueue(songs)
            }
        }
    }

    fun playNext(song: Song) {
        viewModelScope.launch {
            playbackRepository.playNext(song)
        }
    }

    fun addToQueue(song: Song) {
        viewModelScope.launch {
            playbackRepository.addToQueue(song)
        }
    }
    
    fun showAddToPlaylistDialog(genreIds: List<Long>) {
        _showAddToPlaylistDialog.value = genreIds
    }

    fun addToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            songRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    // ============================================
    // BULK ACTIONS
    // ============================================

    fun playSelectedGenres() {
        val ids = _selectedGenreIds.value
        if (ids.isEmpty()) return
        
        viewModelScope.launch {
            val allSelectedSongs = ids.flatMap { id ->
                songRepository.getSongsByGenreId(id).firstOrNull() ?: emptyList()
            }
            if (allSelectedSongs.isNotEmpty()) {
                playbackRepository.playQueue(allSelectedSongs, startIndex = 0)
                toggleSelectionMode(false)
            }
        }
    }

    fun playSelectedNext() {
        val ids = _selectedGenreIds.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            val allSelectedSongs = ids.flatMap { id ->
                songRepository.getSongsByGenreId(id).firstOrNull() ?: emptyList()
            }
            if (allSelectedSongs.isNotEmpty()) {
                playbackRepository.playNext(allSelectedSongs)
                toggleSelectionMode(false)
            }
        }
    }

    fun addSelectedToQueue() {
        val ids = _selectedGenreIds.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            val allSelectedSongs = ids.flatMap { id ->
                songRepository.getSongsByGenreId(id).firstOrNull() ?: emptyList()
            }
            if (allSelectedSongs.isNotEmpty()) {
                playbackRepository.addToQueue(allSelectedSongs)
                toggleSelectionMode(false)
            }
        }
    }

    fun addSelectedToPlaylist(playlistId: Long) {
        val ids = _selectedGenreIds.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            ids.forEach { id ->
                songRepository.getSongsByGenreId(id).firstOrNull()?.forEach { song ->
                    songRepository.addSongToPlaylist(playlistId, song.id)
                }
            }
            toggleSelectionMode(false)
        }
    }

    fun deleteSelectedGenres() {
        val ids = _selectedGenreIds.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            val songs = ids.flatMap { id ->
                songRepository.getSongsByGenreId(id).firstOrNull() ?: emptyList()
            }

            if (songs.isEmpty()) {
                _deleteResult.value = DeleteHelper.DeleteResult.Success
                toggleSelectionMode(false)
                return@launch
            }

            val result = DeleteHelper.deleteSongs(context, songs)
            _deleteResult.value = result
            if (result is DeleteHelper.DeleteResult.Success) {
                toggleSelectionMode(false)
            }
        }
    }

    fun clearDeleteResult() {
        _deleteResult.value = null
    }

    fun scanMediaLibrary() {
        viewModelScope.launch {
            songRepository.scanLocalMusic()
        }
    }

    fun updateGenreName(oldName: String, newName: String) {
        viewModelScope.launch {
            songRepository.updateGenreTags(oldName, newName)
        }
    }
}
