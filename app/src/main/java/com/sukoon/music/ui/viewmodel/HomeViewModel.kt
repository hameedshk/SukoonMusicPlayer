package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.LyricsState
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.ScanState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.LyricsRepository
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home Screen.
 *
 * Responsibilities:
 * - Expose song list from SongRepository
 * - Expose scan state for UI feedback
 * - Expose playback state for player controls
 * - Expose lyrics state for Now Playing screen
 * - Handle user actions (scan, play, pause, seek, lyrics)
 *
 * Follows MVVM architecture with StateFlow for reactive UI updates.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playbackRepository: PlaybackRepository,
    private val lyricsRepository: LyricsRepository,
    val adMobManager: com.sukoon.music.data.ads.AdMobManager
) : ViewModel() {

    // Song List State
    val songs: StateFlow<List<Song>> = songRepository.getAllSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Recently Played State
    val recentlyPlayed: StateFlow<List<Song>> = songRepository.getRecentlyPlayed()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Scan State
    val scanState: StateFlow<ScanState> = songRepository.scanState

    // Playback State
    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    // Lyrics State
    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Loading)
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()

    // User Actions

    /**
     * Trigger MediaStore scan to discover local audio files.
     * Only call on explicit user action (foreground, user-initiated).
     */
    fun scanLocalMusic() {
        viewModelScope.launch {
            songRepository.scanLocalMusic()
        }
    }

    /**
     * Play a specific song.
     * Sets it as the current media item and starts playback.
     */
    fun playSong(song: Song) {
        viewModelScope.launch {
            playbackRepository.playSong(song)
        }
    }

    /**
     * Toggle play/pause state.
     */
    fun playPause() {
        viewModelScope.launch {
            playbackRepository.playPause()
        }
    }

    /**
     * Play the entire song queue starting from a specific index.
     */
    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        viewModelScope.launch {
            playbackRepository.playQueue(songs, startIndex)
        }
    }

    /**
     * Shuffle and play all songs in the library.
     * Enables shuffle mode and starts playback from the beginning.
     */
    fun shuffleAll() {
        viewModelScope.launch {
            val allSongs = songs.value
            if (allSongs.isNotEmpty()) {
                playbackRepository.setShuffleEnabled(true)
                playbackRepository.playQueue(allSongs, startIndex = 0)
            }
        }
    }

    /**
     * Play all songs in the library from the beginning.
     * Disables shuffle mode and starts sequential playback.
     */
    fun playAll() {
        viewModelScope.launch {
            val allSongs = songs.value
            if (allSongs.isNotEmpty()) {
                playbackRepository.setShuffleEnabled(false)
                playbackRepository.playQueue(allSongs, startIndex = 0)
            }
        }
    }

    /**
     * Seek to next track in queue.
     */
    fun seekToNext() {
        viewModelScope.launch {
            playbackRepository.seekToNext()
        }
    }

    /**
     * Seek to previous track in queue.
     */
    fun seekToPrevious() {
        viewModelScope.launch {
            playbackRepository.seekToPrevious()
        }
    }

    /**
     * Seek to a specific position in the current track.
     */
    fun seekTo(positionMs: Long) {
        viewModelScope.launch {
            playbackRepository.seekTo(positionMs)
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

    /**
     * Toggle shuffle mode on/off.
     */
    fun toggleShuffle() {
        viewModelScope.launch {
            val currentState = playbackState.value.shuffleEnabled
            playbackRepository.setShuffleEnabled(!currentState)
        }
    }

    /**
     * Cycle through repeat modes: OFF -> ALL -> ONE -> OFF
     */
    fun toggleRepeat() {
        viewModelScope.launch {
            val currentMode = playbackState.value.repeatMode
            val nextMode = when (currentMode) {
                com.sukoon.music.domain.model.RepeatMode.OFF -> com.sukoon.music.domain.model.RepeatMode.ALL
                com.sukoon.music.domain.model.RepeatMode.ALL -> com.sukoon.music.domain.model.RepeatMode.ONE
                com.sukoon.music.domain.model.RepeatMode.ONE -> com.sukoon.music.domain.model.RepeatMode.OFF
            }
            playbackRepository.setRepeatMode(nextMode)
        }
    }

    /**
     * Fetch lyrics for a song from LRCLIB.
     * Checks cache first, then fetches from API if needed.
     */
    fun fetchLyrics(song: Song) {
        viewModelScope.launch {
            lyricsRepository.getLyrics(
                trackId = song.id,
                audioUri = song.uri,
                artist = song.artist,
                title = song.title,
                album = song.album,
                duration = (song.duration / 1000).toInt() // Convert ms to seconds
            ).collect { state ->
                _lyricsState.value = state
            }
        }
    }

    /**
     * Update manual sync offset for lyrics.
     * Used when lyrics drift out of sync with playback.
     */
    fun updateLyricsSyncOffset(trackId: Long, offsetMs: Long) {
        viewModelScope.launch {
            lyricsRepository.updateSyncOffset(trackId, offsetMs)
            // Refetch lyrics to apply new offset
            playbackState.value.currentSong?.let { song ->
                fetchLyrics(song)
            }
        }
    }

    /**
     * Clear cached lyrics for a track (force refresh).
     */
    fun clearLyrics(trackId: Long) {
        viewModelScope.launch {
            lyricsRepository.clearLyrics(trackId)
            _lyricsState.value = LyricsState.Loading
        }
    }

    /**
     * Add a song to the end of the current queue.
     */
    fun addToQueue(song: Song) {
        viewModelScope.launch {
            playbackRepository.addToQueue(song)
        }
    }

    /**
     * Remove a song from the queue at the specified index.
     */
    fun removeFromQueue(index: Int) {
        viewModelScope.launch {
            playbackRepository.removeFromQueue(index)
        }
    }

    /**
     * Jump to a specific song in the queue.
     */
    fun jumpToQueueIndex(index: Int) {
        viewModelScope.launch {
            playbackRepository.seekToQueueIndex(index)
        }
    }
}
