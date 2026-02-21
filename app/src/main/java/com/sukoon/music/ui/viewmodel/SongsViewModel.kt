package com.sukoon.music.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SongSortMode(val storageToken: String) {
    TITLE("title"),
    ARTIST("artist"),
    ALBUM("album"),
    FOLDER("folder"),
    DATE_ADDED("date_added"),
    PLAY_COUNT("play_count"),
    YEAR("year"),
    DURATION("duration"),
    SIZE("size");

    companion object {
        fun fromStorage(value: String?): SongSortMode {
            return entries.firstOrNull { it.storageToken == value } ?: TITLE
        }
    }
}

enum class SortOrder(val storageToken: String) {
    ASC("asc"),
    DESC("desc");

    companion object {
        fun fromStorage(value: String?): SortOrder {
            return entries.firstOrNull { it.storageToken == value } ?: ASC
        }
    }
}

@HiltViewModel
class SongsViewModel @Inject constructor(
    private val songRepository: SongRepository,
    val playbackRepository: PlaybackRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "SongsViewModel"
    }

    val songs: StateFlow<List<Song>> = songRepository.getAllSongs().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = emptyList()
    )

    val playbackState = playbackRepository.playbackState

    private val _sortMode = MutableStateFlow(SongSortMode.TITLE)
    val sortMode: StateFlow<SongSortMode> = _sortMode.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedArtistFilter = MutableStateFlow<String?>(null)
    val selectedArtistFilter: StateFlow<String?> = _selectedArtistFilter.asStateFlow()

    private val _selectedAlbumFilter = MutableStateFlow<String?>(null)
    val selectedAlbumFilter: StateFlow<String?> = _selectedAlbumFilter.asStateFlow()

    private val _showSortDialog = MutableStateFlow(false)
    val showSortDialog: StateFlow<Boolean> = _showSortDialog.asStateFlow()

    private val _selectedSongId = MutableStateFlow<Long?>(null)
    val selectedSongId: StateFlow<Long?> = _selectedSongId.asStateFlow()

    private val _showMenu = MutableStateFlow(false)
    val showMenu: StateFlow<Boolean> = _showMenu.asStateFlow()

    private val _songToDelete = MutableStateFlow<Song?>(null)
    val songToDelete: StateFlow<Song?> = _songToDelete.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _showInfoForSong = MutableStateFlow<Song?>(null)
    val showInfoForSong: StateFlow<Song?> = _showInfoForSong.asStateFlow()

    private val _scrollPosition = MutableStateFlow(0)
    val scrollPosition: StateFlow<Int> = _scrollPosition.asStateFlow()

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    private val _requiresDeletePermission = MutableStateFlow<android.content.IntentSender?>(null)
    val requiresDeletePermission: StateFlow<android.content.IntentSender?> =
        _requiresDeletePermission.asStateFlow()

    private val _isScreenActive = MutableStateFlow(true)
    val isScreenActive: StateFlow<Boolean> = _isScreenActive.asStateFlow()

    val hasActiveFilters: StateFlow<Boolean> = combine(
        _searchQuery,
        _selectedArtistFilter,
        _selectedAlbumFilter
    ) { query, artistFilter, albumFilter ->
        query.isNotBlank() || artistFilter != null || albumFilter != null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = false
    )

    val availableArtists: StateFlow<List<String>> = songs.map { songsList ->
        songsList
            .map { it.artist.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = emptyList()
    )

    val availableAlbums: StateFlow<List<String>> = songs.map { songsList ->
        songsList
            .map { it.album.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = emptyList()
    )

    private val filteredSongs: StateFlow<List<Song>> = combine(
        songs,
        _searchQuery,
        _selectedArtistFilter,
        _selectedAlbumFilter
    ) { allSongs, query, artistFilter, albumFilter ->
        val normalizedQuery = query.trim()
        var filtered = allSongs

        if (!artistFilter.isNullOrBlank()) {
            filtered = filtered.filter { it.artist.equals(artistFilter, ignoreCase = true) }
        }
        if (!albumFilter.isNullOrBlank()) {
            filtered = filtered.filter { it.album.equals(albumFilter, ignoreCase = true) }
        }
        if (normalizedQuery.isNotBlank()) {
            filtered = filtered.filter { song ->
                song.title.contains(normalizedQuery, ignoreCase = true) ||
                    song.artist.contains(normalizedQuery, ignoreCase = true) ||
                    song.album.contains(normalizedQuery, ignoreCase = true)
            }
        }
        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = emptyList()
    )

    val visibleSongs: StateFlow<List<Song>> = combine(
        filteredSongs,
        _sortMode,
        _sortOrder
    ) { filtered, mode, order ->
        val sorted = when (mode) {
            SongSortMode.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SongSortMode.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
            SongSortMode.ALBUM -> filtered.sortedBy { it.album.lowercase() }
            SongSortMode.FOLDER -> filtered.sortedBy { it.path.lowercase() }
            SongSortMode.DATE_ADDED -> filtered.sortedBy { it.dateAdded }
            SongSortMode.PLAY_COUNT -> filtered.sortedBy { it.playCount }
            SongSortMode.YEAR -> filtered.sortedBy { it.year }
            SongSortMode.DURATION -> filtered.sortedBy { it.duration }
            SongSortMode.SIZE -> filtered.sortedBy { it.size }
        }

        if (order == SortOrder.DESC) sorted.reversed() else sorted
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = emptyList()
    )

    private val dataModificationMutex = Mutex()

    init {
        observePersistedState()
    }

    override fun onCleared() {
        _isScreenActive.value = false
        super.onCleared()
    }

    private fun observePersistedState() {
        viewModelScope.launch {
            preferencesManager.getSongsSortModeFlow().collect { mode ->
                _sortMode.value = SongSortMode.fromStorage(mode)
            }
        }
        viewModelScope.launch {
            preferencesManager.getSongsSortOrderFlow().collect { order ->
                _sortOrder.value = SortOrder.fromStorage(order)
            }
        }
        viewModelScope.launch {
            preferencesManager.getSongsSearchQueryFlow().collect { query ->
                _searchQuery.value = query
            }
        }
        viewModelScope.launch {
            preferencesManager.getSongsArtistFilterFlow().collect { artist ->
                _selectedArtistFilter.value = artist
            }
        }
        viewModelScope.launch {
            preferencesManager.getSongsAlbumFilterFlow().collect { album ->
                _selectedAlbumFilter.value = album
            }
        }
    }

    fun setScreenActive(active: Boolean) {
        _isScreenActive.value = active
    }

    fun setSortMode(mode: SongSortMode, order: SortOrder = _sortOrder.value) {
        _sortMode.value = mode
        _sortOrder.value = order
        viewModelScope.launch {
            preferencesManager.setSongsSortMode(mode.storageToken)
            preferencesManager.setSongsSortOrder(order.storageToken)
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        viewModelScope.launch {
            preferencesManager.setSongsSortOrder(order.storageToken)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            preferencesManager.setSongsSearchQuery(query)
        }
    }

    fun setArtistFilter(artist: String?) {
        _selectedArtistFilter.value = artist
        viewModelScope.launch {
            preferencesManager.setSongsArtistFilter(artist)
        }
    }

    fun setAlbumFilter(album: String?) {
        _selectedAlbumFilter.value = album
        viewModelScope.launch {
            preferencesManager.setSongsAlbumFilter(album)
        }
    }

    fun clearFilters() {
        _selectedArtistFilter.value = null
        _selectedAlbumFilter.value = null
        viewModelScope.launch {
            preferencesManager.setSongsArtistFilter(null)
            preferencesManager.setSongsAlbumFilter(null)
        }
    }

    fun clearSearchAndFilters() {
        _searchQuery.value = ""
        _selectedArtistFilter.value = null
        _selectedAlbumFilter.value = null
        viewModelScope.launch {
            preferencesManager.setSongsSearchQuery("")
            preferencesManager.setSongsArtistFilter(null)
            preferencesManager.setSongsAlbumFilter(null)
        }
    }

    fun showSortDialog() {
        _showSortDialog.value = true
    }

    fun hideSortDialog() {
        _showSortDialog.value = false
    }

    fun selectSong(songId: Long) {
        _selectedSongId.value = songId
    }

    fun deselectSong() {
        _selectedSongId.value = null
    }

    fun openMenuForSong(songId: Long) {
        _selectedSongId.value = songId
        _showMenu.value = true
    }

    fun closeMenu() {
        _showMenu.value = false
    }

    fun showDeleteConfirmation(song: Song) {
        _songToDelete.value = song
    }

    fun hideDeleteConfirmation() {
        _songToDelete.value = null
    }

    fun showSongInfo(song: Song) {
        _showInfoForSong.value = song
    }

    fun hideSongInfo() {
        _showInfoForSong.value = null
    }

    fun saveScrollPosition(index: Int) {
        _scrollPosition.value = index
    }

    fun clearDeleteError() {
        _deleteError.value = null
    }

    fun playQueue(queueSongs: List<Song>, index: Int) {
        viewModelScope.launch {
            playbackRepository.playQueue(queueSongs, index)
        }
    }

    fun shuffleAll() {
        viewModelScope.launch {
            playbackRepository.shuffleAndPlayQueue(visibleSongs.value)
        }
    }

    fun playAll() {
        viewModelScope.launch {
            playbackRepository.playQueue(visibleSongs.value, 0)
        }
    }

    fun toggleLike(songId: Long, isLiked: Boolean) {
        viewModelScope.launch {
            songRepository.updateLikeStatus(songId, isLiked)
        }
    }

    fun confirmDelete(song: Song, callback: (DeleteHelper.DeleteResult) -> Unit) {
        if (_isDeleting.value) return

        _isDeleting.value = true

        viewModelScope.launch {
            try {
                val isCurrentlyPlaying = playbackState.value.currentSong?.id == song.id
                if (isCurrentlyPlaying) {
                    playbackRepository.pause()
                }

                dataModificationMutex.withLock {
                    val result = DeleteHelper.deleteSongs(context, listOf(song))
                    if (_isScreenActive.value) {
                        callback(result)
                        when (result) {
                            is DeleteHelper.DeleteResult.Success -> {
                                _songToDelete.value = null
                                _isDeleting.value = false
                                songRepository.scanLocalMusic()
                            }

                            is DeleteHelper.DeleteResult.RequiresPermission -> {
                                _requiresDeletePermission.value = result.intentSender
                                _isDeleting.value = false
                            }

                            is DeleteHelper.DeleteResult.Error -> {
                                _deleteError.value = result.message
                                _songToDelete.value = null
                                _isDeleting.value = false
                            }
                        }
                    } else {
                        _isDeleting.value = false
                    }
                }
            } catch (e: CancellationException) {
                Log.w(TAG, "Delete cancelled mid-operation", e)
                _isDeleting.value = false
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Delete failed", e)
                _deleteError.value = e.message
                _songToDelete.value = null
                _isDeleting.value = false
            }
        }
    }

    fun finalizeDeletion() {
        _songToDelete.value = null
        _isDeleting.value = false
    }
}
