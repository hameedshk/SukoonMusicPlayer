package com.sukoon.music.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.data.lyrics.LrcParser
import com.sukoon.music.domain.model.LyricLine
import com.sukoon.music.domain.model.LyricsState
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.RepeatMode
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.LyricsModalSheet
import com.sukoon.music.ui.components.QueueModalSheet
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.util.accentColor
import com.sukoon.music.ui.util.rememberAlbumPalette
import com.sukoon.music.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Now Playing Screen - Full-screen Spotify-style music player.
 *
 * Features:
 * - Dynamic gradient background (extracted from album art)
 * - Large album art with rounded corners
 * - Song metadata with like button
 * - Interactive seek bar with real-time progress
 * - Spotify-green playback controls
 * - Shuffle and repeat toggles
 * - Lyrics and queue tabs
 * - Smooth animations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onBackClick: () -> Unit,
    onNavigateToQueue: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    // Queue modal state
    var showQueueModal by remember { mutableStateOf(false) }

    // Extract color palette from album art
    val palette = rememberAlbumPalette(playbackState.currentSong?.albumArtUri)
    val accentColor = palette.accentColor

    // Dynamic background gradient colors using album art palette
    // Subtle vertical gradient: lighter at top, darker at bottom
    val backgroundColor = MaterialTheme.colorScheme.background
    val gradientColors = listOf(
        accentColor.copy(alpha = 0.08f),  // Very subtle hint at top
        accentColor.copy(alpha = 0.12f),  // Slightly more visible in middle
        backgroundColor.copy(alpha = 0.95f)  // Darker at bottom
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = gradientColors,
                    startY = 0f,
                    endY = 1500f
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        // Queue button - opens modal
                        IconButton(onClick = { showQueueModal = true }) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "View Queue"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            // Swipe down gesture to collapse Now Playing screen
            var dragOffset by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                // If dragged down more than 150dp, collapse the screen
                                if (dragOffset > 150f) {
                                    onBackClick()
                                }
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                dragOffset = 0f
                            },
                            onVerticalDrag = { _, dragAmount ->
                                // Only allow downward drag (positive values)
                                if (dragAmount > 0) {
                                    dragOffset += dragAmount
                                }
                            }
                        )
                    }
            ) {
                if (playbackState.currentSong != null) {
                    NowPlayingContent(
                        playbackState = playbackState,
                        accentColor = accentColor,
                        onPlayPauseClick = { viewModel.playPause() },
                        onNextClick = { viewModel.seekToNext() },
                        onPreviousClick = { viewModel.seekToPrevious() },
                        onSeekTo = { position -> viewModel.seekTo(position) },
                        onLikeClick = {
                            playbackState.currentSong?.let { song ->
                                viewModel.toggleLike(song.id, song.isLiked)
                            }
                        },
                        onShuffleClick = { viewModel.toggleShuffle() },
                        onRepeatClick = { viewModel.toggleRepeat() },
                        viewModel = viewModel
                    )
                } else {
                    EmptyNowPlayingState()
                }
            }
        }

        // Queue Modal Bottom Sheet
        if (showQueueModal && playbackState.currentSong != null) {
            QueueModalSheet(
                queue = playbackState.queue,
                currentIndex = playbackState.currentQueueIndex,
                accentColor = accentColor,
                onDismiss = { showQueueModal = false },
                onSongClick = { index -> viewModel.jumpToQueueIndex(index) },
                onRemoveClick = { index -> viewModel.removeFromQueue(index) }
            )
        }
    }
}

