package com.sukoon.music.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.theme.*

private val MidnightColorScheme = darkColorScheme(
    // Brand colors - S green
    primary = androidx.compose.ui.graphics.Color(0xFF1DB954),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF1AA34A),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF000000),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFFFFFFF),

    // Secondary colors - muted green
    secondary = androidx.compose.ui.graphics.Color(0xFF1ED760),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF1AA34A),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF000000),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFFFFF),

    // Tertiary colors - subtle grey-green
    tertiary = androidx.compose.ui.graphics.Color(0xFF535353),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF404040),
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFB3B3B3),

    // Background and surfaces - Midnight deep navy (reduced eye strain, better OLED battery, premium feel)
    background = androidx.compose.ui.graphics.Color(0xFF0F111A),
    onBackground = androidx.compose.ui.graphics.Color(0xFFB3B3B3),

    surface = androidx.compose.ui.graphics.Color(0xFF0F111A),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF0F111A),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFF131722),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF1A1F2E),
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF1A1F2E),
    onSurface = androidx.compose.ui.graphics.Color(0xFFB3B3B3),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF7F7F7F),

    // Error colors
    error = androidx.compose.ui.graphics.Color(0xFFCF6679),
    errorContainer = androidx.compose.ui.graphics.Color(0xFF93000A),
    onError = androidx.compose.ui.graphics.Color(0xFF000000),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),

    // Outline
    outline = androidx.compose.ui.graphics.Color(0xFF808080),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF404040)
)

private val AmoledColorScheme = darkColorScheme(
    // Brand colors - S green (brightness boosted for OLED pure black)
    primary = androidx.compose.ui.graphics.Color(0xFF1DB954),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF1AA34A),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF000000),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFFFFFFF),

    // Secondary colors - bright green on pure black
    secondary = androidx.compose.ui.graphics.Color(0xFF1ED760),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF1AA34A),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF000000),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFFFFF),

    // Tertiary colors - slightly brighter for contrast on pure black
    tertiary = androidx.compose.ui.graphics.Color(0xFF707070),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF505050),
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFDDDDDD),

    // Background and surfaces - Pure AMOLED black (#000000) for maximum battery on OLED devices
    background = androidx.compose.ui.graphics.Color(0xFF000000),
    onBackground = androidx.compose.ui.graphics.Color(0xFFC0C0C0),

    surface = androidx.compose.ui.graphics.Color(0xFF000000),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF000000),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFF0A0A0A),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF121212),
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF121212),
    onSurface = androidx.compose.ui.graphics.Color(0xFFC0C0C0),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF909090),

    // Error colors
    error = androidx.compose.ui.graphics.Color(0xFFCF6679),
    errorContainer = androidx.compose.ui.graphics.Color(0xFF93000A),
    onError = androidx.compose.ui.graphics.Color(0xFF000000),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),

    // Outline
    outline = androidx.compose.ui.graphics.Color(0xFF909090),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF505050)
)

private val LightColorScheme = lightColorScheme(
    // Brand colors - darker orange for light theme (better contrast)
    primary = androidx.compose.ui.graphics.Color(0xFFD84315),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFFFCCBC),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF3E2723),

    // Secondary colors - darker amber
    secondary = androidx.compose.ui.graphics.Color(0xFFF57C00),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE0B2),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF3E2723),

    // Tertiary colors
    tertiary = androidx.compose.ui.graphics.Color(0xFFEF6C00),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE0B2),
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF3E2723),

    // Background and surfaces
    background = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1B1F),

    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFE8E8E8),
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF666666),

    // Error colors
    error = androidx.compose.ui.graphics.Color(0xFFBA1A1A),
    errorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),
    onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFF410002),

    // Outline
    outline = androidx.compose.ui.graphics.Color(0xFF999999),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFDDDDDD)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SukoonMusicPlayerTheme(
    theme: AppTheme = AppTheme.SYSTEM,
    // Dynamic color is available on Android 12+ (only applied when not using AMOLED)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Determine if dark theme should be used
    val isDarkTheme = when (theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.AMOLED -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        // AMOLED pure black theme (always use AmoledColorScheme, no dynamic colors)
        theme == AppTheme.AMOLED -> AmoledColorScheme

        // Material You dynamic colors (only for Android 12+ and non-AMOLED dark themes)
        dynamicColor && isDarkTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && theme != AppTheme.AMOLED -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context)
        }

        // Material You dynamic colors for light theme
        dynamicColor && !isDarkTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context)
        }

        // Static color schemes
        isDarkTheme && theme == AppTheme.DARK -> MidnightColorScheme
        isDarkTheme -> MidnightColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
