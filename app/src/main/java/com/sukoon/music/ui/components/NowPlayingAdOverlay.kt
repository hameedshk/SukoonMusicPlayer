package com.sukoon.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.util.AdAnalytics
import com.sukoon.music.util.DevLogger
import com.sukoon.music.util.RemoteConfigManager
import kotlinx.coroutines.delay

/**
 * Now Playing overlay ad component with smart timing logic.
 *
 * Shows native ads after user has:
 * - Listened actively for 12+ minutes OR
 * - Played 6+ songs (with 30s minimum per song)
 *
 * Features:
 * - Premium users see no ad
 * - Pauses timer when music is paused or app backgrounded
 * - Requires 30s minimum listening before counting song
 * - Auto-dismisses after 10 seconds with fade animation
 * - Manual dismiss via X button
 * - Lifecycle-aware (pauses on background, resumes on foreground)
 * - Analytics logging for dismissals
 *
 * @param adMobManager AdMob configuration manager
 * @param preferencesManager Preferences manager for premium check
 * @param playbackState Current playback state from MediaController
 * @param modifier Optional modifier
 */
@Composable
fun NowPlayingAdOverlay(
    adMobManager: AdMobManager,
    preferencesManager: PreferencesManager,
    remoteConfigManager: RemoteConfigManager,
    playbackState: PlaybackState,
    modifier: Modifier = Modifier
) {
    val isPremium by preferencesManager.isPremiumUserFlow().collectAsStateWithLifecycle(initialValue = false)
    val showAds by remoteConfigManager.nowPlayingAdsEnabled.collectAsStateWithLifecycle(initialValue = true)

    // Premium users or globally disabled ads don't see now playing overlay ads
    if (isPremium || !showAds) {
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Timing state
    var activeListeningTimeMs by remember { mutableLongStateOf(0L) }
    var songsPlayedCount by remember { mutableStateOf(0) }
    var lastSongMediaId by remember { mutableStateOf<String?>(null) }
    var timeSinceLastSongChangeMs by remember { mutableLongStateOf(0L) }

    // Lifecycle state
    var isAppActive by remember { mutableStateOf(true) }

    // Ad state
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var showAd by remember { mutableStateOf(false) }

    // Lifecycle tracking - pause/resume timer based on app state
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppActive = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
            DevLogger.d("NowPlayingAdOverlay", "App lifecycle: ${event.targetState} (active: $isAppActive)")
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Track active listening time (only when playing AND app is active)
    LaunchedEffect(playbackState.isPlaying, isAppActive) {
        while (playbackState.isPlaying && isAppActive) {
            delay(1000) // Update every second
            activeListeningTimeMs += 1000
            timeSinceLastSongChangeMs += 1000

            DevLogger.v("NowPlayingAdOverlay", "Listening: ${activeListeningTimeMs / 1000}s, Songs: $songsPlayedCount")

            // Check if threshold reached to show ad
            if (!showAd && (activeListeningTimeMs >= 720_000 || songsPlayedCount >= 6)) {
                showAd = true
                DevLogger.d("NowPlayingAdOverlay", "Ad trigger threshold reached (listening: ${activeListeningTimeMs / 1000}s, songs: $songsPlayedCount)")
            }
        }
    }

    // Load native ad when showAd becomes true
    LaunchedEffect(showAd) {
        if (showAd && nativeAd == null) {
            val adLoader = AdLoader.Builder(context, adMobManager.getNativeAdId())
                .forNativeAd { ad ->
                    nativeAd = ad
                    AdAnalytics.logAdImpression("now_playing", "now_playing")
                    DevLogger.d("NowPlayingAdOverlay", "Native ad loaded for now playing")
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        AdAnalytics.logAdFailed("now_playing", error.code)
                        DevLogger.d("NowPlayingAdOverlay", "Native ad failed to load: ${error.message}")
                        showAd = false
                    }
                })
                .build()

            val adRequest = AdRequest.Builder().build()
            adLoader.loadAd(adRequest)
        }
    }

    // Track song changes (increment counter if 30s minimum listening time met)
    LaunchedEffect(playbackState.currentSong?.id) {
        val currentSongId = playbackState.currentSong?.id?.toString()

        if (currentSongId != null && currentSongId != lastSongMediaId) {
            // Song changed
            if (lastSongMediaId != null && timeSinceLastSongChangeMs >= 30_000) {
                // Previous song met minimum 30s listening time
                songsPlayedCount++
                DevLogger.d("NowPlayingAdOverlay", "Song counted (time: ${timeSinceLastSongChangeMs / 1000}s, total: $songsPlayedCount)")
            }
            lastSongMediaId = currentSongId
            timeSinceLastSongChangeMs = 0L
        }
    }

    // Auto-dismiss timer
    LaunchedEffect(showAd) {
        if (showAd && nativeAd != null) {
            delay(10_000) // 10 seconds
            showAd = false
            nativeAd?.destroy()
            nativeAd = null
            AdAnalytics.logNowPlayingAdDismissed(autoDismiss = true)
            // Reset timers
            activeListeningTimeMs = 0L
            songsPlayedCount = 0
            DevLogger.d("NowPlayingAdOverlay", "Ad auto-dismissed after 10s")
        }
    }

    // Render overlay
    AnimatedVisibility(
        visible = showAd && nativeAd != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Semi-transparent scrim background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = false) { } // Block clicks through
            )

            // Native ad card at bottom third
            if (nativeAd != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp
                    )
                ) {
                    Box {
                        // Ad content column
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            nativeAd!!.headline?.let { headline ->
                                Text(
                                    text = headline,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                            }

                            nativeAd!!.body?.let { body ->
                                Text(
                                    text = body,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp),
                                    maxLines = 2
                                )
                            }

                            nativeAd!!.callToAction?.let { cta ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                        )
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cta,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Close button
                        IconButton(
                            onClick = {
                                showAd = false
                                nativeAd?.destroy()
                                nativeAd = null
                                AdAnalytics.logNowPlayingAdDismissed(autoDismiss = false)
                                // Reset timers
                                activeListeningTimeMs = 0L
                                songsPlayedCount = 0
                                DevLogger.d("NowPlayingAdOverlay", "Ad manually dismissed")
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss ad",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}