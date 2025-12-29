package com.sukoon.music

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.navigation.SukoonNavHost
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

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

            SukoonMusicPlayerTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                SukoonNavHost(navController = navController)
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
                controller.hide(WindowInsets.Type.systemBars())
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
