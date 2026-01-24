package com.sukoon.music.ui.components

import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.sukoon.music.R
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.util.DevLogger
import com.sukoon.music.ui.theme.*

/**
 * Native Ad composable component.
 *
 * Displays a native ad that blends with the app's UI.
 * Native ads are less intrusive and provide better UX.
 *
 * IMPORTANT:
 * - Loads ad asynchronously
 * - Shows placeholder while loading
 * - Destroys ad when composable leaves composition
 * - NEVER interrupts audio playback
 */
@Composable
fun NativeAdCard(
    adMobManager: AdMobManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isAdLoading by remember { mutableStateOf(true) }
    var adLoadFailed by remember { mutableStateOf(false) }

    // Load native ad
    DisposableEffect(Unit) {
        val adLoader = AdLoader.Builder(context, adMobManager.getNativeAdId())
            .forNativeAd { ad ->
                nativeAd = ad
                isAdLoading = false
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    DevLogger.e("NativeAdCard", "Ad failed to load: ${error.message}")
                    isAdLoading = false
                    adLoadFailed = true
                }

                override fun onAdLoaded() {
                    DevLogger.d("NativeAdCard", "Native ad loaded successfully")
                }
            })
            .build()

        val adRequest = AdRequest.Builder().build()
        adLoader.loadAd(adRequest)

        onDispose {
            nativeAd?.destroy()
        }
    }

    // Display native ad or placeholder
    if (nativeAd != null) {
        AndroidView(
            factory = { ctx ->
                val inflater = LayoutInflater.from(ctx)
                val adView = inflater.inflate(
                    R.layout.native_ad_layout,
                    null,
                    false
                ) as NativeAdView

                populateNativeAdView(nativeAd!!, adView)
                adView
            },
            modifier = modifier.fillMaxWidth()
        )
    }
}

/**
 * Populate native ad view with ad content.
 * Maps ad assets to view components.
 */
private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    // Set headline
    adView.headlineView = adView.findViewById(R.id.ad_headline)
    nativeAd.headline?.let {
        (adView.headlineView as? android.widget.TextView)?.text = it
    }

    // Set body text
    adView.bodyView = adView.findViewById(R.id.ad_body)
    nativeAd.body?.let {
        (adView.bodyView as? android.widget.TextView)?.text = it
    }

    // Set call to action
    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
    nativeAd.callToAction?.let {
        (adView.callToActionView as? android.widget.Button)?.text = it
    }

    // Set icon
    adView.iconView = adView.findViewById(R.id.ad_icon)
    nativeAd.icon?.let { icon ->
        (adView.iconView as? android.widget.ImageView)?.setImageDrawable(icon.drawable)
    }

    // Set media content (images/videos)
    adView.mediaView = adView.findViewById(R.id.ad_media)
    adView.mediaView?.setMediaContent(nativeAd.mediaContent)

    // Set advertiser
    adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
    nativeAd.advertiser?.let {
        (adView.advertiserView as? android.widget.TextView)?.text = it
    }

    // Register the native ad view
    adView.setNativeAd(nativeAd)
}
