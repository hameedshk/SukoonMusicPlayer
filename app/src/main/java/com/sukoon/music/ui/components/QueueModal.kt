package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Song

/**
 * Queue Modal Bottom Sheet - Full-screen overlay for queue management.
 *
 * Features:
 * - Modal bottom sheet with drag-to-dismiss
 * - Three logical sections: Now Playing, Up Next, Previously Played
 * - Clear visual distinction for current track
 * - Safe interactions: tap to jump, remove (except current)
 * - Observes queue state from MediaController (via PlaybackState)
 * - Handles empty and edge states gracefully
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueModalSheet(
    queue: List<Song>,
    currentIndex: Int,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSongClick: (Int) -> Unit,
    onRemoveClick: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(32.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 24.dp)
        ) {
            // Header
            QueueModalHeader(
                queueSize = queue.size,
                onDismiss = onDismiss
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Queue content
            if (queue.isEmpty()) {
                EmptyQueueState()
            } else {
                QueueModalContent(
                    queue = queue,
                    currentIndex = currentIndex,
                    accentColor = accentColor,
                    onSongClick = onSongClick,
                    onRemoveClick = onRemoveClick
                )
            }
        }
    }
}

/**
 * Header for the Queue modal.
 */
@Composable
private fun QueueModalHeader(
    queueSize: Int,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Queue",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$queueSize ${if (queueSize == 1) "song" else "songs"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close queue",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Content area for the queue modal - displays queue in sections.
 */
@Composable
private fun QueueModalContent(
    queue: List<Song>,
    currentIndex: Int,
    accentColor: Color,
    onSongClick: (Int) -> Unit,
    onRemoveClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Section 1: Now Playing (if there's a current track)
        if (currentIndex >= 0 && currentIndex < queue.size) {
            item {
                SectionHeader(text = "Now Playing")
            }
            item {
                QueueModalSongItem(
                    song = queue[currentIndex],
                    isCurrentSong = true,
                    accentColor = accentColor,
                    showDuration = true,
                    onSongClick = { onSongClick(currentIndex) },
                    onRemoveClick = null // Cannot remove current track
                )
            }
        }

        // Section 2: Up Next (tracks after current)
        val upNextItems = if (currentIndex >= 0 && currentIndex < queue.size - 1) {
            queue.subList(currentIndex + 1, queue.size)
        } else {
            emptyList()
        }

        if (upNextItems.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(text = "Up Next (${upNextItems.size})")
            }
            itemsIndexed(
                items = upNextItems,
                key = { index, song -> "${song.id}_${currentIndex + 1 + index}" }
            ) { relativeIndex, song ->
                val absoluteIndex = currentIndex + 1 + relativeIndex
                QueueModalSongItem(
                    song = song,
                    isCurrentSong = false,
                    accentColor = accentColor,
                    showDuration = true,
                    onSongClick = { onSongClick(absoluteIndex) },
                    onRemoveClick = { onRemoveClick(absoluteIndex) }
                )
            }
        }

        // Section 3: Previous tracks (if any, shown at bottom)
        val previousItems = if (currentIndex > 0) {
            queue.subList(0, currentIndex)
        } else {
            emptyList()
        }

        if (previousItems.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(text = "Previously Played (${previousItems.size})")
            }
            itemsIndexed(
                items = previousItems,
                key = { index, song -> "${song.id}_$index" }
            ) { index, song ->
                QueueModalSongItem(
                    song = song,
                    isCurrentSong = false,
                    accentColor = accentColor,
                    showDuration = true,
                    onSongClick = { onSongClick(index) },
                    onRemoveClick = { onRemoveClick(index) }
                )
            }
        }

        // Bottom padding
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Section header for queue sections.
 */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
    )
}

/**
 * Queue item for the modal with enhanced visual design.
 */
@Composable
private fun QueueModalSongItem(
    song: Song,
    isCurrentSong: Boolean,
    accentColor: Color,
    showDuration: Boolean,
    onSongClick: () -> Unit,
    onRemoveClick: (() -> Unit)?
) {
    Surface(
        onClick = onSongClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        color = if (isCurrentSong) {
            accentColor.copy(alpha = 0.15f)
        } else {
            Color.Transparent
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            Card(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(6.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isCurrentSong) 4.dp else 2.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "Album art for ${song.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        error = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentSong) {
                        accentColor
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (showDuration && !isCurrentSong) {
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            text = formatDuration(song.duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Currently Playing Indicator or Remove Button
            if (isCurrentSong) {
                // Playing indicator
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Currently playing",
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            } else if (onRemoveClick != null) {
                // Remove button for non-current tracks
                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove from queue",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Empty queue state for modal.
 */
@Composable
private fun EmptyQueueState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.QueueMusic,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Queue is empty",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add songs to start playing",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Format duration in milliseconds to MM:SS format.
 */
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
