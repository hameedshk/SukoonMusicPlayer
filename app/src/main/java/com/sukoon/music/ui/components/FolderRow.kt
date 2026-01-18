package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sukoon.music.domain.model.Folder

/**
 * Reusable list item for folders.
 *
 * Features:
 * - Leading: Folder icon or album art (48dp rounded)
 * - Middle: Column with folder name + subtitle (path • song count)
 * - Trailing: Three-dot menu button
 * - Context Menu with various actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderRow(
    folder: Folder,
    isHidden: Boolean,
    onFolderClick: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onHide: () -> Unit,
    onUnhide: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onFolderClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading: Icon/Art
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (folder.albumArtUri != null) {
                    SubcomposeAsyncImage(
                        model = folder.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = { FolderIconPlaceholder() }
                    )
                } else {
                    FolderIconPlaceholder()
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Middle: Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${folder.path} • ${folder.songCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Trailing: Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // Header (non-clickable info)
                    Text(
                        text = folder.path,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider()

                    DropdownMenuItem(
                        text = { Text("Play") },
                        onClick = { onPlay(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Play next") },
                        onClick = { onPlayNext(); showMenu = false },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to queue") },
                        onClick = { onAddToQueue(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.AddToQueue, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to playlist") },
                        onClick = { onAddToPlaylist(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) }
                    )
                    if (isHidden) {
                        DropdownMenuItem(
                            text = { Text("Unhide") },
                            onClick = { onUnhide(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Visibility, null) }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Hide") },
                            onClick = { onHide(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.VisibilityOff, null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete from device") },
                        onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderIconPlaceholder() {
    Icon(
        imageVector = Icons.Default.Folder,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(48.dp)
    )
}
