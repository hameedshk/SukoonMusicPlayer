package com.sukoon.music

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets as ViewInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.components.GlobalBannerAdView
import com.sukoon.music.ui.components.GLOBAL_AD_HEIGHT_DP
import com.sukoon.music.ui.components.MiniPlayer
import com.sukoon.music.ui.navigation.Routes
import com.sukoon.music.ui.navigation.SukoonNavHost
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var adMobManager: AdMobManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge content
        enableEdgeToEdge()

        // Enable full screen immersive mode
        setupFullScreenMode()

        setContent {
            // Observe user preferences for theme selection
            val userPreferences by preferencesManager.userPreferencesFlow.collectAsStateWithLifecycle(
                initialValue = com.sukoon.music.domain.model.UserPreferences()
            )

            // Determine dark theme based on user preference
            val darkTheme = when (userPreferences.theme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            SukoonMusicPlayerTheme(
                darkTheme = darkTheme,
                dynamicColor = false
            ) {
                val navController = rememberNavController()

                // Observe current route for ad reload trigger
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                // Get playback state for MiniPlayer visibility and padding
                val homeViewModel: HomeViewModel = hiltViewModel()
                val playbackState by homeViewModel.playbackState.collectAsStateWithLifecycle()

                // Calculate dynamic bottom padding based on playback state
                // Hide mini player when on NowPlayingScreen
                val isOnNowPlayingScreen = currentRoute == Routes.NowPlaying.route
                val bottomPadding = if (playbackState.currentSong != null && !isOnNowPlayingScreen) {
                    (GLOBAL_AD_HEIGHT_DP + 80).dp // Ad + MiniPlayer
                } else {
                    GLOBAL_AD_HEIGHT_DP.dp // Ad only
                }

                // Global layout: NavHost + MiniPlayer + Ad
                Box(modifier = Modifier.fillMaxSize()) {
                    // Main navigation content with bottom padding
                    SukoonNavHost(
                        navController = navController,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomPadding)
                    )

                    // MiniPlayer above ad (z-index 1) - only when playing and not on NowPlayingScreen
                    if (playbackState.currentSong != null && !isOnNowPlayingScreen) {
                        MiniPlayer(
                            playbackState = playbackState,
                            onPlayPauseClick = { homeViewModel.playPause() },
                            onNextClick = { homeViewModel.seekToNext() },
                            onClick = {
                                navController.navigate(Routes.NowPlaying.route)
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = GLOBAL_AD_HEIGHT_DP.dp) // Offset by ad height
                        )
                    }

                    // Global ad banner at absolute bottom (z-index 0)
                    GlobalBannerAdView(
                        adMobManager = adMobManager,
                        currentRoute = currentRoute,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars) // Respect gesture bar
                    )
                }
            }
        }
    }

    /**
     * Configure full screen immersive mode.
     * Hides system bars while allowing them to be revealed with a swipe.
     */
    private fun setupFullScreenMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            window.insetsController?.let { controller ->
                controller.hide(ViewInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 10 and below
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }
}