@Composable
private fun NowPlayingContent(
    playbackState: PlaybackState,
    accentColor: Color,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onLikeClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    viewModel: HomeViewModel
) {
    val song = playbackState.currentSong ?: return

    // Real-time progress tracking
    var currentPosition by remember { mutableLongStateOf(playbackState.currentPosition) }

    // Immersive mode state (hides controls temporarily)
    var isImmersiveMode by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Lyrics modal state
    var showLyricsModal by remember { mutableStateOf(false) }

    LaunchedEffect(playbackState.isPlaying, playbackState.currentPosition) {
        currentPosition = playbackState.currentPosition
        if (playbackState.isPlaying) {
            while (isActive && currentPosition < playbackState.duration) {
                delay(100)
                currentPosition += 100
            }
        }
    }

    // Update position when playback state changes
    LaunchedEffect(playbackState.currentPosition) {
        currentPosition = playbackState.currentPosition
    }

    // Auto-restore from immersive mode after 3 seconds
    LaunchedEffect(isImmersiveMode) {
        if (isImmersiveMode) {
            delay(3000)
            isImmersiveMode = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Album Art with tap gesture for immersive mode
        AlbumArtSection(
            song = song,
            onAlbumArtClick = { isImmersiveMode = !isImmersiveMode }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Song Info and Like Button with animation
        AnimatedVisibility(
            visible = !isImmersiveMode,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            )
        ) {
            SongInfoSection(
                song = song,
                accentColor = accentColor,
                onLikeClick = onLikeClick
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Control Layer - Visually separated with subtle surface
        AnimatedVisibility(
            visible = !isImmersiveMode,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)
                ) {
                    // Seek Bar
                    SeekBarSection(
                        currentPosition = currentPosition,
                        duration = playbackState.duration,
                        accentColor = accentColor,
                        onSeekTo = onSeekTo
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Playback Controls
                    PlaybackControlsSection(
                        playbackState = playbackState,
                        accentColor = accentColor,
                        onShuffleClick = onShuffleClick,
                        onPreviousClick = onPreviousClick,
                        onPlayPauseClick = onPlayPauseClick,
                        onNextClick = onNextClick,
                        onRepeatClick = onRepeatClick
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary Action Row
                    SecondaryActionRow(
                        accentColor = accentColor,
                        onLyricsClick = { showLyricsModal = true },
                        onMoreClick = { /* TODO: Show overflow menu */ }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Lyrics/Queue Tabs Section
        AnimatedVisibility(
            visible = !isImmersiveMode,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            )
        ) {
            LyricsQueueSection(
                song = song,
                currentPosition = currentPosition,
                accentColor = accentColor,
                viewModel = viewModel,
                selectedTab = selectedTab,
                onTabChange = { selectedTab = it }
            )
        }
    }

    // Lyrics Modal Bottom Sheet
    if (showLyricsModal) {
        LyricsModalSheet(
            song = song,
            currentPosition = currentPosition,
            accentColor = accentColor,
            lyricsState = viewModel.lyricsState.collectAsStateWithLifecycle().value,
            onDismiss = { showLyricsModal = false },
            onOffsetAdjust = { offsetDelta ->
                val currentState = viewModel.lyricsState.value
                if (currentState is LyricsState.Success) {
                    val newOffset = currentState.lyrics.syncOffset + offsetDelta
                    viewModel.updateLyricsSyncOffset(song.id, newOffset)
                }
            }
        )
    }
}

@Composable
private fun AlbumArtSection(
    song: Song,
    onAlbumArtClick: () -> Unit
) {
    // Enhanced album art with deeper elevation for visual separation and tap gesture
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(horizontal = 16.dp)
            .clickable(
                onClick = onAlbumArtClick,
                indication = null,  // No ripple effect for cleaner immersive toggle
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = song.albumArtUri,
                contentDescription = "Album art for ${song.album}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                },
                error = {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            )
        }
    }
}

@Composable
private fun SongInfoSection(
    song: Song,
    accentColor: Color,
    onLikeClick: () -> Unit
) {
    // Animation state for like button
    var likeAnimationTrigger by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (likeAnimationTrigger) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        finishedListener = {
            if (likeAnimationTrigger) {
                likeAnimationTrigger = false
            }
        },
        label = "like_scale"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Song title - larger and bolder for visual hierarchy
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize * 1.05f
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Artist name - smaller and lower opacity for contrast
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Animated like button
        IconButton(
            onClick = {
                likeAnimationTrigger = true
                onLikeClick()
            }
        ) {
            Icon(
                imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (song.isLiked) "Unlike" else "Like",
                tint = if (song.isLiked) accentColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(32.dp)
                    .scale(scale)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeekBarSection(
    currentPosition: Long,
    duration: Long,
    accentColor: Color,
    onSeekTo: (Long) -> Unit
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Slider(
            value = currentPosition.toFloat(),
            onValueChange = { onSeekTo(it.toLong()) },
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp), // Increased touch target for better ergonomics
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f) // Improved contrast
            ),
            // Enhanced thumb for better visibility and grab-ability
            thumb = {
                Box(
                    modifier = Modifier
                        .size(16.dp) // Larger, more visible thumb
                        .background(accentColor, CircleShape)
                        .clip(CircleShape)
                )
            },
            // Thicker track for better visibility
            track = { sliderState ->
                val fraction = (sliderState.value - sliderState.valueRange.start) /
                        (sliderState.valueRange.endInclusive - sliderState.valueRange.start)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp) // Increased from default 2dp for better visibility
                        .clip(CircleShape)
                ) {
                    // Inactive track (remaining portion)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                                CircleShape
                            )
                    )
                    // Active track (played portion)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(accentColor, CircleShape)
                    )
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentPosition),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium // Better readability
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f) // Increased from 0.6f
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium // Better readability
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f) // Increased from 0.6f
            )
        }
    }
}

