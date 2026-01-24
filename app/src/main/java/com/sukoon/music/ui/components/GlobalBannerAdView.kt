package com.sukoon.music.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.data.premium.PremiumManager
import com.sukoon.music.ui.theme.*

/**
 * Approximate height of global ad banner for padding calculations.
 * Actual height may vary slightly based on device width.
 */
const val GLOBAL_AD_HEIGHT_DP = 60

/**
 * Global Banner Ad component that appears at the bottom of all screens.
 *
 * Key features:
 * - Reloads ad when navigation route changes (fresh ad per screen)
 * - Hides ads for premium users (respects isPremiumUser flow)
 * - Always visible (not affected by selection mode)
 * - Positioned below MiniPlayer in z-index
 * - Automatically handles ad lifecycle (load/destroy)
 *
 * @param adMobManager Injected AdMob configuration manager
 * @param premiumManager Injected premium subscription manager
 * @param currentRoute Current navigation route (used as reload trigger)
 * @param modifier Optional modifier for positioning
 */
@Composable
fun GlobalBannerAdView(
    adMobManager: AdMobManager,
    premiumManager: PremiumManager,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Observe premium status
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

    // Reload ad when route changes (key = currentRoute)
    DisposableEffect(currentRoute) {
        val newAdView = AdView(context).apply {
            setAdSize(adSize)
            adUnitId = adMobManager.getBannerAdId()
        }

        adView = newAdView

        // Load fresh ad
        val adRequest = AdRequest.Builder().build()
        newAdView.loadAd(adRequest)

        onDispose {
            // Destroy ad when route changes or component leaves composition
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
