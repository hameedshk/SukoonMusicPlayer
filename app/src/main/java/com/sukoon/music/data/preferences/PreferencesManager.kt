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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
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
        // Premium
        private val KEY_IS_PREMIUM_USER = booleanPreferencesKey("is_premium_user")
        private val KEY_PREMIUM_BANNER_DISMISSED = booleanPreferencesKey("premium_banner_dismissed")

        // Privacy
        private val KEY_PRIVATE_SESSION = booleanPreferencesKey("private_session_enabled")

        // Appearance
        private val KEY_THEME = stringPreferencesKey("app_theme")
        private val KEY_ACCENT_PROFILE = stringPreferencesKey("accent_profile")
        private val KEY_APP_LANGUAGE_TAG = stringPreferencesKey("app_language_tag")

        // Library
        private val KEY_SCAN_ON_STARTUP = booleanPreferencesKey("scan_on_startup")
        private val KEY_LAST_SCAN_TIME = stringPreferencesKey("last_scan_time_ms") // String to support Long
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
        private val KEY_SLEEP_TIMER_TARGET_TIME = stringPreferencesKey("sleep_timer_target_time_ms") // String for Long support

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

        // Playback State (for process death recovery)
        private val KEY_LAST_PLAYBACK_POSITION = stringPreferencesKey("last_playback_position_ms") // String to support Long
        private val KEY_LAST_QUEUE_INDEX = intPreferencesKey("last_queue_index")
        private val KEY_LAST_SONG_ID = stringPreferencesKey("last_song_id") // String to support Long
        private val KEY_LAST_QUEUE_NAME = stringPreferencesKey("last_queue_name")

        // Onboarding
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")

        // UI State
        private val KEY_SELECTED_HOME_TAB = stringPreferencesKey("selected_home_tab")
        private val KEY_SONGS_SORT_MODE = stringPreferencesKey("songs_sort_mode")
        private val KEY_SONGS_SORT_ORDER = stringPreferencesKey("songs_sort_order")
        private val KEY_SONGS_SEARCH_QUERY = stringPreferencesKey("songs_search_query")
        private val KEY_SONGS_ARTIST_FILTER = stringPreferencesKey("songs_artist_filter")
        private val KEY_SONGS_ALBUM_FILTER = stringPreferencesKey("songs_album_filter")

        // Ratings & Feedback
        private val KEY_APP_LAUNCH_COUNT = intPreferencesKey("app_launch_count")
        private val KEY_FIRST_INSTALL_TIME = stringPreferencesKey("first_install_time_ms")
        private val KEY_RATING_BANNER_DISMISSED = booleanPreferencesKey("rating_banner_dismissed")
        private val KEY_HAS_RATED_APP = booleanPreferencesKey("has_rated_app")
        private val KEY_LAST_RATING_PROMPT_TIME = stringPreferencesKey("last_rating_prompt_time_ms")

        // Firebase Feedback
        private val KEY_ANONYMOUS_USER_ID = stringPreferencesKey("anonymous_user_id")
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
                accentProfile = parseAccentProfile(preferences[KEY_ACCENT_PROFILE]),
                // Library
                scanOnStartup = preferences[KEY_SCAN_ON_STARTUP] ?: false,
                excludedFolderPaths = preferences[KEY_EXCLUDED_FOLDER_PATHS] ?: emptySet(),
                folderSortMode = parseFolderSortMode(preferences[KEY_FOLDER_SORT_MODE]),
                minimumAudioDuration = preferences[KEY_MINIMUM_AUDIO_DURATION] ?: 30,
                showAllAudioFiles = preferences[KEY_SHOW_ALL_AUDIO_FILES] ?: false,
                lastQueueName = preferences[KEY_LAST_QUEUE_NAME],
                // Playback
                showNotificationControls = preferences[KEY_SHOW_NOTIFICATIONS] ?: true,
                gaplessPlaybackEnabled = preferences[KEY_GAPLESS_PLAYBACK] ?: true,
                crossfadeDurationMs = preferences[KEY_CROSSFADE_DURATION] ?: 0,
                pauseOnAudioNoisy = preferences[KEY_PAUSE_ON_AUDIO_NOISY] ?: true,
                resumeOnAudioFocus = preferences[KEY_RESUME_ON_AUDIO_FOCUS] ?: false,
                // Audio Quality
                audioQuality = parseAudioQuality(preferences[KEY_AUDIO_QUALITY]),
                audioBufferMs = preferences[KEY_AUDIO_BUFFER] ?: 500,
                audioNormalizationEnabled = preferences[KEY_AUDIO_NORMALIZATION] ?: false,
                // Onboarding
                username = preferences[KEY_USERNAME] ?: "",
                hasCompletedOnboarding = preferences[KEY_HAS_COMPLETED_ONBOARDING] ?: false,
                // Sleep Timer
                sleepTimerTargetTimeMs = preferences[KEY_SLEEP_TIMER_TARGET_TIME]?.toLongOrNull() ?: 0L
            )
        }

    /**
     * Observe notification controls visibility preference as a reactive Flow.
     * Used by MusicPlaybackService to gate notification display.
     */
    val showNotificationsFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SHOW_NOTIFICATIONS] ?: true
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
     * Update accent color profile.
     *
     * @param profile Accent profile (Teal, Steel Blue, Soft Cyan)
     */
    suspend fun setAccentProfile(profile: com.sukoon.music.domain.model.AccentProfile) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACCENT_PROFILE] = profile.id
        }
    }

    /**
     * Observe app language tag.
     * Returns null when system language should be used.
     */
    fun appLanguageTagFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_APP_LANGUAGE_TAG]
        }
    }

    /**
     * Set app language tag.
     * Pass null or "system" to follow the device language.
     * Waits for the preference to be persisted before returning.
     */
    suspend fun setAppLanguageTag(languageTag: String?) {
        context.dataStore.edit { preferences ->
            if (languageTag.isNullOrBlank() || languageTag == "system") {
                preferences.remove(KEY_APP_LANGUAGE_TAG)
            } else {
                preferences[KEY_APP_LANGUAGE_TAG] = languageTag
            }
        }
        // Wait for persistence to complete before returning
        context.dataStore.data.first { preferences ->
            val saved = preferences[KEY_APP_LANGUAGE_TAG]
            if (languageTag.isNullOrBlank() || languageTag == "system") {
                saved == null
            } else {
                saved == languageTag
            }
        }
    }

    /**
     * Get app language tag once.
     * Returns null when system language should be used.
     */
    suspend fun getAppLanguageTag(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[KEY_APP_LANGUAGE_TAG]
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
     * Get the last scan time in milliseconds since epoch.
     *
     * @return Last scan time in ms, or 0 if never scanned
     */
    suspend fun getLastScanTime(): Long {
        val preferences = context.dataStore.data.first()
        return preferences[KEY_LAST_SCAN_TIME]?.toLongOrNull() ?: 0L
    }

    /**
     * Update the last scan time to current system time.
     * Call this after a successful MediaStore scan.
     */
    suspend fun setLastScanTime(timeMs: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_SCAN_TIME] = timeMs.toString()
        }
    }

    /**
     * Update notification controls setting.
     *
     * @param enabled True to show playback controls in notification
     */
    suspend fun setShowNotificationControls(enabled: Boolean) {
        android.util.Log.d("PreferencesManager", "setShowNotificationControls: writing $enabled to DataStore")
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_NOTIFICATIONS] = enabled
        }
        android.util.Log.d("PreferencesManager", "setShowNotificationControls: DataStore write completed for $enabled")
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
     * Update sleep timer target time.
     * @param targetTimeMs Target time in milliseconds since epoch (0 = disabled)
     */
    suspend fun setSleepTimerTargetTime(targetTimeMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SLEEP_TIMER_TARGET_TIME] = targetTimeMs.toString()
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
     * Set user's display name.
     * Waits for the update to propagate through DataStore before returning.
     *
     * @param name User's display name (optional, can be empty)
     */
    suspend fun setUsername(name: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USERNAME] = name
        }
        // Wait for the updated value to be emitted from the flow
        context.dataStore.data.first { preferences ->
            preferences[KEY_USERNAME] == name
        }
    }

    /**
     * Mark onboarding as completed (user granted permission).
     * Waits for the update to propagate through DataStore before returning.
     */
    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[KEY_HAS_COMPLETED_ONBOARDING] = true
        }
        // Wait for the updated value to be emitted from the flow
        context.dataStore.data.first { preferences ->
            preferences[KEY_HAS_COMPLETED_ONBOARDING] == true
        }
    }

    /**
     * Get the last selected home tab as a Flow.
     * Returns null if no tab was previously selected.
     */
    fun getSelectedHomeTabFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_SELECTED_HOME_TAB]
        }
    }

    /**
     * Save the selected home tab.
     * @param tabName Name of the selected tab
     */
    suspend fun setSelectedHomeTab(tabName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_HOME_TAB] = tabName
        }
    }

    fun getSongsSortModeFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_SONGS_SORT_MODE]
        }
    }

    suspend fun setSongsSortMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SONGS_SORT_MODE] = mode
        }
    }

    fun getSongsSortOrderFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_SONGS_SORT_ORDER]
        }
    }

    suspend fun setSongsSortOrder(order: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SONGS_SORT_ORDER] = order
        }
    }

    fun getSongsSearchQueryFlow(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_SONGS_SEARCH_QUERY] ?: ""
        }
    }

    suspend fun setSongsSearchQuery(query: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SONGS_SEARCH_QUERY] = query
        }
    }

    fun getSongsArtistFilterFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_SONGS_ARTIST_FILTER]
        }
    }

    suspend fun setSongsArtistFilter(artist: String?) {
        context.dataStore.edit { preferences ->
            if (artist.isNullOrBlank()) {
                preferences.remove(KEY_SONGS_ARTIST_FILTER)
            } else {
                preferences[KEY_SONGS_ARTIST_FILTER] = artist
            }
        }
    }

    fun getSongsAlbumFilterFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_SONGS_ALBUM_FILTER]
        }
    }

    suspend fun setSongsAlbumFilter(album: String?) {
        context.dataStore.edit { preferences ->
            if (album.isNullOrBlank()) {
                preferences.remove(KEY_SONGS_ALBUM_FILTER)
            } else {
                preferences[KEY_SONGS_ALBUM_FILTER] = album
            }
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
     * Parse accent profile string from DataStore.
     * Returns Teal (DEFAULT) if parsing fails or key is missing.
     */
    private fun parseAccentProfile(profileId: String?): com.sukoon.music.domain.model.AccentProfile {
        return try {
            profileId?.let { com.sukoon.music.domain.model.AccentProfile.fromId(it) }
                ?: com.sukoon.music.domain.model.AccentProfile.DEFAULT
        } catch (e: Exception) {
            com.sukoon.music.domain.model.AccentProfile.DEFAULT
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

    /**
     * Get or create an anonymous user ID for feedback submissions.
     * Generates a new UUID on first call and persists it.
     *
     * @return Anonymous user ID (UUID format)
     */
    suspend fun getOrCreateAnonymousUserId(): String {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_ANONYMOUS_USER_ID] ?: run {
            val newId = UUID.randomUUID().toString()
            context.dataStore.edit { it[KEY_ANONYMOUS_USER_ID] = newId }
            newId
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

    // --- Playback State (for process death recovery) ---

    /**
     * Save current playback state for recovery after process death.
     *
     * @param songId ID of currently playing song
     * @param queueIndex Current index in queue
     * @param positionMs Current playback position in milliseconds
     */
    suspend fun savePlaybackState(songId: Long, queueIndex: Int, positionMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_SONG_ID] = songId.toString()
            preferences[KEY_LAST_QUEUE_INDEX] = queueIndex
            preferences[KEY_LAST_PLAYBACK_POSITION] = positionMs.toString()
        }
    }

    /**
     * Save current playback state for recovery after process death.
     * @param songId ID of currently playing song
     * @param queueIndex Current index in queue
     * @param positionMs Current playback position in milliseconds
     * @param queueName Name of the source (Album, Playlist, etc.)
     */
    suspend fun savePlaybackStateExtended(songId: Long, queueIndex: Int, positionMs: Long, queueName: String?) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_SONG_ID] = songId.toString()
            preferences[KEY_LAST_QUEUE_INDEX] = queueIndex
            preferences[KEY_LAST_PLAYBACK_POSITION] = positionMs.toString()
            if (queueName != null) {
                preferences[KEY_LAST_QUEUE_NAME] = queueName
            } else {
                preferences.remove(KEY_LAST_QUEUE_NAME)
            }
        }
    }

    /**
     * Get saved playback state for recovery after process death.
     *
     * @return Triple of (songId, queueIndex, positionMs) or null if not saved
     */
    suspend fun getPlaybackState(): Triple<Long, Int, Long>? {
        val preferences = context.dataStore.data
            .map { prefs ->
                val songId = prefs[KEY_LAST_SONG_ID]?.toLongOrNull()
                val queueIndex = prefs[KEY_LAST_QUEUE_INDEX]
                val position = prefs[KEY_LAST_PLAYBACK_POSITION]?.toLongOrNull()

                if (songId != null && queueIndex != null && position != null) {
                    Triple(songId, queueIndex, position)
                } else {
                    null
                }
            }
        return preferences.first()
    }

    /**
     * Clear saved playback state.
     * Call this after successfully restoring state or when user explicitly clears it.
     */
    suspend fun clearPlaybackState() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_LAST_SONG_ID)
            preferences.remove(KEY_LAST_QUEUE_INDEX)
            preferences.remove(KEY_LAST_PLAYBACK_POSITION)
        }
    }

    // --- Premium / Subscriptions ---

    /**
     * Observe premium user status as a reactive Flow.
     * True if user has purchased premium subscription.
     */
    fun isPremiumUserFlow(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_IS_PREMIUM_USER] ?: false
        }
    }

    /**
     * Set premium user status.
     * Called after successful in-app purchase.
     *
     * @param isPremium True if user purchased premium
     */
    suspend fun setIsPremiumUser(isPremium: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_PREMIUM_USER] = isPremium
        }
    }

    /**
     * Observe premium banner dismissed state as a reactive Flow.
     * True if user has dismissed the premium banner.
     */
    fun isPremiumBannerDismissedFlow(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_PREMIUM_BANNER_DISMISSED] ?: false
        }
    }

    /**
     * Set premium banner dismissed state.
     * Called when user clicks the X button on the premium banner.
     *
     * @param dismissed True if banner has been dismissed
     */
    suspend fun setPremiumBannerDismissed(dismissed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PREMIUM_BANNER_DISMISSED] = dismissed
        }
    }

    // --- Ratings & Feedback ---

    /**
     * Increment app launch count.
     * Call this on every app launch to track engagement.
     */
    suspend fun incrementAppLaunchCount() {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_APP_LAUNCH_COUNT] ?: 0
            preferences[KEY_APP_LAUNCH_COUNT] = current + 1
        }
    }

    /**
     * Get app launch count.
     */
    suspend fun getAppLaunchCount(): Int {
        val preferences = context.dataStore.data.first()
        return preferences[KEY_APP_LAUNCH_COUNT] ?: 0
    }

    /**
     * Set first install time (only on first launch).
     * @param timeMs Time in milliseconds since epoch
     */
    suspend fun setFirstInstallTime(timeMs: Long) {
        context.dataStore.edit { preferences ->
            if (preferences[KEY_FIRST_INSTALL_TIME] == null) {
                preferences[KEY_FIRST_INSTALL_TIME] = timeMs.toString()
            }
        }
    }

    /**
     * Get first install time.
     */
    suspend fun getFirstInstallTime(): Long {
        val preferences = context.dataStore.data.first()
        return preferences[KEY_FIRST_INSTALL_TIME]?.toLongOrNull() ?: 0L
    }

    /**
     * Observe rating banner dismissed state.
     */
    fun isRatingBannerDismissedFlow(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_RATING_BANNER_DISMISSED] ?: false
        }
    }

    /**
     * Set rating banner dismissed state.
     */
    suspend fun setRatingBannerDismissed(dismissed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RATING_BANNER_DISMISSED] = dismissed
        }
    }

    /**
     * Observe "has rated app" state.
     */
    fun hasRatedAppFlow(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_HAS_RATED_APP] ?: false
        }
    }

    /**
     * Set has rated app state.
     * Called after user completes rating flow.
     */
    suspend fun setHasRatedApp(rated: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HAS_RATED_APP] = rated
        }
    }

    /**
     * Check if should show rating banner based on smart conditions.
     */
    fun shouldShowRatingBannerFlow(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            val launchCount = preferences[KEY_APP_LAUNCH_COUNT] ?: 0
            val firstInstallTimeMs = preferences[KEY_FIRST_INSTALL_TIME]?.toLongOrNull() ?: 0L
            val isBannerDismissed = preferences[KEY_RATING_BANNER_DISMISSED] ?: false
            val hasRated = preferences[KEY_HAS_RATED_APP] ?: false

            val daysSinceInstall = if (firstInstallTimeMs > 0) {
                (System.currentTimeMillis() - firstInstallTimeMs) / (1000 * 60 * 60 * 24)
            } else {
                0L
            }

            !hasRated && !isBannerDismissed && launchCount >= 10 && daysSinceInstall >= 3
        }
    }
}
