package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

/**
 * Displays a 2x2 grid of album art URIs (collage) for folder visualization.
 *
 * Features:
 * - 2x2 grid layout with 1dp spacing between cells
 * - Graceful fallbacks for 0-4 images
 * - PlaceholderAlbumArt for missing images
 * - Folder icon placeholder when no URIs available
 *
 * @param albumArtUris List of album art URIs (max 4 used for grid)
 * @param size Total size of the collage (default 64dp)
 */
@Composable
fun FolderAlbumArtCollage(
    albumArtUris: List<String>,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val cellSize = (size - 1.dp) / 2  // Account for 1dp spacing between cells

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            albumArtUris.isEmpty() -> {
                // Empty state: Folder icon
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Folder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
            albumArtUris.size == 1 -> {
                // Single image: Full size
                SubcomposeAsyncImage(
                    model = albumArtUris[0],
                    contentDescription = "Folder album art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        PlaceholderAlbumArt.Placeholder(
                            seed = albumArtUris[0]
                        )
                    },
                    error = {
                        PlaceholderAlbumArt.Placeholder(
                            seed = albumArtUris[0]
                        )
                    }
                )
            }
            else -> {
                // 2x2 Grid: Display up to 4 images
                val displayUris = albumArtUris.take(4)

                // Pad list to exactly 4 items with repeats if needed
                val paddedUris = when {
                    displayUris.size == 2 -> {
                        listOf(displayUris[0], displayUris[1], displayUris[0], displayUris[1])
                    }
                    displayUris.size == 3 -> {
                        listOf(displayUris[0], displayUris[1], displayUris[2], displayUris[0])
                    }
                    else -> displayUris  // size == 4
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    // Top row
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        CollageCell(paddedUris[0], cellSize)
                        CollageCell(paddedUris[1], cellSize)
                    }

                    // Bottom row
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        CollageCell(paddedUris[2], cellSize)
                        CollageCell(paddedUris[3], cellSize)
                    }
                }
            }
        }
    }
}

@Composable
private fun CollageCell(
    uri: String,
    size: Dp
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            loading = {
                PlaceholderAlbumArt.Placeholder(
                    seed = uri,
                    modifier = Modifier.fillMaxSize()
                )
            },
            error = {
                PlaceholderAlbumArt.Placeholder(
                    seed = uri,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    }
}
