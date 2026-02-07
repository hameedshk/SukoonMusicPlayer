package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.ColorUtils
import com.sukoon.music.ui.util.AlbumPalette
import com.sukoon.music.ui.theme.*
import kotlin.math.max
import kotlin.math.min

/**
 * LiquidMeshBackground - Dynamic gradient background extracted from album art palette.
 *
 * Creates smooth vertical gradients that adapt to the album's color mood:
 * - Dark theme: Subtle muted dark colors from album (luminance 0.04 → 0.08)
 * - Light theme: Soft desaturated light colors from album (luminance 0.92 → white)
 *
 * Falls back to fixed gradients if palette extraction fails.
 */
@Composable
fun LiquidMeshBackground(
    palette: AlbumPalette,
    songId: Long?,
    modifier: Modifier = Modifier
) {
    // Detect theme from Material scheme
    // Dark theme: onBackground is light (red > 0.5)
    // Light theme: onBackground is dark (red < 0.5)
    val isDarkTheme = MaterialTheme.colorScheme.onBackground.red > 0.5f

    // Generate dynamic gradient from album palette with fallback to fixed colors
    val gradientBrush = remember(palette, isDarkTheme, songId) {
        try {
            Brush.verticalGradient(
                colors = if (isDarkTheme) {
                    // Dark theme: Use muted dark from palette with very low luminance
                    val baseColor = palette.mutedDark
                    listOf(
                        baseColor.adjustLuminance(0.08f),  // Top: slightly lighter
                        baseColor.adjustLuminance(0.04f)   // Bottom: deeper shadow
                    )
                } else {
                    // Light theme: Use muted light from palette with high luminance + desaturation
                    val baseColor = palette.mutedLight
                    listOf(
                        baseColor
                            .adjustLuminance(0.92f)
                            .reduceSaturation(0.70f),  // Top: very light with reduced saturation
                        Color.White                    // Bottom: pure white for clean base
                    )
                }
            )
        } catch (e: Exception) {
            // Fallback to fixed gradients if palette extraction fails
            Brush.verticalGradient(
                colors = if (isDarkTheme) {
                    listOf(
                        Color(0xFF0F1C1E),  // Fixed dark: top
                        Color(0xFF0B0F11)   // Fixed dark: bottom
                    )
                } else {
                    listOf(
                        Color(0xFFEAF6F6),  // Fixed light: top
                        Color(0xFFFFFFFF)   // Fixed light: bottom
                    )
                }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush)
    )
}

// ============================================================================
// Color Space Helpers - HSL Conversion and Manipulation
// ============================================================================

/**
 * Adjusts the luminance (lightness) of a color to a target value.
 *
 * Uses HSL color space for perceptually accurate luminance adjustment.
 * Preserves hue and saturation while clamping luminance to 0.0-1.0 range.
 *
 * @param targetLuminance Desired luminance (0.0 = black, 1.0 = white)
 * @return New Color with adjusted luminance
 */
fun Color.adjustLuminance(targetLuminance: Float): Color {
    val hsl = this.toHsl()
    // HSL[2] is lightness component (0.0-1.0)
    hsl[2] = targetLuminance.coerceIn(0f, 1f)
    return Color.fromHsl(hsl)
}

/**
 * Reduces the saturation of a color by a given factor.
 *
 * Uses HSL color space to desaturate colors while preserving hue and lightness.
 * Useful for creating muted background colors that don't overwhelm content.
 *
 * @param factor Saturation reduction factor (0.0 = no change, 1.0 = fully desaturated to gray)
 * @return New Color with reduced saturation
 */
fun Color.reduceSaturation(factor: Float): Color {
    val hsl = this.toHsl()
    // HSL[1] is saturation component (0.0-1.0)
    // Multiply by (1 - factor) to reduce saturation
    hsl[1] = hsl[1] * (1f - factor.coerceIn(0f, 1f))
    return Color.fromHsl(hsl)
}

/**
 * Converts Compose Color to HSL array [H, S, L].
 *
 * H (Hue): 0-360 degrees
 * S (Saturation): 0.0-1.0 (0% to 100%)
 * L (Lightness): 0.0-1.0 (0% to 100%)
 *
 * Uses Android ColorUtils for accurate conversion.
 */
fun Color.toHsl(): FloatArray {
    val hsl = FloatArray(3)
    val rgb = android.graphics.Color.rgb(
        (this.red * 255).toInt(),
        (this.green * 255).toInt(),
        (this.blue * 255).toInt()
    )
    ColorUtils.colorToHSL(rgb, hsl)
    return hsl
}

/**
 * Creates Compose Color from HSL array [H, S, L].
 *
 * H (Hue): 0-360 degrees
 * S (Saturation): 0.0-1.0
 * L (Lightness): 0.0-1.0
 *
 * Uses Android ColorUtils for accurate conversion.
 */
fun Color.Companion.fromHsl(hsl: FloatArray): Color {
    val rgb = ColorUtils.HSLToColor(hsl)
    return Color(rgb)
}
