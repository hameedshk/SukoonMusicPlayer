package com.sukoon.music.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.domain.model.AccentProfile
import com.sukoon.music.domain.model.AccentTokens
import com.sukoon.music.ui.theme.*

// Gradient colors (LOCKED - not user-configurable)
private val GradientTop = Color(0xFF121212)
private val GradientBottom = Color(0xFF0A0A0A)

// Accent tokens CompositionLocal
internal val LocalAccentTokens = staticCompositionLocalOf {
    AccentTokens.fromProfile(AccentProfile.DEFAULT)
}

// Dark Surface Gradients (Level 1 - Passive tiles)
private val DarkSurfaceLevel1Top = Color(0xFF1E1E1E)
private val DarkSurfaceLevel1Bottom = Color(0xFF181818)

// Dark Surface Gradients (Level 2 - Important passive content)
private val DarkSurfaceLevel2Top = Color(0xFF282828)
private val DarkSurfaceLevel2Bottom = Color(0xFF222222)

// Light Surface Gradients (Level 1 - Passive tiles)
private val LightSurfaceLevel1Top = Color(0xFFF5F5F5)
private val LightSurfaceLevel1Bottom = Color(0xFFEFEFEF)

// Light Surface Gradients (Level 2 - Important passive content)
private val LightSurfaceLevel2Top = Color(0xFFFAFAFA)
private val LightSurfaceLevel2Bottom = Color(0xFFF0F0F0)

/**
 * Create a dark color scheme dynamically based on accent color.
 * All accent slots (primary, secondary, tertiary) use the same accent color.
 */
private fun getDarkColorScheme(accentColor: Color): androidx.compose.material3.ColorScheme {
    return darkColorScheme(
        // Accent: User-selected or default - used for play button, seek bar, emphasis
        primary = accentColor,
        onPrimary = androidx.compose.ui.graphics.Color(0xFF000000),

        // Material 3 required but not visually used: map to primary
        primaryContainer = accentColor,
        onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF000000),
        secondary = accentColor,
        onSecondary = androidx.compose.ui.graphics.Color(0xFF000000),
        secondaryContainer = accentColor,
        onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF000000),
        tertiary = accentColor,
        onTertiary = androidx.compose.ui.graphics.Color(0xFF000000),
        tertiaryContainer = accentColor,
        onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF000000),

        // Surfaces: Neutral dark grays with clear elevation
        background = androidx.compose.ui.graphics.Color(0xFF121212),
        onBackground = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
        surface = androidx.compose.ui.graphics.Color(0xFF121212),
        surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF282828),
        onSurface = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB3B3B3),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1E1E1E),

        // Error: Minimal (rarely used in music player)
        error = androidx.compose.ui.graphics.Color(0xFFCF6679),
        onError = androidx.compose.ui.graphics.Color(0xFF000000),
        errorContainer = androidx.compose.ui.graphics.Color(0xFF93000A),
        onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),

        // Outline: Single value
        outline = androidx.compose.ui.graphics.Color(0xFF808080),
        outlineVariant = androidx.compose.ui.graphics.Color(0xFF808080)
    )
}

/**
 * Create a light color scheme dynamically based on accent color.
 * All accent slots (primary, secondary, tertiary) use the same accent color.
 *
 * Design Reference: Apple Music light mode
 * - Background: Cool neutral gray (#F2F2F7) so white cards pop
 * - Text: Pure black for maximum readability
 */
private fun getLightColorScheme(accentColor: Color): androidx.compose.material3.ColorScheme {
    return lightColorScheme(
        // Accent: User-selected or default
        primary = accentColor,
        onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),

        // Material 3 required: map to primary
        primaryContainer = accentColor.copy(alpha = 0.2f),
        onPrimaryContainer = accentColor.copy(alpha = 0.8f),
        secondary = accentColor,
        onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        secondaryContainer = accentColor.copy(alpha = 0.2f),
        onSecondaryContainer = accentColor.copy(alpha = 0.8f),
        tertiary = accentColor,
        onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        tertiaryContainer = accentColor.copy(alpha = 0.2f),
        onTertiaryContainer = accentColor.copy(alpha = 0.8f),

        // Surfaces: Apple Music-style cool neutral backgrounds
        background = androidx.compose.ui.graphics.Color(0xFFF2F2F7),  // Apple's SystemGroupedBackground
        onBackground = androidx.compose.ui.graphics.Color(0xFF000000), // Pure black text
        surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),      // Pure white cards
        surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFE5E5EA), // Apple's SystemGray5
        onSurface = androidx.compose.ui.graphics.Color(0xFF000000),    // Pure black text
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF666666),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF2F2F7),

        // Error
        error = androidx.compose.ui.graphics.Color(0xFFBA1A1A),
        onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        errorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),
        onErrorContainer = androidx.compose.ui.graphics.Color(0xFF410002),

        // Outline
        outline = androidx.compose.ui.graphics.Color(0xFF8E8E93),      // Apple's SystemGray
        outlineVariant = androidx.compose.ui.graphics.Color(0xFFD1D1D6) // Apple's SystemGray4
    )
}

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
    accentProfile: AccentProfile = AccentProfile.DEFAULT,
    dynamicColor: Boolean = false,  // Kept for compatibility; dynamic colors disabled
    content: @Composable () -> Unit
) {
    // Get the accent color from the profile
    val accentColor = accentProfile.accentPrimary

    // Determine which color scheme to use based on theme selection
    // ColorScheme is now generated dynamically based on accentProfile
    val colorScheme = when (theme) {
        AppTheme.LIGHT -> getLightColorScheme(accentColor)
        AppTheme.DARK -> getDarkColorScheme(accentColor)
        AppTheme.AMOLED -> getDarkColorScheme(accentColor)  // AMOLED falls back to Dark
        AppTheme.SYSTEM -> if (isSystemInDarkTheme()) getDarkColorScheme(accentColor) else getLightColorScheme(accentColor)
    }

    // Resolve accent tokens from profile
    val accentTokens = AccentTokens.fromProfile(accentProfile)

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAccentTokens provides accentTokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}

/**
 * Get the current accent tokens from the theme.
 * Use this in any composable to access accent colors.
 */
@Composable
fun accent(): AccentTokens = LocalAccentTokens.current
