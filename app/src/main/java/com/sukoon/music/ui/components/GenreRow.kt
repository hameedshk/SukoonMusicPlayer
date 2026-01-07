package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Genre

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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading: Checkbox or Icon
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectionToggle() },
                    modifier = Modifier.padding(end = 16.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
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
                            error = { GenreIconPlaceholder() }
                        )
                    } else {
                        GenreIconPlaceholder()
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            // Middle: Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = genre.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = genre.formattedSongCount(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Trailing: Menu Button
            if (!isSelectionMode) {
                IconButton(onClick = { onMoreClick(genre) }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GenreIconPlaceholder() {
    Icon(
        imageVector = Icons.Default.MusicNote,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        modifier = Modifier.size(32.dp)
    )
}
