package com.sukoon.music

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sukoon.music.data.ads.AdMobDecisionAgent
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.data.premium.PremiumManager
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.components.GlobalBannerAdView
import com.sukoon.music.ui.components.AD_CONTAINER_HEIGHT_DP
import com.sukoon.music.ui.components.MiniPlayer
import com.sukoon.music.ui.theme.MiniPlayerHeight
import com.sukoon.music.ui.theme.SpacingMedium
import com.sukoon.music.ui.navigation.Routes
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    lateinit var adMobDecisionAgent: AdMobDecisionAgent

    @Inject
    lateinit var premiumManager: PremiumManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        // Enable edge-to-edge content with fully transparent system bars
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // Disable navigation bar contrast enforcement on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            val controller = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
            controller.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
        }

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
                accentProfile = userPreferences.accentProfile,
                dynamicColor = false
            ) {
                // Configure system bar appearance based on theme
                val isDarkTheme = when (userPreferences.theme) {
                    AppTheme.LIGHT -> false
                    AppTheme.DARK, AppTheme.AMOLED -> true
                    AppTheme.SYSTEM -> isSystemInDarkTheme()
                }

                // Get background color from theme (must be done in Composable context)
                val backgroundColor = MaterialTheme.colorScheme.background

                SideEffect {
                    // Set status bar color to match theme background
                    window.statusBarColor = backgroundColor.toArgb()

                    val controller = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)

                    // Light/dark icons based on theme
                    controller.isAppearanceLightStatusBars = !isDarkTheme

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        controller.isAppearanceLightNavigationBars = !isDarkTheme
    }

    // THIS LINE removes the reserved nav-bar rectangle
    controller.systemBarsBehavior =
        androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    // Disable Android grey scrim
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
                }

                val navController = rememberNavController()

                // Observe current route for ad reload trigger
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                // Get playback state for MiniPlayer visibility and padding
                val homeViewModel: HomeViewModel = hiltViewModel()
                val playbackState by homeViewModel.playbackState.collectAsStateWithLifecycle()

                // Collect premium status for conditional ad rendering
                val isPremium by premiumManager.isPremiumUser.collectAsStateWithLifecycle(false)

                // Track actual ad height (updated dynamically from GlobalBannerAdView)
                var actualAdHeight by remember(isPremium) {
                    mutableStateOf(if (isPremium) 0f else AD_CONTAINER_HEIGHT_DP.toFloat())
                }

                // Update ad height reactively when premium status changes
                LaunchedEffect(isPremium) {
                    actualAdHeight = if (isPremium) 0f else AD_CONTAINER_HEIGHT_DP.toFloat()
                }

                // Calculate dynamic bottom padding based on playback state
                // Hide mini player when on NowPlayingScreen
                val isOnNowPlayingScreen = currentRoute == Routes.NowPlaying.route
                val bottomPadding = if (playbackState.currentSong != null && !isOnNowPlayingScreen) {
                    // Ad + MiniPlayer + spacing between them
                    (actualAdHeight + MiniPlayerHeight.value + SpacingMedium.value).dp
                } else {
                    // Ad only
                    actualAdHeight.dp
                }

                // Global layout: NavHost + MiniPlayer + Ad
                // No system bar padding - content draws edge-to-edge behind both status and nav bars
                Box(modifier = Modifier.fillMaxSize()) {
                    // Main navigation content with bottom padding
                    SukoonNavHost(
                        navController = navController,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomPadding),
                        startDestination = startDestination,
                        userPreferences = userPreferences
                    )

                    // MiniPlayer + Ad overlay positioning
                    val isMiniPlayerVisible = playbackState.currentSong != null && !isOnNowPlayingScreen

                    // MiniPlayer positioned above ad
                    if (isMiniPlayerVisible) {
                        MiniPlayer(
                            playbackState = playbackState,
                            onPlayPauseClick = { homeViewModel.playPause() },
                            onNextClick = { homeViewModel.seekToNext() },
                            onSeek = { positionMs -> homeViewModel.seekTo(positionMs) },
                            userPreferences = userPreferences,
                            onClick = { navController.navigate(Routes.NowPlaying.route) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = actualAdHeight.dp + SpacingMedium)
                        )
                    }

                    // Ad banner at bottom (only for non-premium users)
                    if (!isPremium) {
                        GlobalBannerAdView(
                            adMobManager = adMobManager,
                            decisionAgent = adMobDecisionAgent,
                            premiumManager = premiumManager,
                            currentRoute = currentRoute,
                            isMiniPlayerVisible = isMiniPlayerVisible,
                            onAdHeightChanged = { height -> actualAdHeight = height },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Notify ad decision agent that app is going to background
        adMobDecisionAgent.onAppBackgrounded()
    }

    override fun onResume() {
        super.onResume()
        // Notify ad decision agent that app is returning to foreground
        adMobDecisionAgent.onAppForegrounded()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Disconnect billing client on app destruction
        premiumManager.disconnect()
    }

}
