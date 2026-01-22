package com.sukoon.music.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.LyricsState
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.ScanState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.ListeningStatsRepository
import com.sukoon.music.domain.repository.ListeningStatsSnapshot
import com.sukoon.music.domain.repository.LyricsRepository
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home Screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    val playbackRepository: PlaybackRepository,
    private val lyricsRepository: LyricsRepository,
    private val listeningStatsRepository: ListeningStatsRepository,
    val adMobManager: com.sukoon.music.data.ads.AdMobManager,
    private val preferencesManager: com.sukoon.music.data.preferences.PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    // Tab State Management (persisted to DataStore, survives app restart)
    private val _selectedTab = MutableStateFlow<String?>(null)
    val selectedTab: StateFlow<String?> = _selectedTab.asStateFlow()

    init {
        // Load saved tab from DataStore on ViewModel creation
        viewModelScope.launch {
            // First, load the saved tab value immediately (or null if not set)
            try {
                val savedTab = preferencesManager.getSelectedHomeTabFlow().first()
                _selectedTab.value = savedTab
            } catch (e: Exception) {
                Log.e(TAG, "Error loading selected tab", e)
            }

            // Then collect for any future updates
            preferencesManager.getSelectedHomeTabFlow().collect { savedTab ->
                _selectedTab.value = savedTab
            }
        }

        // Load listening stats on HomeScreen open (lazy computation)
        viewModelScope.launch {
            loadListeningStats()
        }
    }

    /**
     * Load today's listening stats for the card.
     * Cleanup old data in background.
     */
    private suspend fun loadListeningStats() {
        try {
            // Clean up old data (older than 7 days)
            listeningStatsRepository.cleanupOldStats()

            // Get today's or most recent stats
            val totalTime = listeningStatsRepository.getTotalListeningTime7Days()
            val topArtist = listeningStatsRepository.getTopArtist7Days()
            val timeOfDay = listeningStatsRepository.getPeakTimeOfDay7Days()

            // Show card if user has at least 30 minutes of listening time
            if (totalTime >= 30) {
                _listeningStats.value = ListeningStatsSnapshot(
                    totalListeningTimeMinutes = totalTime,
                    topArtist = topArtist,
                    peakTimeOfDay = timeOfDay ?: "unknown"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading listening stats", e)
        }
    }

    fun setSelectedTab(tab: String) {
        _selectedTab.value = tab
        viewModelScope.launch {
            preferencesManager.setSelectedHomeTab(tab)
        }
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

    // Rediscover Albums State (albums not played in last 30 days)
    val rediscoverAlbums: StateFlow<List<Album>> = songRepository.getRediscoverAlbums()
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

    // Track lyrics fetch job to prevent race conditions
    private var lyricsFetchJob: Job? = null

    // Listening Stats State
    private val _listeningStats = MutableStateFlow<ListeningStatsSnapshot?>(null)
    val listeningStats: StateFlow<ListeningStatsSnapshot?> = _listeningStats.asStateFlow()

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
                playbackRepository.shuffleAndPlayQueue(allSongs)
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
            // Refresh playback state to reflect the like status change
            playbackRepository.refreshCurrentSong()
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
        Log.d(TAG, "=== FETCH LYRICS CALLED ===")
        Log.d(TAG, "Song: ${song.title} by ${song.artist}")

        // Cancel previous fetch job to prevent race conditions
        lyricsFetchJob?.cancel()
        Log.d(TAG, "Previous job cancelled (if any)")

        // Validate song data before fetching
        if (song.title.isBlank() || song.artist.isBlank()) {
            Log.w(TAG, "Invalid song data - title or artist is blank")
            _lyricsState.value = LyricsState.NotFound
            return
        }

        // Reset to loading state
        _lyricsState.value = LyricsState.Loading
        Log.d(TAG, "State set to Loading")

        // Start new fetch job
        lyricsFetchJob = viewModelScope.launch {
            try {
                Log.d(TAG, "Starting lyrics repository flow collection...")
                lyricsRepository.getLyrics(
                    trackId = song.id,
                    audioUri = song.uri,
                    artist = song.artist,
                    title = song.title,
                    album = song.album,
                    duration = (song.duration / 1000).toInt()
                ).collect { state ->
                    Log.d(TAG, "Received lyrics state: ${state::class.simpleName}")
                    _lyricsState.value = state
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchLyrics", e)
                _lyricsState.value = LyricsState.Error("Failed to fetch lyrics: ${e.message}")
            }
        }
    }

    fun updateLyricsSyncOffset(trackId: Long, offsetMs: Long) {
        viewModelScope.launch {
            lyricsRepository.updateSyncOffset(trackId, offsetMs)
            // Re-fetch lyrics with updated offset
            playbackState.value.currentSong?.let { song ->
                fetchLyrics(song)
            }
        }
    }

    fun clearLyrics(trackId: Long) {
        // Cancel any ongoing fetch
        lyricsFetchJob?.cancel()
        lyricsFetchJob = null

        viewModelScope.launch {
            lyricsRepository.clearLyrics(trackId)
            _lyricsState.value = LyricsState.NotFound
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
