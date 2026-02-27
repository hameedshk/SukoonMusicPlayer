package com.sukoon.music.data.config

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.sukoon.music.util.DevLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private val Context.configDataStore by preferencesDataStore(name = "remote_config_cache")

@Singleton
class RemoteConfigManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val remoteConfig = FirebaseRemoteConfig.getInstance()
    private val _state = MutableStateFlow(RemoteConfigState())
    val state: StateFlow<RemoteConfigState> = _state.asStateFlow()

    private val tag = "RemoteConfigManager"

    // DataStore preference keys for caching
    private val ADS_ENABLED_FREE = booleanPreferencesKey("ads_enabled_free_users")
    private val ADS_ENABLED_PREMIUM = booleanPreferencesKey("ads_enabled_premium_users")
    private val ADS_PLACEMENT_HOME = booleanPreferencesKey("ads_placement_home_screen")
    private val ADS_PLACEMENT_NOWPLAY = booleanPreferencesKey("ads_placement_now_playing")
    private val BANNER_AD_UNIT = stringPreferencesKey("ads_banner_unit_id")
    private val NATIVE_AD_UNIT = stringPreferencesKey("ads_native_unit_id")
    private val LAST_FETCH_TIME = longPreferencesKey("last_fetch_timestamp")

    /**
     * Initialize Remote Config on app startup.
     * Fetches from Firebase (with caching), then loads cached values from DataStore as fallback.
     *
     * This is non-blocking and runs asynchronously.
     */
    suspend fun initialize() {
        try {
            // Load cached values first (immediate availability)
            loadCachedConfig()

            // Configure Remote Config fetch intervals
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // 1 hour minimum between fetches
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)

            // Set default values (used if Remote Config never loads)
            remoteConfig.setDefaultsAsync(getDefaultConfigMap())

            // Attempt to fetch from Firebase (non-blocking)
            fetchAndCacheConfig()

            // Setup real-time listener for config updates
            setupConfigUpdateListener()

            DevLogger.d(tag, "RemoteConfig initialized successfully")
        } catch (e: Exception) {
            DevLogger.e(tag, "Failed to initialize RemoteConfig", e)
            // Continue with cached values or defaults
        }
    }

    /**
     * Fetch Remote Config from Firebase and cache locally.
     */
    private suspend fun fetchAndCacheConfig() {
        try {
            remoteConfig.fetchAndActivate().await()
            DevLogger.d(tag, "Remote Config fetched and activated")
            updateState()
            saveConfigToCache()
        } catch (e: Exception) {
            DevLogger.e(tag, "Failed to fetch Remote Config, using cached values", e)
            // Cached values already loaded in initialize()
        }
    }

    /**
     * Load cached Remote Config from DataStore.
     */
    private suspend fun loadCachedConfig() {
        try {
            val prefs = context.configDataStore.data.first()
            val newState = RemoteConfigState(
                adsEnabledForFreeUsers = prefs[ADS_ENABLED_FREE] ?: true,
                adsEnabledForPremiumUsers = prefs[ADS_ENABLED_PREMIUM] ?: false,
                adsPlacementHomeScreen = prefs[ADS_PLACEMENT_HOME] ?: true,
                adsPlacementNowPlaying = prefs[ADS_PLACEMENT_NOWPLAY] ?: false,
                bannerAdUnitId = prefs[BANNER_AD_UNIT]
                    ?: "ca-app-pub-3940256099942544/6300978111",
                nativeAdUnitId = prefs[NATIVE_AD_UNIT]
                    ?: "ca-app-pub-3940256099942544/2247696110",
                lastFetchTimestamp = prefs[LAST_FETCH_TIME] ?: 0
            )
            _state.value = newState
        } catch (e: Exception) {
            DevLogger.e(tag, "Failed to load cached config", e)
        }
    }

    /**
     * Update state from current Firebase Remote Config values.
     */
    private fun updateState() {
        val newState = RemoteConfigState(
            adsEnabledForFreeUsers = remoteConfig.getBoolean("ads_enabled_free_users"),
            adsEnabledForPremiumUsers = remoteConfig.getBoolean("ads_enabled_premium_users"),
            adsPlacementHomeScreen = remoteConfig.getBoolean("ads_placement_home_screen"),
            adsPlacementNowPlaying = remoteConfig.getBoolean("ads_placement_now_playing"),
            bannerAdUnitId = remoteConfig.getString("ads_banner_unit_id"),
            nativeAdUnitId = remoteConfig.getString("ads_native_unit_id"),
            lastFetchTimestamp = System.currentTimeMillis()
        )
        _state.value = newState
        DevLogger.d(tag, "Remote Config state updated: $newState")
    }

    /**
     * Save current Remote Config state to DataStore for offline access.
     */
    private suspend fun saveConfigToCache() {
        try {
            context.configDataStore.edit { prefs ->
                val current = _state.value
                prefs[ADS_ENABLED_FREE] = current.adsEnabledForFreeUsers
                prefs[ADS_ENABLED_PREMIUM] = current.adsEnabledForPremiumUsers
                prefs[ADS_PLACEMENT_HOME] = current.adsPlacementHomeScreen
                prefs[ADS_PLACEMENT_NOWPLAY] = current.adsPlacementNowPlaying
                prefs[BANNER_AD_UNIT] = current.bannerAdUnitId
                prefs[NATIVE_AD_UNIT] = current.nativeAdUnitId
                prefs[LAST_FETCH_TIME] = current.lastFetchTimestamp
            }
            DevLogger.d(tag, "Remote Config cached to DataStore")
        } catch (e: Exception) {
            DevLogger.e(tag, "Failed to save config to cache", e)
        }
    }

    /**
     * Setup real-time listener for Remote Config updates (for future use).
     * Note: ConfigUpdateListener requires Firebase Remote Config 21.3.0+
     */
    private fun setupConfigUpdateListener() {
        try {
            // This requires Firebase Remote Config with real-time updates support
            // Currently using fetch + activate pattern which is compatible with all versions
            DevLogger.d(tag, "Real-time config listener not configured (using periodic fetch instead)")
        } catch (e: Exception) {
            DevLogger.e(tag, "Error setting up config listener", e)
        }
    }

    /**
     * Get default Remote Config values (used on first install or if Firebase is down).
     */
    private fun getDefaultConfigMap(): Map<String, Any> {
        return mapOf(
            "ads_enabled_free_users" to true,
            "ads_enabled_premium_users" to false,
            "ads_placement_home_screen" to true,
            "ads_placement_now_playing" to false,
            "ads_banner_unit_id" to "ca-app-pub-3940256099942544/6300978111",
            "ads_native_unit_id" to "ca-app-pub-3940256099942544/2247696110"
        )
    }

    /**
     * Helper to check if ads should show for given user tier and placement.
     */
    fun shouldShowAds(isPremiumUser: Boolean, placement: AdPlacement): Boolean {
        return _state.value.shouldShowAds(isPremiumUser, placement)
    }

    /**
     * Get current banner ad unit ID (may be overridden by Remote Config).
     */
    fun getBannerAdUnitId(): String {
        return _state.value.bannerAdUnitId
    }

    /**
     * Get current native ad unit ID (may be overridden by Remote Config).
     */
    fun getNativeAdUnitId(): String {
        return _state.value.nativeAdUnitId
    }
}
