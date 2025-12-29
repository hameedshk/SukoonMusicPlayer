package com.sukoon.music.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.domain.model.AudioQuality
import com.sukoon.music.domain.model.FolderSortMode
import com.sukoon.music.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sukoon_preferences")

/**
 * Manager for user preferences using DataStore.
 *
 * Provides reactive access to app settings with automatic persistence.
 * All preferences are stored as key-value pairs and exposed as Flow for observation.
 */
@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {

    companion object {
        // Privacy
        private val KEY_PRIVATE_SESSION = booleanPreferencesKey("private_session_enabled")

        // Appearance
        private val KEY_THEME = stringPreferencesKey("app_theme")

        // Library
        private val KEY_SCAN_ON_STARTUP = booleanPreferencesKey("scan_on_startup")
        private val KEY_EXCLUDED_FOLDER_PATHS = stringSetPreferencesKey("excluded_folder_paths")
        private val KEY_FOLDER_SORT_MODE = stringPreferencesKey("folder_sort_mode")
        private val KEY_MINIMUM_AUDIO_DURATION = intPreferencesKey("minimum_audio_duration_seconds")
        private val KEY_SHOW_ALL_AUDIO_FILES = booleanPreferencesKey("show_all_audio_files")

        // Playback
        private val KEY_SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notification_controls")
        private val KEY_GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback_enabled")
        private val KEY_CROSSFADE_DURATION = intPreferencesKey("crossfade_duration_ms")
        private val KEY_PAUSE_ON_AUDIO_NOISY = booleanPreferencesKey("pause_on_audio_noisy")
        private val KEY_RESUME_ON_AUDIO_FOCUS = booleanPreferencesKey("resume_on_audio_focus")

        // Audio Quality
        private val KEY_AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        private val KEY_AUDIO_BUFFER = intPreferencesKey("audio_buffer_ms")
        private val KEY_AUDIO_NORMALIZATION = booleanPreferencesKey("audio_normalization_enabled")

        // Equalizer
        private val KEY_EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        private val KEY_EQ_PRESET_ID = stringPreferencesKey("eq_preset_id") // String to support Long
        private val KEY_EQ_BAND_0 = intPreferencesKey("eq_band_60hz")
        private val KEY_EQ_BAND_1 = intPreferencesKey("eq_band_230hz")
        private val KEY_EQ_BAND_2 = intPreferencesKey("eq_band_910hz")
        private val KEY_EQ_BAND_3 = intPreferencesKey("eq_band_3600hz")
        private val KEY_EQ_BAND_4 = intPreferencesKey("eq_band_14000hz")
        private val KEY_EQ_BASS_BOOST = intPreferencesKey("eq_bass_boost")
        private val KEY_EQ_VIRTUALIZER = intPreferencesKey("eq_virtualizer")
    }

    /**
     * Observe user preferences as a reactive Flow.
     * Emits updated preferences whenever they change.
     */
    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                // Privacy
                isPrivateSessionEnabled = preferences[KEY_PRIVATE_SESSION] ?: false,
                // Appearance
                theme = parseTheme(preferences[KEY_THEME]),
                // Library
                scanOnStartup = preferences[KEY_SCAN_ON_STARTUP] ?: false,
                excludedFolderPaths = preferences[KEY_EXCLUDED_FOLDER_PATHS] ?: emptySet(),
                folderSortMode = parseFolderSortMode(preferences[KEY_FOLDER_SORT_MODE]),
                minimumAudioDuration = preferences[KEY_MINIMUM_AUDIO_DURATION] ?: 30,
                showAllAudioFiles = preferences[KEY_SHOW_ALL_AUDIO_FILES] ?: false,
                // Playback
                showNotificationControls = preferences[KEY_SHOW_NOTIFICATIONS] ?: true,
                gaplessPlaybackEnabled = preferences[KEY_GAPLESS_PLAYBACK] ?: true,
                crossfadeDurationMs = preferences[KEY_CROSSFADE_DURATION] ?: 0,
                pauseOnAudioNoisy = preferences[KEY_PAUSE_ON_AUDIO_NOISY] ?: true,
                resumeOnAudioFocus = preferences[KEY_RESUME_ON_AUDIO_FOCUS] ?: false,
                // Audio Quality
                audioQuality = parseAudioQuality(preferences[KEY_AUDIO_QUALITY]),
                audioBufferMs = preferences[KEY_AUDIO_BUFFER] ?: 500,
                audioNormalizationEnabled = preferences[KEY_AUDIO_NORMALIZATION] ?: false
            )
        }

    /**
     * Update private session setting.
     *
     * @param enabled True to enable private session (don't save listening history)
     */
    suspend fun setPrivateSessionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PRIVATE_SESSION] = enabled
        }
    }

    /**
     * Update app theme.
     *
     * @param theme Theme preference (LIGHT, DARK, SYSTEM)
     */
    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = theme.name
        }
    }

    /**
     * Update scan on startup setting.
     *
     * @param enabled True to automatically scan media library on app startup
     */
    suspend fun setScanOnStartup(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SCAN_ON_STARTUP] = enabled
        }
    }

    /**
     * Update notification controls setting.
     *
     * @param enabled True to show playback controls in notification
     */
    suspend fun setShowNotificationControls(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_NOTIFICATIONS] = enabled
        }
    }

    /**
     * Update gapless playback setting.
     */
    suspend fun setGaplessPlaybackEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GAPLESS_PLAYBACK] = enabled
        }
    }

    /**
     * Update crossfade duration.
     * @param durationMs Duration in milliseconds (0 = disabled)
     */
    suspend fun setCrossfadeDuration(durationMs: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CROSSFADE_DURATION] = durationMs.coerceIn(0, 10000)
        }
    }

    /**
     * Update pause on audio noisy setting.
     */
    suspend fun setPauseOnAudioNoisy(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PAUSE_ON_AUDIO_NOISY] = enabled
        }
    }

    /**
     * Update resume on audio focus setting.
     */
    suspend fun setResumeOnAudioFocus(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RESUME_ON_AUDIO_FOCUS] = enabled
        }
    }

    /**
     * Update audio quality preset.
     */
    suspend fun setAudioQuality(quality: AudioQuality) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUDIO_QUALITY] = quality.name
        }
    }

    /**
     * Update audio buffer size.
     * @param bufferMs Buffer size in milliseconds (100-2000ms)
     */
    suspend fun setAudioBuffer(bufferMs: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUDIO_BUFFER] = bufferMs.coerceIn(100, 2000)
        }
    }

    /**
     * Update audio normalization setting.
     */
    suspend fun setAudioNormalizationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUDIO_NORMALIZATION] = enabled
        }
    }

    /**
     * Update excluded folder paths.
     */
    suspend fun setExcludedFolderPaths(paths: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_EXCLUDED_FOLDER_PATHS] = paths
        }
    }

    /**
     * Add a folder path to the exclusion list.
     */
    suspend fun addExcludedFolderPath(path: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_EXCLUDED_FOLDER_PATHS] ?: emptySet()
            preferences[KEY_EXCLUDED_FOLDER_PATHS] = current + path
        }
    }

    /**
     * Remove a folder path from the exclusion list.
     */
    suspend fun removeExcludedFolderPath(path: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_EXCLUDED_FOLDER_PATHS] ?: emptySet()
            preferences[KEY_EXCLUDED_FOLDER_PATHS] = current - path
        }
    }

    /**
     * Update folder sort mode.
     */
    suspend fun setFolderSortMode(mode: FolderSortMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FOLDER_SORT_MODE] = mode.name
        }
    }

    /**
     * Update minimum audio duration in seconds.
     */
    suspend fun setMinimumAudioDuration(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MINIMUM_AUDIO_DURATION] = seconds
        }
    }

    /**
     * Update whether to show all audio files.
     */
    suspend fun setShowAllAudioFiles(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_ALL_AUDIO_FILES] = show
        }
    }

    /**
     * Clear all preferences (reset to defaults).
     * Used for logout or factory reset.
     */
    suspend fun clearAllPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Parse theme string from DataStore.
     * Returns SYSTEM as default if parsing fails.
     */
    private fun parseTheme(themeString: String?): AppTheme {
        return try {
            themeString?.let { AppTheme.valueOf(it) } ?: AppTheme.SYSTEM
        } catch (e: IllegalArgumentException) {
            AppTheme.SYSTEM
        }
    }

    /**
     * Parse audio quality string from DataStore.
     * Returns HIGH as default if parsing fails.
     */
    private fun parseAudioQuality(qualityString: String?): AudioQuality {
        return try {
            qualityString?.let { AudioQuality.valueOf(it) } ?: AudioQuality.HIGH
        } catch (e: IllegalArgumentException) {
            AudioQuality.HIGH
        }
    }

    /**
     * Parse folder sort mode string from DataStore.
     * Returns NAME_ASC as default if parsing fails.
     */
    private fun parseFolderSortMode(modeString: String?): FolderSortMode {
        return try {
            modeString?.let { FolderSortMode.valueOf(it) } ?: FolderSortMode.NAME_ASC
        } catch (e: IllegalArgumentException) {
            FolderSortMode.NAME_ASC
        }
    }

    // --- Equalizer Settings ---

    /**
     * Observe equalizer settings as a reactive Flow.
     * Emits updated settings whenever they change.
     */
    val equalizerSettingsFlow: Flow<com.sukoon.music.domain.model.EqualizerSettings> = context.dataStore.data
        .map { preferences ->
            com.sukoon.music.domain.model.EqualizerSettings(
                isEnabled = preferences[KEY_EQ_ENABLED] ?: false,
                currentPresetId = preferences[KEY_EQ_PRESET_ID]?.toLongOrNull() ?: -1L,
                bandLevels = listOf(
                    preferences[KEY_EQ_BAND_0] ?: 0,
                    preferences[KEY_EQ_BAND_1] ?: 0,
                    preferences[KEY_EQ_BAND_2] ?: 0,
                    preferences[KEY_EQ_BAND_3] ?: 0,
                    preferences[KEY_EQ_BAND_4] ?: 0
                ),
                bassBoost = preferences[KEY_EQ_BASS_BOOST] ?: 0,
                virtualizerStrength = preferences[KEY_EQ_VIRTUALIZER] ?: 0
            )
        }

    /**
     * Update equalizer settings.
     *
     * @param settings New equalizer settings
     */
    suspend fun updateEqualizerSettings(settings: com.sukoon.music.domain.model.EqualizerSettings) {
        context.dataStore.edit { preferences ->
            preferences[KEY_EQ_ENABLED] = settings.isEnabled
            preferences[KEY_EQ_PRESET_ID] = settings.currentPresetId.toString()
            preferences[KEY_EQ_BAND_0] = settings.bandLevels.getOrElse(0) { 0 }
            preferences[KEY_EQ_BAND_1] = settings.bandLevels.getOrElse(1) { 0 }
            preferences[KEY_EQ_BAND_2] = settings.bandLevels.getOrElse(2) { 0 }
            preferences[KEY_EQ_BAND_3] = settings.bandLevels.getOrElse(3) { 0 }
            preferences[KEY_EQ_BAND_4] = settings.bandLevels.getOrElse(4) { 0 }
            preferences[KEY_EQ_BASS_BOOST] = settings.bassBoost
            preferences[KEY_EQ_VIRTUALIZER] = settings.virtualizerStrength
        }
    }

    /**
     * Enable or disable equalizer effects.
     *
     * @param enabled True to enable, false to disable
     */
    suspend fun setEqualizerEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_EQ_ENABLED] = enabled
        }
    }
}
