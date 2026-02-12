package com.sukoon.music.ui.components

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.sukoon.music.BuildConfig
import com.sukoon.music.data.ads.AdDecision
import com.sukoon.music.data.ads.AdFormat
import com.sukoon.music.data.ads.AdMobDecisionAgent
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.data.premium.PremiumManager
import com.sukoon.music.ui.theme.*
import com.sukoon.music.util.DevLogger
import kotlinx.coroutines.delay

/**
 * Height of the ad banner in dp.
 * This is dynamically calculated based on the adaptive ad size.
 * Use this constant for layout calculations in MainActivity.
 */
const val AD_CONTAINER_HEIGHT_DP = 60

/**
 * Shimmer effect for ad skeleton loader.
 */
private fun Modifier.adShimmer(): Modifier = composed {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.background.copy(alpha = 0.3f)
    )

    val transition = rememberInfiniteTransition(label = "ad_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ad_shimmer_translate"
    )

    this.background(
        brush = Brush.horizontalGradient(
            colors = shimmerColors,
            startX = translateAnim - 1000f,
            endX = translateAnim
        )
    )
}

/**
 * Global Banner Ad component that appears at the bottom of all screens.
 *
 * Key features:
 * - Uses AdMobDecisionAgent to determine if ads should show
 * - Lifecycle-aware timer refresh (60-90s) - pauses when app is backgrounded
 * - Debounced decision evaluation (16ms) to prevent rapid recomposition
 * - Respects premium user status via decision agent
 * - Hides if mini player overlaps
 * - Automatically handles ad lifecycle (load/destroy)
 * - Records load success/failure and impressions for analytics
 * - Adaptive banner sizing with rotation handling
 * - Shimmer skeleton loader during ad load
 *
 * @param adMobManager Injected AdMob configuration manager
 * @param decisionAgent Injected AdMob decision agent for intelligent ad delivery
 * @param premiumManager Injected premium subscription manager (for premium check)
 * @param currentRoute Current navigation route (for decision logging)
 * @param isMiniPlayerVisible True if mini player is visible (overlaps banner)
 * @param onAdHeightChanged Callback when actual ad height changes (for layout sync)
 * @param modifier Optional modifier for positioning
 */
