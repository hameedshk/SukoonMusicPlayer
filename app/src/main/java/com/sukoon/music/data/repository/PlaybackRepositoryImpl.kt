package com.sukoon.music.data.repository

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.sukoon.music.data.service.MusicPlaybackService
import com.sukoon.music.di.ApplicationScope
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.RepeatMode
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.PlaybackRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
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
    private val preferencesManager: com.sukoon.music.data.preferences.PreferencesManager
) : PlaybackRepository {

    // State Management
    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // MediaController Reference
    private var mediaController: MediaController? = null
    private var connectionJob: Job? = null

    // Audio Focus State Tracking
    private var pausedByAudioFocusLoss = false
    private var pausedByNoisyAudio = false

    // Recently Played Tracking
    private var lastLoggedSongId: Long? = null

    // Player Listener
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // Reset flags when user explicitly plays
            if (isPlaying) {
                pausedByAudioFocusLoss = false
                pausedByNoisyAudio = false
            }
            updatePlaybackState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Log to recently played when a new song starts
            mediaItem?.mediaId?.toLongOrNull()?.let { songId ->
                // Only log if it's a different song than the last one we logged
                if (songId != lastLoggedSongId) {
                    lastLoggedSongId = songId
                    // Check private session mode before logging
                    scope.launch {
                        preferencesManager.userPreferencesFlow.collect { prefs ->
                            if (!prefs.isPrivateSessionEnabled) {
                                songRepository.logSongPlay(songId)
                            }
                            // Cancel collection after first emit
                            return@collect
                        }
                    }
                }
            }
            updatePlaybackState()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            updatePlaybackState()
        }

        override fun onShuffleModeEnabledChanged(shuffleEnabled: Boolean) {
            updatePlaybackState()
        }

        override fun onPlayerErrorChanged(error: PlaybackException?) {
            updatePlaybackState()
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
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
                    // Explicit user action - clear flags
                    pausedByAudioFocusLoss = false
                    pausedByNoisyAudio = false
                }
            }
            updatePlaybackState()
        }
    }

    // Lifecycle Methods

    override suspend fun connect() {
        // Avoid duplicate connections
        if (mediaController != null) return

        connectionJob = scope.launch {
            try {
                val sessionToken = SessionToken(
                    context,
                    ComponentName(context, MusicPlaybackService::class.java)
                )

                val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

                mediaController = suspendCancellableCoroutine { continuation ->
                    controllerFuture.addListener({
                        try {
                            val controller = controllerFuture.get()
                            continuation.resume(controller)
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        }
                    }, MoreExecutors.directExecutor())

                    continuation.invokeOnCancellation {
                        controllerFuture.cancel(true)
                    }
                }

                // Register listener after successful connection
                mediaController?.addListener(playerListener)
                updatePlaybackState()
            } catch (e: Exception) {
                _playbackState.update {
                    it.copy(error = "Failed to connect to playback service: ${e.message}")
                }
            }
        }

        connectionJob?.join()
    }

    override fun disconnect() {
        mediaController?.removeListener(playerListener)
        mediaController?.release()
        mediaController = null
        connectionJob?.cancel()
        connectionJob = null
    }

    // State Synchronization

    private fun updatePlaybackState() {
        val controller = mediaController ?: return

        // Extract queue from MediaController
        val queue = mutableListOf<Song>()
        for (i in 0 until controller.mediaItemCount) {
            controller.getMediaItemAt(i).toSong()?.let { queue.add(it) }
        }

        _playbackState.update { currentState ->
            currentState.copy(
                isPlaying = controller.isPlaying,
                isLoading = controller.playbackState == Player.STATE_BUFFERING,
                currentPosition = controller.currentPosition,
                duration = controller.duration.takeIf { it != C.TIME_UNSET } ?: 0L,
                currentSong = controller.currentMediaItem?.toSong(),
                playbackSpeed = controller.playbackParameters.speed,
                repeatMode = controller.repeatMode.toRepeatMode(),
                shuffleEnabled = controller.shuffleModeEnabled,
                error = controller.playerError?.message,

                // Queue state
                queue = queue,
                currentQueueIndex = controller.currentMediaItemIndex,

                // Audio focus state
                pausedByAudioFocusLoss = pausedByAudioFocusLoss,
                pausedByNoisyAudio = pausedByNoisyAudio
            )
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
        mediaController?.seekTo(positionMs)
    }

    override suspend fun seekToNext() {
        mediaController?.seekToNext()
    }

    override suspend fun seekToPrevious() {
        mediaController?.seekToPrevious()
    }

    // Queue Management

    override suspend fun playSong(song: Song) {
        mediaController?.let { controller ->
            try {
                controller.setMediaItem(song.toMediaItem())
                controller.prepare()
                controller.play()
            } catch (e: Exception) {
                _playbackState.update {
                    it.copy(error = "Failed to play song: ${e.message}")
                }
                android.util.Log.e("PlaybackRepository", "Error playing song: ${song.title}", e)
            }
        }
    }

    override suspend fun playQueue(songs: List<Song>, startIndex: Int) {
        mediaController?.let { controller ->
            try {
                controller.setMediaItems(
                    songs.map { it.toMediaItem() },
                    startIndex,
                    0L
                )
                controller.prepare()
                controller.play()
            } catch (e: Exception) {
                _playbackState.update {
                    it.copy(error = "Failed to play queue: ${e.message}")
                }
                android.util.Log.e("PlaybackRepository", "Error playing queue", e)
            }
        }
    }

    override suspend fun addToQueue(song: Song) {
        mediaController?.addMediaItem(song.toMediaItem())
    }

    override suspend fun addToQueue(songs: List<Song>) {
        mediaController?.addMediaItems(songs.map { it.toMediaItem() })
    }

    override suspend fun playNext(song: Song) {
        mediaController?.let { controller ->
            val currentIndex = controller.currentMediaItemIndex
            val insertIndex = if (currentIndex == -1) 0 else currentIndex + 1
            controller.addMediaItem(insertIndex, song.toMediaItem())
        }
    }

    override suspend fun playNext(songs: List<Song>) {
        mediaController?.let { controller ->
            val currentIndex = controller.currentMediaItemIndex
            val insertIndex = if (currentIndex == -1) 0 else currentIndex + 1
            controller.addMediaItems(insertIndex, songs.map { it.toMediaItem() })
        }
    }

    override suspend fun removeFromQueue(index: Int) {
        mediaController?.removeMediaItem(index)
    }

    override suspend fun seekToQueueIndex(index: Int) {
        mediaController?.seekToDefaultPosition(index)
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

    // Mapper Extensions

    /**
     * Convert Song domain model to Media3 MediaItem.
     */
    private fun Song.toMediaItem(): MediaItem {
        // Validate URI is not empty
        if (uri.isBlank()) {
            throw IllegalArgumentException("Song URI cannot be empty for song: $title (ID: $id)")
        }

        return MediaItem.Builder()
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
            .build()
    }

    /**
     * Extract Song domain model from Media3 MediaItem.
     */
    private fun MediaItem.toSong(): Song? {
        val metadata = mediaMetadata
        val mediaId = mediaId.toLongOrNull() ?: return null

        return Song(
            id = mediaId,
            title = metadata.title?.toString() ?: "",
            artist = metadata.artist?.toString() ?: "",
            album = metadata.albumTitle?.toString() ?: "",
            duration = 0L, // Duration is tracked separately in PlaybackState
            uri = localConfiguration?.uri?.toString() ?: "",
            albumArtUri = metadata.artworkUri?.toString(),
            dateAdded = 0L, // Not available from MediaItem
            isLiked = false // Not available from MediaItem, should be queried from Room
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
