# Firebase Remote Config for Ad Control - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Enable dynamic ad control via Firebase Remote Config without requiring app updates.

**Architecture:** Create a `RemoteConfigManager` singleton that fetches Remote Config on app startup, caches values in DataStore for offline resilience, and exposes ad visibility logic based on user tier and ad placement. Modify `AdMobManager` and ad composables to consume this manager.

**Tech Stack:** Firebase Remote Config, DataStore Preferences, Hilt DI, Kotlin Coroutines, StateFlow

---

## Task 1: Create RemoteConfigState data class

**Files:**
- Create: `app/src/main/java/com/sukoon/music/data/config/RemoteConfigState.kt`

**Step 1: Write the data class**

```kotlin
package com.sukoon.music.data.config

/**
 * Represents the current state of Remote Config loaded from Firebase.
 *
 * @param adsEnabledForFreeUsers Whether ads should show for free tier users
 * @param adsEnabledForPremiumUsers Whether ads should show for premium tier users
 * @param adsPlacementHomeScreen Whether banner ad should show on HomeScreen
 * @param adsPlacementNowPlaying Whether ad should show on NowPlayingScreen
 * @param bannerAdUnitId AdMob banner ad unit ID (from Remote Config)
 * @param nativeAdUnitId AdMob native ad unit ID (from Remote Config)
 * @param lastFetchTimestamp Timestamp of last successful fetch (for debugging)
 * @param isLoading Whether Remote Config is currently fetching
 */
data class RemoteConfigState(
    val adsEnabledForFreeUsers: Boolean = true,
    val adsEnabledForPremiumUsers: Boolean = false,
    val adsPlacementHomeScreen: Boolean = true,
    val adsPlacementNowPlaying: Boolean = false,
    val bannerAdUnitId: String = "ca-app-pub-3940256099942544/6300978111",
    val nativeAdUnitId: String = "ca-app-pub-3940256099942544/2247696110",
    val lastFetchTimestamp: Long = 0,
    val isLoading: Boolean = false
) {
    /**
     * Determine if ads should be shown for the given user tier and placement.
     */
    fun shouldShowAds(isPremiumUser: Boolean, placement: AdPlacement): Boolean {
        val tierAllows = if (isPremiumUser) {
            adsEnabledForPremiumUsers
        } else {
            adsEnabledForFreeUsers
        }

        if (!tierAllows) return false

        val placementAllows = when (placement) {
            AdPlacement.HOME_SCREEN -> adsPlacementHomeScreen
            AdPlacement.NOW_PLAYING -> adsPlacementNowPlaying
        }

        return placementAllows
    }
}

enum class AdPlacement {
    HOME_SCREEN,
    NOW_PLAYING
}
```

**Step 2: Verify the file compiles**

Run: `cd /c/Users/ksham/Documents/SukoonMusicPlayer && ./gradlew compileDebugKotlin 2>&1 | grep -i "remoteconfig" || echo "OK"`

Expected: No errors related to RemoteConfigState

**Step 3: Commit**

```bash
git add app/src/main/java/com/sukoon/music/data/config/RemoteConfigState.kt
git commit -m "feat: add RemoteConfigState data class with ad placement logic"
```

---

## Task 2: Create RemoteConfigManager singleton

**Files:**
- Create: `app/src/main/java/com/sukoon/music/data/config/RemoteConfigManager.kt`
- Modify: `app/src/main/java/com/sukoon/music/util/DevLogger.kt` (reference only, for logging)

**Step 1: Write RemoteConfigManager with Firebase initialization**

