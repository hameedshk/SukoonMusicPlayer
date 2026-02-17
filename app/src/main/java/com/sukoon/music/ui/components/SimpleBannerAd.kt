package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.util.AdAnalytics
import com.sukoon.music.util.DevLogger
import com.sukoon.music.util.RemoteConfigManager

/**
 * Simple banner ad component with premium check, adaptive sizing, and analytics.
 *
 * Features:
 * - Premium users see no ad
 * - Adaptive sizing based on screen width
 * - AdListener for impression, click, and failure tracking
 * - Graceful failure handling (no retry logic)
 * - Auto-cleanup on dispose
 */
@Composable
fun SimpleBannerAd(
    adMobManager: AdMobManager,
    preferencesManager: PreferencesManager,
    remoteConfigManager: RemoteConfigManager,
    modifier: Modifier = Modifier
) {
    val isPremium by preferencesManager.isPremiumUserFlow().collectAsStateWithLifecycle(initialValue = false)
    val showAds by remoteConfigManager.bannerAdsEnabled.collectAsStateWithLifecycle(initialValue = true)

    // Premium users or globally disabled ads don't see banner ads
    if (isPremium || !showAds) {
        return
    }

    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }
    var hadError by remember { mutableStateOf(false) }

    // Calculate adaptive banner size
    val adSize = remember {
        val display = context.resources.displayMetrics
        val adWidthPixels = display.widthPixels.toFloat()
        val density = display.density
        val adWidth = (adWidthPixels / density).toInt()

        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    // Create AdView with listeners
    val adView = remember {
        AdView(context).apply {
            setAdSize(adSize)
            this.adUnitId = adMobManager.getBannerAdId()

            // Set up ad listener for analytics and lifecycle
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    isAdLoaded = true
                    hadError = false
                    AdAnalytics.logAdImpression("banner", "home")
                    DevLogger.d("SimpleBannerAd", "Banner ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    isAdLoaded = false
                    hadError = true
                    AdAnalytics.logAdFailed("banner", error.code)
                    DevLogger.d("SimpleBannerAd", "Banner ad failed to load: ${error.message} (code: ${error.code})")
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    AdAnalytics.logAdClicked("banner")
                    DevLogger.d("SimpleBannerAd", "Banner ad clicked")
                }
            }
        }
    }

    // Load ad and cleanup on dispose
    DisposableEffect(Unit) {
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        onDispose {
            adView.destroy()
        }
    }

    // Show ad only if loaded and no error, or show placeholder
    if (isAdLoaded && !hadError) {
        AndroidView(
            factory = { adView },
            modifier = modifier
                .fillMaxWidth()
                .height(adSize.height.dp)
        )
    } else if (hadError) {
        // Hide on error (graceful degradation)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(0.dp)
        )
    } else {
        // Show shimmer skeleton while loading
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(adSize.height.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder while loading
        }
    }
}
