package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.SearchHistory
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.model.SortMode
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SearchHistoryRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the enhanced Search Screen.
 *
 * Responsibilities:
 * - Manage search query state
 * - Manage filter state (liked only toggle)
 * - Manage sort mode
 * - Expose search history
 * - Compute filtered & sorted results as derived state
 * - Handle user actions (save search, clear history, play song, toggle like)
 *
 * Performance:
 * - Uses `combine()` to derive filtered results only when dependencies change
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    val playbackRepository: PlaybackRepository
) : ViewModel() {

    // --- Input State (Mutable) ---

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showLikedOnly = MutableStateFlow(false)
    val showLikedOnly: StateFlow<Boolean> = _showLikedOnly.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.RELEVANCE)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    // --- Data Sources ---

    private val allSongs: StateFlow<List<Song>> = songRepository.getAllSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val searchHistory: StateFlow<List<SearchHistory>> = searchHistoryRepository.getSearchHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    // --- Derived State (Computed) ---

    /**
     * Filtered and sorted search results.
     * Recomputed whenever searchQuery, showLikedOnly, sortMode, or allSongs changes.
     * Uses `combine()` for efficient reactive computation.
     */
    val searchResults: StateFlow<List<Song>> = combine(
        _searchQuery,
        _showLikedOnly,
        _sortMode,
        allSongs
    ) { query, likedOnly, sortMode, songs ->
        // Step 1: Filter by search query
        val queriedSongs = if (query.isBlank()) {
            emptyList()
        } else {
            songs.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                song.artist.contains(query, ignoreCase = true) ||
                song.album.contains(query, ignoreCase = true)
            }
        }

        // Step 2: Filter by liked status if toggled
        val filteredSongs = if (likedOnly) {
            queriedSongs.filter { it.isLiked }
        } else {
            queriedSongs
        }

        // Step 3: Sort based on selected mode
        when (sortMode) {
            SortMode.RELEVANCE -> sortByRelevance(filteredSongs, query)
            SortMode.TITLE -> filteredSongs.sortedBy { it.title.lowercase() }
            SortMode.ARTIST -> filteredSongs.sortedBy { it.artist.lowercase() }
            SortMode.DATE_ADDED -> filteredSongs.sortedByDescending { it.dateAdded }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- User Actions ---

    /**
     * Update search query.
     * Triggers recomputation of searchResults via combine().
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Save current search query to history.
     * Only saves non-blank queries.
     * Called when user performs a search (enters query, taps history chip).
     */
    fun saveSearchToHistory() {
        val query = _searchQuery.value.trim()
        if (query.isNotBlank()) {
            viewModelScope.launch {
                searchHistoryRepository.saveSearch(query)
            }
        }
    }

    /**
     * Apply a search query from history.
     * Updates search query and saves to history (moves to top).
     */
    fun applySearchFromHistory(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            searchHistoryRepository.saveSearch(query)
        }
    }

    /**
     * Toggle "Liked songs only" filter.
     */
    fun toggleLikedFilter() {
        _showLikedOnly.value = !_showLikedOnly.value
    }

    /**
     * Update sort mode.
     * Triggers recomputation of searchResults via combine().
     */
    fun updateSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    /**
     * Delete a specific search from history.
     */
    fun deleteSearchHistory(query: String) {
        viewModelScope.launch {
            searchHistoryRepository.deleteSearch(query)
        }
    }

    /**
     * Clear all search history.
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            searchHistoryRepository.clearAllHistory()
        }
    }

    /**
     * Play a song from search results.
     */
    fun playSong(song: Song) {
        viewModelScope.launch {
            playbackRepository.playSong(song)
        }
    }

    /**
     * Toggle like status for a song.
     */
    fun toggleLike(songId: Long, currentLikeStatus: Boolean) {
        viewModelScope.launch {
            songRepository.updateLikeStatus(songId, !currentLikeStatus)
        }
    }

    // --- Helper Functions ---

    /**
     * Sort songs by search relevance.
     *
     * Scoring algorithm:
     * - Exact title match: 1000 points
     * - Title starts with query: 500 points
     * - Title contains query: 100 points
     * - Artist exact match: 800 points
     * - Artist starts with query: 400 points
     * - Artist contains query: 80 points
     * - Album exact match: 600 points
     * - Album starts with query: 300 points
     * - Album contains query: 60 points
     *
     * Higher score = more relevant = appears first
     */
    private fun sortByRelevance(songs: List<Song>, query: String): List<Song> {
        if (query.isBlank()) return songs

        val lowerQuery = query.lowercase()

        return songs.sortedByDescending { song ->
            var score = 0

            val lowerTitle = song.title.lowercase()
            val lowerArtist = song.artist.lowercase()
            val lowerAlbum = song.album.lowercase()

            // Title scoring
            when {
                lowerTitle == lowerQuery -> score += 1000
                lowerTitle.startsWith(lowerQuery) -> score += 500
                lowerTitle.contains(lowerQuery) -> score += 100
            }

            // Artist scoring
            when {
                lowerArtist == lowerQuery -> score += 800
                lowerArtist.startsWith(lowerQuery) -> score += 400
                lowerArtist.contains(lowerQuery) -> score += 80
            }

            // Album scoring
            when {
                lowerAlbum == lowerQuery -> score += 600
                lowerAlbum.startsWith(lowerQuery) -> score += 300
                lowerAlbum.contains(lowerQuery) -> score += 60
            }

            score
        }
    }
}