@Composable
private fun PlaybackControlsSection(
    playbackState: PlaybackState,
    accentColor: Color,
    onShuffleClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onRepeatClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp), // Padding for better spacing from edges
        horizontalArrangement = Arrangement.SpaceBetween, // Changed from SpaceEvenly for more controlled spacing
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle button - Tertiary control (smallest)
        IconButton(
            onClick = onShuffleClick,
            modifier = Modifier.size(48.dp) // Minimum touch target
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (playbackState.shuffleEnabled) {
                    accentColor // Clear ON state
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f) // Reduced from 0.5f for clearer OFF state
                },
                modifier = Modifier.size(24.dp) // Smaller icon for tertiary control
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Previous button - Secondary control (medium)
        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f), // Neutral, slightly subdued
                modifier = Modifier.size(42.dp) // Larger than tertiary, smaller than primary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Play/Pause button - PRIMARY control (largest, highest contrast)
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .size(76.dp) // Increased from 72dp for even more prominence
                .background(accentColor, CircleShape)
        ) {
            Icon(
                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                tint = Color.White, // Always pure white for maximum contrast on accent background
                modifier = Modifier.size(44.dp) // Largest icon
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Next button - Secondary control (medium)
        IconButton(
            onClick = onNextClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f), // Neutral, slightly subdued
                modifier = Modifier.size(42.dp) // Larger than tertiary, smaller than primary
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Repeat button - Tertiary control (smallest)
        IconButton(
            onClick = onRepeatClick,
            modifier = Modifier.size(48.dp) // Minimum touch target
        ) {
            Icon(
                imageVector = when (playbackState.repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                contentDescription = "Repeat",
                tint = when (playbackState.repeatMode) {
                    RepeatMode.OFF -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f) // Clear OFF state
                    RepeatMode.ONE -> accentColor.copy(alpha = 1.0f) // Full accent for Repeat One
                    RepeatMode.ALL -> accentColor.copy(alpha = 0.85f) // Slightly subdued for Repeat All (differentiation)
                },
                modifier = Modifier.size(24.dp) // Smaller icon for tertiary control
            )
        }
    }
}

/**
 * Secondary Action Row - Minimal, discoverable actions below main controls.
 *
 * Features only 2 actions to avoid clutter:
 * - Lyrics: Quick jump to lyrics tab
 * - More: Overflow menu for additional options
 *
 * Visually secondary with smaller size and lower opacity.
 */
@Composable
private fun SecondaryActionRow(
    accentColor: Color,
    onLyricsClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lyrics action - jumps to lyrics tab
        IconButton(
            onClick = onLyricsClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Article,
                contentDescription = "View Lyrics",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(32.dp))

        // More action - overflow menu
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More Options",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun LyricsQueueSection(
    song: Song,
    currentPosition: Long,
    accentColor: Color,
    viewModel: HomeViewModel,
    selectedTab: Int,
    onTabChange: (Int) -> Unit
) {
    val tabs = listOf("Lyrics", "Queue")
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()

    // Fetch lyrics when song changes
    LaunchedEffect(song.id) {
        viewModel.fetchLyrics(song)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = accentColor
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabChange(index) },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            when (selectedTab) {
                0 -> LyricsView(
                    lyricsState = lyricsState,
                    currentPosition = currentPosition,
                    accentColor = accentColor
                )
                1 -> QueueView(
                    queue = viewModel.playbackState.collectAsStateWithLifecycle().value.queue,
                    currentIndex = viewModel.playbackState.collectAsStateWithLifecycle().value.currentQueueIndex,
                    accentColor = accentColor,
                    onSongClick = { index -> viewModel.jumpToQueueIndex(index) },
                    onRemoveClick = { index -> viewModel.removeFromQueue(index) }
                )
            }
        }
    }
}

