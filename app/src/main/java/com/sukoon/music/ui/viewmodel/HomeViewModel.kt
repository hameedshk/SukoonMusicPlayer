package com.sukoon.music.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.LyricsState
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.ScanState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.ListeningStatsRepository
import com.sukoon.music.domain.repository.ListeningStatsSnapshot
import com.sukoon.music.domain.repository.LyricsRepository
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SettingsRepository
import com.sukoon.music.domain.repository.SongRepository
import com.sukoon.music.ui.model.HomeTabKey
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sukoon.music.ui.theme.*

/**
 * ViewModel for the Home Screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    val playbackRepository: PlaybackRepository,
    private val lyricsRepository: LyricsRepository,
    private val listeningStatsRepository: ListeningStatsRepository,
    private val settingsRepository: SettingsRepository,
    val adMobManager: com.sukoon.music.data.ads.AdMobManager,
    private val preferencesManager: com.sukoon.music.data.preferences.PreferencesManager,
    private val sessionController: com.sukoon.music.domain.usecase.SessionController,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    // Tab State Management (persisted to DataStore, survives app restart)
    private val _selectedTab = MutableStateFlow(HomeTabKey.HOME)
    val selectedTab: StateFlow<HomeTabKey> = _selectedTab.asStateFlow()

    init {
        // Load saved tab from DataStore on ViewModel creation
        viewModelScope.launch {
            // First, load the saved tab value immediately (or null if not set)
            try {
                val savedTab = preferencesManager.getSelectedHomeTabFlow().first()
                _selectedTab.value = HomeTabKey.fromStoredValue(savedTab)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading selected tab", e)
            }

            // Then collect for any future updates
            preferencesManager.getSelectedHomeTabFlow().collect { savedTab ->
                _selectedTab.value = HomeTabKey.fromStoredValue(savedTab)
            }
        }

        // Load listening stats on HomeScreen open (lazy computation)
        viewModelScope.launch {
            loadListeningStats()
        }

        // Conditional startup scan: check preference and dedup by time
        viewModelScope.launch {
            tryStartupScan()
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

    /**
     * Conditional startup scan: check if scanOnStartup is enabled and last scan was > 30 min ago.
     * Launches scan asynchronously without blocking UI.
     */
    private suspend fun tryStartupScan() {
        try {
            // Check if user has enabled "Scan on Startup"
            val preferences = preferencesManager.userPreferencesFlow.first()
            if (!preferences.scanOnStartup) {
                return // User disabled auto-scan
            }

            // Check if enough time has passed since last scan (30 minutes)
            val lastScanTime = settingsRepository.getLastScanTime()
            val timeSinceLastScan = System.currentTimeMillis() - lastScanTime
            val thirtyMinutesMs = 30 * 60 * 1000L

            if (timeSinceLastScan < thirtyMinutesMs) {
                return // Last scan was recent, skip
            }

            // Trigger scan in background (non-blocking)
            songRepository.scanLocalMusic()
        } catch (e: Exception) {
            Log.e(TAG, "Error in startup scan", e)
        }
    }

    fun setSelectedTab(tab: HomeTabKey) {
        _selectedTab.value = tab
        viewModelScope.launch {
            preferencesManager.setSelectedHomeTab(tab.storageToken)
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

    // Track if current scan was user-initiated (for toast visibility)
    private val _isUserInitiatedScan = MutableStateFlow(false)
    val isUserInitiatedScan: StateFlow<Boolean> = _isUserInitiatedScan.asStateFlow()

    // Playback State
    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    // Private Session State (for visual indicator)
    val sessionState: StateFlow<com.sukoon.music.domain.model.PlaybackSessionState> =
        sessionController.sessionState()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = com.sukoon.music.domain.model.PlaybackSessionState()
            )

    // Lyrics State
    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Loading)
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()

    // Track lyrics fetch job to prevent race conditions
    private var lyricsFetchJob: Job? = null

    // Listening Stats State
    private val _listeningStats = MutableStateFlow<ListeningStatsSnapshot?>(null)
    val listeningStats: StateFlow<ListeningStatsSnapshot?> = _listeningStats.asStateFlow()

    // Song Selection State
    private val _selectedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSongIds: StateFlow<Set<Long>> = _selectedSongIds.asStateFlow()

    private val _isSongSelectionMode = MutableStateFlow(false)
    val isSongSelectionMode: StateFlow<Boolean> = _isSongSelectionMode.asStateFlow()

    // User Actions

    /**
     * Manual scan triggered by user (shows toast on completion).
     */
    fun scanLocalMusic() {
        viewModelScope.launch {
            _isUserInitiatedScan.value = true
            songRepository.scanLocalMusic()
        }
    }

    /**
     * Reset the user-initiated scan flag after toast is shown.
     */
    fun resetUserInitiatedScanFlag() {
        _isUserInitiatedScan.value = false
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

    fun playQueue(songs: List<Song>, startIndex: Int = 0, queueName: String? = null) {
        viewModelScope.launch {
            playbackRepository.playQueue(songs, startIndex, queueName = queueName)
        }
    }

    fun shuffleAll() {
        viewModelScope.launch {
            val allSongs = songs.value
            if (allSongs.isNotEmpty()) {
                playbackRepository.shuffleAndPlayQueue(allSongs, queueName = "All Songs")
            }
        }
    }

    fun playAll() {
        viewModelScope.launch {
            val allSongs = songs.value
            if (allSongs.isNotEmpty()) {
                playbackRepository.setShuffleEnabled(false)
                playbackRepository.playQueue(allSongs, startIndex = 0, queueName = "All Songs")
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

    // Song Selection Methods
    fun toggleSongSelectionMode(enabled: Boolean) {
        _isSongSelectionMode.value = enabled
        if (!enabled) {
            _selectedSongIds.value = emptySet()
        }
    }

    fun toggleSongSelection(songId: Long) {
        val current = _selectedSongIds.value.toMutableSet()
        if (current.contains(songId)) {
            current.remove(songId)
        } else {
            current.add(songId)
        }
        _selectedSongIds.value = current
    }

    fun selectAllSongs() {
        _selectedSongIds.value = songs.value.map { it.id }.toSet()
    }

    fun clearSongSelection() {
        _selectedSongIds.value = emptySet()
    }

    fun playSelectedSongs() {
        viewModelScope.launch {
            val selectedSongs = songs.value.filter { it.id in _selectedSongIds.value }
            if (selectedSongs.isNotEmpty()) {
                playbackRepository.playQueue(selectedSongs, queueName = "Selection")
                _isSongSelectionMode.value = false
                _selectedSongIds.value = emptySet()
            }
        }
    }

    fun playSelectedSongsNext() {
        viewModelScope.launch {
            val selectedSongs = songs.value.filter { it.id in _selectedSongIds.value }
            if (selectedSongs.isNotEmpty()) {
                selectedSongs.forEach { song ->
                    playbackRepository.playNext(song)
                }
            }
        }
    }

    fun addSelectedSongsToQueue() {
        viewModelScope.launch {
            val selectedSongs = songs.value.filter { it.id in _selectedSongIds.value }
            if (selectedSongs.isNotEmpty()) {
                selectedSongs.forEach { song ->
                    playbackRepository.addToQueue(song)
                }
            }
        }
    }

    fun deleteSelectedSongsWithResult(onResult: (DeleteHelper.DeleteResult) -> Unit) {
        viewModelScope.launch {
            val selectedSongsList = _selectedSongIds.value.toList()
            val selectedSongs = songs.value.filter { it.id in selectedSongsList }
            if (selectedSongs.isNotEmpty()) {
                val result = DeleteHelper.deleteSongs(context, selectedSongs)
                onResult(result)
                _selectedSongIds.value = emptySet()
                _isSongSelectionMode.value = false
            }
        }
    }

    // Sleep Timer Management (reactive from persistent store)
    val isSleepTimerActive: StateFlow<Boolean> = settingsRepository.userPreferences
        .map { it.sleepTimerTargetTimeMs > System.currentTimeMillis() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Set a sleep timer to pause playback after [minutes].
     * Passing 0 cancels the current timer.
     */
    fun setSleepTimer(minutes: Int) {
        viewModelScope.launch {
            if (minutes > 0) {
                val targetTimeMs = System.currentTimeMillis() + (minutes * 60 * 1000L)
                settingsRepository.setSleepTimerTargetTime(targetTimeMs)
            } else {
                settingsRepository.setSleepTimerTargetTime(0L)
            }
        }
    }
}
