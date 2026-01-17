package com.sukoon.music.domain.model

/**
 * User preferences and settings.
 * Stored in DataStore for persistence across app sessions.
 */
data class UserPreferences(
    // --- Privacy ---
    /**
     * Private session mode.
     * When enabled, listening history is not saved to Recently Played.
     */
    val isPrivateSessionEnabled: Boolean = false,

    // --- Appearance ---
    /**
     * Theme preference.
     */
    val theme: AppTheme = AppTheme.SYSTEM,

    // --- Library ---
    /**
     * Auto-scan media library on app startup.
     */
    val scanOnStartup: Boolean = false,

    // --- Playback ---
    /**
     * Show notification controls.
     */
    val showNotificationControls: Boolean = true,

    /**
     * Enable gapless playback (seamless transitions between tracks).
     */
    val gaplessPlaybackEnabled: Boolean = true,

    /**
     * Crossfade duration in milliseconds (0 = disabled).
     */
    val crossfadeDurationMs: Int = 0,

    /**
     * Automatically pause playback when audio becomes noisy (headphones unplugged).
     */
    val pauseOnAudioNoisy: Boolean = true,

    /**
     * Resume playback when audio focus is regained.
     */
    val resumeOnAudioFocus: Boolean = false,

    // --- Audio Quality ---
    /**
     * Preferred audio quality mode.
     */
    val audioQuality: AudioQuality = AudioQuality.HIGH,

    /**
     * Audio buffer size in milliseconds.
     * Higher values = more stable playback, but higher latency.
     */
    val audioBufferMs: Int = 500,

    /**
     * Enable audio normalization (ReplayGain).
     */
    val audioNormalizationEnabled: Boolean = false,

    /**
     * Set of folder paths to be excluded from library scan.
     */
    val excludedFolderPaths: Set<String> = emptySet(),

    /**
     * Preferred sort mode for folders.
     */
    val folderSortMode: FolderSortMode = FolderSortMode.NAME_ASC,

    /**
     * Minimum duration in seconds for audio files to be included in library.
     */
    val minimumAudioDuration: Int = 30,

    /**
     * Whether to show all audio files including those in hidden folders.
     */
    val showAllAudioFiles: Boolean = false,

    // --- Onboarding ---
    /**
     * User's display name (optional).
     */
    val username: String = "",

    /**
     * Whether user has completed onboarding (permission granted).
     */
    val hasCompletedOnboarding: Boolean = false
)

/**
 * App theme options.
 */
enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM  // Follow system setting
}

/**
 * Audio quality presets.
 */
enum class AudioQuality {
    LOW,      // 128 kbps equivalent
    MEDIUM,   // 192 kbps equivalent
    HIGH,     // 320 kbps equivalent
    LOSSLESS  // Original quality (no resampling)
}
