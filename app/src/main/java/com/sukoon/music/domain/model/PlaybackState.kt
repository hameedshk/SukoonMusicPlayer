package com.sukoon.music.domain.model

/**
 * Immutable data class representing the current playback state.
 * This is the single source of truth for UI to observe playback status.
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentSong: Song? = null,
    val playbackSpeed: Float = 1.0f,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val error: String? = null,
    val lastUpdateSource: PlaybackUpdateSource = PlaybackUpdateSource.UNKNOWN,

    /**
     * The current playback queue.
     * Contains all songs in the queue, including the currently playing song.
     */
    val queue: List<Song> = emptyList(),

    /**
     * Index of the currently playing song in the queue.
     * -1 if no song is playing or queue is empty.
     */
    val currentQueueIndex: Int = -1,

    /**
     * Name of the currently active saved queue.
     * Null if the queue hasn't been saved or is a temporary queue.
     */
    val currentQueueName: String? = null,

    /**
     * ID of the currently active saved queue.
     * Null if the queue hasn't been saved or is a temporary queue.
     */
    val currentQueueId: Long? = null,

    /**
     * Timestamp when the current queue was last modified.
     * Used for tracking queue history and auto-save timing.
     */
    val queueTimestamp: Long = 0L,

    /**
     * Indicates if playback was paused due to audio focus loss.
     * When true, playback should NOT auto-resume even after regaining focus.
     * Only explicit user action (play button) should resume.
     */
    val pausedByAudioFocusLoss: Boolean = false,

    /**
     * Indicates if playback was paused due to headphone unplug (ACTION_AUDIO_BECOMING_NOISY).
     * When true, playback should NEVER auto-resume.
     */
    val pausedByNoisyAudio: Boolean = false
)

/**
 * Identifies the pipeline stage that most recently refreshed [PlaybackState].
 */
enum class PlaybackUpdateSource {
    PLAYER_LISTENER,
    NOTIFICATION,
    ACTIVITY_RESUME,
    PERIODIC_UI_TICK,
    SERVICE_RECOVERY,
    UNKNOWN
}

/**
 * Repeat mode for playback.
 */
enum class RepeatMode {
    OFF,    // No repeat
    ONE,    // Repeat current track
    ALL     // Repeat all tracks in queue
}
