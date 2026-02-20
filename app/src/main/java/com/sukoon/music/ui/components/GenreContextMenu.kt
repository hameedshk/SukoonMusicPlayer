package com.sukoon.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sukoon.music.domain.model.Genre
import com.sukoon.music.ui.theme.*

/**
 * Bottom sheet context menu for a Genre.
 * Provides actions like playing, queuing, adding to playlist, and more.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreContextMenu(
    genre: Genre,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEditTags: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header with genre details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GenreIcon(
                    genreName = genre.name,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = genre.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = androidx.compose.ui.res.pluralStringResource(
                            com.sukoon.music.R.plurals.common_song_count,
                            genre.songCount,
                            genre.songCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play)) },
                leadingContent = { Icon(Icons.Default.PlayArrow, null) },
                modifier = Modifier.clickable {
                    onPlay()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play_next)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                modifier = Modifier.clickable {
                    onPlayNext()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_queue)) },
                leadingContent = { Icon(Icons.Default.AddToQueue, null) },
                modifier = Modifier.clickable {
                    onAddToQueue()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_playlist)) },
                leadingContent = { Icon(Icons.Default.PlaylistAdd, null) },
                modifier = Modifier.clickable {
                    onAddToPlaylist()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_edit_tags)) },
                leadingContent = { Icon(Icons.Default.Edit, null) },
                modifier = Modifier.clickable {
                    onEditTags()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_change_cover)) },
                leadingContent = { Icon(Icons.Default.Image, null) },
                modifier = Modifier.clickable {
                    onChangeCover()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_delete_from_device), color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable {
                    onDelete()
                    onDismiss()
                }
            )
        }
    }
}

