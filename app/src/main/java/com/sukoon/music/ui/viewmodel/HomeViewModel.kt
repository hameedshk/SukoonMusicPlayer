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
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playbackRepository: PlaybackRepository,
    private val lyricsRepository: LyricsRepository,
    val adMobManager: com.sukoon.music.data.ads.AdMobManager
) : ViewModel() {

    // Tab State Management
    private val _selectedTab = MutableStateFlow("For you")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    fun setSelectedTab(tab: String) {
        _selectedTab.value = tab
    }

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

    fun scanLocalMusic() {
        viewModelScope.launch {
            songRepository.scanLocalMusic()
        }
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            playbackRepository.playSong(song)
        }
    }

    fun playPause() {
        viewModelScope.launch {
            playbackRepository.playPause()
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        viewModelScope.launch {
            playbackRepository.playQueue(songs, startIndex)
        }
    }

    fun shuffleAll() {
        viewModelScope.launch {
            val allSongs = songs.value
            if (allSongs.isNotEmpty()) {
                playbackRepository.setShuffleEnabled(true)
                playbackRepository.playQueue(allSongs, startIndex = 0)
            }
        }
    }

    fun playAll() {
        viewModelScope.launch {
            val allSongs = songs.value
            if (allSongs.isNotEmpty()) {
                playbackRepository.setShuffleEnabled(false)
                playbackRepository.playQueue(allSongs, startIndex = 0)
            }
        }
    }

    fun seekToNext() {
        viewModelScope.launch {
            playbackRepository.seekToNext()
        }
    }

    fun seekToPrevious() {
        viewModelScope.launch {
            playbackRepository.seekToPrevious()
        }
    }

    fun seekTo(positionMs: Long) {
        viewModelScope.launch {
            playbackRepository.seekTo(positionMs)
        }
    }

    fun toggleLike(songId: Long, currentLikeStatus: Boolean) {
        viewModelScope.launch {
            songRepository.updateLikeStatus(songId, !currentLikeStatus)
        }
    }

    fun toggleShuffle() {
        viewModelScope.launch {
            val currentState = playbackState.value.shuffleEnabled
            playbackRepository.setShuffleEnabled(!currentState)
        }
    }

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

    fun fetchLyrics(song: Song) {
        viewModelScope.launch {
            lyricsRepository.getLyrics(
                trackId = song.id,
                audioUri = song.uri,
                artist = song.artist,
                title = song.title,
                album = song.album,
                duration = (song.duration / 1000).toInt()
            ).collect { state ->
                _lyricsState.value = state
            }
        }
    }

    fun updateLyricsSyncOffset(trackId: Long, offsetMs: Long) {
        viewModelScope.launch {
            lyricsRepository.updateSyncOffset(trackId, offsetMs)
            playbackState.value.currentSong?.let { song ->
                fetchLyrics(song)
            }
        }
    }

    fun clearLyrics(trackId: Long) {
        viewModelScope.launch {
            lyricsRepository.clearLyrics(trackId)
            _lyricsState.value = LyricsState.Loading
        }
    }

    fun addToQueue(song: Song) {
        viewModelScope.launch {
            playbackRepository.addToQueue(song)
        }
    }

    fun playNext(song: Song) {
        viewModelScope.launch {
            playbackRepository.playNext(song)
        }
    }

    fun removeFromQueue(index: Int) {
        viewModelScope.launch {
            playbackRepository.removeFromQueue(index)
        }
    }

    fun jumpToQueueIndex(index: Int) {
        viewModelScope.launch {
            playbackRepository.seekToQueueIndex(index)
        }
    }
}
