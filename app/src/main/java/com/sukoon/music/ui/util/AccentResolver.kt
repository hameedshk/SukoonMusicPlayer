package com.sukoon.music.ui.util

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import androidx.compose.ui.graphics.luminance

object AccentResolver {

    fun resolve(
        extractedAccent: Color,
        fallbackSeed: Int
    ): Color {
        // Reject near-gray colors
        if (isNearGray(extractedAccent)) {
            return deterministicFallback(fallbackSeed)
        }

        // Reject very dark colors
        if (extractedAccent.luminance() < 0.25f) {
            return deterministicFallback(fallbackSeed)
        }

        return extractedAccent
    }

    private fun deterministicFallback(seed: Int): Color {
        val hue = abs(seed % 360).toFloat()
        return Color.hsv(
            hue = hue,
            saturation = 0.55f,
            value = 0.75f
        )
    }

    private fun isNearGray(color: Color): Boolean {
        val r = color.red
        val g = color.green
        val b = color.blue
        return abs(r - g) < 0.04f &&
               abs(g - b) < 0.04f &&
               abs(r - b) < 0.04f
    }
}
