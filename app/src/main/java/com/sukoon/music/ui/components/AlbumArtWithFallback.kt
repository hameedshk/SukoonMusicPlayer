package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Song
import androidx.compose.ui.res.stringResource
import com.sukoon.music.R

/**
 * Album art display component with intelligent fallback chain.
 *
 * Fallback chain (in order):
 * 1. Embedded album art from MediaStore (song.albumArtUri)
 * 2. Loading spinner (CircularProgressIndicator)
 * 3. Generated gradient (PlaceholderAlbumArt) ← FALLBACK
 * 4. Box background color (graceful crash recovery)
 *
 * @param song Song to display album art for
 * @param modifier Modifier for sizing/positioning
 * @param size Fixed size (width = height, square aspect ratio)
 * @param contentScale How to scale the image (default: Crop for consistent appearance)
 * @param onDominantColorExtracted Callback fired when fallback gradient computed
 *                                 (color ready for player UI tinting)
 * @param style Fallback style (ICON, LETTER, NONE) - currently only ICON supported
 */
@Composable
fun AlbumArtWithFallback(
    song: Song,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    contentScale: ContentScale = ContentScale.Crop,
    onDominantColorExtracted: (Color) -> Unit = {},
    style: GeneratedArtStyle = GeneratedArtStyle.ICON
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Memoize fallback seed once per song metadata
    val fallbackSeed = remember(song.album, song.artist, song.id) {
        song.album.takeIf { it.isNotBlank() }?.trim()
            ?: song.artist.takeIf { it.isNotBlank() }?.trim()
            ?: song.id.toString()
    }

    // Memoize dominant color: recompute only on theme change
    val fallbackDominantColor = remember(fallbackSeed, isDark) {
        val hash = PlaceholderAlbumArt.hashString(fallbackSeed)
        val colors = PlaceholderAlbumArt.selectColors(hash, isDark)
        PlaceholderAlbumArt.extractDominantColor(
            color1 = colors[0],
            color2 = colors.getOrElse(1) { colors[0] },
            isDark = isDark
        )
    }

    // Fire callback when color computed or theme changes
    LaunchedEffect(fallbackDominantColor) {
        onDominantColorExtracted(fallbackDominantColor)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = song.albumArtUri,
            contentDescription = stringResource(
                R.string.common_album_art_for_song,
                song.title
            ),
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            loading = {
                // Tier 2: Show spinner while Coil loads
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            },
            error = {
                // Tier 3: Fallback to generated gradient
                PlaceholderAlbumArt.Placeholder(
                    seed = fallbackSeed,
                    modifier = Modifier.fillMaxSize(),
                    icon = Icons.Default.Album,
                    iconSize = 56,
                    iconOpacity = 0.65f
                )
            }
        )
    }
}

/**
 * Style options for generated fallback album art.
 * Currently only ICON is implemented; LETTER and NONE are placeholders for future.
 */
enum class GeneratedArtStyle {
    NONE,    // Gradient only (future)
    ICON,    // Gradient + music note icon (current)
    LETTER   // Gradient + first letter of song (future)
}
