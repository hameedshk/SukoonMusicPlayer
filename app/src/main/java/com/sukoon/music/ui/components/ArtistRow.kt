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
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.ui.theme.*

/**
 * Reusable list item for artists.
 *
 * Features:
 * - Leading: Artist artwork (56dp circular)
 * - Middle: Column with artist name + metadata (X albums · Y songs)
 * - Trailing: Three-dot menu button
 * - Context Menu with various actions
 */
@Composable
fun ArtistRow(
    artist: Artist,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onPlayNextClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onEditTagsClick: () -> Unit,
    onChangeCoverClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading: Artwork (Circular)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (artist.artworkUri != null) {
                    SubcomposeAsyncImage(
                        model = artist.artworkUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = { ArtistIconPlaceholder() }
                    )
                } else {
                    ArtistIconPlaceholder()
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Middle: Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        com.sukoon.music.R.string.artist_row_metadata_format,
                        androidx.compose.ui.res.pluralStringResource(
                            com.sukoon.music.R.plurals.library_screens_b_album_count,
                            artist.albumCount,
                            artist.albumCount
                        ),
                        androidx.compose.ui.res.pluralStringResource(
                            com.sukoon.music.R.plurals.library_screens_b_song_count,
                            artist.songCount,
                            artist.songCount
                        )
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Trailing: Menu
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_more),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // Header (non-clickable info)
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider()

                    DropdownMenuItem(
                        text = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play)) },
                        onClick = { onPlayClick(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play_next)) },
                        onClick = { onPlayNextClick(); showMenu = false },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, modifier = Modifier.size(20.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_queue)) },
                        onClick = { onAddToQueueClick(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.AddToQueue, null, modifier = Modifier.size(20.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_playlist)) },
                        onClick = { onAddToPlaylistClick(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, null, modifier = Modifier.size(20.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_edit_tags)) },
                        onClick = { onEditTagsClick(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_change_cover)) },
                        onClick = { onChangeCoverClick(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Image, null, modifier = Modifier.size(20.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_delete_from_device)) },
                        onClick = { onDeleteClick(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistIconPlaceholder() {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        modifier = Modifier.size(28.dp)
    )
}

