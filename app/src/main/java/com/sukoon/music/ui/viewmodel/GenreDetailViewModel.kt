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
import com.sukoon.music.ui.theme.*

/**
 * Sort modes for genre songs.
 */
enum class GenreSongSortMode {
    TITLE,
    ARTIST,
    ALBUM,
    DURATION
}

/**
 * ViewModel for Genre Detail screen.
 */
@HiltViewModel
class GenreDetailViewModel @Inject constructor(
    private val songRepository: SongRepository,
    val playbackRepository: PlaybackRepository,
    @ApplicationContext private val context: Context,
    val adMobManager: com.sukoon.music.data.ads.AdMobManager
) : ViewModel() {

    private val _genreId = MutableStateFlow<Long>(-1)

    private val _deleteResult = MutableStateFlow<DeleteHelper.DeleteResult?>(null)
    val deleteResult: StateFlow<DeleteHelper.DeleteResult?> = _deleteResult.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val genre: StateFlow<Genre?> = _genreId
        .flatMapLatest { id ->
            if (id == -1L) flowOf(null)
            else songRepository.getGenreById(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val genreSongs: StateFlow<List<Song>> = _genreId
        .flatMapLatest { id ->
            if (id == -1L) flowOf(emptyList())
            else songRepository.getSongsByGenreId(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    /**
     * Selection mode state for multi-select functionality.
     */
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    /**
     * Selected song IDs for batch operations.
     */
    private val _selectedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSongIds: StateFlow<Set<Long>> = _selectedSongIds.asStateFlow()

    /**
     * Sort mode for songs.
     */
    private val _sortMode = MutableStateFlow(GenreSongSortMode.TITLE)
    val sortMode: StateFlow<GenreSongSortMode> = _sortMode.asStateFlow()

    fun loadGenre(genreId: Long) {
        _genreId.value = genreId
    }

    /**
     * Play all songs in the genre from the beginning.
     */
    fun playGenre(songs: List<Song>) {
        if (songs.isEmpty()) return
        val genreName = genre.value?.name ?: "Genre"
        viewModelScope.launch {
            playbackRepository.playQueue(songs, startIndex = 0, queueName = "Genre: $genreName")
        }
    }

    /**
     * Shuffle and play all songs in the genre.
     */
    fun shuffleGenre(songs: List<Song>) {
        if (songs.isEmpty()) return
        val genreName = genre.value?.name ?: "Genre"
        viewModelScope.launch {
            playbackRepository.shuffleAndPlayQueue(songs, queueName = "Genre: $genreName")
        }
    }

    /**
     * Play a specific song from the genre.
     */
    fun playSong(song: Song, songs: List<Song>) {
        val genreName = genre.value?.name ?: "Genre"
        viewModelScope.launch {
            val index = songs.indexOf(song)
            if (index >= 0) {
                playbackRepository.playQueue(songs, startIndex = index, queueName = "Genre: $genreName")
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

    /**
     * Toggle like status of a song.
     */
    fun toggleLike(songId: Long, isLiked: Boolean) {
        viewModelScope.launch {
            songRepository.updateLikeStatus(songId, !isLiked)
        }
    }

    /**
     * Request deletion of a song.
     */
    fun deleteSong(song: Song) {
        _deleteResult.value = DeleteHelper.deleteSongs(context, listOf(song))
    }

    fun resetDeleteResult() {
        _deleteResult.value = null
    }

    /**
     * Toggle selection mode on/off.
     */
    fun toggleSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) {
            _selectedSongIds.value = emptySet()
        }
    }

    /**
     * Toggle selection of a specific song.
     */
    fun toggleSongSelection(songId: Long) {
        val current = _selectedSongIds.value
        _selectedSongIds.value = if (current.contains(songId)) {
            current - songId
        } else {
            current + songId
        }
    }

    /**
     * Select all songs in the genre.
     */
    fun selectAllSongs(songs: List<Song>) {
        _selectedSongIds.value = songs.map { it.id }.toSet()
    }

    /**
     * Clear all selections.
     */
    fun clearSelection() {
        _selectedSongIds.value = emptySet()
    }

    /**
     * Play selected songs from the beginning.
     */
    fun playSelectedSongs(allSongs: List<Song>) {
        val ids = _selectedSongIds.value
        if (ids.isEmpty()) return
        val genreName = genre.value?.name ?: "Genre"

        viewModelScope.launch {
            val selectedSongs = allSongs.filter { ids.contains(it.id) }
            if (selectedSongs.isNotEmpty()) {
                playbackRepository.playQueue(selectedSongs, startIndex = 0, queueName = "Selection from $genreName")
                toggleSelectionMode(false)
            }
        }
    }

    /**
     * Play selected songs next in queue.
     */
    fun playSelectedSongsNext(allSongs: List<Song>) {
        val ids = _selectedSongIds.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            val selectedSongs = allSongs.filter { ids.contains(it.id) }
            if (selectedSongs.isNotEmpty()) {
                playbackRepository.playNext(selectedSongs)
            }
        }
    }

    /**
     * Add selected songs to queue.
     */
    fun addSelectedSongsToQueueBatch(allSongs: List<Song>) {
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

    /**
     * Delete selected songs with result handling for permissions.
     */
    fun deleteSelectedSongsWithResult(allSongs: List<Song>, onResult: (DeleteHelper.DeleteResult) -> Unit) {
        val ids = _selectedSongIds.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            val selectedSongs = allSongs.filter { ids.contains(it.id) }
            if (selectedSongs.isNotEmpty()) {
                val result = DeleteHelper.deleteSongs(context, selectedSongs)
                onResult(result)
                toggleSelectionMode(false)
            }
        }
    }

    /**
     * Set the sort mode for songs.
     */
    fun setSortMode(mode: GenreSongSortMode) {
        _sortMode.value = mode
    }
}
