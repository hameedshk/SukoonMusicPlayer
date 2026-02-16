package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sukoon.music.ui.theme.*

/**
 * ViewModel for the Liked Songs Screen.
 *
 * Responsibilities:
 * - Manage liked songs list from repository
 * - Handle filtering by artist/album
 * - Handle sorting (Title, Artist, Date Added, Recently Liked)
 * - Provide playback controls (play song, play all, shuffle)
 * - Manage like/unlike operations
 *
 * Architecture:
 * - Uses SongRepository.getLikedSongs() as data source (not client-side filtering)
 * - Reactive state management with StateFlow
 * - Derived state using combine() for efficient filtering/sorting
 */
@HiltViewModel
class LikedSongsViewModel @Inject constructor(
    private val songRepository: SongRepository,
    val playbackRepository: PlaybackRepository
) : ViewModel() {

    // --- Filter and Sort State ---

    private val _selectedArtist = MutableStateFlow<String?>(null)
    val selectedArtist: StateFlow<String?> = _selectedArtist.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<String?>(null)
    val selectedAlbum: StateFlow<String?> = _selectedAlbum.asStateFlow()

    private val _sortMode = MutableStateFlow(LikedSongsSortMode.DATE_ADDED)
    val sortMode: StateFlow<LikedSongsSortMode> = _sortMode.asStateFlow()

    // --- Data Sources ---

    private val likedSongsFromRepo: StateFlow<List<Song>> = songRepository.getLikedSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    // --- Derived State ---

    /**
     * Filtered and sorted liked songs.
     * Recomputed when filters, sort mode, or liked songs change.
     */
    val likedSongs: StateFlow<List<Song>> = combine(
        likedSongsFromRepo,
        _selectedArtist,
        _selectedAlbum,
        _sortMode
    ) { songs, artist, album, sortMode ->
        // Step 1: Filter by artist if selected
        val artistFiltered = if (artist != null) {
            songs.filter { it.artist == artist }
        } else songs

        // Step 2: Filter by album if selected
        val filtered = if (album != null) {
            artistFiltered.filter { it.album == album }
        } else artistFiltered

        // Step 3: Sort based on mode
        when (sortMode) {
            LikedSongsSortMode.TITLE -> filtered.sortedBy { it.title.lowercase() }
            LikedSongsSortMode.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
            LikedSongsSortMode.ALBUM -> filtered.sortedBy { it.album.lowercase() }
            LikedSongsSortMode.DATE_ADDED -> filtered.sortedByDescending { it.dateAdded }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Unique artists from liked songs for filter dropdown.
     */
    val availableArtists: StateFlow<List<String>> = likedSongsFromRepo.map { songs ->
        songs.map { it.artist }.distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Unique albums from liked songs for filter dropdown.
     */
    val availableAlbums: StateFlow<List<String>> = likedSongsFromRepo.map { songs ->
        songs.map { it.album }.distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- User Actions ---

    /**
     * Play a specific song from the liked songs list.
     */
    fun playSong(song: Song) {
        viewModelScope.launch {
            playbackRepository.playSong(song)
        }
    }

    /**
     * Play all liked songs (with current filter/sort applied).
     */
    fun playAll() {
        viewModelScope.launch {
            val songs = likedSongs.value
            if (songs.isNotEmpty()) {
                playbackRepository.playQueue(songs, startIndex = 0, queueName = "Liked Songs")
            }
        }
    }

    /**
     * Shuffle and play all liked songs (with current filter/sort applied).
     */
    fun shuffleAll() {
        viewModelScope.launch {
            val songs = likedSongs.value
            if (songs.isNotEmpty()) {
                playbackRepository.shuffleAndPlayQueue(songs, queueName = "Liked Songs")
            }
        }
    }

    /**
     * Toggle like status for a song (unlike it).
     */
    fun toggleLike(songId: Long, currentLikeStatus: Boolean) {
        viewModelScope.launch {
            songRepository.updateLikeStatus(songId, !currentLikeStatus)
        }
    }

    /**
     * Filter by artist.
     * Pass null to clear artist filter.
     */
    fun filterByArtist(artist: String?) {
        _selectedArtist.value = artist
        // Clear album filter when changing artist
        if (artist != _selectedArtist.value) {
            _selectedAlbum.value = null
        }
    }

    /**
     * Filter by album.
     * Pass null to clear album filter.
     */
    fun filterByAlbum(album: String?) {
        _selectedAlbum.value = album
    }

    /**
     * Clear all filters.
     */
    fun clearFilters() {
        _selectedArtist.value = null
        _selectedAlbum.value = null
    }

    /**
     * Update sort mode.
     */
    fun updateSortMode(mode: LikedSongsSortMode) {
        _sortMode.value = mode
    }
}

/**
 * Sort modes for liked songs.
 */
enum class LikedSongsSortMode {
    TITLE,       // A-Z by title
    ARTIST,      // A-Z by artist
    ALBUM,       // A-Z by album
    DATE_ADDED   // Most recent first
}
