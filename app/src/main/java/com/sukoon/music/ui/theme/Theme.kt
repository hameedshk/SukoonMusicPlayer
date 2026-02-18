package com.sukoon.music.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode

// ============================================================================
// Golden Ratio & Baseline Grid (Swiss Minimalism)
// ============================================================================
const val BaselineGridDp = 4
const val CompactVerticalPadding = 10
const val StandardHorizontalPadding = 16

// Gradient colors (LOCKED - not user-configurable)
private val GradientTop = Color(0xFF121212)
private val GradientBottom = Color(0xFF0A0A0A)

// AMOLED Gradient colors (true black for OLED battery savings)
private val AmoledGradientTop = Color(0xFF000000)
private val AmoledGradientBottom = Color(0xFF000000)

// Accent tokens CompositionLocal
internal val LocalAccentTokens = staticCompositionLocalOf {
    AccentTokens.fromProfile(AccentProfile.DEFAULT)
}

// Theme mode CompositionLocal (to differentiate Dark vs AMOLED in gradient functions)
internal val LocalIsAmoled = staticCompositionLocalOf { false }

// Dark Surface Gradients (Level 1 - Passive tiles) - S style
private val DarkSurfaceLevel1Top = Color(0xFF1A1A1A)
private val DarkSurfaceLevel1Bottom = Color(0xFF161616)

// Dark Surface Gradients (Level 2 - Important passive content)
private val DarkSurfaceLevel2Top = Color(0xFF282828)
private val DarkSurfaceLevel2Bottom = Color(0xFF222222)

// AMOLED Surface Gradients (Level 1 - Minimal elevation for true black)
private val AmoledSurfaceLevel1Top = Color(0xFF0D0D0D)
private val AmoledSurfaceLevel1Bottom = Color(0xFF080808)

// AMOLED Surface Gradients (Level 2 - Subtle elevation)
private val AmoledSurfaceLevel2Top = Color(0xFF1A1A1A)
private val AmoledSurfaceLevel2Bottom = Color(0xFF141414)

// Light Surface Gradients (Level 1 - Passive tiles) - A Music style
private val LightSurfaceLevel1Top = Color(0xFFFFFFFF)
private val LightSurfaceLevel1Bottom = Color(0xFFF8F8F8)

// Light Surface Gradients (Level 2 - Important passive content) - A Music style
private val LightSurfaceLevel2Top = Color(0xFFFFFFFF)
private val LightSurfaceLevel2Bottom = Color(0xFFF2F2F7)

/**
 * Create an AMOLED color scheme for true black backgrounds.
 * Optimized for OLED displays where black pixels are completely off.
 *
 * Design Reference: PowerAmp, BlackPlayer AMOLED modes
 * - Background: True black (#000000) for maximum battery savings
 * - Surfaces: Near-black with minimal elevation
 * - Text: Pure white for contrast
 */
private fun getAmoledColorScheme(accentColor: Color): androidx.compose.material3.ColorScheme {
    return darkColorScheme(
        primary = accentColor,
        onPrimary = Color(0xFF000000),

        primaryContainer = accentColor,
        onPrimaryContainer = Color(0xFF000000),
        secondary = accentColor,
        onSecondary = Color(0xFF000000),
        secondaryContainer = accentColor,
        onSecondaryContainer = Color(0xFF000000),
        tertiary = accentColor,
        onTertiary = Color(0xFF000000),
        tertiaryContainer = accentColor,
        onTertiaryContainer = Color(0xFF000000),

        // True black surfaces for OLED
        background = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF000000),
        surfaceContainerHigh = Color(0xFF1A1A1A),
        onSurface = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFFB3B3B3),
        surfaceVariant = Color(0xFF0D0D0D),

        error = Color(0xFFCF6679),
        onError = Color(0xFF000000),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),

        outline = Color(0xFF404040),
        outlineVariant = Color(0xFF2A2A2A)
    )
}

