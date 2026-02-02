package com.sukoon.music.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.sukoon.music.data.ads.AdDecision
import com.sukoon.music.data.ads.AdFormat
import com.sukoon.music.data.ads.AdMobDecisionAgent
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.data.premium.PremiumManager
import com.sukoon.music.ui.theme.*
import com.sukoon.music.util.DevLogger
import kotlinx.coroutines.delay

/**
 * Approximate height of global ad banner for padding calculations.
 * Actual height may vary slightly based on device width.
 */
const val GLOBAL_AD_HEIGHT_DP = 60

/**
 * Global Banner Ad component that appears at the bottom of all screens.
 *
 * Key features:
 * - Uses AdMobDecisionAgent to determine if ads should show
 * - Timer-based refresh (60-90s) instead of route-based
 * - Respects premium user status via decision agent
 * - Respects private session status via decision agent
 * - Hides if mini player overlaps
 * - Automatically handles ad lifecycle (load/destroy)
 * - Records load success/failure and impressions for analytics
 *
 * @param adMobManager Injected AdMob configuration manager
 * @param decisionAgent Injected AdMob decision agent for intelligent ad delivery
 * @param premiumManager Injected premium subscription manager (for premium check)
 * @param currentRoute Current navigation route (for decision logging)
 * @param isMiniPlayerVisible True if mini player is visible (overlaps banner)
 * @param modifier Optional modifier for positioning
 */
@Composable
fun GlobalBannerAdView(
    adMobManager: AdMobManager,
    decisionAgent: AdMobDecisionAgent,
    premiumManager: PremiumManager,
    currentRoute: String?,
    isMiniPlayerVisible: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val TAG = "GlobalBannerAdView"

    // Observe premium status (decision agent also checks this, but good for early exit)
    val isPremium by premiumManager.isPremiumUser.collectAsStateWithLifecycle(false)

    // Don't show ads for premium users
    if (isPremium) {
        return
    }

    // Calculate adaptive ad size based on screen width
    val adSize = remember {
        val display = context.resources.displayMetrics
        val adWidthPixels = display.widthPixels.toFloat()
        val density = display.density
        val adWidth = (adWidthPixels / density).toInt()

        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    // Get actual ad height for layout calculations
    val adHeightDp = remember(adSize) {
        adSize.getHeightInPixels(context) / context.resources.displayMetrics.density
    }

    var adView by remember { mutableStateOf<AdView?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var decision by remember { mutableStateOf<AdDecision?>(null as AdDecision?) }

    // Timer-based refresh (60-90s) instead of route-based
    LaunchedEffect(Unit) {
        while (true) {
            delay(70_000) // 70 seconds average (between 60-90s range)
            refreshTrigger++
        }
    }

    // Evaluate decision from agent in coroutine context
    LaunchedEffect(refreshTrigger, isMiniPlayerVisible, currentRoute) {
        decision = try {
            decisionAgent.shouldShowBanner(isMiniPlayerVisible, currentRoute)
        } catch (e: Exception) {
            DevLogger.e(TAG, "Error evaluating banner decision: ${e.message}")
            AdDecision(false, AdFormat.BANNER, "Error evaluating decision: ${e.message}")
        }
    }

    // Consult decision agent and load ad if approved
    DisposableEffect(decision) {
        val currentDecision = decision
        if (currentDecision == null) {
            return@DisposableEffect onDispose { }
        }

        // If decision is to suppress, clean up and return
        if (!currentDecision.shouldShow) {
            DevLogger.d(TAG, "Banner suppressed: ${currentDecision.reason}")
            adView?.destroy()
            adView = null
            return@DisposableEffect onDispose { }
        }

        // Decision is to show - load ad
        DevLogger.d(TAG, "Banner approved: ${currentDecision.reason}")

        val startTime = System.currentTimeMillis()
        val newAdView = AdView(context).apply {
            setAdSize(adSize)
            adUnitId = adMobManager.getBannerAdId()
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    val loadTimeMs = System.currentTimeMillis() - startTime
                    DevLogger.d(TAG, "Banner ad loaded in ${loadTimeMs}ms")
                    decisionAgent.recordAdLoaded(AdFormat.BANNER, loadTimeMs)
                    decisionAgent.recordAdImpression(AdFormat.BANNER)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    DevLogger.e(TAG, "Banner ad failed to load: ${error.message}")
                    decisionAgent.recordAdFailed(AdFormat.BANNER, error.code, error.message)
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

    // Render ad view with fixed height for consistent layout
    adView?.let { view ->
        AndroidView(
            factory = { view },
            modifier = modifier
                .fillMaxWidth()
                .height(adHeightDp.dp)
        )
    }
}
