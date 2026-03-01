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
import com.sukoon.music.data.analytics.AnalyticsTracker
import com.google.firebase.analytics.FirebaseAnalytics
import com.sukoon.music.ui.search.SearchMatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject
import com.sukoon.music.ui.theme.*

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
    private val analyticsTracker: AnalyticsTracker,
    val playbackRepository: PlaybackRepository
) : ViewModel() {
    private data class ScoredSong(
        val song: Song,
        val score: Int
    )


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
        val queryContext = SearchMatcher.createQueryContext(query)

        // Step 1: Match by normalized/token/fuzzy scorer
        val scoredMatches = if (queryContext.normalizedQuery.isBlank()) {
            emptyList()
        } else {
            songs.mapNotNull { song ->
                val matchResult = SearchMatcher.scoreSong(
                    context = queryContext,
                    title = song.title,
                    artist = song.artist,
                    album = song.album
                )
                if (matchResult.isMatch) {
                    ScoredSong(song = song, score = matchResult.score)
                } else {
                    null
                }
            }
        }

        // Step 2: Filter by liked status if toggled
        val filteredMatches = if (likedOnly) {
            scoredMatches.filter { it.song.isLiked }
        } else {
            scoredMatches
        }

        // Step 3: Sort based on selected mode
        val sortedMatches = when (sortMode) {
            SortMode.RELEVANCE -> filteredMatches.sortedWith(
                compareByDescending<ScoredSong> { it.score }
                    .thenBy { it.song.title.lowercase() }
            )
            SortMode.TITLE -> filteredMatches.sortedBy { it.song.title.lowercase() }
            SortMode.ARTIST -> filteredMatches.sortedBy { it.song.artist.lowercase() }
            SortMode.DATE_ADDED -> filteredMatches.sortedByDescending { it.song.dateAdded }
        }

        sortedMatches.map { it.song }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val topMatchSongId: StateFlow<Long?> = combine(
        searchResults,
        _searchQuery,
        _sortMode
    ) { results, query, sortMode ->
        if (query.isBlank() || sortMode != SortMode.RELEVANCE) {
            null
        } else {
            results.firstOrNull()?.id
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
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
            analyticsTracker.logEvent(
                name = FirebaseAnalytics.Event.SEARCH,
                params = mapOf(
                    "query_length" to query.length,
                    "results_count" to searchResults.value.size,
                    "from_history" to false
                )
            )
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
        val trimmedQuery = query.trim()
        viewModelScope.launch {
            if (trimmedQuery.isNotBlank()) {
                yield()
                analyticsTracker.logEvent(
                    name = FirebaseAnalytics.Event.SEARCH,
                    params = mapOf(
                        "query_length" to trimmedQuery.length,
                        "results_count" to searchResults.value.size,
                        "from_history" to true
                    )
                )
            }
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

}
