package com.sukoon.music.data.repository

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.sukoon.music.data.local.dao.SongAudioSettingsDao
import com.sukoon.music.data.local.entity.SongAudioSettingsEntity
import com.google.common.util.concurrent.MoreExecutors
import com.sukoon.music.data.service.MusicPlaybackService
import com.sukoon.music.di.ApplicationScope
import com.sukoon.music.domain.model.PlaybackUpdateSource
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.RepeatMode
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.model.SongAudioSettings
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.data.playback.PerSongSettingsCommand
import com.sukoon.music.data.playback.PlaybackCustomCommandBridge
import com.sukoon.music.util.DevLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementation of PlaybackRepository that wraps MediaController.
 * This is the single source of truth for playback state, following the critical
 * requirement that UI must never interact with ExoPlayer directly.
 */
@UnstableApi
@Singleton
class PlaybackRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val songRepository: com.sukoon.music.domain.repository.SongRepository,
    private val preferencesManager: com.sukoon.music.data.preferences.PreferencesManager,
    private val queueRepository: com.sukoon.music.domain.repository.QueueRepository,
    private val listeningStatsRepository: com.sukoon.music.domain.repository.ListeningStatsRepository,
    private val songAudioSettingsDao: SongAudioSettingsDao,
    private val sessionController: com.sukoon.music.domain.usecase.SessionController,
    private val customCommandBridge: PlaybackCustomCommandBridge
) : PlaybackRepository {

    // State Management
    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // MediaController Reference
    private var mediaController: MediaController? = null
    private var connectionJob: Job? = null

    // Track the current source name (e.g., "Album: Graduation", "Playlist: Favorites")
    private var currentSourceName: String? = null

    // Audio Focus State Tracking
    private var pausedByAudioFocusLoss = false
    private var pausedByNoisyAudio = false

    // Recently Played Tracking
    private var lastLoggedSongId: Long? = null

    // Connection State Tracking
    private var isConnected = false

    // Thread safety locks
    private val stateUpdateMutex = Mutex()
    private val connectionLock = Any()

    // Listening Stats Tracking - track actual playback duration per song
    private var currentSongId: Long? = null
    private var currentSongStartTimeMs: Long = 0L
    private var currentSongArtist: String = ""

    // Queue Auto-Save Tracking
    private var queueAutoSaveJob: Job? = null
    private var lastSavedQueue: List<Song> = emptyList()
    private var currentSavedQueueId: Long? = null

    // Player Listener
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // Reset flags when user explicitly plays
            if (isPlaying) {
                pausedByAudioFocusLoss = false
                pausedByNoisyAudio = false
            }
            requestStateRefresh(
                source = PlaybackUpdateSource.PLAYER_LISTENER,
                persistRecoveryState = true
            )
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            requestStateRefresh(source = PlaybackUpdateSource.PLAYER_LISTENER)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Record previous song's actual listening duration (if any)
            if (currentSongId != null && currentSongArtist.isNotEmpty()) {
                val actualListeningDurationMs = System.currentTimeMillis() - currentSongStartTimeMs
                // Only record if user listened for at least 1 second (ignore accidental skips)
                if (actualListeningDurationMs >= 1000) {
                    scope.launch {
                        // Check if private session is active before logging stats
                        if (!sessionController.isSessionPrivate()) {
                            listeningStatsRepository.recordPlayEvent(currentSongArtist, actualListeningDurationMs)
                        }
                        // Refresh inactivity timer on playback event
                        sessionController.refreshInactivityTimer()
                    }
                }
            }

            // Log new song to recently played
            mediaItem?.mediaId?.toLongOrNull()?.let { songId ->
                // Only log if it's a different song than the last one we logged
                if (songId != lastLoggedSongId) {
                    lastLoggedSongId = songId

                    // Check private session mode before logging
                    scope.launch {
                        if (!sessionController.isSessionPrivate()) {
                            songRepository.logSongPlay(songId)
                        }
                        // Refresh inactivity timer on playback event
                        sessionController.refreshInactivityTimer()
                    }
                }
            }

            scope.launch {
                stateUpdateMutex.withLock {
                    // Update tracking for current song (within mutex to prevent race conditions)
                    mediaItem?.mediaId?.toLongOrNull()?.let { songId ->
                        currentSongId = songId
                        currentSongArtist = mediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown Artist"
                        currentSongStartTimeMs = System.currentTimeMillis()
                    }

                    updatePlaybackState(source = PlaybackUpdateSource.PLAYER_LISTENER)
                    savePlaybackStateForRecovery()
                }
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            requestStateRefresh(source = PlaybackUpdateSource.PLAYER_LISTENER)
        }

        override fun onShuffleModeEnabledChanged(shuffleEnabled: Boolean) {
            requestStateRefresh(source = PlaybackUpdateSource.PLAYER_LISTENER)
        }

        override fun onPlayerErrorChanged(error: PlaybackException?) {
            requestStateRefresh(source = PlaybackUpdateSource.PLAYER_LISTENER)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (playWhenReady) {
                pausedByAudioFocusLoss = false
                pausedByNoisyAudio = false
            } else {
                when (reason) {
                    Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> {
                        pausedByAudioFocusLoss = true
                        pausedByNoisyAudio = false
                    }
                    Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY -> {
                        pausedByNoisyAudio = true
                        pausedByAudioFocusLoss = false
                    }
                    Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE,
                    Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> {
                        // Explicit user/remote action - clear focus/noisy flags
                        pausedByAudioFocusLoss = false
                        pausedByNoisyAudio = false
                    }
                }
            }
            requestStateRefresh(source = PlaybackUpdateSource.PLAYER_LISTENER)
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            requestStateRefresh(
                source = PlaybackUpdateSource.PLAYER_LISTENER,
                persistRecoveryState = true
            )
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            requestStateRefresh(source = PlaybackUpdateSource.PLAYER_LISTENER)
        }
    }

    // Lifecycle Methods

    private fun releaseCurrentControllerLocked() {
        try {
            mediaController?.removeListener(playerListener)
            mediaController?.release()
        } catch (e: Exception) {
            DevLogger.e("PlaybackRepository", "Error releasing MediaController", e)
        }
        mediaController = null
    }

    private fun requestStateRefresh(
        source: PlaybackUpdateSource,
        persistRecoveryState: Boolean = false,
        forcePositionResync: Boolean = false
    ) {
        scope.launch {
            stateUpdateMutex.withLock {
                updatePlaybackState(
                    source = source,
                    forcePositionResync = forcePositionResync
                )
                if (persistRecoveryState) {
                    savePlaybackStateForRecovery()
                }
            }
        }
    }

    override suspend fun connect() {
        val jobToJoin: Job? = synchronized(connectionLock) {
            if (isConnected && mediaController != null) {
                null
            } else {
                connectionJob?.takeIf { it.isActive } ?: run {
                    if (mediaController != null) {
                        releaseCurrentControllerLocked()
                    }

                    val connectJob = scope.launch {
                        val thisJob = coroutineContext[Job]
                        try {
                            val sessionToken = SessionToken(
                                context,
                                ComponentName(context, MusicPlaybackService::class.java)
                            )

                            val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

                            val connectedController = suspendCancellableCoroutine<MediaController> { continuation ->
                                controllerFuture.addListener({
                                    try {
                                        continuation.resume(controllerFuture.get())
                                    } catch (e: Exception) {
                                        continuation.resumeWithException(e)
                                    }
                                }, MoreExecutors.directExecutor())

                                continuation.invokeOnCancellation {
                                    controllerFuture.cancel(true)
                                }
                            }

                            synchronized(connectionLock) {
                                if (mediaController != null && mediaController !== connectedController) {
                                    releaseCurrentControllerLocked()
                                }
                                mediaController = connectedController
                                mediaController?.addListener(playerListener)
                                isConnected = true
                            }

                            stateUpdateMutex.withLock {
                                updatePlaybackState(source = PlaybackUpdateSource.SERVICE_RECOVERY)
                            }

                            // Restore last queue on app launch if queue is empty
                            if (connectedController.mediaItemCount == 0) {
                                restoreLastQueue()
                            }
                        } catch (e: Exception) {
                            synchronized(connectionLock) {
                                isConnected = false
                                releaseCurrentControllerLocked()
                            }
                            stateUpdateMutex.withLock {
                                _playbackState.update {
                                    it.copy(
                                        error = "Failed to connect to playback service: ${e.message}",
                                        lastUpdateSource = PlaybackUpdateSource.SERVICE_RECOVERY
                                    )
                                }
                            }
                            DevLogger.e("PlaybackRepository", "Connection failed", e)
                        } finally {
                            synchronized(connectionLock) {
                                if (connectionJob === thisJob) {
                                    connectionJob = null
                                }
                            }
                        }
                    }

                    connectionJob = connectJob
                    connectJob
                }
            }
        }

        jobToJoin?.join()
    }

    /**
     * Restore the last saved queue on app launch.
     * Also restores playback position if available (for process death recovery).
     * Includes validation to verify song at index matches saved song ID.
     */
    private fun restoreLastQueue() {
        scope.launch {
            try {
                val playbackState = preferencesManager.getPlaybackState()
                val currentQueue = queueRepository.getCurrentQueueWithSongs()

                if (currentQueue == null || currentQueue.songs.isEmpty()) {
                    DevLogger.d("PlaybackRepository", "No queue to restore")
                    return@launch
                }

                val mediaItems = mutableListOf<MediaItem>()
                for (song in currentQueue.songs) {
                    try {
                        mediaItems.add(song.toMediaItemWithSettings())
                    } catch (e: Exception) {
                        DevLogger.e("PlaybackRepository", "Invalid song URI: ${song.title}", e)
                    }
                }

                if (mediaItems.isEmpty()) {
                    DevLogger.e("PlaybackRepository", "Failed to convert any songs to MediaItems")
                    return@launch
                }

                // Restore playback position if available
                if (playbackState != null) {
                    val (savedSongId, savedIndex, position) = playbackState

                    // Verify saved index is valid
                    val validIndex = savedIndex.coerceIn(0, mediaItems.size - 1)

                    // Verify song at index matches saved song ID (prevent mismatch)
                    val songAtIndex = currentQueue.songs.getOrNull(validIndex)
                    if (songAtIndex?.id != savedSongId) {
                        // Song mismatch - find correct position
                        val correctIndex = currentQueue.songs.indexOfFirst { it.id == savedSongId }
                        if (correctIndex >= 0) {
                            DevLogger.d("PlaybackRepository",
                                "Song mismatch: expected=$savedSongId at index=$savedIndex, found at=$correctIndex")
                            mediaController?.setMediaItems(mediaItems, correctIndex, position)
                        } else {
                            // Saved song no longer in queue, restore to saved index
                            DevLogger.w("PlaybackRepository",
                                "Saved song ($savedSongId) not in queue, restoring to index=$validIndex")
                            mediaController?.setMediaItems(mediaItems, validIndex, position)
                        }
                    } else {
                        // Match confirmed, restore to saved position
                        mediaController?.setMediaItems(mediaItems, validIndex, position)
                    }

                    preferencesManager.clearPlaybackState()
                } else {
                    // No saved position, just restore queue
                    mediaController?.setMediaItems(mediaItems)
                }

                // Restore source name
                val userPrefs = preferencesManager.userPreferencesFlow.first()
                currentSourceName = userPrefs.lastQueueName

                // Prepare but don't auto-play
                mediaController?.prepare()

                currentSavedQueueId = currentQueue.queue.id
                lastSavedQueue = currentQueue.songs
                stateUpdateMutex.withLock {
                    updatePlaybackState(source = PlaybackUpdateSource.SERVICE_RECOVERY)
                }

                DevLogger.d("PlaybackRepository", "Queue restored: ${currentQueue.songs.size} songs")
            } catch (e: Exception) {
                DevLogger.e("PlaybackRepository", "Failed to restore queue", e)
                stateUpdateMutex.withLock {
                    _playbackState.update {
                        it.copy(
                            error = "Failed to restore playback: ${e.message}",
                            lastUpdateSource = PlaybackUpdateSource.SERVICE_RECOVERY
                        )
                    }
                }
            }
        }
    }

    /**
     * Save current playback state for recovery after process death.
     * Called when playback state changes.
     */
    private fun savePlaybackStateForRecovery() {
        val controller = mediaController ?: return

        // Only save if we have a valid current item
        val currentSongId = controller.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        val currentIndex = controller.currentMediaItemIndex
        val currentPosition = controller.currentPosition

        // Save state in background
        scope.launch {
            try {
                preferencesManager.savePlaybackStateExtended(
                    songId = currentSongId,
                    queueIndex = currentIndex,
                    positionMs = currentPosition,
                    queueName = currentSourceName
                )
            } catch (e: Exception) {
                // Log but don't crash - state saving is not critical
                DevLogger.e("PlaybackRepository", "Failed to save playback state", e)
            }
        }
    }

    override fun disconnect() {
        synchronized(connectionLock) {
            connectionJob?.cancel()
            connectionJob = null
            releaseCurrentControllerLocked()
            isConnected = false
        }
    }

    // State Synchronization

    private fun updatePlaybackState(
        source: PlaybackUpdateSource = PlaybackUpdateSource.UNKNOWN,
        forcePositionResync: Boolean = false
    ) {
        val controller = mediaController ?: return

        // Extract queue from MediaController
        val queue = mutableListOf<Song>()
        for (i in 0 until controller.mediaItemCount) {
            try {
                controller.getMediaItemAt(i).toSong()?.let { queue.add(it) }
            } catch (e: Exception) {
                DevLogger.e("PlaybackRepository", "Failed to convert queue item at index $i", e)
            }
        }

        if (queue.size != controller.mediaItemCount) {
            DevLogger.w(
                "PlaybackRepository",
                "Queue conversion incomplete: converted=${queue.size}, total=${controller.mediaItemCount}"
            )
        }

        // Auto-save queue if it has changed
        if (queue != lastSavedQueue && queue.isNotEmpty()) {
            autoSaveQueue(queue)
        }

        // Get current song ID for async liked status fetch
        val currentMediaItem = controller.currentMediaItem
        val currentSongBasic = currentMediaItem?.toSong()

        _playbackState.update { currentState ->
            val duration = controller.duration
                .takeIf { it != C.TIME_UNSET && it > 0L }
                ?: 0L
            val rawPosition = controller.currentPosition.coerceAtLeast(0L)
            val currentPosition = if (duration > 0L) {
                rawPosition.coerceAtMost(duration)
            } else {
                rawPosition
            }

            currentState.copy(
                isPlaying = controller.isPlaying,
                isLoading = controller.playbackState == Player.STATE_BUFFERING,
                currentPosition = currentPosition,
                duration = duration,
                currentSong = currentSongBasic,
                playbackSpeed = controller.playbackParameters.speed,
                repeatMode = controller.repeatMode.toRepeatMode(),
                shuffleEnabled = controller.shuffleModeEnabled,
                error = controller.playerError?.message,

                // Queue state
                queue = queue,
                currentQueueIndex = controller.currentMediaItemIndex.coerceAtLeast(-1),
                currentQueueId = currentSavedQueueId,
                currentQueueName = currentSourceName ?: if (currentSavedQueueId != null) "Current Queue" else null,
                queueTimestamp = System.currentTimeMillis(),
                lastUpdateSource = source,

                // Audio focus state
                pausedByAudioFocusLoss = pausedByAudioFocusLoss,
                pausedByNoisyAudio = pausedByNoisyAudio
            )
        }

        if (forcePositionResync) {
            DevLogger.d("PlaybackRepository", "Forced position resync from ${source.name}")
        }

        // Fetch accurate liked status asynchronously for current song
        // Only emit update if liked status differs from initial (prevents duplicate recompositions)
        if (currentSongBasic != null) {
            scope.launch {
                val songWithLikedStatus = songRepository.getSongById(currentSongBasic.id)
                if (songWithLikedStatus != null && songWithLikedStatus.isLiked != currentSongBasic.isLiked) {
                    stateUpdateMutex.withLock {
                        _playbackState.update { currentState ->
                            // Only update if this is still the current song (avoid race conditions)
                            if (currentState.currentSong?.id == songWithLikedStatus.id) {
                                currentState.copy(
                                    currentSong = songWithLikedStatus,
                                    lastUpdateSource = source
                                )
                            } else {
                                currentState
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Auto-save the current queue to the database.
     * Debounces rapid queue changes to avoid excessive database writes.
     */
    private fun autoSaveQueue(queue: List<Song>) {
        // Cancel any pending auto-save
        queueAutoSaveJob?.cancel()

        // Schedule new auto-save after a short delay (debounce)
        queueAutoSaveJob = scope.launch {
            kotlinx.coroutines.delay(2000) // 2 second debounce
            try {
                // Check if user has enabled queue auto-save in preferences
                preferencesManager.userPreferencesFlow.collect { prefs ->
                    // Save the queue
                    currentSavedQueueId = queueRepository.saveCurrentPlaybackQueue(
                        name = "Current Queue",
                        songs = queue,
                        markAsCurrent = true
                    )
                    lastSavedQueue = queue
                    // Cancel collection after first emit
                    return@collect
                }
            } catch (e: Exception) {
                // Log error but don't crash - queue saving is not critical
                DevLogger.e("PlaybackRepository", "Failed to auto-save queue", e)
            }
        }
    }

    // Transport Controls

    override suspend fun play() {
        mediaController?.play()
    }

    override suspend fun pause() {
        mediaController?.pause()
    }

    override suspend fun playPause() {
        mediaController?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    override suspend fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        val duration = controller.duration.takeIf { it != C.TIME_UNSET } ?: 0L

        // Clamp seek position to valid range
        val validPosition = positionMs.coerceIn(0L, duration)

        try {
            controller.seekTo(validPosition)
        } catch (e: Exception) {
            stateUpdateMutex.withLock {
                _playbackState.update {
                    it.copy(
                        error = "Seek failed: ${e.message}",
                        lastUpdateSource = PlaybackUpdateSource.UNKNOWN
                    )
                }
            }
            DevLogger.e("PlaybackRepository", "Error seeking to $validPosition", e)
        }
    }

    override suspend fun seekToNext() {
        mediaController?.seekToNext()
    }

    override suspend fun seekToPrevious() {
        mediaController?.seekToPrevious()
    }

    // Queue Management

    override suspend fun playSong(song: Song, queueName: String?) {
        // Guard: Don't restart if already playing this song
        if (_playbackState.value.currentSong?.id == song.id) {
            return
        }

        currentSourceName = queueName
        mediaController?.let { controller ->
            try {
                controller.setMediaItem(song.toMediaItemWithSettings())
                controller.prepare()
                controller.play()
            } catch (e: Exception) {
                stateUpdateMutex.withLock {
                    _playbackState.update {
                        it.copy(
                            error = "Failed to play song: ${e.message}",
                            lastUpdateSource = PlaybackUpdateSource.UNKNOWN
                        )
                    }
                }
                DevLogger.e("PlaybackRepository", "Error playing song: ${song.title}", e)
            }
        }
    }

    override suspend fun playQueue(songs: List<Song>, startIndex: Int, queueName: String?) {
        // Guard: Skip only when queue content/order and index are already identical.
        // If only the current song matches but queue differs (e.g., filtered/visible order),
        // we must rebuild queue so Next/Previous follows the visible list order.
        val currentState = _playbackState.value
        val isSameSongAtIndex = startIndex in songs.indices &&
            currentState.currentSong?.id == songs[startIndex].id
        val hasSameIndex = currentState.currentQueueIndex == startIndex
        val hasSameQueueOrder = currentState.queue.size == songs.size &&
            currentState.queue.map { it.id } == songs.map { it.id }
        if (isSameSongAtIndex && hasSameIndex && hasSameQueueOrder) return

        currentSourceName = queueName
        mediaController?.let { controller ->
            try {
                val safeStartIndex = if (songs.isEmpty()) 0 else startIndex.coerceIn(0, songs.lastIndex)
                val mediaItems = songs.toMediaItemsWithSettings()
                controller.setMediaItems(
                    mediaItems,
                    safeStartIndex,
                    0L
                )
                controller.prepare()
                controller.play()
            } catch (e: Exception) {
                stateUpdateMutex.withLock {
                    _playbackState.update {
                        it.copy(
                            error = "Failed to play queue: ${e.message}",
                            lastUpdateSource = PlaybackUpdateSource.UNKNOWN
                        )
                    }
                }
                DevLogger.e("PlaybackRepository", "Error playing queue", e)
            }
        }
    }

    override suspend fun shuffleAndPlayQueue(songs: List<Song>, queueName: String?) {
        if (songs.isEmpty()) return

        // Fisher-Yates shuffle algorithm
        val shuffled = songs.toMutableList()
        for (i in shuffled.size - 1 downTo 1) {
            val j = (0..i).random()
            val temp = shuffled[i]
            shuffled[i] = shuffled[j]
            shuffled[j] = temp
        }

        // Play shuffled queue and disable ExoPlayer's shuffle since we shuffled manually
        playQueue(shuffled, startIndex = 0, queueName = queueName)
        setShuffleEnabled(false)
    }

    override suspend fun addToQueue(song: Song) {
        mediaController?.addMediaItem(song.toMediaItemWithSettings())
    }

    override suspend fun addToQueue(songs: List<Song>) {
        mediaController?.addMediaItems(songs.toMediaItemsWithSettings())
    }

    override suspend fun playNext(song: Song) {
        mediaController?.let { controller ->
            val currentIndex = controller.currentMediaItemIndex
            val insertIndex = if (currentIndex == -1) 0 else currentIndex + 1
            controller.addMediaItem(insertIndex, song.toMediaItemWithSettings())
        }
    }

    override suspend fun playNext(songs: List<Song>) {
        mediaController?.let { controller ->
            val currentIndex = controller.currentMediaItemIndex
            val insertIndex = if (currentIndex == -1) 0 else currentIndex + 1
            controller.addMediaItems(insertIndex, songs.toMediaItemsWithSettings())
        }
    }

    override suspend fun addToQueueNext(songs: List<Song>) {
        mediaController?.let { controller ->
            val currentIndex = controller.currentMediaItemIndex
            val insertIndex = if (currentIndex == -1) 0 else currentIndex + 1
            controller.addMediaItems(insertIndex, songs.toMediaItemsWithSettings())
        }
    }

    override suspend fun removeFromQueue(index: Int) {
        mediaController?.removeMediaItem(index)
    }

    override suspend fun seekToQueueIndex(index: Int) {
        val controller = mediaController ?: return
        if (controller.mediaItemCount <= 0) {
            DevLogger.w("PlaybackRepository", "seekToQueueIndex ignored because queue is empty")
            return
        }

        // Validate index is in bounds
        val validIndex = index.coerceIn(0, controller.mediaItemCount - 1)

        if (validIndex != index) {
            DevLogger.w("PlaybackRepository",
                "Queue index out of bounds: requested=$index, clamped=$validIndex, max=${controller.mediaItemCount - 1}")
        }

        try {
            controller.seekToDefaultPosition(validIndex)
        } catch (e: Exception) {
            stateUpdateMutex.withLock {
                _playbackState.update {
                    it.copy(
                        error = "Failed to seek to index $validIndex: ${e.message}",
                        lastUpdateSource = PlaybackUpdateSource.UNKNOWN
                    )
                }
            }
            DevLogger.e("PlaybackRepository", "Error seeking to queue index", e)
        }
    }

    // Playback Configuration

    override suspend fun setRepeatMode(mode: RepeatMode) {
        mediaController?.repeatMode = mode.toPlayerRepeatMode()
    }

    override suspend fun setShuffleEnabled(enabled: Boolean) {
        mediaController?.shuffleModeEnabled = enabled
    }

    override suspend fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
    }

    override suspend fun refreshCurrentSong() {
        val currentSongId = _playbackState.value.currentSong?.id ?: return
        val freshSong = songRepository.getSongById(currentSongId) ?: return

        stateUpdateMutex.withLock {
            _playbackState.update { currentState ->
                // Only update if this is still the current song
                if (currentState.currentSong?.id == currentSongId) {
                    currentState.copy(
                        currentSong = freshSong,
                        lastUpdateSource = PlaybackUpdateSource.UNKNOWN
                    )
                } else {
                    currentState
                }
            }
        }
    }

    override fun refreshPlaybackState(forcePositionResync: Boolean) {
        val source = if (forcePositionResync) {
            PlaybackUpdateSource.PERIODIC_UI_TICK
        } else {
            PlaybackUpdateSource.ACTIVITY_RESUME
        }

        if (stateUpdateMutex.tryLock()) {
            try {
                updatePlaybackState(
                    source = source,
                    forcePositionResync = forcePositionResync
                )
            } finally {
                stateUpdateMutex.unlock()
            }
            return
        }

        requestStateRefresh(
            source = source,
            forcePositionResync = forcePositionResync
        )
    }

    override suspend fun savePlaybackState() {
        val controller = mediaController ?: return

        val currentSongId = controller.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        val currentIndex = controller.currentMediaItemIndex
        val currentPosition = controller.currentPosition.coerceAtLeast(0L)

        try {
            preferencesManager.savePlaybackStateExtended(
                songId = currentSongId,
                queueIndex = currentIndex,
                positionMs = currentPosition,
                queueName = currentSourceName
            )
            DevLogger.d("PlaybackRepository", "Playback state saved: song=$currentSongId, pos=${currentPosition}ms")
        } catch (e: Exception) {
            DevLogger.e("PlaybackRepository", "Failed to save playback state", e)
        }
    }

    override suspend fun reapplyCurrentSongSettings(songId: Long) {
        val persisted = songAudioSettingsDao.getSettings(songId)?.toDomain()
            ?: SongAudioSettings(songId = songId)
        applyCurrentSongSettingsImmediately(songId, persisted)
    }

    override suspend fun previewCurrentSongSettings(songId: Long, settings: SongAudioSettings) {
        try {
            val controller = mediaController ?: return
            val currentSongId = controller.currentMediaItem?.mediaId?.toLongOrNull() ?: return
            if (currentSongId != songId) return

            val desiredClipping = settings.toClippingConfigurationOrNull()
            val clippingUnchanged = controller.currentMediaItem.hasEquivalentClipping(desiredClipping)
            if (!clippingUnchanged) {
                replaceCurrentItemWithSettings(controller, settings)
            }
            sendPerSongSettingsCommand(controller, settings)
            stateUpdateMutex.withLock {
                updatePlaybackState(source = PlaybackUpdateSource.PLAYER_LISTENER)
            }
        } catch (e: Exception) {
            DevLogger.e("PlaybackRepository", "Failed to preview per-song settings", e)
        }
    }

    override suspend fun applyCurrentSongSettingsImmediately(songId: Long, settings: SongAudioSettings) {
        try {
            val controller = mediaController ?: return
            val currentSongId = controller.currentMediaItem?.mediaId?.toLongOrNull() ?: return
            if (currentSongId != songId) return

            replaceCurrentItemWithSettings(controller, settings)
            sendPerSongSettingsCommand(controller, settings)
            stateUpdateMutex.withLock {
                updatePlaybackState(source = PlaybackUpdateSource.PLAYER_LISTENER)
            }
        } catch (e: Exception) {
            DevLogger.e("PlaybackRepository", "Failed to apply per-song settings immediately", e)
        }
    }

    private suspend fun replaceCurrentItemWithSettings(
        controller: MediaController,
        settings: SongAudioSettings
    ) {
        val currentIndex = controller.currentMediaItemIndex
        if (currentIndex < 0) return

        val currentSong = (
            _playbackState.value.currentSong?.takeIf { it.id == settings.songId }
                ?: controller.currentMediaItem?.toSong()
            ) ?: return
        if (currentSong.id != settings.songId) return

        val updatedCurrentItem = try {
            currentSong.toMediaItemWithSettings(overrideSettings = settings)
        } catch (e: Exception) {
            DevLogger.e(
                "PlaybackRepository",
                "Failed to build media item for song ${currentSong.id}",
                e
            )
            return
        }
        val currentPosition = controller.currentPosition.coerceAtLeast(0L)
        val shouldResume = controller.isPlaying
        val targetPosition = settings.adjustedPositionForClipping(currentPosition)

        val mediaItems = mutableListOf<MediaItem>()
        for (index in 0 until controller.mediaItemCount) {
            mediaItems.add(if (index == currentIndex) updatedCurrentItem else controller.getMediaItemAt(index))
        }

        controller.setMediaItems(mediaItems, currentIndex, targetPosition)
        controller.prepare()
        if (shouldResume) controller.play()
    }

    private fun sendPerSongSettingsCommand(controller: MediaController, settings: SongAudioSettings) {
        controller.playbackParameters = if (settings.isEnabled) {
            PlaybackParameters(settings.speed, settings.pitch)
        } else {
            PlaybackParameters(1.0f, 1.0f)
        }

        val args = PerSongSettingsCommand.toBundle(settings)
        val bridgeResult = customCommandBridge.dispatch(PerSongSettingsCommand.ACTION_APPLY, args)
        if (bridgeResult == SessionResult.RESULT_SUCCESS) return

        try {
            controller.sendCustomCommand(
                SessionCommand(PerSongSettingsCommand.ACTION_APPLY, Bundle.EMPTY),
                args
            )
        } catch (e: Exception) {
            DevLogger.e("PlaybackRepository", "Failed to send per-song settings command", e)
        }
    }

    // Mapper Extensions

    /**
     * Convert Song domain model to Media3 MediaItem.
     */
    private suspend fun Song.toMediaItemWithSettings(
        overrideSettings: SongAudioSettings? = null
    ): MediaItem {
        // Validate URI is not empty
        if (uri.isBlank()) {
            throw IllegalArgumentException("Song URI cannot be empty for song: $title (ID: $id)")
        }

        val clippingConfig = overrideSettings?.toClippingConfigurationOrNull()
            ?: songAudioSettingsDao.getSettings(id)?.toClippingConfigurationOrNull()
        val builder = MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(albumArtUri?.let { Uri.parse(it) })
                    .build()
            )
        clippingConfig?.let(builder::setClippingConfiguration)
        return builder.build()
    }

    private suspend fun List<Song>.toMediaItemsWithSettings(): List<MediaItem> {
        val mediaItems = mutableListOf<MediaItem>()
        for (song in this) {
            mediaItems.add(song.toMediaItemWithSettings())
        }
        return mediaItems
    }

    private fun SongAudioSettings.adjustedPositionForClipping(currentPosition: Long): Long {
        if (!isEnabled) return currentPosition
        val startMs = trimStartMs.coerceAtLeast(0L)
        val endMs = trimEndMs.takeIf { it > startMs }
        var position = currentPosition.coerceAtLeast(0L).coerceAtLeast(startMs)
        if (endMs != null) {
            position = position.coerceAtMost((endMs - 1L).coerceAtLeast(startMs))
        }
        return position
    }

    private fun MediaItem?.hasEquivalentClipping(
        desired: MediaItem.ClippingConfiguration?
    ): Boolean {
        val current = this?.clippingConfiguration
        if (desired == null) {
            return current == null ||
                (current.startPositionMs <= 0L && current.endPositionMs == C.TIME_END_OF_SOURCE)
        }
        return current != null &&
            current.startPositionMs == desired.startPositionMs &&
            current.endPositionMs == desired.endPositionMs
    }

    private fun SongAudioSettingsEntity.toClippingConfigurationOrNull(): MediaItem.ClippingConfiguration? {
        if (!isEnabled) return null

        val startMs = trimStartMs.coerceAtLeast(0L)
        val endMs = trimEndMs.takeIf { it > startMs }
        if (startMs <= 0L && endMs == null) return null

        val clippingBuilder = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(startMs)
        if (endMs != null) {
            clippingBuilder.setEndPositionMs(endMs)
        }
        return clippingBuilder.build()
    }

    private fun SongAudioSettings.toClippingConfigurationOrNull(): MediaItem.ClippingConfiguration? {
        if (!isEnabled) return null

        val startMs = trimStartMs.coerceAtLeast(0L)
        val endMs = trimEndMs.takeIf { it > startMs }
        if (startMs <= 0L && endMs == null) return null

        val clippingBuilder = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(startMs)
        if (endMs != null) {
            clippingBuilder.setEndPositionMs(endMs)
        }
        return clippingBuilder.build()
    }

    private fun SongAudioSettingsEntity.toDomain(): SongAudioSettings {
        return SongAudioSettings(
            songId = songId,
            isEnabled = isEnabled,
            trimStartMs = trimStartMs,
            trimEndMs = trimEndMs,
            eqEnabled = eqEnabled,
            band60Hz = band60Hz,
            band230Hz = band230Hz,
            band910Hz = band910Hz,
            band3600Hz = band3600Hz,
            band14000Hz = band14000Hz,
            bassBoost = bassBoost,
            virtualizerStrength = virtualizerStrength,
            reverbPreset = reverbPreset,
            pitch = pitch,
            speed = speed,
            updatedAt = updatedAt
        )
    }

    /**
     * Extract Song domain model from Media3 MediaItem.
     */
    private fun MediaItem.toSong(): Song? {
        val metadata = mediaMetadata
        val mediaId = mediaId.toLongOrNull() ?: return null

        // Get duration from player if available
        val duration = if (mediaController != null && mediaController?.currentMediaItem == this) {
            mediaController?.duration?.takeIf { it != C.TIME_UNSET } ?: 0L
        } else {
            0L // Duration will be updated in PlaybackState.duration
        }

        return Song(
            id = mediaId,
            title = metadata.title?.toString() ?: "",
            artist = metadata.artist?.toString() ?: "",
            album = metadata.albumTitle?.toString() ?: "",
            duration = duration, // Now properly fetched
            uri = localConfiguration?.uri?.toString() ?: "",
            albumArtUri = metadata.artworkUri?.toString(),
            dateAdded = 0L,
            isLiked = false // Will be updated asynchronously in updatePlaybackState
        )
    }

    /**
     * Convert Player repeat mode constant to RepeatMode enum.
     */
    private fun Int.toRepeatMode(): RepeatMode {
        return when (this) {
            Player.REPEAT_MODE_OFF -> RepeatMode.OFF
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }
    }

    /**
     * Convert RepeatMode enum to Player repeat mode constant.
     */
    private fun RepeatMode.toPlayerRepeatMode(): Int {
        return when (this) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }
}
