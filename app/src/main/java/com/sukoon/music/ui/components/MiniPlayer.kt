package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.ui.theme.DarkCard
import com.sukoon.music.ui.theme.DarkCardElevated
import com.sukoon.music.ui.theme.MiniPlayerAlbumArtSize
import com.sukoon.music.ui.theme.MiniPlayerHeight
import com.sukoon.music.ui.theme.MiniPlayerShape
import com.sukoon.music.ui.theme.SpacingLarge
import com.sukoon.music.ui.theme.SpacingMedium
import com.sukoon.music.ui.theme.TextPrimary
import com.sukoon.music.ui.theme.TextSecondary
import com.sukoon.music.ui.util.accentColor
import com.sukoon.music.ui.util.rememberAlbumPalette

/**
 * Redesigned Mini Player component for global use.
 * Ported and improved from RedesignedMiniPlayer in HomeScreen.
 *
 * Features:
 * - Album art (56dp rounded)
 * - Song title + artist (truncated)
 * - Large, centered Play/Pause button with dynamic accent color
 * - Skip next button
 * - Clickable area to open Now Playing
 */
@Composable
fun MiniPlayer(
    playbackState: PlaybackState,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (playbackState.currentSong == null) return

    val palette = rememberAlbumPalette(playbackState.currentSong.albumArtUri)
    val accentColor = palette.accentColor

    // Real-time position tracking
    var currentPosition by remember { mutableLongStateOf(playbackState.currentPosition) }

    LaunchedEffect(playbackState.isPlaying, playbackState.currentPosition) {
        currentPosition = playbackState.currentPosition
        if (playbackState.isPlaying) {
            while (isActive && currentPosition < playbackState.duration) {
                delay(100) // Update every 100ms
                currentPosition += 100
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingLarge, vertical = SpacingMedium)
            .clickable(onClick = onClick),
        shape = MiniPlayerShape,
        color = DarkCard
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MiniPlayerHeight)
                    .padding(SpacingMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
            ) {
            // Album Art
            SubcomposeAsyncImage(
                model = playbackState.currentSong.albumArtUri,
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(MiniPlayerAlbumArtSize)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkCardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )

            // Song Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = playbackState.currentSong.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playbackState.currentSong.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Large Play/Pause Button
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Next Button
            IconButton(
                onClick = onNextClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = TextPrimary
                )
            }
        }

            // Thin progress bar at bottom edge
            LinearProgressIndicator(
                progress = {
                    if (playbackState.duration > 0) {
                        (currentPosition.toFloat() / playbackState.duration.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
            )
        }
    }
}
