package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.Artist
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
 * ViewModel for Artists screen.
 *
 * Responsibilities:
 * - Expose artist list from SongRepository
 * - Provide playback actions for entire artist discography
 * - Observe current playback state for UI highlighting
 *
 * Follows the architecture established by AlbumsViewModel.
 */
@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playbackRepository: PlaybackRepository
) : ViewModel() {

    /**
     * All artists grouped from song library.
     * Updates reactively when songs are added/removed or metadata changes.
     */
    val artists: StateFlow<List<Artist>> = songRepository.getAllArtists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Current playback state for UI feedback.
     * Used to highlight currently playing artist.
     */
    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    /**
     * Play all songs by an artist from the beginning.
     * Clears current queue and starts artist playback.
     *
     * @param artistId The unique ID of the artist to play
     */
    fun playArtist(artistId: Long) {
        viewModelScope.launch {
            // Get all songs by the artist
            songRepository.getSongsByArtistId(artistId)
                .stateIn(viewModelScope)
                .value
                .takeIf { it.isNotEmpty() }
                ?.let { artistSongs ->
                    // Play queue from the beginning
                    playbackRepository.playQueue(artistSongs, startIndex = 0)
                }
        }
    }

    /**
     * Shuffle and play all songs by an artist.
     * Randomizes song order before playback.
     *
     * @param artistId The unique ID of the artist to shuffle
     */
    fun shuffleArtist(artistId: Long) {
        viewModelScope.launch {
            // Get all songs by the artist
            songRepository.getSongsByArtistId(artistId)
                .stateIn(viewModelScope)
                .value
                .takeIf { it.isNotEmpty() }
                ?.let { artistSongs ->
                    // Shuffle the songs and play queue
                    val shuffledSongs = artistSongs.shuffled()
                    playbackRepository.playQueue(shuffledSongs, startIndex = 0)
                }
        }
    }

    /**
     * Navigate to artist detail screen.
     * (Navigation logic handled by composable, this is a placeholder for future actions)
     */
    fun onArtistClick(artistId: Long) {
        // Navigation is handled by the screen composable
        // This method is reserved for future artist-specific actions
        // (e.g., analytics tracking, pre-loading artist info)
    }
}