@Composable
fun GlobalBannerAdView(
    adMobManager: AdMobManager,
    decisionAgent: AdMobDecisionAgent,
    premiumManager: PremiumManager,
    currentRoute: String?,
    isMiniPlayerVisible: Boolean = false,
    onAdHeightChanged: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val TAG = "GlobalBannerAdView"
    // GlobalBannerAdView.kt - Add decision caching & stabilize ad recreation
  var lastDecisionState by remember { mutableStateOf<AdDecision?>(null) }
  var shouldReloadAd by remember { mutableStateOf(false) }

    // Observe premium status (decision agent also checks this, but good for early exit)
    val isPremium by premiumManager.isPremiumUser.collectAsStateWithLifecycle(false)

    if (BuildConfig.DEBUG) {
        Log.d(TAG, "GlobalBannerAdView composing | isPremium=$isPremium | route=$currentRoute | miniPlayer=$isMiniPlayerVisible")
    }

    // Don't show ads for premium users
    if (isPremium) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Skipping ads - premium user")
        return
    }

    // Calculate adaptive ad size based on screen width
    // Recalculate on configuration change (rotation)
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val adSize = remember(configuration.screenWidthDp, configuration.orientation) {
        val display = context.resources.displayMetrics
        val adWidthPixels = display.widthPixels.toFloat()
        val density = display.density
        val adWidth = (adWidthPixels / density).toInt()

        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    // Get actual ad height for layout calculations
    val adHeightDp = remember(adSize) {
        val height = adSize.getHeightInPixels(context) / context.resources.displayMetrics.density
        onAdHeightChanged(height) // Notify parent of height change
        height
    }

    var adView by remember { mutableStateOf<AdView?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var decision by remember { mutableStateOf<AdDecision?>(null as AdDecision?) }
    var loadAttempt by remember { mutableStateOf(0) }
    var lastFailureTime by remember { mutableStateOf(0L) }

    // Issue #4 Fix: Lifecycle-aware timer - only runs when app is active (RESUMED state)
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppActive by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppActive = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Lifecycle state changed: isActive=$isAppActive")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Timer-based refresh (60-90s) - pauses when app is backgrounded
    LaunchedEffect(isAppActive) {
        if (isAppActive) {
            while (true) {
                delay(70_000) // 70 seconds average (between 60-90s range)
                refreshTrigger++
                DevLogger.d(TAG, "Timer triggered ad refresh (trigger=$refreshTrigger)")
            }
        } else {
            DevLogger.d(TAG, "Timer paused - app in background")
        }
    }

    // Issue #6 Fix: Debounced decision evaluation to prevent rapid recomposition
    LaunchedEffect(refreshTrigger, isMiniPlayerVisible, currentRoute) {
         delay(16)
      val newDecision = decisionAgent.shouldShowBanner(isMiniPlayerVisible, currentRoute)
         decision = newDecision   // ⭐ REQUIRED
     if (newDecision.shouldShow != lastDecisionState?.shouldShow) {
          shouldReloadAd = !shouldReloadAd // Toggle only on actual change
          lastDecisionState = newDecision
          loadAttempt = 0 // Reset retries on new decision cycle
      }
    }

    // Retry backoff delay - waits before attempting next retry
    LaunchedEffect(lastFailureTime) {
        if (lastFailureTime > 0) {
            val backoffMs = (2000L * (1L shl (loadAttempt - 1))).coerceAtMost(8000L)
            delay(backoffMs)
            // loadAttempt change will trigger ad load retry via DisposableEffect
        }
    }

    // Consult decision agent and load ad if approved
    DisposableEffect(decision, shouldReloadAd, loadAttempt) {

 val currentDecision = decision ?: run {
        DevLogger.d(TAG, "Decision null, waiting...")
        return@DisposableEffect onDispose { }
    }

    if (!currentDecision.shouldShow) {
        DevLogger.d(TAG, "Banner suppressed: ${currentDecision.reason}")
        adView?.destroy()
        adView = null
        loadAttempt = 0 // Reset retry count
        return@DisposableEffect onDispose { }
    }

    DevLogger.d(TAG, "Banner approved: ${currentDecision.reason}")

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Loading ad | adUnitId=${adMobManager.getBannerAdId()} | attempt=${loadAttempt}")
        }

        val startTime = System.currentTimeMillis()
        val newAdView = AdView(context).apply {
            setAdSize(adSize)
            adUnitId = adMobManager.getBannerAdId()
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    val loadTimeMs = System.currentTimeMillis() - startTime
                    DevLogger.d(TAG, "Banner ad loaded in ${loadTimeMs}ms")
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Ad loaded successfully | loadTime=${loadTimeMs}ms")
                    }
                    decisionAgent.recordAdLoaded(AdFormat.BANNER, loadTimeMs)
                    decisionAgent.recordAdImpression(AdFormat.BANNER)
                    loadAttempt = 0 // Reset retry count on success
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    DevLogger.e(TAG, "Banner ad failed to load: ${error.message} (attempt ${loadAttempt + 1}/3)")
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Ad failed to load | code=${error.code} | msg=${error.message} | attempt=${loadAttempt + 1}")
                    }
                    decisionAgent.recordAdFailed(AdFormat.BANNER, error.code, error.message)

                    // Retry with exponential backoff (max 3 attempts)
                    if (loadAttempt < 2) {
                        val backoffMs = (2000L * (1L shl loadAttempt)).coerceAtMost(8000L)
                        lastFailureTime = System.currentTimeMillis()
                        loadAttempt++ // Trigger retry via state change
                        DevLogger.d(TAG, "Scheduling retry in ${backoffMs}ms (attempt ${loadAttempt}/3)")
                    } else {
                        DevLogger.d(TAG, "Max retries (3) reached, giving up on ad load")
                        loadAttempt = 0
                    }
                }

                override fun onAdClicked() {
                    DevLogger.d(TAG, "Banner ad clicked")
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Ad clicked by user")
                    }
                }

                override fun onAdOpened() {
                    DevLogger.d(TAG, "Banner ad opened (fullscreen)")
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Ad opened fullscreen overlay")
                    }
                }

                override fun onAdClosed() {
                    DevLogger.d(TAG, "Banner ad closed")
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Ad overlay closed, user returned to app")
                    }
                }

                override fun onAdImpression() {
                    DevLogger.d(TAG, "Banner ad impression recorded")
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Ad impression counted by AdMob")
                    }
                }
            }
        }

        adView = newAdView

        // Load ad
        val adRequest = AdRequest.Builder().build()
        newAdView.loadAd(adRequest)

        onDispose {
            // Destroy ad when component leaves composition
            newAdView.destroy()
            adView = null
        }
    }

    // Render ad container
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adHeightDp.dp)
    ) {
        adView?.let { view ->
            DevLogger.d(TAG, "Rendering AdView in UI")
            AndroidView(
                factory = { view },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(adHeightDp.dp)
            )
        } ?: run {
            // Skeleton loader while ad is loading
            DevLogger.d(TAG, "AdView is null, showing skeleton")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(adHeightDp.dp)
                    .adShimmer()
            )
        }
    }
}