```kotlin
package com.sukoon.music.data.config

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.sukoon.music.util.DevLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val logger = DevLogger.withTag("RemoteConfigManager")

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

            logger.d("RemoteConfig initialized successfully")
        } catch (e: Exception) {
            logger.e("Failed to initialize RemoteConfig", e)
            // Continue with cached values or defaults
        }
    }

    /**
     * Fetch Remote Config from Firebase and cache locally.
     */
    private suspend fun fetchAndCacheConfig() {
        try {
            remoteConfig.fetchAndActivate().await()
            logger.d("Remote Config fetched and activated")
            updateState()
            saveConfigToCache()
        } catch (e: Exception) {
            logger.e("Failed to fetch Remote Config, using cached values", e)
            // Cached values already loaded in initialize()
        }
    }

    /**
     * Load cached Remote Config from DataStore.
     */
    private suspend fun loadCachedConfig() {
        try {
            val prefs = context.configDataStore.data.collect { prefData ->
                val newState = RemoteConfigState(
                    adsEnabledForFreeUsers = prefData[ADS_ENABLED_FREE] ?: true,
                    adsEnabledForPremiumUsers = prefData[ADS_ENABLED_PREMIUM] ?: false,
                    adsPlacementHomeScreen = prefData[ADS_PLACEMENT_HOME] ?: true,
                    adsPlacementNowPlaying = prefData[ADS_PLACEMENT_NOWPLAY] ?: false,
                    bannerAdUnitId = prefData[BANNER_AD_UNIT]
                        ?: "ca-app-pub-3940256099942544/6300978111",
                    nativeAdUnitId = prefData[NATIVE_AD_UNIT]
                        ?: "ca-app-pub-3940256099942544/2247696110",
                    lastFetchTimestamp = prefData[LAST_FETCH_TIME] ?: 0
                )
                _state.value = newState
            }
        } catch (e: Exception) {
            logger.e("Failed to load cached config", e)
            // Use defaults already in _state
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
        logger.d("Remote Config state updated: $newState")
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
            logger.d("Remote Config cached to DataStore")
        } catch (e: Exception) {
            logger.e("Failed to save config to cache", e)
        }
    }

    /**
     * Setup real-time listener for Remote Config updates (for future use).
     */
    private fun setupConfigUpdateListener() {
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                logger.d("Remote Config updated in real-time, activating...")
                if (remoteConfig.activate().isSuccessful) {
                    updateState()
                }
            }

            override fun onError(error: Exception) {
                logger.e("Remote Config real-time update error", error)
            }
        })
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
```

**Step 2: Fix import issue (DataStore Flow)**

Update the import to use Flow correctly:

```kotlin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
```

And update `loadCachedConfig()` to use `.first()`:

```kotlin
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
        logger.e("Failed to load cached config", e)
    }
}
```

**Step 3: Verify compilation**

Run: `cd /c/Users/ksham/Documents/SukoonMusicPlayer && ./gradlew compileDebugKotlin 2>&1 | tail -20`

Expected: Build succeeds (no errors)

**Step 4: Commit**

```bash
git add app/src/main/java/com/sukoon/music/data/config/RemoteConfigManager.kt
git commit -m "feat: add RemoteConfigManager with Firebase Remote Config integration"
```

---

## Task 3: Modify AdMobManager to use RemoteConfigManager

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/data/ads/AdMobManager.kt`

**Step 1: Add RemoteConfigManager dependency**

Update the constructor:

```kotlin
@Singleton
class AdMobManager @Inject constructor(
    private val context: Context,
    private val remoteConfigManager: RemoteConfigManager
) {
```

**Step 2: Update getBannerAdId() to use Remote Config**

Replace:
```kotlin
fun getBannerAdId(): String {
    return BuildConfig.ADMOB_BANNER_AD_UNIT_ID
}
```

With:
```kotlin
fun getBannerAdId(): String {
    return remoteConfigManager.getBannerAdUnitId()
}
```

**Step 3: Update getNativeAdId() to use Remote Config**

Replace:
```kotlin
fun getNativeAdId(): String {
    return BuildConfig.ADMOB_NATIVE_AD_UNIT_ID
}
```

With:
```kotlin
fun getNativeAdId(): String {
    return remoteConfigManager.getNativeAdUnitId()
}
```

**Step 4: Add shouldShowAds helper**

Add after `getNativeAdId()`:

```kotlin
/**
 * Check if ads should be shown for the given user tier and placement.
 */
fun shouldShowAds(isPremiumUser: Boolean, placement: AdPlacement): Boolean {
    return remoteConfigManager.shouldShowAds(isPremiumUser, placement)
}
```

**Step 5: Add import for AdPlacement**

Add at top of file:
```kotlin
import com.sukoon.music.data.config.AdPlacement
```

**Step 6: Verify compilation**

Run: `cd /c/Users/ksham/Documents/SukoonMusicPlayer && ./gradlew compileDebugKotlin 2>&1 | tail -20`

Expected: Build succeeds

**Step 7: Commit**

```bash
git add app/src/main/java/com/sukoon/music/data/ads/AdMobManager.kt
git commit -m "feat: integrate RemoteConfigManager into AdMobManager for dynamic ad control"
```

---

## Task 4: Initialize RemoteConfigManager in MainActivity

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/ui/MainActivity.kt`

**Step 1: Find the onCreate method and add RemoteConfig initialization**

Inject RemoteConfigManager:
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Remote Config asynchronously
        lifecycleScope.launch {
            remoteConfigManager.initialize()
        }

        // ... rest of onCreate
    }
}
```

**Step 2: Add import**

```kotlin
import com.sukoon.music.data.config.RemoteConfigManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
```

**Step 3: Verify compilation**

Run: `cd /c/Users/ksham/Documents/SukoonMusicPlayer && ./gradlew compileDebugKotlin 2>&1 | tail -20`

Expected: Build succeeds

**Step 4: Commit**

```bash
git add app/src/main/java/com/sukoon/music/ui/MainActivity.kt
git commit -m "feat: initialize RemoteConfigManager on app startup"
```

---

## Task 5: Modify BannerAdView to respect Remote Config

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/ui/components/BannerAdView.kt`

