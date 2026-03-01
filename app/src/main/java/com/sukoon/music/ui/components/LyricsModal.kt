package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sukoon.music.R
import com.sukoon.music.data.lyrics.LrcParser
import com.sukoon.music.domain.model.LyricLine
import com.sukoon.music.domain.model.LyricsState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.theme.*

/**
 * Lyrics Modal Bottom Sheet - Full-screen overlay for lyrics display.
 *
 * Features:
 * - Modal bottom sheet with drag-to-dismiss
 * - All lyrics states (loading, synced, unsynced, error, not found)
 * - Synced lyrics with active line highlighting
 * - Auto-scroll to keep active line centered
 * - Manual offset adjustment controls
 * - Themed, non-intrusive design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsModalSheet(
    song: Song,
    currentPosition: Long,
    accentColor: Color,
    lyricsState: LyricsState,
    onDismiss: () -> Unit,
    onOffsetAdjust: (Long) -> Unit,
    onRetry: () -> Unit = {}
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
                .fillMaxHeight(0.9f)
                .padding(horizontal = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.lyrics_modal_title),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.lyrics_modal_song_metadata, song.title, song.artist),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.lyrics_modal_close_content_description),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lyrics Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LyricsModalContent(
                    lyricsState = lyricsState,
                    currentPosition = currentPosition,
                    accentColor = accentColor,
                    onOffsetAdjust = onOffsetAdjust,
                    onRetry = onRetry
                )
            }
        }
    }
}

/**
 * Content area for the lyrics modal - handles all UI states.
 */
@Composable
private fun LyricsModalContent(
    lyricsState: LyricsState,
    currentPosition: Long,
    accentColor: Color,
    onOffsetAdjust: (Long) -> Unit,
    onRetry: () -> Unit
) {
    when (lyricsState) {
        is LyricsState.Loading -> {
            LyricsLoadingState(accentColor = accentColor)
        }

        is LyricsState.Success -> {
            val parsedLines = lyricsState.parsedLines
            if (parsedLines.isNotEmpty()) {
                // Synced lyrics with offset controls
                Column(modifier = Modifier.fillMaxSize()) {
                    // Offset adjustment controls
                    SyncOffsetControls(
                        currentOffset = lyricsState.lyrics.syncOffset,
                        accentColor = accentColor,
                        onOffsetAdjust = onOffsetAdjust
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Synced lyrics display
                    ModalSyncedLyricsView(
                        lines = parsedLines,
                        currentPosition = currentPosition,
                        accentColor = accentColor
                    )
                }
            } else if (!lyricsState.lyrics.plainLyrics.isNullOrBlank()) {
                // Unsynced lyrics
                Column(modifier = Modifier.fillMaxSize()) {
                    // Unsynced indicator
                    UnsyncedLyricsIndicator()

                    Spacer(modifier = Modifier.height(16.dp))

                    // Plain lyrics display
                    ModalPlainLyricsView(text = lyricsState.lyrics.plainLyrics)
                }
            } else {
                // No lyrics available
                LyricsNotFoundState(onRetry = onRetry)
            }
        }

        is LyricsState.Error -> {
            LyricsErrorState(message = lyricsState.message, onRetry = onRetry)
        }

        is LyricsState.NotFound -> {
            LyricsNotFoundState(onRetry = onRetry)
        }
    }
}

/**
 * Loading state for lyrics modal.
 */
@Composable
private fun LyricsLoadingState(accentColor: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = accentColor,
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.lyrics_modal_loading_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.lyrics_modal_loading_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Error state for lyrics modal.
 */
@Composable
private fun LyricsErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.lyrics_modal_error_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.lyrics_modal_retry))
        }
    }
}

/**
 * Not found state for lyrics modal.
 */
@Composable
private fun LyricsNotFoundState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.lyrics_modal_not_found_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.lyrics_modal_not_found_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.lyrics_modal_not_found_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(onClick = onRetry) {
            Text(text = stringResource(R.string.lyrics_modal_retry))
        }
    }
}

/**
 * Sync offset adjustment controls.
 */
@Composable
private fun SyncOffsetControls(
    currentOffset: Long,
    accentColor: Color,
    onOffsetAdjust: (Long) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.lyrics_modal_sync_adjustment),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Text(
                    text = stringResource(
                        R.string.lyrics_modal_sync_offset_value,
                        if (currentOffset >= 0) "+" else "",
                        currentOffset
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lyrics too fast button (move timestamps forward)
                OutlinedButton(
                    onClick = { onOffsetAdjust(100) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.lyrics_modal_too_fast),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Reset button
                OutlinedButton(
                    onClick = { onOffsetAdjust(-currentOffset) },
                    modifier = Modifier.weight(0.6f),
                    enabled = currentOffset != 0L,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Lyrics too slow button (move timestamps backward)
                OutlinedButton(
                    onClick = { onOffsetAdjust(-100) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.lyrics_modal_too_slow),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

/**
 * Unsynced lyrics indicator.
 */
@Composable
private fun UnsyncedLyricsIndicator() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.lyrics_modal_unsynced_indicator),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Synced lyrics view for the modal with improved auto-scroll.
 */
@Composable
private fun ModalSyncedLyricsView(
    lines: List<LyricLine>,
    currentPosition: Long,
    accentColor: Color
) {
    val listState = rememberLazyListState()

    // Use derivedStateOf with binary search for optimal performance
    // Only recalculates when currentPosition actually changes the active line
    val activeLine by remember {
        derivedStateOf {
            LrcParser.findActiveLine(lines, currentPosition)
        }
    }

    // Smooth auto-scroll to keep active line near center
    // Debounced automatically via activeLine changes
    LaunchedEffect(activeLine) {
        if (activeLine >= 0 && activeLine < lines.size) {
            // Smooth scroll with offset to center the active line
            val targetIndex = (activeLine - 1).coerceAtLeast(0)
            listState.animateScrollToItem(
                index = targetIndex,
                scrollOffset = -200 // Offset to keep active line near center
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 16.dp)
    ) {
        itemsIndexed(
            items = lines,
            key = { _, line -> line.timestamp } // Stable key based on timestamp
        ) { index, line ->
            val isActive = index == activeLine
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = if (isActive) {
                        MaterialTheme.typography.bodyLarge.fontSize * 1.1f
                    } else {
                        MaterialTheme.typography.bodyLarge.fontSize
                    }
                ),
                color = if (isActive) {
                    accentColor
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                },
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Plain lyrics view for the modal.
 */
@Composable
private fun ModalPlainLyricsView(text: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 16.dp)
    ) {
        item {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.6f
            )
        }
    }
}
