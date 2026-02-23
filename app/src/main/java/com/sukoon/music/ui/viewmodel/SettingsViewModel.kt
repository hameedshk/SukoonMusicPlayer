package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.app.Activity
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.domain.model.AccentProfile
import com.sukoon.music.domain.model.AudioQuality
import com.sukoon.music.domain.model.UserPreferences
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SettingsRepository
import com.sukoon.music.domain.repository.SongRepository
import com.sukoon.music.domain.repository.StorageStats
import com.sukoon.music.util.InAppReviewHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sukoon.music.ui.theme.*
import com.sukoon.music.data.billing.BillingState
import com.sukoon.music.data.premium.PremiumManager
import com.sukoon.music.util.AppLocaleManager

/**
 * ViewModel for the Settings Screen.
 *
 * Responsibilities:
 * - Manage user preferences (private session, theme, etc.)
 * - Provide storage statistics
 * - Handle data clearing operations
 * - Manage app configuration
 * - Manage premium banner dismissal state
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val songRepository: SongRepository,
    private val playbackRepository: PlaybackRepository,
    private val sessionController: com.sukoon.music.domain.usecase.SessionController,
    private val premiumManager: PremiumManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    internal var currentTimeProvider: () -> Long = { System.currentTimeMillis() }

    // --- Premium Banner State (Persisted via DataStore) ---

    val premiumBannerDismissed: StateFlow<Boolean> = settingsRepository.premiumBannerDismissedFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun dismissPremiumBanner() {
        viewModelScope.launch {
            settingsRepository.setPremiumBannerDismissed(true)
        }
    }

    // --- Billing State ---

    val billingState: StateFlow<BillingState> = premiumManager.billingState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BillingState.Idle
        )

    fun resetBillingState() {
        premiumManager.resetBillingState()
    }

    fun restorePurchases() {
        viewModelScope.launch {
            premiumManager.restorePurchases()
        }
    }

    // --- Rating Banner State (Persisted via DataStore) ---

    val shouldShowRatingBanner: StateFlow<Boolean> = settingsRepository.shouldShowRatingBannerFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun triggerInAppReview(activity: Activity) {
        viewModelScope.launch {
            val reviewHelper = InAppReviewHelper(activity)
            reviewHelper.requestReview()
                .onSuccess {
                    settingsRepository.setHasRatedApp(true)
                }
                .onFailure { error ->
                    android.util.Log.e("SettingsViewModel", "In-app review failed", error)
                }
        }
    }

    fun dismissRatingBanner() {
        viewModelScope.launch {
            settingsRepository.setRatingBannerDismissed(true)
        }
    }

    // --- User Preferences ---

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val appLanguageTag: StateFlow<String?> = settingsRepository.appLanguageTagFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // --- Private Session State ---

    val sessionState: StateFlow<com.sukoon.music.domain.model.PlaybackSessionState> =
        sessionController.sessionState()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = com.sukoon.music.domain.model.PlaybackSessionState()
            )

    // --- Storage Stats ---

    private val _storageStats = MutableStateFlow<StorageStats?>(null)
    val storageStats: StateFlow<StorageStats?> = _storageStats.asStateFlow()

    private val _isLoadingStats = MutableStateFlow(false)
    val isLoadingStats: StateFlow<Boolean> = _isLoadingStats.asStateFlow()

    // --- Operation States ---

    private val _isClearingCache = MutableStateFlow(false)
    val isClearingCache: StateFlow<Boolean> = _isClearingCache.asStateFlow()

    private val _isClearingData = MutableStateFlow(false)
    val isClearingData: StateFlow<Boolean> = _isClearingData.asStateFlow()

    // --- Library Scan State ---

    val scanState: StateFlow<com.sukoon.music.domain.model.ScanState> = songRepository.scanState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.sukoon.music.domain.model.ScanState.Idle
        )

    val isScanning: StateFlow<Boolean> = songRepository.isScanning
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        loadStorageStats()
    }

    // --- User Actions ---

    /**
     * Toggle private session mode.
     * - If turning ON: start session-scoped private session with auto-expiry
     * - If turning OFF: stop session and clear private mode
     * Also updates DataStore preference for UI synchronization.
     */
    fun togglePrivateSession() {
        viewModelScope.launch {
            val currentValue = userPreferences.value.isPrivateSessionEnabled
            val newValue = !currentValue

            // Update DataStore preference (for UI toggle persistence)
            settingsRepository.setPrivateSessionEnabled(newValue)

            // Update session controller
            if (newValue) {
                sessionController.startPrivateSession()
            } else {
                sessionController.stopPrivateSession()
            }
        }
    }

    /**
     * Update app theme.
     */
    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    /**
     * Update app language and apply it immediately.
     * Pass null/system to follow device language.
     * This is async and does not wait for completion.
     */
    fun setAppLanguageTag(languageTag: String?) {
        viewModelScope.launch {
            val normalizedTag = AppLocaleManager.normalizeLanguageTag(languageTag)
            val persistedTag = if (normalizedTag == AppLocaleManager.LANGUAGE_SYSTEM) {
                null
            } else {
                normalizedTag
            }
            settingsRepository.setAppLanguageTag(persistedTag)
        }
    }

    /**
     * Update app language and wait for persistence to complete.
     * Pass null/system to follow device language.
     * This suspend function ensures the save is fully persisted before returning.
     * CRITICAL: Use this when you need to ensure the change is saved (e.g., before Activity.recreate()).
     */
    suspend fun setAppLanguageTagAndWait(languageTag: String?) {
        val normalizedTag = AppLocaleManager.normalizeLanguageTag(languageTag)
        val persistedTag = if (normalizedTag == AppLocaleManager.LANGUAGE_SYSTEM) {
            null
        } else {
            normalizedTag
        }
        settingsRepository.setAppLanguageTag(persistedTag)
    }

    /**
     * Update accent color profile.
     */
    fun setAccentProfile(profile: AccentProfile) {
        viewModelScope.launch {
            settingsRepository.setAccentProfile(profile)
        }
    }

    /**
     * Set a sleep timer to pause playback after [minutes].
     * Passing 0 cancels the current timer.
     */
    fun setSleepTimer(minutes: Int) {
        viewModelScope.launch {
            if (minutes > 0) {
                val targetTimeMs = currentTimeProvider() + (minutes * 60 * 1000L)
                settingsRepository.setSleepTimerTargetTime(targetTimeMs)
            } else {
                settingsRepository.setSleepTimerTargetTime(0L)
            }
        }
    }

    /**
     * Set the timer target directly as epoch millis.
     */
    fun setSleepTimerTargetTime(targetTimeMs: Long) {
        viewModelScope.launch {
            settingsRepository.setSleepTimerTargetTime(targetTimeMs)
        }
    }

    /**
     * Set timer to the end of currently playing track.
     */
    fun setSleepTimerEndOfTrack(): EndOfTrackResult {
        playbackRepository.refreshPlaybackState()
        val playbackState = playbackRepository.playbackState.value
        if (playbackState.currentSong == null) {
            return EndOfTrackResult.NoActiveTrack
        }

        val durationMs = playbackState.duration
        val currentPositionMs = playbackState.currentPosition.coerceAtLeast(0L)
        val remainingMs = (durationMs - currentPositionMs).coerceAtLeast(0L)
        if (durationMs <= 0L || remainingMs <= 0L) {
            return EndOfTrackResult.TrackEndNotAvailable
        }

        setSleepTimerTargetTime(currentTimeProvider() + remainingMs)
        return EndOfTrackResult.Success
    }

    /**
     * Toggle scan on startup setting.
     */
    fun toggleScanOnStartup() {
        viewModelScope.launch {
            val currentValue = userPreferences.value.scanOnStartup
            settingsRepository.setScanOnStartup(!currentValue)
        }
    }

    /**
     * Set notification controls visibility directly (from UI parameter).
     * Avoids race condition from reading stale StateFlow value.
     */
    fun setShowNotificationControls(enabled: Boolean) {
        android.util.Log.d("SettingsViewModel", "setShowNotificationControls called with: $enabled")
        viewModelScope.launch {
            settingsRepository.setShowNotificationControls(enabled)
            android.util.Log.d("SettingsViewModel", "setShowNotificationControls completed: $enabled")
        }
    }

    /**
     * Toggle gapless playback.
     */
    fun toggleGaplessPlayback() {
        viewModelScope.launch {
            val currentValue = userPreferences.value.gaplessPlaybackEnabled
            settingsRepository.setGaplessPlaybackEnabled(!currentValue)
        }
    }

    /**
     * Set crossfade duration.
     */
    fun setCrossfadeDuration(durationMs: Int) {
        viewModelScope.launch {
            settingsRepository.setCrossfadeDuration(durationMs)
        }
    }

    /**
     * Toggle pause on audio noisy.
     */
    fun togglePauseOnAudioNoisy() {
        viewModelScope.launch {
            val currentValue = userPreferences.value.pauseOnAudioNoisy
            settingsRepository.setPauseOnAudioNoisy(!currentValue)
        }
    }

    /**
     * Toggle resume on audio focus.
     */
    fun toggleResumeOnAudioFocus() {
        viewModelScope.launch {
            val currentValue = userPreferences.value.resumeOnAudioFocus
            settingsRepository.setResumeOnAudioFocus(!currentValue)
        }
    }

    /**
     * Set audio quality.
     */
    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch {
            settingsRepository.setAudioQuality(quality)
        }
    }

    /**
     * Set audio buffer size.
     */
    fun setAudioBuffer(bufferMs: Int) {
        viewModelScope.launch {
            settingsRepository.setAudioBuffer(bufferMs)
        }
    }

    /**
     * Toggle audio normalization.
     */
    fun toggleAudioNormalization() {
        viewModelScope.launch {
            val currentValue = userPreferences.value.audioNormalizationEnabled
            settingsRepository.setAudioNormalizationEnabled(!currentValue)
        }
    }

    /**
     * Set minimum audio duration in seconds.
     */
    fun setMinimumAudioDuration(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.setMinimumAudioDuration(seconds)
        }
    }

    /**
     * Toggle show all audio files setting.
     * When toggled, triggers a full rescan to include/exclude non-music audio files.
     */
    fun toggleShowAllAudioFiles() {
        viewModelScope.launch {
            val currentValue = userPreferences.value.showAllAudioFiles
            settingsRepository.setShowAllAudioFiles(!currentValue)

            // Trigger library rescan to update the database with the new filter setting
            songRepository.scanLocalMusic()
        }
    }

    /**
     * Reset scan state to Idle.
     * Call this before opening the rescan dialog to clear previous results.
     */
    fun resetScanState() {
        viewModelScope.launch {
            songRepository.resetScanState()
        }
    }

    /**
     * Trigger a rescan of the media library.
     */
    fun rescanLibrary() {
        viewModelScope.launch {
            songRepository.scanLocalMusic()
        }
    }

    /**
     * Load storage usage statistics.
     */
    fun loadStorageStats() {
        viewModelScope.launch {
            _isLoadingStats.value = true
            try {
                val stats = settingsRepository.getStorageStats()
                _storageStats.value = stats
            } catch (e: Exception) {
                // Handle error silently or emit error state
                _storageStats.value = null
            } finally {
                _isLoadingStats.value = false
            }
        }
    }

    /**
     * Clear image cache (album art).
     */
    fun clearImageCache() {
        viewModelScope.launch {
            _isClearingCache.value = true
            try {
                settingsRepository.clearImageCache()
                loadStorageStats() // Refresh stats after clearing
            } finally {
                _isClearingCache.value = false
            }
        }
    }

    /**
     * Clear song database metadata.
     * IMPORTANT: Does NOT delete physical music files.
     */
    fun clearDatabase() {
        viewModelScope.launch {
            _isClearingData.value = true
            try {
                songRepository.clearDatabase()
                loadStorageStats() // Refresh stats after clearing
            } finally {
                _isClearingData.value = false
            }
        }
    }

    /**
     * Clear recently played history.
     */
    fun clearRecentlyPlayed() {
        viewModelScope.launch {
            songRepository.clearRecentlyPlayed()
        }
    }

    /**
     * Logout - clear all app data.
     * - Clears database
     * - Clears preferences
     * - Clears cache
     */
    fun logout() {
        viewModelScope.launch {
            _isClearingData.value = true
            try {
                settingsRepository.clearAllData()
                loadStorageStats() // Refresh stats after clearing
            } finally {
                _isClearingData.value = false
            }
        }
    }

    /**
     * Get app version from BuildConfig.
     */
    fun getAppVersion(): String = "1.0.0" // TODO: Get from BuildConfig

    /**
     * Get package name.
     */
    fun getPackageName(): String = "com.sukoon.music"
}

enum class EndOfTrackResult {
    Success,
    NoActiveTrack,
    TrackEndNotAvailable
}