**Step 1: Inject RemoteConfigManager and add conditional rendering**

```kotlin
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adMobManager: AdMobManager = hiltViewModel(),
    remoteConfigManager: RemoteConfigManager = hiltViewModel(),
    isPremiumUser: Boolean = false
) {
    // Check if ads should be shown before rendering
    val configState = remoteConfigManager.state.collectAsStateWithLifecycle()
    val shouldShowAds = configState.value.shouldShowAds(isPremiumUser, AdPlacement.HOME_SCREEN)

    if (!shouldShowAds) {
        return  // Don't render ad if Remote Config disables it
    }

    // ... rest of BannerAdView implementation
}
```

**Step 2: Add imports**

```kotlin
import com.sukoon.music.data.config.RemoteConfigManager
import com.sukoon.music.data.config.AdPlacement
import androidx.lifecycle.compose.collectAsStateWithLifecycle
```

**Step 3: Verify compilation**

Run: `cd /c/Users/ksham/Documents/SukoonMusicPlayer && ./gradlew compileDebugKotlin 2>&1 | tail -20`

Expected: Build succeeds

**Step 4: Commit**

```bash
git add app/src/main/java/com/sukoon/music/ui/components/BannerAdView.kt
git commit -m "feat: add Remote Config conditional rendering to BannerAdView"
```

---