/**
 * Create a dark color scheme dynamically based on accent color.
 * All accent slots (primary, secondary, tertiary) use the same accent color.
 *
 * Design Reference: S dark mode
 * - Background: Deep charcoal (#121212) for depth without true black
 * - Primary text: Pure white (#FFFFFF) for maximum contrast
 * - Secondary text: S's exact gray (#B3B3B3)
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

        // Surfaces: S-style neutral dark grays
        background = androidx.compose.ui.graphics.Color(0xFF121212),  // S's exact background
        onBackground = androidx.compose.ui.graphics.Color(0xFFFFFFFF), // Pure white (was #E0E0E0)
        surface = androidx.compose.ui.graphics.Color(0xFF121212),
        surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF282828),
        onSurface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),    // Pure white (was #E0E0E0)
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB3B3B3), // S's secondary text
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1A1A1A),   // Slightly darker (was #1E1E1E)

        // Error: Minimal (rarely used in music player)
        error = androidx.compose.ui.graphics.Color(0xFFCF6679),
        onError = androidx.compose.ui.graphics.Color(0xFF000000),
        errorContainer = androidx.compose.ui.graphics.Color(0xFF93000A),
        onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),

        // Outline: Subtle separation
        outline = androidx.compose.ui.graphics.Color(0xFF535353),         // S's divider color
        outlineVariant = androidx.compose.ui.graphics.Color(0xFF404040)
    )
}

/**
 * Create a light color scheme dynamically based on accent color.
 * All accent slots (primary, secondary, tertiary) use the same accent color.
 *
 * Design Reference: A's Music light mode
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

        // Surfaces: A Music-style cool neutral backgrounds
        background = androidx.compose.ui.graphics.Color(0xFFF2F2F7),  // A's SystemGroupedBackground
        onBackground = androidx.compose.ui.graphics.Color(0xFF000000), // Pure black text
        surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),      // Pure white cards
        surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFE5E5EA), // A's SystemGray5
        onSurface = androidx.compose.ui.graphics.Color(0xFF000000),    // Pure black text
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF666666),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF2F2F7),

        // Error
        error = androidx.compose.ui.graphics.Color(0xFFBA1A1A),
        onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        errorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),
        onErrorContainer = androidx.compose.ui.graphics.Color(0xFF410002),

        // Outline
        outline = androidx.compose.ui.graphics.Color(0xFF8E8E93),      // A's SystemGray
        outlineVariant = androidx.compose.ui.graphics.Color(0xFFD1D1D6) // A's SystemGray4
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
fun Modifier.gradientBackground(): Modifier {
    val isAmoled = LocalIsAmoled.current
    val isDarkTheme = MaterialTheme.colorScheme.onBackground.red > 0.5f

    return if (isDarkTheme) {
        // Dark/AMOLED: use gradient
        this.background(
            brush = Brush.verticalGradient(
                colors = if (isAmoled) {
                    listOf(AmoledGradientTop, AmoledGradientBottom)
                } else {
                    listOf(GradientTop, GradientBottom)
                }
            )
        )
    } else {
        // Light mode: flat color (A Music style - no gradient)
        this.background(color = Color(0xFFF2F2F7))
    }
}

@Composable
fun Modifier.surfaceLevel1Gradient(): Modifier {
    val isAmoled = LocalIsAmoled.current
    val isDarkTheme = MaterialTheme.colorScheme.onBackground.red > 0.5f

    return if (isDarkTheme) {
        this.background(
            brush = Brush.verticalGradient(
                colors = if (isAmoled) {
                    listOf(AmoledSurfaceLevel1Top, AmoledSurfaceLevel1Bottom)
                } else {
                    listOf(DarkSurfaceLevel1Top, DarkSurfaceLevel1Bottom)
                }
            )
        )
    } else {
        // Light mode: flat white for cards (stands out against #F2F2F7 background)
        this.background(color = Color(0xFFFFFFFF))
    }
}

@Composable
fun Modifier.surfaceLevel2Gradient(): Modifier {
    val isAmoled = LocalIsAmoled.current
    val isDarkTheme = MaterialTheme.colorScheme.onBackground.red > 0.5f

    return if (isDarkTheme) {
        this.background(
            brush = Brush.verticalGradient(
                colors = if (isAmoled) {
                    listOf(AmoledSurfaceLevel2Top, AmoledSurfaceLevel2Bottom)
                } else {
                    listOf(DarkSurfaceLevel2Top, DarkSurfaceLevel2Bottom)
                }
            )
        )
    } else {
        // Light mode: flat white for elevated content
        this.background(color = Color(0xFFFFFFFF))
    }
}

@Composable
fun surfaceLevel1Colors(): Pair<Color, Color> {
    val isAmoled = LocalIsAmoled.current
    val isDarkTheme = MaterialTheme.colorScheme.onBackground.red > 0.5f

    return when {
        isAmoled -> Pair(AmoledSurfaceLevel1Top, AmoledSurfaceLevel1Bottom)
        isDarkTheme -> Pair(DarkSurfaceLevel1Top, DarkSurfaceLevel1Bottom)
        else -> Pair(Color(0xFFFFFFFF), Color(0xFFFFFFFF)) // Light: flat white
    }
}

@Composable
fun surfaceLevel2Colors(): Pair<Color, Color> {
    val isAmoled = LocalIsAmoled.current
    val isDarkTheme = MaterialTheme.colorScheme.onBackground.red > 0.5f

    return when {
        isAmoled -> Pair(AmoledSurfaceLevel2Top, AmoledSurfaceLevel2Bottom)
        isDarkTheme -> Pair(DarkSurfaceLevel2Top, DarkSurfaceLevel2Bottom)
        else -> Pair(Color(0xFFFFFFFF), Color(0xFFFFFFFF)) // Light: flat white
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
        AppTheme.AMOLED -> getAmoledColorScheme(accentColor)
        AppTheme.SYSTEM -> if (isSystemInDarkTheme()) getDarkColorScheme(accentColor) else getLightColorScheme(accentColor)
    }

    // Resolve accent tokens from profile
    val accentTokens = AccentTokens.fromProfile(accentProfile)

    // Track if AMOLED mode is active (for gradient functions)
    val isAmoled = theme == AppTheme.AMOLED

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAccentTokens provides accentTokens,
        LocalIsAmoled provides isAmoled
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

/**
 * Audit Fix #5: Switch padding modifier for precise 6.5dp alignment.
 * Ensures Switch has exactly MinimumTouchSpacing (6.5dp) padding for proper ripple feedback.
 */
