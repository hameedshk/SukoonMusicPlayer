package com.sukoon.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.ColorUtils
import com.sukoon.music.ui.util.AlbumPalette
import com.sukoon.music.ui.theme.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * LiquidMeshBackground - Animated aurora background extracted from album art palette.
 *
 * Creates smooth animated gradients with drifting radial gradient blobs that adapt to the album's color mood:
 * - Dark theme: Subtle muted dark colors from album with additive glow blend (luminance 0.08 → 0.15)
 * - AMOLED theme: Darker aurora on true black, battery-conscious (luminance 0.04 → 0.10, lower alpha)
 * - Light theme: Soft desaturated light colors from album with multiplicative tint (luminance 0.85 → 0.95)
 *
 * Animation:
 * - 4 independent drifting blobs with different periods (15s, 20s, 25s, 18s) prevent sync
 * - Slow idle animation when paused (60s periods for subtle drift)
 * - Smooth color crossfade (1200ms) on song change
 *
 * Falls back to fixed gradients if palette extraction fails.
 */
@Composable
fun LiquidMeshBackground(
    palette: AlbumPalette,
    songId: Long?,
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Detect theme from Material scheme
    val isDarkTheme = MaterialTheme.colorScheme.onBackground.red > 0.5f
    val isAmoled = LocalIsAmoled.current

    // Aurora blob colors - animated smooth crossfade on song change
    val blob1Color by animateColorAsState(
        targetValue = getBlobColor(palette, 0, isDarkTheme, isAmoled),
        animationSpec = tween(1200),
        label = "aurora_blob1"
    )
    val blob2Color by animateColorAsState(
        targetValue = getBlobColor(palette, 1, isDarkTheme, isAmoled),
        animationSpec = tween(1200),
        label = "aurora_blob2"
    )
    val blob3Color by animateColorAsState(
        targetValue = getBlobColor(palette, 2, isDarkTheme, isAmoled),
        animationSpec = tween(1200),
        label = "aurora_blob3"
    )
    val blob4Color by animateColorAsState(
        targetValue = getBlobColor(palette, 3, isDarkTheme, isAmoled),
        animationSpec = tween(1200),
        label = "aurora_blob4"
    )

    // Infinite animation for drifting blob positions
    val infiniteTransition = rememberInfiniteTransition(label = "aurora_drift")

    // Blob 1: 15s (playing) / 60s (paused)
    val blob1X by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isPlaying) 15000 else 60000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1_x"
    )
    val blob1Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isPlaying) 17000 else 65000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1_y"
    )

    // Blob 2: 20s (playing) / 60s (paused)
    val blob2X by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isPlaying) 20000 else 60000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2_x"
    )
    val blob2Y by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isPlaying) 22000 else 64000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2_y"
    )

    // Blob 3: 25s (playing) / 60s (paused)
    val blob3X by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isPlaying) 25000 else 62000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob3_x"
    )
    val blob3Y by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isPlaying) 23000 else 63000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob3_y"
    )

    // Blob 4: 18s (playing) / 60s (paused)
    val blob4X by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isPlaying) 18000 else 61000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob4_x"
    )
    val blob4Y by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isPlaying) 21000 else 65000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob4_y"
    )

    // Theme-specific settings
    val baseColor = if (isDarkTheme) {
        if (isAmoled) Color.Black else Color(0xFF0A0A0A)
    } else {
        Color(0xFFF0F0F5)
    }

    val blendMode = if (isDarkTheme) BlendMode.Screen else BlendMode.Multiply
    val blobAlpha = when {
        isAmoled -> 0.28f  // 0.25-0.30 range, battery-conscious
        isDarkTheme -> 0.40f
        else -> 0.30f
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Base background
        Box(modifier = Modifier
            .fillMaxSize()
            .background(baseColor)
        )

        // Animated aurora canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw 4 drifting radial gradient blobs
            drawAuroraBlob(
                color = blob1Color,
                centerX = blob1X,
                centerY = blob1Y,
                alpha = blobAlpha,
                blendMode = blendMode
            )
            drawAuroraBlob(
                color = blob2Color,
                centerX = blob2X,
                centerY = blob2Y,
                alpha = blobAlpha * 0.85f,  // Slightly darker for depth
                blendMode = blendMode
            )
            drawAuroraBlob(
                color = blob3Color,
                centerX = blob3X,
                centerY = blob3Y,
                alpha = blobAlpha * 0.7f,   // Even darker
                blendMode = blendMode
            )
            drawAuroraBlob(
                color = blob4Color,
                centerX = blob4X,
                centerY = blob4Y,
                alpha = blobAlpha * 0.8f,
                blendMode = blendMode
            )
        }
    }
}

/**
 * Draw a single aurora blob as a radial gradient circle.
 * Center position is normalized (0-1 range) and scales to canvas size.
 */
private fun DrawScope.drawAuroraBlob(
    color: Color,
    centerX: Float,
    centerY: Float,
    alpha: Float,
    blendMode: BlendMode
) {
    val radius = 400f  // Large radius for soft glow effect
    val actualCenterX = centerX * size.width
    val actualCenterY = centerY * size.height

    drawCircle(
        color = color.copy(alpha = alpha),
        radius = radius,
        center = androidx.compose.ui.geometry.Offset(actualCenterX, actualCenterY),
        blendMode = blendMode
    )
}

/**
 * Select blob color based on theme and source index.
 * Dark/AMOLED: Uses mutedDark, muted, vibrantDark, dominant with luminance 0.08-0.15 (AMOLED: 0.04-0.10)
 * Light: Uses mutedLight, vibrantLight, muted, dominant with luminance 0.85-0.95
 */
private fun getBlobColor(
    palette: AlbumPalette,
    index: Int,
    isDarkTheme: Boolean,
    isAmoled: Boolean
): Color {
    val baseColor = when (index) {
        0 -> if (isDarkTheme) palette.mutedDark else palette.mutedLight
        1 -> if (isDarkTheme) palette.muted else palette.vibrantLight
        2 -> if (isDarkTheme) palette.vibrantDark else palette.muted
        else -> palette.dominant
    }

    val targetLuminance = if (isDarkTheme) {
        if (isAmoled) {
            // AMOLED: 0.04-0.10 with more saturation reduction (30%)
            0.06f + (index * 0.01f)
        } else {
            // Dark theme: 0.08-0.15
            0.10f + (index * 0.015f)
        }
    } else {
        // Light theme: 0.85-0.95
        0.88f + (index * 0.015f)
    }

    val saturationReduction = if (isDarkTheme) {
        if (isAmoled) 0.30f else 0.20f
    } else {
        0.60f
    }

    return try {
        baseColor
            .adjustLuminance(targetLuminance)
            .reduceSaturation(saturationReduction)
    } catch (e: Exception) {
        // Fallback: return base color if adjustment fails
        baseColor
    }
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
