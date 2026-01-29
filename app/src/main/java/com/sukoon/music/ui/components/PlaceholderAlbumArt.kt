package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
     * Muted neutral palette - inspired by Spotify/Apple Music placeholder strategy.
     * Uses desaturated grays and dark surface tones (NO bright accent colors).
     */
    private val brandPalette = listOf(
        // Charcoal grays (Spotify-style)
        Color(0xFF2A2A2A),
        Color(0xFF1E1E1E),
        Color(0xFF333333),
        Color(0xFF3D3D3D),
        // Warm dark grays
        Color(0xFF3A3634),
        Color(0xFF2D2B29),
        Color(0xFF474340),
        // Cool dark grays
        Color(0xFF2B2E33),
        Color(0xFF353A40),
        Color(0xFF1F2326),
        // Very subtle muted tones
        Color(0xFF4A3F3F),
        Color(0xFF3F4A4A),
        Color(0xFF4A4A3F),
        // Deep neutral variants
        Color(0xFF262626),
        Color(0xFF3B3B3B),
        Color(0xFF424242)
    )

    /**
     * Generate a deterministic hash from a seed string.
     * Uses simple polynomial rolling hash for consistency.
     */
    private fun hashString(seed: String): Int {
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
     */
    private fun selectColors(hash: Int): List<Color> {
        val paletteSize = brandPalette.size
        val startIndex = hash % paletteSize

        // Use spacing of 3-5 for more visible color variation
        val spacing = 3 + (hash % 3)
        val colorCount = 2 + (hash % 2) // Either 2 or 3 colors

        return List(colorCount) { i ->
            brandPalette[(startIndex + i * spacing) % paletteSize]
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
     * Gold standard: Muted gradient + centered semi-transparent icon (Spotify/Apple Music style).
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
        icon: ImageVector = Icons.Default.MusicNote,
        iconSize: Int = 32,
        iconOpacity: Float = 0.35f
    ) {
        val hash = hashString(seed)
        val colors = selectColors(hash)
        val direction = getGradientAngle(hash)
        val brush = createGradientBrush(direction, colors)

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
                tint = Color.White.copy(alpha = iconOpacity)
            )
        }
    }
}
