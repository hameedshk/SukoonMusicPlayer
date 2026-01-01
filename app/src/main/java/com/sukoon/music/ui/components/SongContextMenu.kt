package com.sukoon.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Song

/**
 * Bottom sheet context menu for a Song.
 * Provides actions like setting ringtone, editing tags, adding to queue/playlist, and more.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongContextMenu(
    song: Song,
    menuHandler: SongMenuHandler,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        sheetMaxWidth = androidx.compose.ui.unit.Dp.Unspecified
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Header with song details and action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art
                Card(
                    modifier = Modifier.size(56.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    SubcomposeAsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                // Action buttons
                IconButton(onClick = {
                    menuHandler.handleShowSongInfo(song)
                    onDismiss() // Close the bottom sheet
                }) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = "Song info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { /* TODO: Share or edit */ }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick action chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Set as ringtone chip
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            menuHandler.handleSetAsRingtone(song)
                            onDismiss()
                        },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Set as ringtone",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                    }
                }

                // Change cover chip
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            menuHandler.handleChangeCover(song)
                            onDismiss()
                        },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Change cover",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                    }
                }

                // Edit tags chip
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            menuHandler.handleEditTags(song)
                            onDismiss()
                        },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Edit tags",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                    }
                }
            }

            // Main menu items
            ListItem(
                headlineContent = { Text("Play next") },
                leadingContent = { Icon(Icons.Default.SkipNext, null) },
                modifier = Modifier.clickable {
                    menuHandler.handlePlayNext(song)
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text("Add to queue") },
                leadingContent = { Icon(Icons.Default.Queue, null) },
                modifier = Modifier.clickable {
                    menuHandler.handleAddToQueue(song)
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text("Add to playlist") },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                modifier = Modifier.clickable {
                    menuHandler.handleAddToPlaylist(song)
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text("Go to album") },
                leadingContent = { Icon(Icons.Default.Album, null) },
                modifier = Modifier.clickable {
                    menuHandler.handleGoToAlbum(song)
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text("Edit audio") },
                leadingContent = { Icon(Icons.Default.AudioFile, null) },
                modifier = Modifier.clickable {
                    menuHandler.handleEditAudio(song)
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text("Update lyrics") },
                leadingContent = { Icon(Icons.Default.Lyrics, null) },
                modifier = Modifier.clickable {
                    menuHandler.handleUpdateLyrics(song)
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text("Delete from device", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable {
                    menuHandler.handleDeleteFromDevice(song)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
