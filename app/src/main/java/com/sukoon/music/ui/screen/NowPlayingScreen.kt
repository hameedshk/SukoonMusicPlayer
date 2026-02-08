package com.sukoon.music.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.data.lyrics.LrcParser
import com.sukoon.music.domain.model.LyricLine
import com.sukoon.music.domain.model.LyricsState
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.RepeatMode
import com.sukoon.music.domain.model.Song
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.ui.components.DeleteConfirmationDialog
import com.sukoon.music.ui.components.SongContextMenu
import com.sukoon.music.ui.components.SongInfoDialog
import com.sukoon.music.ui.components.rememberShareHandler
import com.sukoon.music.ui.components.rememberSongMenuHandler
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.util.rememberAlbumPalette
import com.sukoon.music.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.draw.blur
import com.sukoon.music.ui.components.LiquidMeshBackground
import androidx.compose.animation.Crossfade
import com.sukoon.music.ui.theme.*

/**
 * Now Playing Screen - Full-screen best style music player.
 *
 * Features:
 * - Dynamic gradient background (extracted from album art)
 * - Large album art with rounded corners
 * - Song metadata with like button
 * - Interactive seek bar with real-time progress
 * - Art based playback controls
 * - Shuffle and repeat toggles
 * - Lyrics and queue tabs
 * - Smooth animations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onBackClick: () -> Unit,
    onNavigateToQueue: () -> Unit = {},
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToArtist: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Refresh playback state when screen becomes visible to get accurate position
    LaunchedEffect(Unit) {
        viewModel.playbackRepository.refreshPlaybackState()
    }

    // Get accent color from user profile (not dynamic from album art)
    val accentTokens = accent()
    val accentColor = accentTokens.primary
    val palette = rememberAlbumPalette(playbackState.currentSong?.albumArtUri)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Liquid Mesh Background - persists outside Crossfade
        key(playbackState.currentSong?.id) {
            LiquidMeshBackground(
                palette = palette,
                songId = playbackState.currentSong?.id,
                modifier = Modifier.fillMaxSize()
            )
        }

        Crossfade(
            targetState = playbackState.currentSong?.id,
            animationSpec = tween(durationMillis = 1000),
            label = "nowPlayingCrossfade"
        ) { songId ->
            Scaffold(
                containerColor = Color.Transparent
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
                        sliderColor = accentColor,
                        onBackClick = onBackClick,
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
                        onRepeatClick = {
                            viewModel.toggleRepeat()
                            val nextMode = when (playbackState.repeatMode) {
                                RepeatMode.OFF -> RepeatMode.ALL
                                RepeatMode.ALL -> RepeatMode.ONE
                                RepeatMode.ONE -> RepeatMode.OFF
                            }
                            val message = when (nextMode) {
                                RepeatMode.OFF -> "Repeat is off"
                                RepeatMode.ALL -> "All songs Repeat is on"
                                RepeatMode.ONE -> "Current song Repeat is on"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        },
                        onNavigateToQueue = onNavigateToQueue,
                        onNavigateToAlbum = onNavigateToAlbum,
                        onNavigateToArtist = onNavigateToArtist,
                        viewModel = viewModel
                    )
                } else {
                    EmptyNowPlayingState()
                }
            }
        }
        }
    }
}

/**
 * A. TopUtilityBar - Icon-only utility bar with collapse button and 3-dot menu.
 * Fixed height: 48dp, Icon size: 24dp, Touch target: 48dp
 */
@Composable
private fun TopUtilityBar(
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Collapse",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 1f)
            )
        }
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 1f)
            )
        }
    }
}

