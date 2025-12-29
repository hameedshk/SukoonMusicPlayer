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
 * Repeat mode for playback.
 */
enum class RepeatMode {
    OFF,    // No repeat
    ONE,    // Repeat current track
    ALL     // Repeat all tracks in queue
}
