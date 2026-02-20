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
import com.sukoon.music.ui.theme.SpacingMedium
import androidx.compose.ui.graphics.Color
import com.sukoon.music.ui.theme.*
import androidx.compose.foundation.border
import com.sukoon.music.ui.theme.LocalIsAmoled

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
    val latestPlaybackState by rememberUpdatedState(playbackState)
    val latestOnPlayPauseClick by rememberUpdatedState(onPlayPauseClick)
    val latestOnNextClick by rememberUpdatedState(onNextClick)
    val latestOnClick by rememberUpdatedState(onClick)
    val latestOnSeek by rememberUpdatedState(onSeek)

    val memoizedContent = remember(playbackState.currentSong?.id, userPreferences, modifier) {
        @Composable {
            MiniPlayerContent(
                playbackState = latestPlaybackState,
                onPlayPauseClick = { latestOnPlayPauseClick() },
                onNextClick = { latestOnNextClick() },
                onClick = { latestOnClick() },
                onSeek = { latestOnSeek(it) },
                userPreferences = userPreferences,
                modifier = modifier
            )
        }
    }

    memoizedContent()
}

@Composable
private fun MiniPlayerProgressBar(
    playbackState: PlaybackState,
    accentColor: Color,
    onSeek: (Long) -> Unit
) {
    val playbackStateSnapshot by rememberUpdatedState(playbackState)

    // Keep ticker state scoped to the progress bar so row content is not recomposed every 100ms.
    var positionOffset by remember(playbackState.currentSong?.id) { mutableLongStateOf(0L) }
    val currentPosition by remember(playbackState.currentPosition, positionOffset, playbackState.duration) {
        derivedStateOf {
            val rawPosition = playbackState.currentPosition + positionOffset
            if (playbackState.duration > 0L) {
                rawPosition.coerceIn(0L, playbackState.duration)
            } else {
                rawPosition.coerceAtLeast(0L)
            }
        }
    }

    // Position ticker - updates every 100ms only when playing
    LaunchedEffect(playbackState.isPlaying, playbackState.currentSong?.id) {
        // Always reset offset when song changes
        positionOffset = 0L
        if (!playbackStateSnapshot.isPlaying) return@LaunchedEffect

        while (isActive) {
            delay(100)

            // Snapshot current state to avoid stale closure reads
            val snapshot = playbackStateSnapshot

            // Exit if state no longer supports ticking
            if (!snapshot.isPlaying || snapshot.isLoading || snapshot.error != null) {
                positionOffset = 0L
                return@LaunchedEffect
            }

            // Exit if reached or exceeded duration
            if (snapshot.duration <= 0L) {
                positionOffset = 0L
                return@LaunchedEffect
            }

            val nextPosition = snapshot.currentPosition + positionOffset + 100
            if (nextPosition >= snapshot.duration) {
                positionOffset = 0L // Will be reset by next song transition
                return@LaunchedEffect
            }

            // Safely increment offset
            positionOffset = (positionOffset + 100L).coerceAtMost(snapshot.duration - snapshot.currentPosition)
        }
    }

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

                        // Validate seek is within bounds before sending
                        if (seekPosition in 0L..playbackState.duration) {
                            onSeek(seekPosition)
                        }
                    }
                }
            }
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
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

@Composable
private fun MiniPlayerContent(
    playbackState: PlaybackState,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onClick: () -> Unit,
    onSeek: (Long) -> Unit = {},
    userPreferences: com.sukoon.music.domain.model.UserPreferences = com.sukoon.music.domain.model.UserPreferences(),
    modifier: Modifier = Modifier
) {
    MiniPlayerContentLayout(
        playbackState = playbackState,
        onPlayPauseClick = onPlayPauseClick,
        onNextClick = onNextClick,
        onClick = onClick,
        onSeek = onSeek,
        userPreferences = userPreferences,
        modifier = modifier
    )
}

@Composable
private fun MiniPlayerContentLayout(
    playbackState: PlaybackState,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onClick: () -> Unit,
    onSeek: (Long) -> Unit = {},
    userPreferences: com.sukoon.music.domain.model.UserPreferences = com.sukoon.music.domain.model.UserPreferences(),
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong ?: return

    // Use theme's primary color (respects accent profile setting changes in real-time)
    // This ensures MiniPlayer updates immediately when accent profile changes in settings
    val accentColor = MaterialTheme.colorScheme.primary

    // Frosted glass styling - theme-aware
    val isDarkTheme = MaterialTheme.colorScheme.onBackground.red > 0.5f
    val isAmoled = LocalIsAmoled.current

    val borderColor = when {
        isAmoled -> Color.White.copy(alpha = 0.08f)
        isDarkTheme -> Color.White.copy(alpha = 0.12f)
        else -> Color.Black.copy(alpha = 0.08f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(MiniPlayerShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, borderColor, MiniPlayerShape)
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
                    .data(song.albumArtUri)
                    .crossfade(300)
                    .build(),
                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_album_art),
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
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                        contentDescription = if (isPlaying) {
                            androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.now_playing_pause)
                        } else {
                            androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play)
                        },
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
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.now_playing_next),
                    tint = accentColor
                )
            }
        }

            MiniPlayerProgressBar(
                playbackState = playbackState,
                accentColor = accentColor,
                onSeek = onSeek
            )
        }
    }
}