@Composable
private fun NowPlayingContent(
    playbackState: PlaybackState,
    accentColor: Color,
    sliderColor: Color = accentColor,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onLikeClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (Long) -> Unit,
    viewModel: HomeViewModel
) {
    val song = playbackState.currentSong ?: return

    // Real-time progress tracking using offset pattern
    var positionOffset by remember { mutableLongStateOf(0L) }
    val currentPosition by remember(playbackState.currentPosition) {
        derivedStateOf { (playbackState.currentPosition + positionOffset).coerceAtMost(playbackState.duration) }
    }

    // Seek state for slider interaction
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableLongStateOf(0L) }

    // Immersive mode state (hides controls temporarily)
    var isImmersiveMode by remember { mutableStateOf(false) }

    // Overlay states
    var showLyricsOverlay by remember { mutableStateOf(false) }
    var showSongContextMenu by remember { mutableStateOf(false) }
    var showInfoForSong by remember { mutableStateOf<Song?>(null) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }

    val context = LocalContext.current

    // Delete result launcher
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "Song deleted successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Delete cancelled", Toast.LENGTH_SHORT).show()
        }
        songToDelete = null
    }

    // Share handler
    val shareHandler = rememberShareHandler()

    // Song menu handler
    val songMenuHandler = rememberSongMenuHandler(
        playbackRepository = viewModel.playbackRepository,
        onNavigateToAlbum = onNavigateToAlbum,
        onNavigateToArtist = onNavigateToArtist,
        onShowSongInfo = { s -> showInfoForSong = s },
        onShowDeleteConfirmation = { s -> songToDelete = s },
        onToggleLike = { id, isLiked -> viewModel.toggleLike(id, isLiked) },
        onShare = shareHandler
    )

    // Lyrics state
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()

    // Fetch lyrics when song changes
    LaunchedEffect(song.id) {
        viewModel.fetchLyrics(song)
    }

    // Position ticker - restarts when playbackState.currentPosition changes (e.g., on repeat)
    LaunchedEffect(playbackState.isPlaying, playbackState.currentPosition) {
        positionOffset = 0L
        if (playbackState.isPlaying && !isSeeking) {
            while (isActive && (playbackState.currentPosition + positionOffset) < playbackState.duration) {
                delay(100)
                positionOffset += 100
            }
        }
    }

    // Auto-restore from immersive mode after 5 seconds
    LaunchedEffect(isImmersiveMode) {
        if (isImmersiveMode) {
            delay(5000)
            isImmersiveMode = false
        }
    }

    // Subtle screen entry fade
    var screenVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        screenVisible = true
    }
    val screenAlpha by animateFloatAsState(
        targetValue = if (screenVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "screen_entry"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp)
            .alpha(screenAlpha)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val topContentPadding = (maxHeight * 0.14f).coerceIn(76.dp, 118.dp)
            val albumToMetadataSpacing = (maxHeight * 0.07f).coerceIn(24.dp, 52.dp)
            val topIntroSpacing = if (maxHeight < 700.dp) 4.dp else 10.dp
            val albumArtWeight = when {
                maxHeight < 700.dp -> 0.40f
                maxHeight < 840.dp -> 0.43f
                else -> 0.45f
            }

            // Content uses adaptive spacing so controls remain balanced on small and tall screens.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = topContentPadding,
                        bottom = ContentBottomPadding
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(topIntroSpacing))

                // B. Album Art - Prominent, album-first design
                AlbumArtSection(
                    song = song,
                    onAlbumArtClick = { isImmersiveMode = !isImmersiveMode },
                    showLyricsOverlay = showLyricsOverlay,
                    lyricsState = lyricsState,
                    currentPosition = currentPosition,
                    accentColor = accentColor,
                    onNextClick = onNextClick,
                    onPreviousClick = onPreviousClick,
                    modifier = Modifier.weight(albumArtWeight)
                )

                Spacer(modifier = Modifier.height(albumToMetadataSpacing))

                // C. Track Metadata with animation (calm, subordinate)
                AnimatedVisibility(
                    visible = !isImmersiveMode,
                    enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) + slideInVertically(
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    ),
                    exit = fadeOut(animationSpec = tween(500, easing = FastOutSlowInEasing)) + slideOutVertically(
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    )
                ) {
                    TrackMetadataSection(
                        song = song,
                        onLikeClick = onLikeClick
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Control Layer - Directly on background (no container)
                AnimatedVisibility(
                    visible = !isImmersiveMode,
                    enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) + slideInVertically(
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    ),
                    exit = fadeOut(animationSpec = tween(500, easing = FastOutSlowInEasing)) + slideOutVertically(
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 0.dp, horizontal = 16.dp)
                    ) {
                        // Seek Bar
                        SeekBarSection(
                            currentPosition = currentPosition,
                            duration = playbackState.duration,
                            sliderColor = sliderColor,
                            isSeeking = isSeeking,
                            seekPosition = seekPosition,
                            onSeekStart = {
                                isSeeking = true
                                seekPosition = currentPosition
                            },
                            onSeekChange = { seekPosition = it },
                            onSeekEnd = {
                                onSeekTo(seekPosition)
                                isSeeking = false
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // D. Primary Playback Controls (Shuffle - Previous - Play - Next - Repeat)
                        PlaybackControlsSection(
                            playbackState = playbackState,
                            accentColor = accentColor,
                            onPreviousClick = onPreviousClick,
                            onPlayPauseClick = onPlayPauseClick,
                            onNextClick = onNextClick,
                            onShuffleClick = onShuffleClick,
                            onRepeatClick = onRepeatClick
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // E. Secondary Actions Section (Lyrics, Like, Share, Queue)
                AnimatedVisibility(
                    visible = !isImmersiveMode,
                    enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) + slideInVertically(
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    ),
                    exit = fadeOut(animationSpec = tween(500, easing = FastOutSlowInEasing)) + slideOutVertically(
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    )
                ) {
                    SecondaryActionsSection(
                        song = song,
                        playbackState = playbackState,
                        accentColor = accentColor,
                        onLyricsClick = { showLyricsOverlay = !showLyricsOverlay },
                        showLyricsOverlay = showLyricsOverlay,
                        onShareClick = { shareHandler(song) },
                        onQueueClick = onNavigateToQueue
                    )
                }
            }
        }

        // A. Pinned TopUtilityBar - Fixed position outside scrollable content
        TopUtilityBar(
            onBackClick = onBackClick,
            onMoreClick = { showSongContextMenu = true },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    // Song Context Menu (3-dot menu)
    if (showSongContextMenu) {
        SongContextMenu(
            song = song,
            menuHandler = songMenuHandler,
            onDismiss = { showSongContextMenu = false }
        )
    }

    // Song info dialog
    showInfoForSong?.let { infoSong ->
        SongInfoDialog(
            song = infoSong,
            onDismiss = { showInfoForSong = null }
        )
    }

    // Delete confirmation dialog
    songToDelete?.let { deleteSong ->
        DeleteConfirmationDialog(
            song = deleteSong,
            onConfirm = {
                when (val result = songMenuHandler.performDelete(deleteSong)) {
                    is DeleteHelper.DeleteResult.RequiresPermission -> {
                        deleteLauncher.launch(
                            IntentSenderRequest.Builder(result.intentSender).build()
                        )
                    }
                    is DeleteHelper.DeleteResult.Success -> {
                        Toast.makeText(context, "Song deleted successfully", Toast.LENGTH_SHORT).show()
                        songToDelete = null
                    }
                    is DeleteHelper.DeleteResult.Error -> {
                        Toast.makeText(context, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                        songToDelete = null
                    }
                }
            },
            onDismiss = { songToDelete = null }
        )
    }
}

@Composable
private fun AlbumArtSection(
    song: Song,
    onAlbumArtClick: () -> Unit,
    showLyricsOverlay: Boolean = false,
    lyricsState: LyricsState = LyricsState.NotFound,
    currentPosition: Long = 0L,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Track horizontal swipe for next/previous navigation
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }

    // Album art container - full-bleed, S-style presentation
    // Extends edge-to-edge with subtle blur effect on edges
    Box(
        modifier = modifier
            .fillMaxWidth()  // Full-bleed across screen width
            .aspectRatio(1f)
            .padding(horizontal = 24.dp)  // More breathing room
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                onClick = onAlbumArtClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        // Swipe left (negative): next track
                        if (horizontalDragOffset < -100f) {
                            onNextClick()
                        }
                        // Swipe right (positive): previous track
                        else if (horizontalDragOffset > 100f) {
                            onPreviousClick()
                        }
                        horizontalDragOffset = 0f
                    },
                    onDragCancel = {
                        horizontalDragOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        horizontalDragOffset += dragAmount
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Album Art Background
            SubcomposeAsyncImage(
                model = song.albumArtUri,
                contentDescription = "Album art for ${song.album}",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            strokeWidth = 3.dp
                        )
                    }
                },
                error = {
                    // Elegant neutral placeholder with soft gradient using surfaceVariant
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            )

            // Edge blur/fade effects - creates S-like sophisticated look
            // Subtle vignette that fades edges to background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f)
                            ),
                            radius = Float.POSITIVE_INFINITY,
                            center = androidx.compose.ui.geometry.Offset(
                                0.5f, 0.5f
                            )
                        )
                    )
            )

            // Subtle bottom gradient glow for depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.20f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // Lyrics Overlay
            if (showLyricsOverlay) {
                // Blurred background scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.60f)
                                )
                            )
                        )
                        .blur(14.dp)
                )

                // Foreground contrast scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f))
                )

                // Lyrics content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.8f)
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    LyricsOverlayContent(
                        lyricsState = lyricsState,
                        currentPosition = currentPosition,
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

/**
 * Lyrics overlay content - displayed on top of album art.
 * Compact design optimized for overlay visibility.
 */
@Composable
private fun LyricsOverlayContent(
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
                    color = MaterialTheme.colorScheme.onSurface,
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Loading lyrics...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
            }
        }

        is LyricsState.Success -> {
            val parsedLines = lyricsState.parsedLines
            if (parsedLines.isNotEmpty()) {
                // Synced lyrics with active line highlighting
                CompactSyncedLyricsView(
                    lines = parsedLines,
                    currentPosition = currentPosition,
                    accentColor = accentColor
                )
            } else if (!lyricsState.lyrics.plainLyrics.isNullOrBlank()) {
                // Plain lyrics without timestamps
                CompactPlainLyricsView(text = lyricsState.lyrics.plainLyrics)
            } else {
                // No lyrics available for the song
                CompactLyricsNotAvailable()
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
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Failed to load lyrics",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }

        is LyricsState.NotFound -> {
            CompactLyricsNotAvailable()
        }
    }
}

