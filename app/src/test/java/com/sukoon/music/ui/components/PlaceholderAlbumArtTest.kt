package com.sukoon.music.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaceholderAlbumArtTest {

    @Test
    fun testExtractDominantColorDarkThemeLightensToooDarkColor() {
        // Arrange: Very dark color for dark theme (luminance < 0.3)
        val color1 = Color(0xFF1A1A1A)  // Very dark gray
        val color2 = Color(0xFF2A2A2A)  // Slightly lighter dark gray

        // Act
        val result = PlaceholderAlbumArt.extractDominantColor(
            color1 = color1,
            color2 = color2,
            isDark = true
        )

        // Assert: Should be lightened
        assertTrue(result.luminance() > 0.3f, "Dark theme should lighten colors < 0.3 luminance")
    }

    @Test
    fun testExtractDominantColorLightThemeDarkensTooLightColor() {
        // Arrange: Very light color for light theme (luminance > 0.7)
        val color1 = Color(0xFFE8E8E8)  // Very light gray
        val color2 = Color(0xFFF2F2F2)  // Nearly white

        // Act
        val result = PlaceholderAlbumArt.extractDominantColor(
            color1 = color1,
            color2 = color2,
            isDark = false
        )

        // Assert: Should be darkened
        assertTrue(result.luminance() < 0.7f, "Light theme should darken colors > 0.7 luminance")
    }

    @Test
    fun testExtractDominantColorMiddleRangeUnchanged() {
        // Arrange: Mid-range color that's readable in both themes
        val color1 = Color(0xFF808080)  // Mid gray
        val color2 = Color(0xFF707070)  // Slightly darker mid gray

        // Act
        val result = PlaceholderAlbumArt.extractDominantColor(
            color1 = color1,
            color2 = color2,
            isDark = true
        )

        // Assert: Should remain similar (within acceptable range)
        val avgLuminance = (color1.luminance() + color2.luminance()) / 2f
        assertTrue(
            abs(result.luminance() - avgLuminance) < 0.1f,
            "Mid-range colors should not be heavily adjusted"
        )
    }

    @Test
    fun testHashStringDeterministic() {
        // Arrange
        val seed = "Test Song Title"

        // Act
        val hash1 = PlaceholderAlbumArt.hashString(seed)
        val hash2 = PlaceholderAlbumArt.hashString(seed)

        // Assert
        assertEquals(hash1, hash2, "Hash should be deterministic")
    }

    @Test
    fun testHashStringDifferentInputs() {
        // Arrange
        val seed1 = "Song A"
        val seed2 = "Song B"

        // Act
        val hash1 = PlaceholderAlbumArt.hashString(seed1)
        val hash2 = PlaceholderAlbumArt.hashString(seed2)

        // Assert: Different inputs should produce different hashes (with high probability)
        assertTrue(hash1 != hash2, "Different seeds should produce different hashes")
    }
}
