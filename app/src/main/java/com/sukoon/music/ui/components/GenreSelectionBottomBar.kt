package com.sukoon.music.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Bottom bar for multi-selection mode in Genres screen.
 * Provides batch actions for selected genres.
 */
@Composable
fun GenreSelectionBottomBar(
    selectedCount: Int,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    BottomAppBar(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row {
                IconButton(onClick = onPlay) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play selected")
                }
                IconButton(onClick = onAddToPlaylist) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to playlist")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Play next") },
                            onClick = {
                                onPlayNext()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to queue") },
                            onClick = {
                                onAddToQueue()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.AddToQueue, null) }
                        )
                    }
                }
            }
        }
    }
}
