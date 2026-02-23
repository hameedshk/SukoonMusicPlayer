package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Deterministic album art placeholder generator.
 *
 * Generates abstract gradient placeholders based on stable album/artist metadata.
 * Same input always produces same visual output (no randomness at render time).
 *
 * Design: Pure gradient backgrounds with no text, icons, or symbolic content.
 */
object PlaceholderAlbumArt {

    /**
     * Clean album disc icon (S-style silhouette).
     */
    private val AlbumDiscIcon: ImageVector
        get() = ImageVector.Builder(
            name = "AlbumDisc",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer circle
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                curveToRelative(5.52f, 0f, 10f, 4.48f, 10f, 10f)
                reflectiveCurveToRelative(-4.48f, 10f, -10f, 10f)
                reflectiveCurveTo(2f, 17.52f, 2f, 12f)
                reflectiveCurveTo(6.48f, 2f, 12f, 2f)
                close()
                // Inner ring (negative space)
                moveTo(12f, 6f)
                curveToRelative(-3.31f, 0f, -6f, 2.69f, -6f, 6f)
                reflectiveCurveToRelative(2.69f, 6f, 6f, 6f)
                reflectiveCurveToRelative(6f, -2.69f, 6f, -6f)
                reflectiveCurveToRelative(-2.69f, -6f, -6f, -6f)
                close()
                // Center hole
                moveTo(12f, 10.5f)
                curveToRelative(-0.83f, 0f, -1.5f, 0.67f, -1.5f, 1.5f)
                reflectiveCurveToRelative(0.67f, 1.5f, 1.5f, 1.5f)
                reflectiveCurveToRelative(1.5f, -0.67f, 1.5f, -1.5f)
                reflectiveCurveToRelative(-0.67f, -1.5f, -1.5f, -1.5f)
                close()
            }
        }.build()

    /**
     * Dark theme palette - muted vibrant colors (Spotify-style gradients).
     * Each song gets a distinct color for better visual differentiation.
     */
    private val darkPalette = listOf(
        Color(0xFF3B1F5E), // Deep purple
        Color(0xFF1A3D4F), // Dark teal
        Color(0xFF1E3D2A), // Forest green
        Color(0xFF4A1C2A), // Burgundy
        Color(0xFF1C2B4A), // Navy blue
        Color(0xFF4A2E1A), // Burnt orange
        Color(0xFF3D1C4A), // Plum
        Color(0xFF1A4A3D), // Dark cyan
        Color(0xFF4A3B1A), // Dark gold
        Color(0xFF2A1A4A), // Indigo
        Color(0xFF1A4A2A), // Emerald dark
        Color(0xFF4A1A1A), // Dark red
        Color(0xFF1A3A4A), // Steel blue
        Color(0xFF3A4A1A), // Olive
        Color(0xFF4A1A3A), // Rose dark
        Color(0xFF1A2A4A)  // Midnight blue
    )

    /**
     * Light theme palette - soft pastel colors for readability.
     * Each song gets a distinct pastel color for better visual differentiation.
     */
    private val lightPalette = listOf(
        Color(0xFFD4B8F0), // Soft lavender
        Color(0xFFB8D4F0), // Sky blue
        Color(0xFFB8F0D4), // Mint
        Color(0xFFF0B8D4), // Blush pink
        Color(0xFFB8E8F0), // Powder blue
        Color(0xFFF0D4B8), // Peach
        Color(0xFFD4F0B8), // Lime pastel
        Color(0xFFF0B8E8), // Lilac
        Color(0xFFB8B8F0), // Periwinkle
        Color(0xFFF0F0B8), // Pale yellow
        Color(0xFFB8F0F0), // Aqua pastel
        Color(0xFFF0C4B8), // Salmon
        Color(0xFFD4B8E0), // Mauve
        Color(0xFFB8D4B8), // Sage
        Color(0xFFE0B8B8), // Dusty rose
        Color(0xFFB8C8F0)  // Cornflower
    )

    /**
     * Generate a deterministic hash from a seed string.
     * Uses simple polynomial rolling hash for consistency.
     *
     * @param seed Input string to hash
     * @return Non-negative hash value
     */
    fun hashString(seed: String): Int {
        if (seed.isEmpty()) return 0
        var hash = 0
        for (char in seed) {
            hash = (31 * hash + char.code) and 0x7FFFFFFF
        }
        return abs(hash)
    }

    /**
     * Select 2-3 colors with spacing from the palette for visible gradients.
     * Uses larger intervals to ensure visual distinction between colors.
     *
     * @param hash Hash value to seed color selection
     * @param isDark True for dark theme palette, false for light theme
     * @return List of 2-3 colors suitable for gradient
     */
    fun selectColors(hash: Int, isDark: Boolean): List<Color> {
        val palette = if (isDark) darkPalette else lightPalette
        val paletteSize = palette.size
        val startIndex = hash % paletteSize

        // Use spacing of 3-5 for more visible color variation
        val spacing = 3 + (hash % 3)
        val colorCount = 2 + (hash % 2) // Either 2 or 3 colors

        return List(colorCount) { i ->
            palette[(startIndex + i * spacing) % paletteSize]
        }
    }

    /**
     * Derive gradient direction from hash (8 cardinal directions).
     */
    private fun getGradientAngle(hash: Int): GradientDirection {
        val directions = GradientDirection.values()
        return directions[hash % directions.size]
    }

    /**
     * Cardinal gradient directions derived from hash.
     */
    private enum class GradientDirection {
        TOP_TO_BOTTOM,
        BOTTOM_TO_TOP,
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT,
        TOP_LEFT_TO_BOTTOM_RIGHT,
        BOTTOM_RIGHT_TO_TOP_LEFT,
        TOP_RIGHT_TO_BOTTOM_LEFT,
        BOTTOM_LEFT_TO_TOP_RIGHT
    }

    /**
     * Create a gradient brush based on direction and colors.
     */
    private fun createGradientBrush(
        direction: GradientDirection,
        colors: List<Color>
    ): Brush {
        return when (direction) {
            GradientDirection.TOP_TO_BOTTOM -> Brush.verticalGradient(colors)
            GradientDirection.BOTTOM_TO_TOP -> Brush.verticalGradient(colors.reversed())
            GradientDirection.LEFT_TO_RIGHT -> Brush.horizontalGradient(colors)
            GradientDirection.RIGHT_TO_LEFT -> Brush.horizontalGradient(colors.reversed())
            GradientDirection.TOP_LEFT_TO_BOTTOM_RIGHT -> Brush.linearGradient(
                colors = colors,
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
            GradientDirection.BOTTOM_RIGHT_TO_TOP_LEFT -> Brush.linearGradient(
                colors = colors.reversed(),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
            GradientDirection.TOP_RIGHT_TO_BOTTOM_LEFT -> Brush.linearGradient(
                colors = colors,
                start = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f),
                end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
            )
            GradientDirection.BOTTOM_LEFT_TO_TOP_RIGHT -> Brush.linearGradient(
                colors = colors.reversed(),
                start = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f),
                end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
            )
        }
    }

    /**
     * Generate a stable seed string from album metadata.
     * Priority: albumName -> artistName -> albumId/songId
     */
    fun generateSeed(
        albumName: String? = null,
        artistName: String? = null,
        albumId: Long? = null,
        songId: Long? = null
    ): String {
        return when {
            !albumName.isNullOrBlank() -> albumName
            !artistName.isNullOrBlank() -> artistName
            albumId != null && albumId > 0 -> albumId.toString()
            songId != null && songId > 0 -> songId.toString()
            else -> "unknown"
        }
    }

    /**
     * Composable that renders a deterministic gradient placeholder.
     * Gold standard: Muted gradient + centered semi-transparent icon (S/A Music style).
     *
     * @param seed Stable identifier (album name, artist name, or ID)
     * @param modifier Modifier for size, shape, etc.
     * @param icon Icon to display (MusicNote for songs, Album for albums)
     * @param iconSize Size of the centered icon
     * @param iconOpacity Opacity of icon (0.3-0.4 recommended)
     */
    @Composable
    fun Placeholder(
        seed: String,
        modifier: Modifier = Modifier,
        icon: ImageVector = AlbumDiscIcon,
        iconSize: Int = 56,
        iconOpacity: Float = 0.4f
    ) {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val hash = hashString(seed)
        val colors = selectColors(hash, isDark)
        val direction = getGradientAngle(hash)
        val brush = createGradientBrush(direction, colors)

        // Icon color adapts to theme
        val iconTint = if (isDark) {
            Color.White.copy(alpha = iconOpacity)
        } else {
            Color.Black.copy(alpha = iconOpacity * 0.6f)
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(brush),
            contentAlignment = Alignment.Center
        ) {
            // Centered semi-transparent icon overlay (industry standard)
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize.dp),
                tint = iconTint
            )
        }
    }

    /**
     * Extract dominant color from 2-color gradient for player UI tinting.
     * Adjusts color based on theme for readability (WCAG AA consideration).
     *
     * @param color1 First gradient color
     * @param color2 Second gradient color
     * @param isDark True for dark theme, false for light theme
     * @return Adjusted dominant color suitable for UI tinting
     */
    fun extractDominantColor(
        color1: Color,
        color2: Color,
        isDark: Boolean
    ): Color {
        // Average the 2 gradient colors
        val avgColor = Color(
            red = (color1.red + color2.red) / 2f,
            green = (color1.green + color2.green) / 2f,
            blue = (color1.blue + color2.blue) / 2f,
            alpha = (color1.alpha + color2.alpha) / 2f
        )

        val luminance = avgColor.luminance()

        // Adjust for theme readability
        return when {
            isDark && luminance < 0.3f -> {
                // Too dark for dark theme → lighten by 20%
                avgColor.copy(
                    red = (avgColor.red * 1.2f).coerceAtMost(1f),
                    green = (avgColor.green * 1.2f).coerceAtMost(1f),
                    blue = (avgColor.blue * 1.2f).coerceAtMost(1f)
                )
            }
            !isDark && luminance > 0.7f -> {
                // Too light for light theme → darken by 20%
                avgColor.copy(
                    red = (avgColor.red * 0.8f),
                    green = (avgColor.green * 0.8f),
                    blue = (avgColor.blue * 0.8f)
                )
            }
            else -> avgColor
        }
    }

    /**
     * Check if color meets WCAG AA contrast ratio (4.5:1) against background.
     * Used to verify UI tinting is readable before applying to seekbar/buttons.
     *
     * @param color Text/foreground color
     * @param background Background color
     * @return True if contrast >= 4.5:1
     */
    fun meetsWcagAA(color: Color, background: Color): Boolean {
        val contrast = (color.luminance() + 0.05f) / (background.luminance() + 0.05f)
        return contrast >= 4.5f
    }
}
