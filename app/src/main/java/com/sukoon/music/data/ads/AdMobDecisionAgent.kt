package com.sukoon.music.data.ads

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.sukoon.music.BuildConfig
import com.sukoon.music.data.premium.PremiumManager
import com.sukoon.music.domain.usecase.SessionController
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.util.DevLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * AdMobDecisionAgent - Intelligent ad delivery controller.
 *
 * This singleton manager evaluates contextual signals (playback state, user behavior, ad performance)
 * and makes real-time decisions about when to show ads, respecting all hard rules and frequency caps.
 *
 * Hard Rules (Never Broken):
 * - NO ads for premium users (ONLY premium flag blocks ads)
 * - NO interstitials while audio is playing
 * - NO blocking UI while waiting for ads
 * - Exponential backoff after NO_FILL (banner: 60s→120s→240s→480s, native: 120s→240s)
 * - Frequency caps: 2 interstitials/session, 1 native/album/session, 1 banner visible
 * - Private session does NOT block ads (only premium status blocks ads)
 *
 * Optimizations:
 * - Timer pauses when app is backgrounded (lifecycle-aware)
 * - Debounced decision evaluation (100ms) to prevent rapid recomposition
 *
 * All state is in-memory and resets on app restart (session-scoped tracking).
 */
@Singleton
class AdMobDecisionAgent @Inject constructor(
    private val premiumManager: PremiumManager,
    private val playbackRepository: PlaybackRepository,
    private val sessionController: SessionController,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AdMobDecisionAgent"

        // Timing constraints (milliseconds)
        private const val BANNER_REFRESH_MIN_MS = 60_000L // 60 seconds
        private const val BANNER_REFRESH_MAX_MS = 90_000L // 90 seconds
        private const val BANNER_NO_FILL_COOLDOWN_MS = 60_000L // Base: 60s (exponential backoff applied)
        private const val NATIVE_FAILURE_COOLDOWN_MS = 120_000L // Base: 120s (exponential backoff applied)
        private const val INTERSTITIAL_MIN_INTERVAL_MS = 180_000L // 3 minutes

        // Frequency caps
        private const val MAX_INTERSTITIALS_PER_SESSION = 2
        private const val MAX_CONSECUTIVE_BANNER_FAILURES = 3
        private const val MAX_CONSECUTIVE_NATIVE_FAILURES = 2

        // Load latency threshold
        private const val LOAD_LATENCY_HIGH_MS = 1500L
    }

    // ============== Internal State Classes ==============

    private data class BannerState(
        var lastRefreshTime: Long = 0,
        var lastFailureTime: Long = 0,
        var consecutiveFailures: Int = 0,
        var isVisible: Boolean = false,
        var isAppInForeground: Boolean = true
    )

    private data class NativeAdState(
        val albumsShownThisSession: MutableSet<Long> = mutableSetOf(),
        var lastFailureTime: Long = 0,
        var consecutiveFailures: Int = 0,
        var totalImpressionsThisSession: Int = 0
    )

    private data class InterstitialState(
        var lastShownTime: Long = 0,
        var timesShownThisSession: Int = 0,
        var isPreloaded: Boolean = false,
        var lastPreloadTime: Long = 0
    )

    private data class AdLoadMetrics(
        val recentLoadTimes: MutableList<Long> = mutableListOf(),
        var averageLoadTimeMs: Long = 0
    )

    // ============== State Tracking ==============

    private val bannerState = BannerState()
    private val nativeState = NativeAdState()
    private val interstitialState = InterstitialState()
    private val loadMetrics = AdLoadMetrics()

    // ============== Reactive State ==============

    private val _premiumUpsellSignal = MutableStateFlow(false)
    val premiumUpsellSignal: StateFlow<Boolean> = _premiumUpsellSignal.asStateFlow()

    // ============== Banner Decision Logic ==============

    /**
     * Decide whether to request/show a banner ad.
     *
     * Evaluates:
     * - Premium user status (hard rule - ONLY blocker)
     * - Recent NO_FILL failures (exponential backoff: 60s → 120s → 240s → 480s)
     * - Consecutive failures (max 3)
     * - Mini player overlap (UX, production only)
     * - App background state
     * - Refresh interval (60-90s)
     *
     * @param isMiniPlayerVisible True if mini player is overlapping banner area
     * @param currentRoute Current navigation route (for logging)
     * @return AdDecision with shouldShow and reason
     */
    suspend fun shouldShowBanner(
        isMiniPlayerVisible: Boolean,
        currentRoute: String?
    ): AdDecision {
        // Hard rule: Premium users never see ads
        if (premiumManager.isPremiumUser.first()) {
            return AdDecision(false, AdFormat.BANNER, "Premium user")
        }

        val now = System.currentTimeMillis()

        // Issue #7 Fix: Exponential backoff for ad failures
        // Formula: 60s * 2^(failures-1)
        // 1 failure: 60s, 2 failures: 120s, 3 failures: 240s, 4+ failures: 480s (capped)
        val backoffMultiplier = if (bannerState.consecutiveFailures > 0) {
            (1 shl (bannerState.consecutiveFailures - 1)).coerceAtMost(8) // Max 8x = 480s
        } else {
            1
        }
        val cooldownMs = BANNER_NO_FILL_COOLDOWN_MS * backoffMultiplier

        // Suppress if recent NO_FILL (with exponential backoff)
        if (bannerState.lastFailureTime > 0 &&
            now - bannerState.lastFailureTime < cooldownMs
        ) {
            val timeSinceFailure = (now - bannerState.lastFailureTime) / 1000
            val cooldownSeconds = cooldownMs / 1000
            return AdDecision(
                false,
                AdFormat.BANNER,
                "Recent NO_FILL (${timeSinceFailure}s ago, backoff: ${cooldownSeconds}s, failures: ${bannerState.consecutiveFailures})",
                recheckAfterMs = cooldownMs - (now - bannerState.lastFailureTime)
            )
        }

        // Suppress if 3+ consecutive failures
        if (bannerState.consecutiveFailures >= MAX_CONSECUTIVE_BANNER_FAILURES) {
            return AdDecision(
                false,
                AdFormat.BANNER,
                "Too many consecutive failures (${bannerState.consecutiveFailures})"
            )
        }

        // Suppress if mini player overlaps in production.
        // For test ads, allow rendering so QA can reliably verify integration.
        if (isMiniPlayerVisible && !BuildConfig.USE_TEST_ADS) {
            return AdDecision(false, AdFormat.BANNER, "Mini player overlap")
        }

        // Suppress if app is in background
        if (!bannerState.isAppInForeground) {
            return AdDecision(false, AdFormat.BANNER, "App in background")
        }

        // Check refresh interval (60-90s randomized)
        val refreshInterval =
            BANNER_REFRESH_MIN_MS + (Math.random() * (BANNER_REFRESH_MAX_MS - BANNER_REFRESH_MIN_MS)).toLong()
        if (bannerState.lastRefreshTime > 0 &&
            now - bannerState.lastRefreshTime < refreshInterval
        ) {
            val timeSinceRefresh = (now - bannerState.lastRefreshTime) / 1000
            val totalRefreshTime = refreshInterval / 1000
            return AdDecision(
                false,
                AdFormat.BANNER,
                "Refresh cooldown (${timeSinceRefresh}s / ${totalRefreshTime}s)",
                recheckAfterMs = refreshInterval - (now - bannerState.lastRefreshTime)
            )
        }

        // All checks passed
        return AdDecision(true, AdFormat.BANNER, "All conditions met")
    }

    // ============== Native Ad Decision Logic ==============

    /**
     * Decide whether to request/show a native ad.
     *
     * Evaluates:
     * - Premium user status (hard rule - ONLY blocker)
     * - User engagement (scroll detection)
     * - Album size (minimum 10 tracks)
     * - Album frequency cap (1 per album per session)
     * - Consecutive failures (suppress after 2)
     * - Recent failure cooldown (exponential backoff: 120s → 240s)
     *
     * @param albumId Unique identifier for the album
     * @param albumTrackCount Number of tracks in the album
     * @param hasUserScrolled True if user has scrolled in the list
     * @return AdDecision with shouldShow and reason
     */
    suspend fun shouldShowNative(
        albumId: Long,
        albumTrackCount: Int,
        hasUserScrolled: Boolean
    ): AdDecision {
        // Hard rules
        if (premiumManager.isPremiumUser.first()) {
            return AdDecision(false, AdFormat.NATIVE, "Premium user")
        }

        val now = System.currentTimeMillis()

        // Policy: Require user scroll (engagement check)
        if (!hasUserScrolled) {
            return AdDecision(false, AdFormat.NATIVE, "User has not scrolled yet")
        }

        // Policy: Album must have >= 10 tracks
        if (albumTrackCount < 10) {
            return AdDecision(
                false,
                AdFormat.NATIVE,
                "Album too small ($albumTrackCount tracks < 10)"
            )
        }

        // Frequency cap: 1 native per album per session
        if (nativeState.albumsShownThisSession.contains(albumId)) {
            return AdDecision(
                false,
                AdFormat.NATIVE,
                "Already shown for this album this session"
            )
        }

        // Suppress if 2+ consecutive NO_FILL failures (suppress for entire session)
        if (nativeState.consecutiveFailures >= MAX_CONSECUTIVE_NATIVE_FAILURES) {
            return AdDecision(
                false,
                AdFormat.NATIVE,
                "Suppressed for session (${nativeState.consecutiveFailures} consecutive failures)"
            )
        }

        // Issue #7 Fix: Exponential backoff for native ad failures
        // Formula: 120s * 2^(failures-1)
        // 1 failure: 120s, 2 failures: 240s (max, since MAX_CONSECUTIVE_NATIVE_FAILURES = 2)
        val nativeBackoffMultiplier = if (nativeState.consecutiveFailures > 0) {
            (1 shl (nativeState.consecutiveFailures - 1)).coerceAtMost(4) // Max 4x = 480s
        } else {
            1
        }
        val nativeCooldownMs = NATIVE_FAILURE_COOLDOWN_MS * nativeBackoffMultiplier

        // Suppress if last failure within cooldown period (with exponential backoff)
        if (nativeState.lastFailureTime > 0 &&
            now - nativeState.lastFailureTime < nativeCooldownMs
        ) {
            val timeSinceFailure = (now - nativeState.lastFailureTime) / 1000
            val cooldownSeconds = nativeCooldownMs / 1000
            return AdDecision(
                false,
                AdFormat.NATIVE,
                "Recent failure cooldown (${timeSinceFailure}s ago, backoff: ${cooldownSeconds}s, failures: ${nativeState.consecutiveFailures})",
                recheckAfterMs = nativeCooldownMs - (now - nativeState.lastFailureTime)
            )
        }

        return AdDecision(true, AdFormat.NATIVE, "All conditions met")
    }

    // ============== Interstitial Decision Logic ==============

    /**
     * Decide whether to show an interstitial ad.
     *
     * Evaluates:
     * - Premium user status (hard rule - ONLY blocker)
     * - CRITICAL: Playback state (NEVER interrupt music)
     * - Session frequency cap (max 2)
     * - Time gate (min 3 minutes since last)
     * - Preload status (must be preloaded)
     *
     * @return AdDecision with shouldShow and reason
     */
    suspend fun shouldShowInterstitial(): AdDecision {
        // Hard rules
        if (premiumManager.isPremiumUser.first()) {
            return AdDecision(false, AdFormat.INTERSTITIAL, "Premium user")
        }

        val playback = playbackRepository.playbackState.value
        val now = System.currentTimeMillis()

        // CRITICAL: Never show if music is playing
        if (playback.isPlaying) {
            return AdDecision(
                false,
                AdFormat.INTERSTITIAL,
                "Playback active (NEVER interrupt music)"
            )
        }

        // Frequency cap: Max 2 per session
        if (interstitialState.timesShownThisSession >= MAX_INTERSTITIALS_PER_SESSION) {
            return AdDecision(
                false,
                AdFormat.INTERSTITIAL,
                "Session limit reached (${interstitialState.timesShownThisSession}/$MAX_INTERSTITIALS_PER_SESSION)"
            )
        }

        // Time gate: Min 3 minutes since last interstitial
        if (interstitialState.lastShownTime > 0 &&
            now - interstitialState.lastShownTime < INTERSTITIAL_MIN_INTERVAL_MS
        ) {
            val timeSinceShown = (now - interstitialState.lastShownTime) / 1000
            val totalMinutes = INTERSTITIAL_MIN_INTERVAL_MS / 1000 / 60
            return AdDecision(
                false,
                AdFormat.INTERSTITIAL,
                "Cooldown period (${timeSinceShown}s / ${totalMinutes * 60}s)",
                recheckAfterMs = INTERSTITIAL_MIN_INTERVAL_MS - (now - interstitialState.lastShownTime)
            )
        }

        // Must be preloaded
        if (!interstitialState.isPreloaded) {
            return AdDecision(
                false,
                AdFormat.INTERSTITIAL,
                "Ad not preloaded yet"
            )
        }

        return AdDecision(true, AdFormat.INTERSTITIAL, "All conditions met")
    }

    // ============== Event Recording ==============

    /**
     * Record successful ad load.
     *
     * @param format Ad format that loaded
     * @param loadTimeMs Time taken to load (milliseconds)
     */
    fun recordAdLoaded(format: AdFormat, loadTimeMs: Long) {
        when (format) {
            AdFormat.BANNER -> {
                bannerState.consecutiveFailures = 0
                bannerState.lastRefreshTime = System.currentTimeMillis()
                DevLogger.d(TAG, "Banner ad loaded in ${loadTimeMs}ms")
            }

            AdFormat.NATIVE -> {
                nativeState.consecutiveFailures = 0
                DevLogger.d(TAG, "Native ad loaded in ${loadTimeMs}ms")
            }

            AdFormat.INTERSTITIAL -> {
                interstitialState.isPreloaded = true
                interstitialState.lastPreloadTime = System.currentTimeMillis()
                DevLogger.d(TAG, "Interstitial ad loaded in ${loadTimeMs}ms")
            }
        }

        // Track load time metrics
        loadMetrics.recentLoadTimes.add(loadTimeMs)
        if (loadMetrics.recentLoadTimes.size > 10) {
            loadMetrics.recentLoadTimes.removeAt(0)
        }
        loadMetrics.averageLoadTimeMs =
            if (loadMetrics.recentLoadTimes.isNotEmpty()) {
                loadMetrics.recentLoadTimes.average().toLong()
            } else {
                0L
            }
    }

    /**
     * Record ad load failure (NO_FILL, timeout, network error, etc).
     *
     * @param format Ad format that failed
     * @param errorCode AdMob error code
     * @param errorMessage Error message
     */
    fun recordAdFailed(format: AdFormat, errorCode: Int, errorMessage: String) {
        val now = System.currentTimeMillis()

        when (format) {
            AdFormat.BANNER -> {
                bannerState.consecutiveFailures++
                bannerState.lastFailureTime = now
                DevLogger.e(TAG, "Banner ad failed: Code $errorCode - $errorMessage")
            }

            AdFormat.NATIVE -> {
                nativeState.consecutiveFailures++
                nativeState.lastFailureTime = now
                DevLogger.e(TAG, "Native ad failed: Code $errorCode - $errorMessage")
            }

            AdFormat.INTERSTITIAL -> {
                interstitialState.isPreloaded = false
                DevLogger.e(TAG, "Interstitial ad failed: Code $errorCode - $errorMessage")
            }
        }
    }

    /**
     * Record ad impression (shown to user).
     *
     * @param format Ad format shown
     * @param albumId Optional album ID (for native ads, tracks which album was shown for)
     */
    fun recordAdImpression(format: AdFormat, albumId: Long? = null) {
        when (format) {
            AdFormat.BANNER -> {
                bannerState.isVisible = true
                DevLogger.d(TAG, "Banner ad impression")
            }

            AdFormat.NATIVE -> {
                nativeState.totalImpressionsThisSession++
                albumId?.let { nativeState.albumsShownThisSession.add(it) }
                DevLogger.d(TAG, "Native ad impression (album: $albumId, total: ${nativeState.totalImpressionsThisSession})")

                // Signal premium upsell after 2nd native impression
                if (nativeState.totalImpressionsThisSession >= 2) {
                    _premiumUpsellSignal.value = true
                    DevLogger.d(TAG, "Premium upsell signal triggered")
                }
            }

            AdFormat.INTERSTITIAL -> {
                interstitialState.timesShownThisSession++
                interstitialState.lastShownTime = System.currentTimeMillis()
                interstitialState.isPreloaded = false // Consumed
                DevLogger.d(TAG, "Interstitial impression (${interstitialState.timesShownThisSession}/$MAX_INTERSTITIALS_PER_SESSION)")
            }
        }
    }

    /**
     * Reset premium upsell signal (e.g., user dismissed the dialog).
     */
    fun dismissPremiumUpsell() {
        _premiumUpsellSignal.value = false
        DevLogger.d(TAG, "Premium upsell signal dismissed")
    }

    // ============== Lifecycle Management ==============

    /**
     * Call when app goes to background.
     * Pauses banner refresh to save resources.
     * Resets refresh timer to allow immediate ad on next foreground.
     */
    fun onAppBackgrounded() {
        bannerState.isAppInForeground = false
        bannerState.lastRefreshTime = 0 // Reset to allow immediate ad on next launch
        DevLogger.d(TAG, "App backgrounded - suppressing banner refresh and resetting timer")
    }

    /**
     * Call when app returns to foreground.
     * Resumes banner refresh.
     */
    fun onAppForegrounded() {
        bannerState.isAppInForeground = true
        DevLogger.d(TAG, "App foregrounded - resuming banner refresh")
    }

    // ============== Metrics & Analysis ==============

    /**
     * Check if load latency is high and should reduce frequency.
     *
     * @return True if average load time exceeds 1500ms threshold
     */
    fun isLoadLatencyHigh(): Boolean {
        return loadMetrics.averageLoadTimeMs > LOAD_LATENCY_HIGH_MS
    }

    /**
     * Get average ad load time in milliseconds.
     *
     * @return Average load time (0 if no loads recorded)
     */
    fun getAverageLoadTimeMs(): Long {
        return loadMetrics.averageLoadTimeMs
    }
}