@Composable
private fun LyricsView(
    lyricsState: LyricsState,
    currentPosition: Long,
    accentColor: Color
) {
    when (lyricsState) {
        is LyricsState.Loading -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading lyrics...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        is LyricsState.Success -> {
            val parsedLines = lyricsState.parsedLines
            if (parsedLines.isNotEmpty()) {
                // Synced lyrics with active line highlighting
                SyncedLyricsView(
                    lines = parsedLines,
                    currentPosition = currentPosition,
                    accentColor = accentColor
                )
            } else if (!lyricsState.lyrics.plainLyrics.isNullOrBlank()) {
                // Plain lyrics without timestamps
                PlainLyricsView(text = lyricsState.lyrics.plainLyrics)
            } else {
                // No lyrics available
                LyricsNotAvailable()
            }
        }

        is LyricsState.Error -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Failed to load lyrics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lyricsState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }

        is LyricsState.NotFound -> {
            LyricsNotAvailable()
        }
    }
}

@Composable
private fun SyncedLyricsView(
    lines: List<LyricLine>,
    currentPosition: Long,
    accentColor: Color
) {
    val listState = rememberLazyListState()
    val activeLine = remember(currentPosition) {
        LrcParser.findActiveLine(lines, currentPosition)
    }

    // Auto-scroll to active line
    LaunchedEffect(activeLine) {
        if (activeLine >= 0 && activeLine < lines.size) {
            listState.animateScrollToItem(
                index = activeLine,
                scrollOffset = -100 // Center the active line
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp)
    ) {
        itemsIndexed(
            items = lines,
            key = { index, _ -> index }
        ) { index, line ->
            val isActive = index == activeLine
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isActive) {
                    accentColor // Dynamic accent color for active line
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                },
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PlainLyricsView(text: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.5f
            )
        }
    }
}

@Composable
private fun LyricsNotAvailable() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No lyrics available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Lyrics not found for this track",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun QueueView(
    queue: List<Song>,
    currentIndex: Int,
    accentColor: Color,
    onSongClick: (Int) -> Unit,
    onRemoveClick: (Int) -> Unit
) {
    if (queue.isEmpty()) {
        // Empty queue state
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Queue is empty",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add songs to start playing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }
    } else {
        // Queue list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(
                items = queue,
                key = { index, song -> "${song.id}_$index" }
            ) { index, song ->
                QueueSongItem(
                    song = song,
                    isCurrentSong = index == currentIndex,
                    accentColor = accentColor,
                    onSongClick = { onSongClick(index) },
                    onRemoveClick = { onRemoveClick(index) }
                )
            }
        }
    }
}

@Composable
private fun QueueSongItem(
    song: Song,
    isCurrentSong: Boolean,
    accentColor: Color,
    onSongClick: () -> Unit,
    onRemoveClick: () -> Unit
) {

    Surface(
        onClick = onSongClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = if (isCurrentSong) {
            accentColor.copy(alpha = 0.1f)
        } else {
            Color.Transparent
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            Card(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        error = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
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
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentSong) {
                        accentColor
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                    fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Currently Playing Indicator
            if (isCurrentSong) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Currently playing",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Remove Button
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove from queue",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyNowPlayingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No song playing",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select a song from your library to start playing",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
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

@Preview(showBackground = true)
@Composable
private fun NowPlayingScreenPreview() {
    SukoonMusicPlayerTheme(darkTheme = true) {
        NowPlayingContent(
            playbackState = PlaybackState(
                isPlaying = true,
                currentPosition = 95000,
                duration = 240000,
                shuffleEnabled = true,
                repeatMode = RepeatMode.ALL,
                currentSong = Song(
                    id = 1,
                    title = "Blinding Lights",
                    artist = "The Weeknd",
                    album = "After Hours",
                    duration = 240000,
                    uri = "",
                    albumArtUri = null,
                    dateAdded = 0,
                    isLiked = true
                )
            ),
            accentColor = MaterialTheme.colorScheme.primary, // Preview with theme primary color
            onPlayPauseClick = {},
            onNextClick = {},
            onPreviousClick = {},
            onSeekTo = {},
            onLikeClick = {},
            onShuffleClick = {},
            onRepeatClick = {},
            viewModel = hiltViewModel()
        )
    }
}
