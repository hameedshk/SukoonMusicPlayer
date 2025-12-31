package com.sukoon.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header with song details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                                    modifier = Modifier.size(32.dp)
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
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Menu Items
            ListItem(
                headlineContent = { Text("Set as ringtone") },
                leadingContent = { Icon(Icons.Default.Notifications, null) },
                modifier = Modifier.clickable {
                    menuHandler.handleSetAsRingtone(song)
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text("Change cover") },
                leadingContent = { Icon(Icons.Default.Image, null) },
                modifier = Modifier.clickable {
                    menuHandler.handleChangeCover(song)
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text("Edit tags") },
                leadingContent = { Icon(Icons.Default.Edit, null) },
                modifier = Modifier.clickable {
                    menuHandler.handleEditTags(song)
                    onDismiss()
                }
            )
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
                leadingContent = { Icon(Icons.Default.PlaylistAdd, null) },
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
        }
    }
}
