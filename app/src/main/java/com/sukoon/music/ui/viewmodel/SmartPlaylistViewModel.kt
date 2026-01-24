package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.SmartPlaylistType
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sukoon.music.ui.theme.*

/**
 * ViewModel for Smart Playlist Detail screen.
 *
 * Responsibilities:
 * - Load and expose songs for different smart playlist types
 * - Handle playback operations for smart playlists
 * - Coordinate with song repository to get appropriate song lists
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SmartPlaylistViewModel @Inject constructor(
    private val songRepository: SongRepository,
    val playbackRepository: PlaybackRepository
) : ViewModel() {

    // ============================================
    // SMART PLAYLIST DATA
    // ============================================

    /**
     * Currently selected smart playlist type.
     */
    private val _currentSmartPlaylistType = MutableStateFlow<SmartPlaylistType?>(null)
    val currentSmartPlaylistType: StateFlow<SmartPlaylistType?> = _currentSmartPlaylistType.asStateFlow()

    /**
     * Songs in the currently selected smart playlist.
     * Updates automatically based on playlist type.
     */
    val currentSmartPlaylistSongs: StateFlow<List<Song>> = _currentSmartPlaylistType
        .flatMapLatest { playlistType ->
            when (playlistType) {
                SmartPlaylistType.MY_FAVOURITE -> songRepository.getLikedSongs()
                SmartPlaylistType.LAST_ADDED -> songRepository.getLastAddedSongs()
                SmartPlaylistType.RECENTLY_PLAYED -> songRepository.getRecentlyPlayed()
                SmartPlaylistType.MOST_PLAYED -> songRepository.getMostPlayedSongs()
                null -> flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ============================================
    // PLAYBACK STATE
    // ============================================

    /**
     * Current playback state (playing song, queue, shuffle, repeat, etc.).
     */
    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    // ============================================
    // SMART PLAYLIST ACTIONS
    // ============================================

    /**
     * Load a smart playlist by type.
     * Call this when navigating to smart playlist detail screen.
     */
    fun loadSmartPlaylist(playlistType: SmartPlaylistType) {
        _currentSmartPlaylistType.value = playlistType
    }

    // ============================================
    // PLAYBACK ACTIONS
    // ============================================

    /**
     * Play a single song immediately.
     */
    fun playSong(song: Song) {
        viewModelScope.launch {
            playbackRepository.playSong(song)
        }
    }

    /**
     * Play all songs in the current smart playlist starting from the first song.
     */
    fun playSmartPlaylist(playlistType: SmartPlaylistType) {
        viewModelScope.launch {
            val songs = currentSmartPlaylistSongs.value

            if (songs.isNotEmpty()) {
                playbackRepository.playQueue(songs, startIndex = 0)
            }
        }
    }

    /**
     * Play all songs in the current smart playlist with shuffle enabled.
     */
    fun shuffleSmartPlaylist(playlistType: SmartPlaylistType) {
        viewModelScope.launch {
            val songs = currentSmartPlaylistSongs.value

            if (songs.isNotEmpty()) {
                playbackRepository.shuffleAndPlayQueue(songs)
            }
        }
    }

    /**
     * Toggle like status of a song.
     */
    fun toggleLike(songId: Long, isCurrentlyLiked: Boolean) {
        viewModelScope.launch {
            songRepository.updateLikeStatus(songId, !isCurrentlyLiked)
        }
    }
}
