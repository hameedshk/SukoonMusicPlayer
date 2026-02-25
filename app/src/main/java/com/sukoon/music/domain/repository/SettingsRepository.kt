package com.sukoon.music.domain.repository

import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.domain.model.AccentProfile
import com.sukoon.music.domain.model.AudioQuality
import com.sukoon.music.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Repository for app settings and preferences.
 *
 * Provides access to user preferences stored in DataStore
 * and operations for storage management.
 */
interface SettingsRepository {

    /**
     * Observe user preferences.
     * Emits updated preferences whenever they change.
     */
    val userPreferences: Flow<UserPreferences>

    /**
     * Update private session setting.
     *
     * @param enabled True to enable private session mode
     */
    suspend fun setPrivateSessionEnabled(enabled: Boolean)

    /**
     * Update Firebase Analytics opt-in/opt-out.
     *
     * @param enabled True to enable analytics sharing
     */
    suspend fun setAnalyticsEnabled(enabled: Boolean)

    /**
     * Update AI Metadata Correction opt-in/opt-out.
     *
     * @param enabled True to enable AI metadata correction
     */
    suspend fun setAiMetadataCorrectionEnabled(enabled: Boolean)

    /**
     * Update app theme.
     *
     * @param theme Theme preference (LIGHT, DARK, SYSTEM)
     */
    suspend fun setTheme(theme: AppTheme)

    /**
     * Update accent color profile.
     *
     * @param profile Accent profile (Teal, Steel Blue, Soft Cyan)
     */
    suspend fun setAccentProfile(profile: AccentProfile)

    /**
     * Update scan on startup setting.
     *
     * @param enabled True to auto-scan media on app startup
     */
    suspend fun setScanOnStartup(enabled: Boolean)

    /**
     * Get the last scan time in milliseconds since epoch.
     *
     * @return Last scan time in ms, or 0 if never scanned
     */
    suspend fun getLastScanTime(): Long

    /**
     * Update the last scan time to current system time.
     * Call this after a successful MediaStore scan.
     *
     * @param timeMs Time in milliseconds since epoch (defaults to current time)
     */
    suspend fun setLastScanTime(timeMs: Long = System.currentTimeMillis())

    /**
     * Update notification controls setting.
     *
     * @param enabled True to show playback controls in notification
     */
    suspend fun setShowNotificationControls(enabled: Boolean)

    /**
     * Update gapless playback setting.
     */
    suspend fun setGaplessPlaybackEnabled(enabled: Boolean)

    /**
     * Update crossfade duration.
     * @param durationMs Duration in milliseconds (0 = disabled)
     */
    suspend fun setCrossfadeDuration(durationMs: Int)

    /**
     * Update pause on audio noisy setting.
     */
    suspend fun setPauseOnAudioNoisy(enabled: Boolean)

    /**
     * Update resume on audio focus setting.
     */
    suspend fun setResumeOnAudioFocus(enabled: Boolean)

    /**
     * Update audio quality preset.
     */
    suspend fun setAudioQuality(quality: AudioQuality)

    /**
     * Update audio buffer size.
     * @param bufferMs Buffer size in milliseconds
     */
    suspend fun setAudioBuffer(bufferMs: Int)

    /**
     * Update audio normalization setting.
     */
    suspend fun setAudioNormalizationEnabled(enabled: Boolean)

    /**
     * Update minimum audio duration setting.
     * @param seconds Minimum duration in seconds
     */
    suspend fun setMinimumAudioDuration(seconds: Int)

    /**
     * Update show all audio files setting.
     * @param enabled True to show all audio files including hidden ones
     */
    suspend fun setShowAllAudioFiles(enabled: Boolean)

    /**
     * Get storage usage statistics.
     *
     * @return Pair of (database size in bytes, cache size in bytes)
     */
    suspend fun getStorageStats(): StorageStats

    /**
     * Clear image cache (Coil cache).
     * Frees up storage space used by cached album art.
     */
    suspend fun clearImageCache()

    /**
     * Clear all app data (logout).
     * - Clears Room database (songs, playlists, lyrics, history)
     * - Clears DataStore preferences
     * - Clears image cache
     * IMPORTANT: Does NOT delete physical music files
     */
    suspend fun clearAllData()

    /**
     * Set the target time for the sleep timer to pause playback.
     * @param targetTimeMs Milliseconds since epoch (0 = disabled)
     */
    suspend fun setSleepTimerTargetTime(targetTimeMs: Long)

    /**
     * Observe premium banner dismissed state.
     * @return Flow emitting true if user has dismissed the premium banner
     */
    fun premiumBannerDismissedFlow(): kotlinx.coroutines.flow.Flow<Boolean>

    /**
     * Set premium banner dismissed state.
     * @param dismissed True if banner has been dismissed
     */
    suspend fun setPremiumBannerDismissed(dismissed: Boolean)

    /**
     * Increment app launch count for rating logic.
     */
    suspend fun incrementAppLaunchCount()

    /**
     * Set first install time (only on first launch).
     */
    suspend fun setFirstInstallTime(timeMs: Long)

    /**
     * Check if rating banner should be shown.
     */
    fun shouldShowRatingBannerFlow(): kotlinx.coroutines.flow.Flow<Boolean>

    /**
     * Dismiss the rating banner.
     */
    suspend fun setRatingBannerDismissed(dismissed: Boolean)

    /**
     * Set that user has rated the app.
     */
    suspend fun setHasRatedApp(rated: Boolean)

    /**
     * Observe selected app language tag.
     * Null means follow system language.
     */
    fun appLanguageTagFlow(): Flow<String?>

    /**
     * Set app language tag.
     * Pass null to follow system language.
     */
    suspend fun setAppLanguageTag(languageTag: String?)

    /**
     * Get current app language tag once.
     * Null means follow system language.
     */
    suspend fun getAppLanguageTag(): String?
}

/**
 * Storage usage statistics.
 */
data class StorageStats(
    val databaseSizeBytes: Long,
    val cacheSizeBytes: Long,
    val audioLibrarySizeBytes: Long = 0
) {
    val totalSizeBytes: Long get() = databaseSizeBytes + cacheSizeBytes + audioLibrarySizeBytes

    fun databaseSizeMB(): String = "%.2f MB".format(databaseSizeBytes / 1024.0 / 1024.0)
    fun cacheSizeMB(): String = "%.2f MB".format(cacheSizeBytes / 1024.0 / 1024.0)
    fun audioLibrarySizeMB(): String = "%.2f MB".format(audioLibrarySizeBytes / 1024.0 / 1024.0)
    fun totalSizeMB(): String = "%.2f MB".format(totalSizeBytes / 1024.0 / 1024.0)
}
