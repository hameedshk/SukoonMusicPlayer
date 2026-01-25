package com.sukoon.music.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.theme.*

// Gradient colors
private val GradientTop = Color(0xFF111214)
private val GradientBottom = Color(0xFF0D0E11)

// Dark Surface Gradients (Level 1 - Passive tiles)
private val DarkSurfaceLevel1Top = Color(0xFF1D2230)
private val DarkSurfaceLevel1Bottom = Color(0xFF1A1D26)

// Dark Surface Gradients (Level 2 - Important passive content)
private val DarkSurfaceLevel2Top = Color(0xFF232B3C)
private val DarkSurfaceLevel2Bottom = Color(0xFF1F2738)

// Light Surface Gradients (Level 1 - Passive tiles)
private val LightSurfaceLevel1Top = Color(0xFFF5F5F5)
private val LightSurfaceLevel1Bottom = Color(0xFFEFEFEF)

// Light Surface Gradients (Level 2 - Important passive content)
private val LightSurfaceLevel2Top = Color(0xFFFAFAFA)
private val LightSurfaceLevel2Bottom = Color(0xFFF0F0F0)

// Dark theme: Midnight navy background with green accent
private val DarkColorScheme = darkColorScheme(
    // Accent: Spotify green only - used for play button, seek bar, emphasis
    primary = androidx.compose.ui.graphics.Color(0xFF1DB954),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF000000),

    // Material 3 required but not visually used: map to primary
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF1DB954),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF000000),
    secondary = androidx.compose.ui.graphics.Color(0xFF1DB954),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF000000),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF1DB954),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF000000),
    tertiary = androidx.compose.ui.graphics.Color(0xFF1DB954),
    onTertiary = androidx.compose.ui.graphics.Color(0xFF000000),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF1DB954),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF000000),

    // Surfaces: All map to Midnight navy
    background = androidx.compose.ui.graphics.Color(0xFF0F111A),
    onBackground = androidx.compose.ui.graphics.Color(0xFFB3B3B3),
    surface = androidx.compose.ui.graphics.Color(0xFF0F111A),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF1A1F2E),
    onSurface = androidx.compose.ui.graphics.Color(0xFFB3B3B3),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF7F7F7F),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF0F111A),

    // Error: Minimal (rarely used in music player)
    error = androidx.compose.ui.graphics.Color(0xFFCF6679),
    onError = androidx.compose.ui.graphics.Color(0xFF000000),
    errorContainer = androidx.compose.ui.graphics.Color(0xFF93000A),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),

    // Outline: Single value
    outline = androidx.compose.ui.graphics.Color(0xFF808080),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF808080)
)

// Light theme: Minimal light color scheme with green accent
private val LightColorScheme = lightColorScheme(
    // Accent: Spotify green
    primary = androidx.compose.ui.graphics.Color(0xFF1DB954),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),

    // Material 3 required: map to primary
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFB3E5B0),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF003300),
    secondary = androidx.compose.ui.graphics.Color(0xFF1DB954),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFB3E5B0),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF003300),
    tertiary = androidx.compose.ui.graphics.Color(0xFF1DB954),
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFB3E5B0),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF003300),

    // Surfaces: Light backgrounds
    background = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFE8E8E8),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF666666),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF5F5F5),

    // Error
    error = androidx.compose.ui.graphics.Color(0xFFBA1A1A),
    onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    errorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFF410002),

    // Outline
    outline = androidx.compose.ui.graphics.Color(0xFF999999),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFDDDDDD)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun Modifier.gradientBackground() = this.then(
    Modifier.background(
        brush = Brush.verticalGradient(
            colors = if (MaterialTheme.colorScheme.onBackground.red > 0.5f) {
                // Dark theme: onBackground is light text (#B3B3B3)
                listOf(GradientTop, GradientBottom)  // Dark: #111214 → #0D0E11
            } else {
                // Light theme: onBackground is dark text (#1C1B1F)
                listOf(Color(0xFFFFFBFE), Color(0xFFFFFFFF))  // Light: Light purple → white
            }
        )
    )
)

@Composable
fun Modifier.surfaceLevel1Gradient() = this.then(
    Modifier.background(
        brush = Brush.verticalGradient(
            colors = if (MaterialTheme.colorScheme.onBackground.red > 0.5f) {
                // Dark theme: onBackground is light text
                listOf(DarkSurfaceLevel1Top, DarkSurfaceLevel1Bottom)
            } else {
                // Light theme: onBackground is dark text
                listOf(LightSurfaceLevel1Top, LightSurfaceLevel1Bottom)
            }
        )
    )
)

@Composable
fun Modifier.surfaceLevel2Gradient() = this.then(
    Modifier.background(
        brush = Brush.verticalGradient(
            colors = if (MaterialTheme.colorScheme.onBackground.red > 0.5f) {
                // Dark theme: onBackground is light text
                listOf(DarkSurfaceLevel2Top, DarkSurfaceLevel2Bottom)
            } else {
                // Light theme: onBackground is dark text
                listOf(LightSurfaceLevel2Top, LightSurfaceLevel2Bottom)
            }
        )
    )
)

@Composable
fun surfaceLevel1Colors(): Pair<Color, Color> {
    // Check user's theme preference via MaterialTheme, not system theme
    return if (MaterialTheme.colorScheme.onBackground.red > 0.5f) {
        // Dark theme: onBackground is light text
        Pair(DarkSurfaceLevel1Top, DarkSurfaceLevel1Bottom)
    } else {
        // Light theme: onBackground is dark text
        Pair(LightSurfaceLevel1Top, LightSurfaceLevel1Bottom)
    }
}

@Composable
fun surfaceLevel2Colors(): Pair<Color, Color> {
    // Check user's theme preference via MaterialTheme, not system theme
    return if (MaterialTheme.colorScheme.onBackground.red > 0.5f) {
        // Dark theme: onBackground is light text
        Pair(DarkSurfaceLevel2Top, DarkSurfaceLevel2Bottom)
    } else {
        // Light theme: onBackground is dark text
        Pair(LightSurfaceLevel2Top, LightSurfaceLevel2Bottom)
    }
}

@Composable
fun SukoonMusicPlayerTheme(
    theme: AppTheme = AppTheme.SYSTEM,
    dynamicColor: Boolean = false,  // Kept for compatibility; dynamic colors disabled
    content: @Composable () -> Unit
) {
    // Determine which color scheme to use based on theme selection
    val colorScheme = when (theme) {
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.DARK -> DarkColorScheme
        AppTheme.AMOLED -> DarkColorScheme  // AMOLED falls back to Dark (no longer a separate scheme)
        AppTheme.SYSTEM -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
