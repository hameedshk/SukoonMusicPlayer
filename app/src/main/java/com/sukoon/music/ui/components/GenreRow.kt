package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Genre
import com.sukoon.music.ui.components.PlaceholderAlbumArt
import com.sukoon.music.ui.theme.*

/**
 * Reusable list item for genres.
 *
 * Features:
 * - Leading: Genre icon (MusicNote, 56dp circular) or Checkbox in selection mode
 * - Middle: Column with genre name + song count
 * - Trailing: Three-dot menu button
 */
@Composable
fun GenreRow(
    genre: Genre,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onPlayNextClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMoreClick: (Genre) -> Unit = {},
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onSelectionToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
    key: Any? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = if (isSelectionMode) onSelectionToggle else onClick),
        color = if (isSelected && isSelectionMode) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading: Genre Icon (always shown)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (genre.artworkUri != null) {
                    SubcomposeAsyncImage(
                        model = genre.artworkUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = {
                            PlaceholderAlbumArt.Placeholder(
                                seed = PlaceholderAlbumArt.generateSeed(
                                    albumName = genre.name,
                                    albumId = genre.id
                                )
                            )
                        }
                    )
                } else {
                    PlaceholderAlbumArt.Placeholder(
                        seed = PlaceholderAlbumArt.generateSeed(
                            albumName = genre.name,
                            albumId = genre.id
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))

            // Middle: Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Genre name with fade-right effect for long text
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = genre.name,
                        style = MaterialTheme.typography.genreTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Gradient fade on right edge for premium truncation
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(32.dp)
                            .height(20.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surface
                                    ),
                                    startX = 0f,
                                    endX = Float.MAX_VALUE
                                )
                            )
                    )
                }
                Text(
                    text = androidx.compose.ui.res.pluralStringResource(
                        com.sukoon.music.R.plurals.common_song_count,
                        genre.songCount,
                        genre.songCount
                    ),
                    style = MaterialTheme.typography.genreMetadata,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Trailing: RadioButton/Checkbox in selection mode, Menu Button otherwise
            if (isSelectionMode) {
                if (isSelected) {
                    Checkbox(
                        checked = true,
                        onCheckedChange = { onSelectionToggle() }
                    )
                } else {
                    RadioButton(
                        selected = false,
                        onClick = { onSelectionToggle() }
                    )
                }
            } else {
                IconButton(onClick = { onMoreClick(genre) }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