/**
 * Compact synced lyrics view for overlay.
 */
@Composable
private fun CompactSyncedLyricsView(
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
                scrollOffset = -80
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp)
    ) {
        itemsIndexed(
            items = lines,
            key = { index, _ -> index }
        ) { index, line ->
            val isActive = index == activeLine
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onSurface
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
 * Compact plain lyrics view for overlay.
 */
@Composable
private fun CompactPlainLyricsView(text: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(12.dp)
    ) {
        item {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.5f
            )
        }
    }
}

/**
 * Compact lyrics not available view for overlay.
 */
@Composable
private fun CompactLyricsNotAvailable() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No lyrics available",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )
    }
}

/**
 * C. TrackMetadataSection - Displays song title and artist name.
 * Title: 18sp SemiBold, lineHeight 22sp, 1 line with ellipsis
 * Artist: 14sp Normal, 70% opacity, 1 line with ellipsis
 * Internal spacing: 4dp (compact S-style)
 * Subtle scrim background ensures readability over any album art
 * Scrim: Semi-transparent dark (dark theme) or light (light theme)
 */
@Composable
private fun TrackMetadataSection(
    song: Song,
    onLikeClick: () -> Unit
) {
    // Metadata directly on screen background - calm and subordinate
    // Proper spacing to prevent visual overlap with album art
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 6.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Row: Song Title + Like Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Song Title - Left-aligned like Spotify
            Text(
                text = song.title.ifBlank { "Unknown Song" },
                style = MaterialTheme.typography.songTitleLarge.copy(
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 1f),
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )

            // Like Button - Right-aligned
            IconButton(
                onClick = onLikeClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (song.isLiked) "Unlike" else "Like",
                    tint = if (song.isLiked)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Artist - Left-aligned
        if (song.artist.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.80f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Album - Left-aligned
        if (song.album.isNotBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = song.album,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeekBarSection(
    currentPosition: Long,
    duration: Long,
    sliderColor: Color,
    isSeeking: Boolean,
    seekPosition: Long,
    onSeekStart: () -> Unit,
    onSeekChange: (Long) -> Unit,
    onSeekEnd: () -> Unit
) {
    // Progress bar: 4dp height, soft neutral inactive track, accent active portion
    // Thumb: 14dp soft circle, no shadow or scale animation
    // Use seekPosition when seeking, otherwise use currentPosition
    val displayPosition = if (isSeeking) seekPosition.toFloat() else currentPosition.toFloat()

    // Dedicated interaction source to isolate slider touch events
    val sliderInteractionSource = remember { MutableInteractionSource() }

    // Track active opacity boost during scrubbing
    val activeTrackOpacity by animateFloatAsState(
        targetValue = if (isSeeking) 1f else 0.9f,
        animationSpec = tween(durationMillis = 150),
        label = "active_track_opacity"
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Slider(
            value = displayPosition,
            onValueChange = {
                if (!isSeeking) onSeekStart()
                onSeekChange(it.toLong())
            },
            onValueChangeFinished = onSeekEnd,
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            interactionSource = sliderInteractionSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = SliderDefaults.colors(
                thumbColor = sliderColor,
                activeTrackColor = sliderColor.copy(alpha = activeTrackOpacity),
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(sliderColor, CircleShape)
                        .clip(CircleShape)
                )
            },
            track = { sliderState ->
                val fraction = (sliderState.value - sliderState.valueRange.start) /
                        (sliderState.valueRange.endInclusive - sliderState.valueRange.start)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(sliderColor.copy(alpha = activeTrackOpacity), RoundedCornerShape(2.dp))
                    )
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(displayPosition.toLong()),
                style = MaterialTheme.typography.compactLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.compactLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
        }
    }
}

/**
 * D. PlaybackControlsSection - Primary playback controls.
 * Layout: Shuffle : Previous : Play/Pause : Next : Repeat
 * Play/Pause dominates at center with 56dp touch target
 * Shuffle/Repeat: 44dp touch targets, 22dp icons
 * Previous/Next: 56dp touch targets, 40dp icons
 * Spacing: 12dp between controls for compact arrangement
 */
@Composable
private fun PlaybackControlsSection(
    playbackState: PlaybackState,
    accentColor: Color,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle Button (left)
        IconButton(
            onClick = onShuffleClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (playbackState.shuffleEnabled)
                    accentColor
                else
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Previous Button
        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f),
                modifier = Modifier.size(30.dp)
            )
        }

        // Play/Pause Button (center, focal point) - S-sized
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .size(64.dp)
                .background(accentColor, CircleShape)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.1f),
                    spotColor = Color.Black.copy(alpha = 0.15f)
                )
        ) {
            AnimatedContent(
                targetState = playbackState.isPlaying,
                transitionSpec = {
                    fadeIn(animationSpec = tween(140)) togetherWith
                            fadeOut(animationSpec = tween(140))
                },
                label = "play_pause_icon"
            ) { isPlaying ->
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Next Button
        IconButton(
            onClick = onNextClick,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f),
                modifier = Modifier.size(30.dp)
            )
        }

        // Repeat Button (right)
        IconButton(
            onClick = onRepeatClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = when (playbackState.repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                contentDescription = "Repeat",
                tint = when (playbackState.repeatMode) {
                    RepeatMode.OFF -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f)
                    else -> accentColor
                },
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * E. SecondaryActionsSection - Secondary actions (Lyrics, Like, Share, Queue).
 * Icon sizes: 20dp (minimal, de-emphasized)
 * Touch targets: 48dp (uniform, equal hierarchy)
 * Spacing: spacedBy(32.dp) for minimal, airy feel
 * Opacity: 0.6f default, 0.9f on active
 */
@Composable
private fun SecondaryActionsSection(
    song: Song,
    playbackState: PlaybackState,
    accentColor: Color,
    onLyricsClick: () -> Unit,
    showLyricsOverlay: Boolean,
    onShareClick: () -> Unit,
    onQueueClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lyrics Button
        IconButton(
            onClick = onLyricsClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lyrics,
                contentDescription = "Lyrics",
                tint = if (showLyricsOverlay) accentColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Share Button
        IconButton(
            onClick = onShareClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Queue Button
        IconButton(
            onClick = onQueueClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = "Queue",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun EmptyNowPlayingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = ContentTopPadding,
                bottom = ContentBottomPadding + 16.dp
            ),
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
    SukoonMusicPlayerTheme(theme = AppTheme.DARK) {
        val previewAccentTokens = accent()
        val previewAccentColor = previewAccentTokens.primary
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
            accentColor = previewAccentColor,
            sliderColor = previewAccentColor.copy(alpha = 0.6f), // Simulated desaturation for preview
            onBackClick = {},
            onPlayPauseClick = {},
            onNextClick = {},
            onPreviousClick = {},
            onSeekTo = {},
            onLikeClick = {},
            onShuffleClick = {},
            onRepeatClick = {},
            onNavigateToQueue = {},
            onNavigateToAlbum = {},
            onNavigateToArtist = {},
            viewModel = hiltViewModel()
        )
    }
}


