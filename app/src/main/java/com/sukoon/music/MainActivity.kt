package com.sukoon.music

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.data.analytics.AnalyticsTracker
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.data.premium.PremiumManager
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.domain.model.UserPreferences
import com.sukoon.music.ui.components.MiniPlayer
import com.sukoon.music.ui.navigation.Routes
import com.sukoon.music.ui.navigation.SukoonNavHost
import com.sukoon.music.ui.screen.MinimumRequiredVersionScreen
import com.sukoon.music.ui.theme.MiniPlayerHeight
import com.sukoon.music.ui.theme.SpacingMedium
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.HomeViewModel
import com.sukoon.music.util.AppVersionProvider
import com.sukoon.music.util.DevLogger
import com.sukoon.music.util.RemoteConfigManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal fun shouldShowGlobalMiniPlayer(currentRoute: String?): Boolean {
    return currentRoute != Routes.NowPlaying.route &&
        currentRoute != Routes.FeedbackReport.route &&
        currentRoute != Routes.AudioEditor.route
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private var cachedUserPreferences: UserPreferences? = null
        private var hasCompletedVersionCheck = false
    }

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var premiumManager: PremiumManager

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    @Inject
    lateinit var appVersionProvider: AppVersionProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Remote Config asynchronously
        lifecycleScope.launch {
            remoteConfigManager.initialize()
        }

        // Set dark window background only on cold start to prevent white flash
        // Skip it on warm start/Activity recreation to prevent black flash during language change
        if (cachedUserPreferences == null) {
            window.setBackgroundDrawableResource(android.R.color.black)
            window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSurface))
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
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

        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        }

        setContent {
            // Initialize from companion cache to skip loading box on warm starts and Activity recreations
            var isVersionCheckComplete by remember { mutableStateOf(hasCompletedVersionCheck) }
            var isUpdateRequired by remember { mutableStateOf(false) }
            var requiredVersionCode by remember { mutableStateOf(1) }
            var requiredVersionMessage by remember { mutableStateOf("") }
            var isRetryingVersionCheck by remember { mutableStateOf(false) }
            val installedVersionCode = remember { appVersionProvider.getInstalledVersionCode() }

            LaunchedEffect(Unit) {
                premiumManager.initialize()
                remoteConfigManager.initialize()
                // Initialize AdMob with UMP GDPR/CCPA consent flow (Play Store compliant)
                adMobManager.initializeWithConsent(this@MainActivity)

                val minimumVersionConfig = remoteConfigManager.getMinimumRequiredVersionConfig()
                requiredVersionCode = minimumVersionConfig.minVersionCode
                requiredVersionMessage = minimumVersionConfig.message
                isUpdateRequired = remoteConfigManager.isUpdateRequired(installedVersionCode)
                isVersionCheckComplete = true
                hasCompletedVersionCheck = true

                if (isUpdateRequired) {
                    logMinVersionBlockShown(installedVersionCode, requiredVersionCode)
                }


                preferencesManager.incrementAppLaunchCount()

                if (preferencesManager.getFirstInstallTime() == 0L) {
                    preferencesManager.setFirstInstallTime(System.currentTimeMillis())
                }
            }

            // Detect if this is Activity recreation (warm start) vs cold start
            val isWarmStart = remember { cachedUserPreferences != null }

            // Initialize with cached value to prevent null on Activity recreation
            var userPreferencesState by remember { mutableStateOf(cachedUserPreferences) }

            // Single LaunchedEffect with conditional behavior to avoid Compose recomposition issues
            LaunchedEffect(Unit) {
                preferencesManager.userPreferencesFlow.collect { prefs ->
                    cachedUserPreferences = prefs
                    // Always propagate preference updates (e.g., theme/language changes) to Compose state.
                    userPreferencesState = prefs
                }
            }

            // Use the state directly - always has value on warm starts
            val effectivePreferences = userPreferencesState

            // Show loading box only on true cold start (first ever launch with no cache)
            // Skip it if we have any cached prefs (warm start / Activity recreation / language change)
            if (effectivePreferences == null && cachedUserPreferences == null) {
                SukoonMusicPlayerTheme(theme = AppTheme.SYSTEM, dynamicColor = false) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = Color(0xFF1E1E1E)),
                        contentAlignment = Alignment.Center
                    ) {}
                }
                return@setContent
            }

            // Always proceed to main UI if we have any preferences (current or cached)
            // This prevents any loading box flash on warm starts and Activity recreations
            val userPreferences = effectivePreferences ?: return@setContent

            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            val hasActualPermission = ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PermissionChecker.PERMISSION_GRANTED

            val startDestination = remember {
                if (userPreferences.hasCompletedOnboarding && hasActualPermission) {
                    Routes.Home.route
                } else {
                    Routes.Onboarding.route
                }
            }

            SukoonMusicPlayerTheme(
                theme = userPreferences.theme,
                accentProfile = userPreferences.accentProfile,
                dynamicColor = false
            ) {
                val isDarkTheme = when (userPreferences.theme) {
                    AppTheme.LIGHT -> false
                    AppTheme.DARK, AppTheme.AMOLED -> true
                    AppTheme.SYSTEM -> isSystemInDarkTheme()
                }
                val backgroundColor = MaterialTheme.colorScheme.background

                SideEffect {
                    val controller = WindowInsetsControllerCompat(window, window.decorView)
                    controller.isAppearanceLightStatusBars = !isDarkTheme

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        controller.isAppearanceLightNavigationBars = !isDarkTheme
                    }

                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }
                }

                val retryScope = rememberCoroutineScope()
                if (isUpdateRequired) {
                    MinimumRequiredVersionScreen(
                        message = requiredVersionMessage,
                        currentVersionCode = installedVersionCode,
                        requiredVersionCode = requiredVersionCode,
                        isRetrying = isRetryingVersionCheck,
                        onUpdateClick = {
                            analyticsTracker.logEvent(
                                "min_version_update_tapped",
                                mapOf(
                                    "current_version_code" to installedVersionCode,
                                    "required_version_code" to requiredVersionCode
                                )
                            )
                            openPlayStoreListing()
                        },
                        onRetryClick = {
                            if (isRetryingVersionCheck) return@MinimumRequiredVersionScreen
                            retryScope.launch {
                                isRetryingVersionCheck = true
                                analyticsTracker.logEvent(
                                    "min_version_retry_tapped",
                                    mapOf(
                                        "current_version_code" to installedVersionCode,
                                        "required_version_code" to requiredVersionCode
                                    )
                                )
                                try {
                                    remoteConfigManager.refreshVersionConfig(force = true)
                                    val minimumVersionConfig =
                                        remoteConfigManager.getMinimumRequiredVersionConfig()
                                    requiredVersionCode = minimumVersionConfig.minVersionCode
                                    requiredVersionMessage = minimumVersionConfig.message
                                    val stillBlocked =
                                        remoteConfigManager.isUpdateRequired(installedVersionCode)
                                    isUpdateRequired = stillBlocked

                                    if (stillBlocked) {
                                        logMinVersionBlockShown(
                                            currentVersionCode = installedVersionCode,
                                            requiredVersionCode = requiredVersionCode
                                        )
                                    } else {
                                        analyticsTracker.logEvent(
                                            "min_version_unblocked_after_retry",
                                            mapOf("current_version_code" to installedVersionCode)
                                        )
                                    }
                                } finally {
                                    isRetryingVersionCheck = false
                                }
                            }
                        }
                    )
                    return@SukoonMusicPlayerTheme
                }

                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                LaunchedEffect(currentRoute) {
                    currentRoute?.let { route ->
                        analyticsTracker.logScreenView(
                            screenName = route,
                            screenClass = "MainActivity"
                        )
                    }
                }

                val homeViewModel: HomeViewModel = hiltViewModel()
                val playbackState by homeViewModel.playbackState.collectAsStateWithLifecycle()
                val isPremium by premiumManager.isPremiumUser.collectAsStateWithLifecycle(false)

                // Save playback state when app goes to background
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val lifecycle = lifecycleOwner.lifecycle
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_PAUSE -> {
                                homeViewModel.playbackRepository.refreshPlaybackState()
                                retryScope.launch {
                                    homeViewModel.playbackRepository.savePlaybackState()
                                }
                            }
                            Lifecycle.Event.ON_DESTROY -> {
                                retryScope.launch {
                                    homeViewModel.playbackRepository.savePlaybackState()
                                }
                            }
                            else -> {}
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose {
                        lifecycle.removeObserver(observer)
                    }
                }

                LaunchedEffect(Unit) {
                    // Force reconnect on app start (re-attach listener if service restarted)
                    homeViewModel.playbackRepository.connect()
                }

                val latestRepository by rememberUpdatedState(homeViewModel.playbackRepository)
                val latestIsPlaying by rememberUpdatedState(playbackState.isPlaying)
                LaunchedEffect(lifecycleOwner) {
                    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        while (isActive) {
                            if (latestIsPlaying) {
                                latestRepository.refreshPlaybackState(forcePositionResync = true)
                            }
                            delay(750)
                        }
                    }
                }

                LaunchedEffect(isPremium) {
                    analyticsTracker.setUserProperty("is_premium", isPremium.toString())
                }

                val shouldShowMiniPlayer = shouldShowGlobalMiniPlayer(currentRoute)
                val bottomPadding = if (playbackState.currentSong != null && shouldShowMiniPlayer) {
                    (MiniPlayerHeight.value + SpacingMedium.value).dp
                } else {
                    0.dp
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    SukoonNavHost(
                        navController = navController,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomPadding),
                        startDestination = startDestination,
                        userPreferences = userPreferences
                    )

                    val isMiniPlayerVisible =
                        playbackState.currentSong != null && shouldShowMiniPlayer

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
                                .padding(bottom = SpacingMedium)
                        )
                    }
                }
            }
        }
    }

    private fun logMinVersionBlockShown(currentVersionCode: Int, requiredVersionCode: Int) {
        analyticsTracker.logEvent(
            "min_version_block_shown",
            mapOf(
                "current_version_code" to currentVersionCode,
                "required_version_code" to requiredVersionCode
            )
        )
    }

    private fun openPlayStoreListing() {
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        )

        try {
            startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            )
            runCatching { startActivity(webIntent) }
                .onFailure { error ->
                    DevLogger.e("MainActivity", "Failed to open Play Store listing", error)
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        premiumManager.disconnect()
    }
}
