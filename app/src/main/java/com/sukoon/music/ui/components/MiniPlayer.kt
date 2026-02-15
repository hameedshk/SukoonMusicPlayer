package com.sukoon.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.ui.theme.MiniPlayerAlbumArtSize
import com.sukoon.music.ui.theme.MiniPlayerHeight
import com.sukoon.music.ui.theme.MiniPlayerShape
import com.sukoon.music.ui.theme.SpacingLarge
import com.sukoon.music.ui.theme.SpacingMedium
import androidx.compose.ui.graphics.Color
import com.sukoon.music.ui.util.candidateAccent
import com.sukoon.music.ui.util.AccentResolver
import com.sukoon.music.ui.util.rememberAlbumPalette
import com.sukoon.music.ui.theme.*

/**
 * Shimmer effect modifier for loading states.
 * Creates a horizontal gradient animation that sweeps across the component.
 */
private fun Modifier.shimmerEffect(): Modifier = composed {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.surfaceContainerHigh
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim - 500f, 0f),
            end = Offset(translateAnim, 0f)
        )
    )
}

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
    onSeek: (Long) -> Unit = {},
    userPreferences: com.sukoon.music.domain.model.UserPreferences = com.sukoon.music.domain.model.UserPreferences(),
    modifier: Modifier = Modifier
) {
    if (playbackState.currentSong == null) return

    val palette = rememberAlbumPalette(playbackState.currentSong.albumArtUri)
    val song = playbackState.currentSong!!

    // Use theme's primary color (respects accent profile setting changes in real-time)
    // This ensures MiniPlayer updates immediately when accent profile changes in settings
    val accentColor = MaterialTheme.colorScheme.primary


    // Real-time position tracking - optimized with derivedStateOf
    // Only updates offset, not full recomposition of MiniPlayer
    var positionOffset by remember { mutableLongStateOf(0L) }
    val currentPosition by remember {
        derivedStateOf { playbackState.currentPosition + positionOffset }
    }

    // Position ticker - 100ms for smooth real-time feedback
    LaunchedEffect(playbackState.isPlaying) {
        positionOffset = 0L
        if (playbackState.isPlaying) {
            while (isActive && (playbackState.currentPosition + positionOffset) < playbackState.duration) {
                delay(100) // 10 updates/sec for smooth visual feedback
                positionOffset += 100
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    // Swipe up (negative dragAmount) triggers full player
                    if (dragAmount < -80) {
                        onClick()
                    }
                }
            }
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MiniPlayerHeight)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
            ) {
            // Album Art with crossfade and loading shimmer
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(playbackState.currentSong.albumArtUri)
                    .crossfade(300)
                    .build(),
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(MiniPlayerAlbumArtSize)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shimmerEffect()
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )

            // Song Info with expand affordance
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = playbackState.currentSong.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = playbackState.currentSong.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    // Subtle chevron affordance (swipe up or tap to expand)
                    Icon(
                        imageVector = Icons.Default.ExpandLess,
                        contentDescription = "Tap or swipe up to expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier
                            .size(21.dp)
                            .padding(start = 8.dp)
                    )
                }
            }

            // Large Play/Pause Button with press feedback
            val playPauseInteractionSource = remember { MutableInteractionSource() }
            var isPlayPausePressed by remember { mutableStateOf(false) }

            LaunchedEffect(playPauseInteractionSource) {
                playPauseInteractionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> isPlayPausePressed = true
                        is PressInteraction.Release -> isPlayPausePressed = false
                        is PressInteraction.Cancel -> isPlayPausePressed = false
                    }
                }
            }

            val playPauseScale by animateFloatAsState(
                targetValue = if (isPlayPausePressed) 0.92f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessHigh
                ),
                label = "play_pause_scale"
            )

            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier
                    .size(40.dp)
                    .scale(playPauseScale),
                interactionSource = playPauseInteractionSource
            ) {
                AnimatedContent(
                    targetState = playbackState.isPlaying,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "play_pause_icon"
                ) { isPlaying ->
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Next Button - Material 3 minimum touch target 48dp
            IconButton(
                onClick = onNextClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = accentColor
                )
            }
        }

            // Interactive seekable progress bar - 3.5dp for visibility + tap to seek
            val progress = if (playbackState.duration > 0) {
                (currentPosition.toFloat() / playbackState.duration.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .pointerInput(playbackState.duration) {
                        detectTapGestures { tapOffset ->
                            if (playbackState.duration > 0) {
                                val seekProgress = (tapOffset.x / size.width).coerceIn(0f, 1f)
                                val seekPosition = (seekProgress * playbackState.duration).toLong()
                                onSeek(seekPosition)
                            }
                        }
                    }
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            ) {
                // Progress fill
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(accentColor)
                )
            }
        }
    }
}