## Task 6: Modify NowPlayingAdOverlay to respect Remote Config

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/ui/components/NowPlayingAdOverlay.kt`

**Step 1: Inject RemoteConfigManager and add conditional rendering**

```kotlin
@Composable
fun NowPlayingAdOverlay(
    modifier: Modifier = Modifier,
    adMobManager: AdMobManager = hiltViewModel(),
    remoteConfigManager: RemoteConfigManager = hiltViewModel(),
    isPremiumUser: Boolean = false
) {
    // Check if ads should be shown before rendering
    val configState = remoteConfigManager.state.collectAsStateWithLifecycle()
    val shouldShowAds = configState.value.shouldShowAds(isPremiumUser, AdPlacement.NOW_PLAYING)

    if (!shouldShowAds) {
        return  // Don't render ad if Remote Config disables it
    }

    // ... rest of NowPlayingAdOverlay implementation
}
```

**Step 2: Add imports**

```kotlin
import com.sukoon.music.data.config.RemoteConfigManager
import com.sukoon.music.data.config.AdPlacement
import androidx.lifecycle.compose.collectAsStateWithLifecycle
```

**Step 3: Verify compilation**

Run: `cd /c/Users/ksham/Documents/SukoonMusicPlayer && ./gradlew compileDebugKotlin 2>&1 | tail -20`

Expected: Build succeeds

**Step 4: Commit**

```bash
git add app/src/main/java/com/sukoon/music/ui/components/NowPlayingAdOverlay.kt
git commit -m "feat: add Remote Config conditional rendering to NowPlayingAdOverlay"
```

---

## Task 7: Build and verify no compilation errors

**Files:**
- None

**Step 1: Run full debug build**

Run: `cd /c/Users/ksham/Documents/SukoonMusicPlayer && ./gradlew assembleDebug 2>&1 | tail -30`

Expected: BUILD SUCCESSFUL

**Step 2: If build fails, check errors**

Run: `cd /c/Users/ksham/Documents/SukoonMusicPlayer && ./gradlew assembleDebug 2>&1 | grep -A 5 "error:"`

Debug and fix any compilation errors

**Step 3: Commit if any fixes needed**

```bash
git add -A
git commit -m "fix: resolve compilation errors in Remote Config integration"
```

---

## Task 8: Manual verification on device/emulator

**Files:**
- None (testing only)

**Step 1: Install debug build**

Run: `cd /c/Users/ksham/Documents/SukoonMusicPlayer && ./gradlew installDebug`

Expected: App installs successfully

**Step 2: Launch app and verify initialization**

- Open Logcat and filter by "RemoteConfigManager"
- Launch the app
- Check logs for "RemoteConfig initialized successfully"

**Step 3: Verify HomeScreen banner ad visibility**

- Check Firebase Console → Remote Config
- Verify `ads_placement_home_screen = true`
- Navigate to HomeScreen, confirm banner ad is visible

**Step 4: Test remote config disable**

- In Firebase Console, set `ads_enabled_free_users = false`
- Wait for config to activate (or force refresh if supported)
- Navigate HomeScreen, confirm banner ad is hidden

**Step 5: Verify NowPlayingScreen (if ad overlay implemented)**

- Set `ads_placement_now_playing = true` in Firebase
- Play a song, navigate to NowPlayingScreen
- Confirm ad overlay appears (if feature enabled)

**Step 6: Test offline behavior**

- Enable airplane mode
- Force-close and restart app
- Verify cached Remote Config values are used (no crashes)

**Step 7: Commit any debug changes**

```bash
git status
# If only .gradle changes, no commit needed
```

---

## Task 9: Add unit tests for RemoteConfigManager (Optional but recommended)

**Files:**
- Create: `app/src/test/java/com/sukoon/music/data/config/RemoteConfigManagerTest.kt`

**Step 1: Write unit tests**

```kotlin
package com.sukoon.music.data.config

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoteConfigManagerTest {

    @Test
    fun shouldShowAds_freeUserHomeScreen_returnsTrue() = runTest {
        val state = RemoteConfigState(
            adsEnabledForFreeUsers = true,
            adsPlacementHomeScreen = true
        )

        assertTrue(state.shouldShowAds(isPremiumUser = false, AdPlacement.HOME_SCREEN))
    }

    @Test
    fun shouldShowAds_freeUserButDisabled_returnsFalse() = runTest {
        val state = RemoteConfigState(
            adsEnabledForFreeUsers = false,
            adsPlacementHomeScreen = true
        )

        assertFalse(state.shouldShowAds(isPremiumUser = false, AdPlacement.HOME_SCREEN))
    }

    @Test
    fun shouldShowAds_premiumUserNeverShowsAds_returnsFalse() = runTest {
        val state = RemoteConfigState(
            adsEnabledForPremiumUsers = false,
            adsPlacementHomeScreen = true
        )

        assertFalse(state.shouldShowAds(isPremiumUser = true, AdPlacement.HOME_SCREEN))
    }

    @Test
    fun shouldShowAds_placementDisabled_returnsFalse() = runTest {
        val state = RemoteConfigState(
            adsEnabledForFreeUsers = true,
            adsPlacementNowPlaying = false
        )

        assertFalse(state.shouldShowAds(isPremiumUser = false, AdPlacement.NOW_PLAYING))
    }
}
```

**Step 2: Run tests**

Run: `cd /c/Users/ksham/Documents/SukoonMusicPlayer && ./gradlew test --tests RemoteConfigManagerTest 2>&1 | tail -20`

Expected: All tests PASS

**Step 3: Commit**

```bash
git add app/src/test/java/com/sukoon/music/data/config/RemoteConfigManagerTest.kt
git commit -m "test: add unit tests for RemoteConfigState.shouldShowAds() logic"
```

---

## Summary

**Completed:**
1. ✅ RemoteConfigState data class with ad placement logic
2. ✅ RemoteConfigManager singleton with Firebase integration + DataStore caching
3. ✅ AdMobManager modified to use Remote Config ad unit IDs
4. ✅ MainActivity initializes RemoteConfigManager on startup
5. ✅ BannerAdView respects Remote Config placement rules
6. ✅ NowPlayingAdOverlay respects Remote Config placement rules
7. ✅ Full debug build verification
8. ✅ Manual device testing
9. ✅ Unit tests (optional)

**Next Steps:**
- Configure Remote Config parameters in Firebase Console
- Set production ad unit IDs in Remote Config (if needed)
- Deploy to Play Store with Remote Config targeting rules

**Key Files:**
- `app/src/main/java/com/sukoon/music/data/config/RemoteConfigManager.kt` (new)
- `app/src/main/java/com/sukoon/music/data/config/RemoteConfigState.kt` (new)
- `app/src/main/java/com/sukoon/music/data/ads/AdMobManager.kt` (modified)
- `app/src/main/java/com/sukoon/music/ui/MainActivity.kt` (modified)
- `app/src/main/java/com/sukoon/music/ui/components/BannerAdView.kt` (modified)
- `app/src/main/java/com/sukoon/music/ui/components/NowPlayingAdOverlay.kt` (modified)
