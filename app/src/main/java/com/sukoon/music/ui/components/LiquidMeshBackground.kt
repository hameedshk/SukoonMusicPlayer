package com.sukoon.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sukoon.music.ui.util.AlbumPalette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

@Composable
fun LiquidMeshBackground(
    palette: AlbumPalette,
    songId: Long?,
    modifier: Modifier = Modifier
) {
    // Extract colors from palette with fallback strategy
    val colorA = palette.vibrant ?: palette.vibrantDark ?: Color(0xFF001A33)
    val colorB = palette.muted ?: generateComplementaryColor(colorA)
    val colorC = palette.vibrantDark ?: palette.vibrant ?: Color(0xFF001A33)

    // Generate random initial positions on track change
    val randomOffsets = remember(songId) {
        RandomOffsets(
            offsetAx = Random.nextFloat() * 0.6f + 0.2f,
            offsetAy = Random.nextFloat() * 0.6f + 0.2f,
            offsetBx = Random.nextFloat() * 0.6f + 0.2f,
            offsetBy = Random.nextFloat() * 0.6f + 0.2f,
            offsetCx = Random.nextFloat() * 0.4f + 0.3f,
            offsetCy = Random.nextFloat() * 0.4f + 0.3f
        )
    }

    // Infinite animation for liquid effect
    val infiniteTransition = rememberInfiniteTransition(label = "meshAnimation")

    // Offset A oscillation (pulsing top-left)
    val offsetAXAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetAX"
    )

    val offsetAYAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetAY"
    )

    // Offset B rotation (circular motion bottom-right)
    val rotationAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Opacity A pulsing
    val opacityAnimation by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "opacity"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .zIndex(-1f)
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        // Calculate animated positions
        val posAx = centerX * (randomOffsets.offsetAx + (offsetAXAnimation - 0.5f) * 0.2f)
        val posAy = centerY * (randomOffsets.offsetAy + (offsetAYAnimation - 0.5f) * 0.2f)

        // Circular rotation for B
        val angleB = (rotationAnimation * PI / 180.0)
        val radiusB = width * 0.25f
        val posBx = centerX + (radiusB * cos(angleB.toFloat())).toFloat()
        val posBy = centerY + (radiusB * sin(angleB.toFloat())).toFloat()

        val posCx = centerX * randomOffsets.offsetCx
        val posCy = centerY * randomOffsets.offsetCy

        // Draw three radial gradients with Screen blend mode
        drawMeshGradient(
            colorA = colorA.copy(alpha = opacityAnimation),
            colorB = colorB,
            colorC = colorC,
            posAx = posAx,
            posAy = posAy,
            posBx = posBx,
            posBy = posBy,
            posCx = posCx,
            posCy = posCy,
            meshRadius = width * 0.7f
        )
    }
}

/**
 * Draws three overlapping radial gradients to create liquid mesh effect
 */
private fun DrawScope.drawMeshGradient(
    colorA: Color,
    colorB: Color,
    colorC: Color,
    posAx: Float,
    posAy: Float,
    posBx: Float,
    posBy: Float,
    posCx: Float,
    posCy: Float,
    meshRadius: Float
) {
    // Gradient A (top-left, pulsing)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(colorA, colorA.copy(alpha = 0.3f), Color.Transparent),
            center = Offset(posAx, posAy),
            radius = meshRadius
        ),
        center = Offset(posAx, posAy),
        radius = meshRadius,
        blendMode = BlendMode.Screen
    )

    // Gradient B (bottom-right, rotating)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(colorB, colorB.copy(alpha = 0.3f), Color.Transparent),
            center = Offset(posBx, posBy),
            radius = meshRadius * 0.85f
        ),
        center = Offset(posBx, posBy),
        radius = meshRadius * 0.85f,
        blendMode = BlendMode.Screen
    )

    // Gradient C (center, static base)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(colorC, colorC.copy(alpha = 0.2f), Color.Transparent),
            center = Offset(posCx, posCy),
            radius = meshRadius * 0.6f
        ),
        center = Offset(posCx, posCy),
        radius = meshRadius * 0.6f,
        blendMode = BlendMode.Screen
    )
}

/**
 * Generate complementary color based on HSV model
 */
private fun generateComplementaryColor(color: Color): Color {
    val rgb = android.graphics.Color.argb(
        (color.alpha * 255).toInt(),
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt()
    )

    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        android.graphics.Color.red(rgb),
        android.graphics.Color.green(rgb),
        android.graphics.Color.blue(rgb),
        hsv
    )

    // Shift hue by 180 degrees for complementary color
    hsv[0] = (hsv[0] + 180f) % 360f

    // Reduce saturation slightly for muted effect
    hsv[1] = (hsv[1] * 0.7f).coerceIn(0f, 1f)

    val complementaryRgb = android.graphics.Color.HSVToColor(hsv)
    return Color(complementaryRgb)
}

/**
 * Data class to hold random offset positions for mesh gradient
 */
private data class RandomOffsets(
    val offsetAx: Float,
    val offsetAy: Float,
    val offsetBx: Float,
    val offsetBy: Float,
    val offsetCx: Float,
    val offsetCy: Float
)