fun Modifier.switchPadding(): Modifier {
    return this.padding(start = 6.5.dp)
}

// ============================================================================
// Baseline Grid & Typography Modifiers (Swiss Minimalism)
// ============================================================================

/**
 * Apply 4dp baseline grid padding for text descender alignment.
 * Ensures all text sits on a consistent baseline grid, preventing visual misalignment.
 */
fun Modifier.baselineGridPadding(topDp: Int = 4, bottomDp: Int = 4): Modifier {
    return this.then(
        Modifier.padding(top = topDp.dp, bottom = bottomDp.dp)
    )
}

/**
 * Fade-to-transparent truncation for long titles.
 * Masks text overflow with a gradient fade instead of hard ellipsis.
 * Used for playlist names and other long text that shouldn't hard-truncate.
 */
fun Modifier.fadeEllipsis(): Modifier {
    return this.drawWithContent {
        val fadeStartX = size.width * 0.75f
        val brush = Brush.horizontalGradient(
            colors = listOf(Color.Black, Color.Black, Color.Transparent),
            startX = fadeStartX,
            endX = size.width + 4.dp.toPx()
        )
        drawContent()
        drawRect(
            brush = brush,
            blendMode = BlendMode.DstIn
        )
    }
}

// ============================================================================
// Color Utility Functions for Album-Centric Theming
// ============================================================================

/**
 * Desaturate a color by a given amount.
 * Converts to HSV, reduces saturation, and converts back to Color.
 *
 * @param desaturationAmount Desaturation amount (0f = fully saturated, 1f = grayscale)
 * @return Color with adjusted saturation, preserving hue and value
 */
fun Color.desaturateForTheme(desaturationAmount: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (this.red * 255).toInt(),
        (this.green * 255).toInt(),
        (this.blue * 255).toInt(),
        hsv
    )

    // Reduce saturation by desaturationAmount (clamp to [0f, 1f])
    hsv[1] = (hsv[1] * (1f - desaturationAmount.coerceIn(0f, 1f)))

    val desaturatedColor = android.graphics.Color.HSVToColor(hsv)
    return Color(desaturatedColor)
}

/**
 * Cap saturation of a color at a maximum value.
 * Converts to HSV, caps saturation, and converts back to Color.
 *
 * @param maxSaturation Maximum saturation level (1f = 100%, 0.7f = 70%)
 * @return Color with capped saturation
 */
fun Color.capSaturation(maxSaturation: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (this.red * 255).toInt(),
        (this.green * 255).toInt(),
        (this.blue * 255).toInt(),
        hsv
    )

    // Cap saturation at maxSaturation (clamp to [0f, 1f])
    hsv[1] = hsv[1].coerceAtMost(maxSaturation.coerceIn(0f, 1f))

    val cappedColor = android.graphics.Color.HSVToColor(hsv)
    return Color(cappedColor)
}
