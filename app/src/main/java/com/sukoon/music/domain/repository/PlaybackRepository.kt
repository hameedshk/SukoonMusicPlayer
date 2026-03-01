package com.sukoon.music.domain.repository

import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.RepeatMode
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.model.SongAudioSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for playback control operations.
 * This is the single point of interaction between UI and the MediaController.
 *
 * All playback state must be observed through [playbackState] StateFlow.
 * UI must never access ExoPlayer or MediaController directly.
 */
interface PlaybackRepository {

    /**
     * Reactive playback state observable by UI.
     * Updates automatically when playback state changes.
     */
    val playbackState: StateFlow<PlaybackState>

    // Transport Controls

    /**
     * Start or resume playback.
     */
    suspend fun play()

    /**
     * Pause playback.
     */
    suspend fun pause()

    /**
     * Toggle between play and pause.
     */
    suspend fun playPause()

    /**
     * Seek to a specific position in the current track.
     * @param positionMs Position in milliseconds
     */
    suspend fun seekTo(positionMs: Long)

    /**
     * Skip to the next track in the queue.
     */
    suspend fun seekToNext()

    /**
     * Skip to the previous track in the queue.
     */
    suspend fun seekToPrevious()

    // Queue Management

    /**
     * Play a single song immediately.
     * Clears the current queue and plays the specified song.
     * @param song Song to play
     */
    suspend fun playSong(song: Song, queueName: String? = null)

    /**
     * Play a queue of songs starting at a specific index.
     * @param songs List of songs to queue
     * @param startIndex Index of the song to start playing (default: 0)
     */
    suspend fun playQueue(songs: List<Song>, startIndex: Int = 0, queueName: String? = null)

    /**
     * Shuffle and play a queue of songs using Fisher-Yates algorithm.
     * @param songs List of songs to shuffle and play
     */
    suspend fun shuffleAndPlayQueue(songs: List<Song>, queueName: String? = null)

    /**
     * Add a song to the end of the current queue.
     * @param song Song to add
     */
    suspend fun addToQueue(song: Song)

    /**
     * Add multiple songs to the end of the current queue.
     * @param songs List of songs to add
     */
    suspend fun addToQueue(songs: List<Song>)

    /**
     * Add a song to play immediately after the current song.
     * @param song Song to play next
     */
    suspend fun playNext(song: Song)

    /**
     * Add multiple songs to play immediately after the current song.
     * @param songs List of songs to play next
     */
    suspend fun playNext(songs: List<Song>)

    /**
     * Add multiple songs to play immediately after the current song.
     * @param songs List of songs to play next
     */
    suspend fun addToQueueNext(songs: List<Song>)

    /**
     * Remove a song from the queue at the specified index.
     * @param index Index of the song to remove
     */
    suspend fun removeFromQueue(index: Int)

    /**
     * Jump to a specific index in the queue.
     * @param index Index of the song to jump to
     */
    suspend fun seekToQueueIndex(index: Int)

    // Playback Configuration

    /**
     * Set the repeat mode.
     * @param mode Repeat mode (OFF, ONE, ALL)
     */
    suspend fun setRepeatMode(mode: RepeatMode)

    /**
     * Enable or disable shuffle mode.
     * @param enabled True to enable shuffle, false to disable
     */
    suspend fun setShuffleEnabled(enabled: Boolean)

    /**
     * Set playback speed multiplier.
     * @param speed Playback speed (0.5x to 2.0x typically)
     */
    suspend fun setPlaybackSpeed(speed: Float)

    /**
     * Refresh the current song's metadata from database.
     * Used after updating song properties (like isLiked) to sync playback state.
     */
    suspend fun refreshCurrentSong()

    /**
     * Force refresh playback state from MediaController.
     * Call when UI becomes visible to get accurate current position.
     */
    fun refreshPlaybackState()

    /**
     * Save current playback state to preferences for recovery after app close.
     * Called when app goes to background or is destroyed.
     */
    suspend fun savePlaybackState()

    /**
     * Rebuild and re-apply the currently playing media item with latest per-song settings.
     * No-op when the provided song is not currently active.
     */
    suspend fun reapplyCurrentSongSettings(songId: Long)

    /**
     * Apply in-memory per-song settings for live preview when the song is currently active.
     * Does not persist settings.
     */
    suspend fun previewCurrentSongSettings(songId: Long, settings: SongAudioSettings)

    /**
     * Immediately apply provided per-song settings to the currently active song.
     * Intended for editor apply/reset actions.
     */
    suspend fun applyCurrentSongSettingsImmediately(songId: Long, settings: SongAudioSettings)

    // Lifecycle

    /**
     * Connect to the MediaSessionService.
     * Must be called before any other operations.
     */
    suspend fun connect()

    /**
     * Disconnect from the MediaSessionService.
     * Releases all resources.
     */
    fun disconnect()
}
