package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.domain.model.AccentProfile
import com.sukoon.music.domain.model.AudioQuality
import com.sukoon.music.domain.model.UserPreferences
import com.sukoon.music.domain.repository.SettingsRepository
import com.sukoon.music.domain.repository.SongRepository
import com.sukoon.music.domain.repository.StorageStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sukoon.music.ui.theme.*

/**
 * ViewModel for the Settings Screen.
 *
 * Responsibilities:
 * - Manage user preferences (private session, theme, etc.)
 * - Provide storage statistics
 * - Handle data clearing operations
 * - Manage app configuration
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val songRepository: SongRepository,
    private val sessionController: com.sukoon.music.domain.usecase.SessionController
) : ViewModel() {

    // --- User Preferences ---

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
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
     * Update accent color profile.
     */
    fun setAccentProfile(profile: AccentProfile) {
        viewModelScope.launch {
            settingsRepository.setAccentProfile(profile)
        }
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
