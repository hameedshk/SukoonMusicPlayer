package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Albums screen.
 *
 * Responsibilities:
 * - Expose album list from SongRepository
 * - Provide playback actions for entire albums
 * - Observe current playback state for UI highlighting
 *
 * Follows the architecture established by HomeViewModel and LikedSongsViewModel.
 */
@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playbackRepository: PlaybackRepository
) : ViewModel() {

    /**
     * All albums grouped from song library.
     * Updates reactively when songs are added/removed or metadata changes.
     */
    val albums: StateFlow<List<Album>> = songRepository.getAllAlbums()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Current playback state for UI feedback.
     * Used to highlight currently playing album.
     */
    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    /**
     * Play all songs in an album from the beginning.
     * Clears current queue and starts album playback.
     *
     * @param albumId The unique ID of the album to play
     */
    fun playAlbum(albumId: Long) {
        viewModelScope.launch {
            // Get all songs in the album
            songRepository.getSongsByAlbumId(albumId)
                .stateIn(viewModelScope)
                .value
                .takeIf { it.isNotEmpty() }
                ?.let { albumSongs ->
                    // Play queue from the beginning
                    playbackRepository.playQueue(albumSongs, startIndex = 0)
                }
        }
    }

    /**
     * Shuffle and play all songs in an album.
     * Randomizes song order before playback.
     *
     * @param albumId The unique ID of the album to shuffle
     */
    fun shuffleAlbum(albumId: Long) {
        viewModelScope.launch {
            // Get all songs in the album
            songRepository.getSongsByAlbumId(albumId)
                .stateIn(viewModelScope)
                .value
                .takeIf { it.isNotEmpty() }
                ?.let { albumSongs ->
                    // Shuffle the songs and play queue
                    val shuffledSongs = albumSongs.shuffled()
                    playbackRepository.playQueue(shuffledSongs, startIndex = 0)
                }
        }
    }

    /**
     * Navigate to album detail screen.
     * (Navigation logic handled by composable, this is a placeholder for future actions)
     */
    fun onAlbumClick(albumId: Long) {
        // Navigation is handled by the screen composable
        // This method is reserved for future album-specific actions
        // (e.g., analytics tracking, pre-loading album art)
    }
}
