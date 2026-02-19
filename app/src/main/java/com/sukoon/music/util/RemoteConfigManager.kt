package com.sukoon.music.util

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.sukoon.music.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for Firebase Remote Config to control app features globally.
 * Allows enabling/disabling ad features without app updates.
 */
@Singleton
class RemoteConfigManager @Inject constructor() {
    private val remoteConfig = FirebaseRemoteConfig.getInstance()

    data class MinimumRequiredVersionConfig(
        val enabled: Boolean = true,
        val minVersionCode: Int = 1,
        val message: String = DEFAULT_MIN_REQUIRED_VERSION_MESSAGE
    )

    private val _albumArtAdsEnabled = MutableStateFlow(true)
    val albumArtAdsEnabled: Flow<Boolean> = _albumArtAdsEnabled.asStateFlow()

    private val _bannerAdsEnabled = MutableStateFlow(true)
    val bannerAdsEnabled: Flow<Boolean> = _bannerAdsEnabled.asStateFlow()

    private val _nowPlayingAdsEnabled = MutableStateFlow(true)
    val nowPlayingAdsEnabled: Flow<Boolean> = _nowPlayingAdsEnabled.asStateFlow()

    private val _minimumRequiredVersionConfig = MutableStateFlow(
        MinimumRequiredVersionConfig(
            enabled = true,
            minVersionCode = DEFAULT_MIN_REQUIRED_VERSION_CODE,
            message = DEFAULT_MIN_REQUIRED_VERSION_MESSAGE
        )
    )
    val minimumRequiredVersionConfig: StateFlow<MinimumRequiredVersionConfig> =
        _minimumRequiredVersionConfig.asStateFlow()

    init {
        // Set default values (fallback if Firebase is down or not configured)
        remoteConfig.setDefaultsAsync(
            mapOf(
                "show_album_art_ads" to true,
                "show_banner_ads" to true,
                "show_now_playing_ads" to true,
                KEY_MIN_REQUIRED_VERSION_ENABLED to true,
                KEY_MIN_REQUIRED_VERSION_CODE to DEFAULT_MIN_REQUIRED_VERSION_CODE.toLong(),
                KEY_MIN_REQUIRED_VERSION_MESSAGE to DEFAULT_MIN_REQUIRED_VERSION_MESSAGE
            )
        )
    }

    /**
     * Initialize remote config by fetching latest values from Firebase.
     * Call this once on app startup.
     */
    suspend fun initialize() {
        refreshInternal(force = false, updateAllState = true)
    }

    /**
     * Refresh only minimum-version config values.
     * Set [force] to true to fetch immediately (ignores production cache interval).
     */
    suspend fun refreshVersionConfig(force: Boolean = false) {
        refreshInternal(force = force, updateAllState = false)
    }

    private suspend fun refreshInternal(force: Boolean, updateAllState: Boolean) {
        try {
            val cacheExpiration = when {
                force -> 0L
                BuildConfig.DEBUG -> 0L
                else -> PRODUCTION_FETCH_INTERVAL_SECONDS
            }

            // Configure settings with cache expiration
            applyFetchInterval(cacheExpiration)

            // Fetch and activate remote config
            remoteConfig.fetchAndActivate().await<Boolean>()

            if (updateAllState) updateAllStates() else updateMinimumVersionState()
            DevLogger.d("RemoteConfigManager", "Remote config initialized successfully")
        } catch (e: Exception) {
            DevLogger.e("RemoteConfigManager", "Failed to fetch remote config: ${e.message}", e)
            // Fall back to defaults
            if (updateAllState) updateAllStates() else updateMinimumVersionState()
        } finally {
            if (force && !BuildConfig.DEBUG) {
                runCatching { applyFetchInterval(PRODUCTION_FETCH_INTERVAL_SECONDS) }
                    .onFailure { error ->
                        DevLogger.w(
                            "RemoteConfigManager",
                            "Failed to restore production fetch interval: ${error.message}"
                        )
                    }
            }
        }
    }

    /**
     * Update all state flows with current remote config values.
     */
    private fun updateAllStates() {
        _albumArtAdsEnabled.value = remoteConfig.getBoolean("show_album_art_ads")
        _bannerAdsEnabled.value = remoteConfig.getBoolean("show_banner_ads")
        _nowPlayingAdsEnabled.value = remoteConfig.getBoolean("show_now_playing_ads")
        updateMinimumVersionState()
    }

    private suspend fun applyFetchInterval(seconds: Long) {
        val settings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = seconds
        }
        remoteConfig.setConfigSettingsAsync(settings).await()
    }

    private fun updateMinimumVersionState() {
        val enabled = remoteConfig.getBoolean(KEY_MIN_REQUIRED_VERSION_ENABLED)
        val minVersionCode = remoteConfig.getLong(KEY_MIN_REQUIRED_VERSION_CODE)
            .coerceIn(1L, Int.MAX_VALUE.toLong())
            .toInt()
        val message = remoteConfig.getString(KEY_MIN_REQUIRED_VERSION_MESSAGE)
            .takeIf { it.isNotBlank() }
            ?: DEFAULT_MIN_REQUIRED_VERSION_MESSAGE

        _minimumRequiredVersionConfig.value = MinimumRequiredVersionConfig(
            enabled = enabled,
            minVersionCode = minVersionCode,
            message = message
        )
    }

    fun getMinimumRequiredVersionConfig(): MinimumRequiredVersionConfig {
        return minimumRequiredVersionConfig.value
    }

    fun isUpdateRequired(currentVersionCode: Int): Boolean {
        val config = minimumRequiredVersionConfig.value
        return config.enabled && currentVersionCode < config.minVersionCode
    }

    /**
     * Check if album art ads should be shown.
     * This controls native ads in content lists (AlbumsScreen, ArtistsScreen, etc.)
     */
    fun shouldShowAlbumArtAds(): Boolean {
        return remoteConfig.getBoolean("show_album_art_ads")
    }

    /**
     * Check if banner ads should be shown.
     * This controls banner ads at bottom of screens.
     */
    fun shouldShowBannerAds(): Boolean {
        return remoteConfig.getBoolean("show_banner_ads")
    }

    /**
     * Check if now playing overlay ads should be shown.
     * This controls ads that appear on the Now Playing screen after listening duration.
     */
    fun shouldShowNowPlayingAds(): Boolean {
        return remoteConfig.getBoolean("show_now_playing_ads")
    }

    companion object {
        private const val PRODUCTION_FETCH_INTERVAL_SECONDS = 24L * 60L * 60L
        private const val KEY_MIN_REQUIRED_VERSION_ENABLED = "min_required_version_enabled"
        private const val KEY_MIN_REQUIRED_VERSION_CODE = "min_required_version_code"
        private const val KEY_MIN_REQUIRED_VERSION_MESSAGE = "min_required_version_message"
        private const val DEFAULT_MIN_REQUIRED_VERSION_CODE = 1
        private const val DEFAULT_MIN_REQUIRED_VERSION_MESSAGE =
            "A new version is required to continue."
    }
}
