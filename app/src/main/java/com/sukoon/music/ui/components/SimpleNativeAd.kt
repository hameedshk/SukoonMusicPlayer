package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.util.AdAnalytics
import com.sukoon.music.util.DevLogger
import com.sukoon.music.util.RemoteConfigManager

/**
 * Simple native ad component rendered entirely in Compose.
 *
 * Features:
 * - Premium users see no ad
 * - Renders native ad content: headline, body, call-to-action, media
 * - Material 3 Card styling with elevation
 * - Analytics logging for impressions and clicks
 * - Graceful failure handling
 * - Auto-cleanup on dispose
 *
 * @param adMobManager AdMob configuration manager
 * @param preferencesManager Preferences manager for premium check
 * @param placement Ad placement identifier for analytics (e.g., "albums", "artists")
 * @param modifier Optional modifier
 */
@Composable
fun SimpleNativeAd(
    adMobManager: AdMobManager,
    preferencesManager: PreferencesManager,
    remoteConfigManager: RemoteConfigManager,
    placement: String = "unknown",
    modifier: Modifier = Modifier
) {
    val isPremium by preferencesManager.isPremiumUserFlow().collectAsStateWithLifecycle(initialValue = false)
    val showAds by remoteConfigManager.albumArtAdsEnabled.collectAsStateWithLifecycle(initialValue = true)

    // Premium users or globally disabled ads don't see native ads
    if (isPremium || !showAds) {
        return
    }

    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    // Load native ad
    DisposableEffect(Unit) {
        val adLoader = AdLoader.Builder(context, adMobManager.getNativeAdId())
            .forNativeAd { ad ->
                nativeAd = ad
                AdAnalytics.logAdImpression("native", placement)
                DevLogger.d("SimpleNativeAd", "Native ad loaded for placement: $placement")
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    nativeAd = null
                    AdAnalytics.logAdFailed("native", error.code)
                    DevLogger.d("SimpleNativeAd", "Native ad failed to load: ${error.message} (code: ${error.code})")
                }
            })
            .build()

        val adRequest = AdRequest.Builder().build()
        adLoader.loadAd(adRequest)

        onDispose {
            nativeAd?.destroy()
        }
    }

    // Render native ad if loaded
    nativeAd?.let { ad ->
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(enabled = true) {
                    AdAnalytics.logAdClicked("native")
                    DevLogger.d("SimpleNativeAd", "Native ad clicked")
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Headline + Icon in row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon (if available)
                    ad.icon?.drawable?.let { drawable ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                                .padding(4.dp)
                        ) {
                            AsyncImage(
                                model = drawable,
                                contentDescription = "Ad icon",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    // Headline
                    ad.headline?.let { headline ->
                        Text(
                            text = headline,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = if (ad.icon != null) 8.dp else 0.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                    }
                }

                // Media content (if available)
                ad.mediaContent?.let { mediaContent ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
                    ) {
                        // Note: In a real app, you'd render the media content properly
                        // For now, we'll use a placeholder
                        AsyncImage(
                            model = ad.images?.firstOrNull()?.uri,
                            contentDescription = "Ad media",
                            modifier = Modifier
                                .fillMaxWidth()
                                .size(200.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Body text
                ad.body?.let { body ->
                    Text(
                        text = body,
                        modifier = Modifier.padding(vertical = 4.dp),
                        fontSize = 12.sp,
                        maxLines = 3
                    )
                }

                // Advertiser (if available)
                ad.advertiser?.let { advertiser ->
                    Text(
                        text = "Ad: $advertiser",
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .align(Alignment.End),
                        fontSize = 10.sp
                    )
                }

                // Call-to-Action button
                ad.callToAction?.let { cta ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                AdAnalytics.logAdClicked("native")
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cta,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
