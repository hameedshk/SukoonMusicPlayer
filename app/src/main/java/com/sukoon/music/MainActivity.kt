package com.sukoon.music

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets as ViewInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.data.premium.PremiumManager
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

    @Inject
    lateinit var premiumManager: PremiumManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge content
        enableEdgeToEdge()

        // Enable full screen immersive mode
        setupFullScreenMode()

        setContent {
            // Initialize billing manager for premium purchases
            LaunchedEffect(Unit) {
                premiumManager.initialize()
            }
            // Observe user preferences for theme selection
            // Use null as initialValue so we can detect when preferences are actually loaded
            val userPreferencesState by preferencesManager.userPreferencesFlow.collectAsStateWithLifecycle(
                initialValue = null
            )

            // Show splash screen until preferences are loaded from DataStore
            if (userPreferencesState == null) {
                SukoonMusicPlayerTheme(theme = AppTheme.SYSTEM, dynamicColor = false) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        // Splash screen while loading preferences
                    }
                }
                return@setContent
            }

            // Extract non-null preferences (guaranteed after null check)
            val userPreferences = userPreferencesState!!

            // Check actual Android permission status
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            val hasActualPermission = ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PermissionChecker.PERMISSION_GRANTED

            // Lock in start destination on first valid emission to prevent flicker
            // Once computed, this value persists even if preferences change during startup
            val startDestination = remember {
                if (userPreferences.hasCompletedOnboarding && hasActualPermission) {
                    Routes.Home.route
                } else {
                    Routes.Onboarding.route
                }
            }

            // Apply theme based on user preference (can update during app lifecycle)
            SukoonMusicPlayerTheme(
                theme = userPreferences.theme,
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
                Box(modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    // Main navigation content with bottom padding
                    SukoonNavHost(
                        navController = navController,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomPadding),
                        startDestination = startDestination,
                        userPreferences = userPreferences
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
                    // Hidden for premium users
                    GlobalBannerAdView(
                        adMobManager = adMobManager,
                        premiumManager = premiumManager,
                        currentRoute = currentRoute,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars) // Respect gesture bar
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Disconnect billing client on app destruction
        premiumManager.disconnect()
    }

    /**
     * Configure full screen immersive mode.
     * Hides navigation bar only; keeps status bar visible.
     */
    private fun setupFullScreenMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+) - Hide navigation bar only, keep status bar
            window.insetsController?.let { controller ->
                controller.hide(ViewInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 10 and below - Hide navigation bar only, keep status bar
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }
}
