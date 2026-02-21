package com.sukoon.music.ui.util

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs

data class NowPlayingAccentColors(
    val controlsColor: Color,
    val sliderColor: Color
)

fun resolveNowPlayingAccentColors(
    palette: AlbumPalette,
    hasAlbumArt: Boolean,
    fallbackAccent: Color
): NowPlayingAccentColors {
    val safeFallback = fallbackAccent.sanitizeColorOr(fallback = Color(0xFF4CAF50))
    if (!hasAlbumArt) {
        return NowPlayingAccentColors(
            controlsColor = safeFallback,
            sliderColor = safeFallback
        )
    }

    val albumAccentCandidate = selectNowPlayingAlbumAccent(
        palette = palette,
        fallbackAccent = safeFallback
    )
    val albumAccent = albumAccentCandidate?.let { candidate ->
        toneNowPlayingAccent(candidate = candidate, fallbackAccent = safeFallback)
    }
    val controlsAccent = (albumAccent ?: safeFallback).sanitizeColorOr(safeFallback)
    val sliderAccent = (albumAccent?.let(::softenAccentForSlider) ?: safeFallback).sanitizeColorOr(safeFallback)

    return NowPlayingAccentColors(
        controlsColor = controlsAccent,
        sliderColor = sliderAccent
    )
}

fun hasUsableAlbumArt(albumArtUri: String?): Boolean {
    val value = albumArtUri?.trim().orEmpty()
    if (value.isEmpty()) return false
    return runCatching {
        val uri = Uri.parse(value)
        uri.scheme != null || value.startsWith("/")
    }.getOrDefault(false)
}

private fun selectNowPlayingAlbumAccent(
    palette: AlbumPalette,
    fallbackAccent: Color
): Color? {
    val candidates = listOf(
        palette.vibrant,
        palette.vibrantLight,
        palette.vibrantDark,
        palette.dominant,
        palette.muted,
        palette.mutedLight
    )
        .map { it.sanitizeColorOr(fallbackAccent) }
        .distinct()

    return candidates
        .map { candidate ->
            val saturation = candidate.saturation()
            val luminance = candidate.luminance()
            val distinctness = candidate.channelDistance(fallbackAccent)
            val saturationScore = saturation.coerceIn(0.08f, 1f)
            val luminanceScore = (1f - abs(luminance - 0.58f)).coerceIn(0f, 1f)
            val score = (saturationScore * 0.52f) +
                (luminanceScore * 0.28f) +
                (distinctness * 0.20f)

            candidate to score
        }
        .maxByOrNull { it.second }
        ?.first
}

private fun normalizeNowPlayingAccent(color: Color): Color {
    val max = maxOf(color.red, color.green, color.blue)
    val min = minOf(color.red, color.green, color.blue)
    val delta = max - min
    val hue = when {
        delta == 0f -> 0f
        max == color.red -> (60f * ((color.green - color.blue) / delta) + 360f) % 360f
        max == color.green -> (60f * ((color.blue - color.red) / delta) + 120f) % 360f
        else -> (60f * ((color.red - color.green) / delta) + 240f) % 360f
    }
    val saturation = if (max == 0f) 0f else delta / max
    val value = max

    return Color.hsv(
        hue = hue,
        saturation = saturation.coerceIn(0.22f, 0.62f),
        value = value.coerceIn(0.42f, 0.74f),
        alpha = color.alpha
    )
}

private fun toneNowPlayingAccent(
    candidate: Color,
    fallbackAccent: Color
): Color {
    val normalizedCandidate = normalizeNowPlayingAccent(candidate)
    val harmonized = blendColors(normalizedCandidate, fallbackAccent, 0.22f)
    return normalizeNowPlayingAccent(harmonized)
}

private fun softenAccentForSlider(color: Color): Color {
    val max = maxOf(color.red, color.green, color.blue)
    val min = minOf(color.red, color.green, color.blue)
    val delta = max - min
    val hue = when {
        delta == 0f -> 0f
        max == color.red -> (60f * ((color.green - color.blue) / delta) + 360f) % 360f
        max == color.green -> (60f * ((color.blue - color.red) / delta) + 120f) % 360f
        else -> (60f * ((color.red - color.green) / delta) + 240f) % 360f
    }
    val saturation = if (max == 0f) 0f else delta / max
    val value = max

    return Color.hsv(
        hue = hue,
        saturation = (saturation * 0.70f).coerceIn(0f, 1f),
        value = value.coerceAtLeast(0.55f),
        alpha = color.alpha
    )
}

private fun Color.channelDistance(other: Color): Float {
    return (abs(red - other.red) + abs(green - other.green) + abs(blue - other.blue)) / 3f
}

private fun blendColors(from: Color, to: Color, ratio: Float): Color {
    val t = ratio.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * t,
        green = from.green + (to.green - from.green) * t,
        blue = from.blue + (to.blue - from.blue) * t,
        alpha = from.alpha + (to.alpha - from.alpha) * t
    )
}

private fun Color.saturation(): Float {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    return if (max == 0f) 0f else delta / max
}

private fun Color.sanitizeColorOr(fallback: Color): Color {
    val safeFallback = Color(
        red = fallback.red.coerceIn(0f, 1f),
        green = fallback.green.coerceIn(0f, 1f),
        blue = fallback.blue.coerceIn(0f, 1f),
        alpha = fallback.alpha.coerceIn(0f, 1f)
    )
    if (!red.isValidChannel() || !green.isValidChannel() || !blue.isValidChannel() || !alpha.isValidChannel()) {
        return safeFallback
    }
    return Color(
        red = red.coerceIn(0f, 1f),
        green = green.coerceIn(0f, 1f),
        blue = blue.coerceIn(0f, 1f),
        alpha = alpha.coerceIn(0f, 1f)
    )
}

private fun Float.isValidChannel(): Boolean = isFinite()
