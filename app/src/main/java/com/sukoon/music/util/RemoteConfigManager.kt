package com.sukoon.music.util

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.sukoon.music.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val _albumArtAdsEnabled = MutableStateFlow(true)
    val albumArtAdsEnabled: Flow<Boolean> = _albumArtAdsEnabled.asStateFlow()

    private val _bannerAdsEnabled = MutableStateFlow(true)
    val bannerAdsEnabled: Flow<Boolean> = _bannerAdsEnabled.asStateFlow()

    private val _nowPlayingAdsEnabled = MutableStateFlow(true)
    val nowPlayingAdsEnabled: Flow<Boolean> = _nowPlayingAdsEnabled.asStateFlow()

    init {
        // Set default values (fallback if Firebase is down or not configured)
        remoteConfig.setDefaultsAsync(
            mapOf(
                "show_album_art_ads" to true,
                "show_banner_ads" to true,
                "show_now_playing_ads" to true
            )
        )
    }

    /**
     * Initialize remote config by fetching latest values from Firebase.
     * Call this once on app startup.
     */
    suspend fun initialize() {
        try {
            // Set cache expiration to 12 hours in production, 0 seconds in debug for testing
            val cacheExpiration = if (BuildConfig.DEBUG) 0L else 12 * 60 * 60

            // Configure settings with cache expiration
            val settings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = cacheExpiration
            }
            remoteConfig.setConfigSettingsAsync(settings).await()

            // Fetch and activate remote config
            remoteConfig.fetchAndActivate().await<Boolean>()

            updateAllStates()
            DevLogger.d("RemoteConfigManager", "Remote config initialized successfully")
        } catch (e: Exception) {
            DevLogger.e("RemoteConfigManager", "Failed to fetch remote config: ${e.message}", e)
            // Fall back to defaults
            updateAllStates()
        }
    }

    /**
     * Update all state flows with current remote config values.
     */
    private fun updateAllStates() {
        _albumArtAdsEnabled.value = remoteConfig.getBoolean("show_album_art_ads")
        _bannerAdsEnabled.value = remoteConfig.getBoolean("show_banner_ads")
        _nowPlayingAdsEnabled.value = remoteConfig.getBoolean("show_now_playing_ads")
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
}
