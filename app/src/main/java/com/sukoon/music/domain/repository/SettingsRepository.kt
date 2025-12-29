package com.sukoon.music.domain.repository

import com.sukoon.music.domain.model.AppTheme
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
     * Update app theme.
     *
     * @param theme Theme preference (LIGHT, DARK, SYSTEM)
     */
    suspend fun setTheme(theme: AppTheme)

    /**
     * Update scan on startup setting.
     *
     * @param enabled True to auto-scan media on app startup
     */
    suspend fun setScanOnStartup(enabled: Boolean)

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
}

/**
 * Storage usage statistics.
 */
data class StorageStats(
    val databaseSizeBytes: Long,
    val cacheSizeBytes: Long
) {
    val totalSizeBytes: Long get() = databaseSizeBytes + cacheSizeBytes

    fun databaseSizeMB(): String = "%.2f MB".format(databaseSizeBytes / 1024.0 / 1024.0)
    fun cacheSizeMB(): String = "%.2f MB".format(cacheSizeBytes / 1024.0 / 1024.0)
    fun totalSizeMB(): String = "%.2f MB".format(totalSizeBytes / 1024.0 / 1024.0)
}
