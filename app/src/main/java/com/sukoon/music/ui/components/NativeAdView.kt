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
import com.sukoon.music.data.ads.AdFormat
import com.sukoon.music.data.ads.AdMobDecisionAgent
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.util.DevLogger
import com.sukoon.music.ui.theme.*

/**
 * Native Ad Loader with Decision Agent Integration.
 *
 * Consults the AdMobDecisionAgent before loading ads, respecting:
 * - Premium user status
 * - Private session status
 * - User scroll engagement
 * - Album size requirements (≥10 tracks)
 * - Album frequency caps (1 per album per session)
 * - Failure tracking and cooldowns
 *
 * IMPORTANT:
 * - Returns early (renders nothing) if decision suppresses ad
 * - Loads ad asynchronously if approved
 * - Destroys ad when composable leaves composition
 * - NEVER interrupts audio playback
 *
 * @param adMobManager AdMob configuration manager
 * @param decisionAgent AdMob decision agent for intelligent ad delivery
 * @param albumId Unique identifier for the album (used for frequency capping)
 * @param albumTrackCount Number of tracks in the album (must be ≥10 to show ad)
 * @param hasUserScrolled True if user has scrolled in the list (engagement check)
 * @param modifier Optional modifier
 */
@Composable
fun NativeAdLoader(
    adMobManager: AdMobManager,
    decisionAgent: AdMobDecisionAgent,
    albumId: Long,
    albumTrackCount: Int,
    hasUserScrolled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val TAG = "NativeAdLoader"
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var decision by remember { mutableStateOf<com.sukoon.music.data.ads.AdDecision?>(null) }

    // Consult decision agent before loading (in coroutine context)
    LaunchedEffect(albumId, albumTrackCount, hasUserScrolled) {
        decision = try {
            decisionAgent.shouldShowNative(albumId, albumTrackCount, hasUserScrolled)
        } catch (e: Exception) {
            DevLogger.e(TAG, "Error evaluating native ad decision: ${e.message}")
            null
        }
    }

    // If decision is null or suppressed, don't render anything
    if (decision == null || !decision!!.shouldShow) {
        if (decision != null) {
            DevLogger.d(TAG, "Native ad suppressed: ${decision!!.reason}")
        }
        return
    }

    // Decision is to show - load ad
    DevLogger.d(TAG, "Native ad approved for album $albumId: ${decision!!.reason}")

    var nativeAdState by remember { mutableStateOf<NativeAd?>(null) }
    nativeAd = nativeAdState

    // Load native ad
    DisposableEffect(albumId) {
        val startTime = System.currentTimeMillis()
        val adLoader = AdLoader.Builder(context, adMobManager.getNativeAdId())
            .forNativeAd { ad ->
                nativeAdState = ad
                val loadTimeMs = System.currentTimeMillis() - startTime
                DevLogger.d(TAG, "Native ad loaded in ${loadTimeMs}ms for album $albumId")
                decisionAgent.recordAdLoaded(AdFormat.NATIVE, loadTimeMs)
                decisionAgent.recordAdImpression(AdFormat.NATIVE, albumId)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    DevLogger.e(TAG, "Native ad failed to load for album $albumId: ${error.message}")
                    decisionAgent.recordAdFailed(AdFormat.NATIVE, error.code, error.message)
                }

                override fun onAdLoaded() {
                    DevLogger.d(TAG, "Native ad loaded callback for album $albumId")
                }
            })
            .build()

        val adRequest = AdRequest.Builder().build()
        adLoader.loadAd(adRequest)

        onDispose {
            nativeAdState?.destroy()
        }
    }

    // Display native ad
    nativeAd?.let { ad ->
        AndroidView(
            factory = { ctx ->
                val inflater = LayoutInflater.from(ctx)
                val adView = inflater.inflate(
                    R.layout.native_ad_layout,
                    null,
                    false
                ) as NativeAdView

                populateNativeAdView(ad, adView)
                adView
            },
            modifier = modifier.fillMaxWidth()
        )
    }
}

/**
 * Legacy Native Ad composable component (deprecated).
 *
 * Use NativeAdLoader instead for decision agent integration.
 *
 * This is kept for backwards compatibility but should be phased out.
 * Displays a native ad that blends with the app's UI.
 *
 * IMPORTANT:
 * - Loads ad asynchronously
 * - Shows placeholder while loading
 * - Destroys ad when composable leaves composition
 * - NEVER interrupts audio playback
 *
 * @deprecated Use NativeAdLoader with decision agent instead
 */
@Deprecated("Use NativeAdLoader with decision agent integration")
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
