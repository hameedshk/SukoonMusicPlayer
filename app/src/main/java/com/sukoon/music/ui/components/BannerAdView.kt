package com.sukoon.music.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.sukoon.music.data.ads.AdMobManager

/**
 * Banner Ad composable component.
 *
 * Displays a standard AdMob banner ad at the bottom of screens.
 * Uses AndroidView to integrate native AdView with Jetpack Compose.
 *
 * IMPORTANT:
 * - Only use Banner ads (no Interstitials that pause music)
 * - Ad is destroyed when composable leaves composition
 * - Automatically handles ad loading and lifecycle
 */
@Composable
fun BannerAdView(
    adMobManager: AdMobManager,
    modifier: Modifier = Modifier,
    adSize: AdSize = AdSize.BANNER
) {
    val context = LocalContext.current

    val adView = remember {
        AdView(context).apply {
            setAdSize(adSize)
            adUnitId = adMobManager.getBannerAdId()
        }
    }

    // Load ad when composable enters composition
    DisposableEffect(Unit) {
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        onDispose {
            // Destroy ad when composable leaves composition
            adView.destroy()
        }
    }

    AndroidView(
        factory = { adView },
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Adaptive Banner Ad that adjusts size based on screen width.
 * Provides better user experience on different device sizes.
 */
@Composable
fun AdaptiveBannerAdView(
    adMobManager: AdMobManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val adSize = remember {
        val display = context.resources.displayMetrics
        val adWidthPixels = display.widthPixels.toFloat()
        val density = display.density
        val adWidth = (adWidthPixels / density).toInt()

        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    BannerAdView(
        adMobManager = adMobManager,
        adSize = adSize,
        modifier = modifier
    )
}
