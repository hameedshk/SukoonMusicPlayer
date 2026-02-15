package com.sukoon.music.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import coil.request.ImageRequest
import coil.size.Precision
import com.sukoon.music.data.lyrics.LrcParser
import com.sukoon.music.domain.model.LyricLine
import com.sukoon.music.domain.model.LyricsState
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.RepeatMode
import com.sukoon.music.domain.model.Song
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.ui.components.DeleteConfirmationDialog
import com.sukoon.music.ui.components.QueueModalSheet
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
 * NowPlaying Screen Spacing Constants
 *
 * IMPORTANT: These values are intentionally off the 8dp grid system.
 * They are derived from precise visual design measurements to achieve optimal
 * balance in the full-screen player layout. The spacing adapts to screen height
 * and creates proper visual hierarchy between album art, metadata, and controls.
 *
 * DO NOT replace these with standard spacing constants (SpacingSmall, etc.)
 * without extensive visual verification across multiple device sizes
 * (compact, medium, expanded height classes).
 */
private val NowPlayingTopPaddingMin = 76.dp
private val NowPlayingTopPaddingMax = 118.dp
private val NowPlayingAlbumToMetadataMin = 40.dp
private val NowPlayingAlbumToMetadataMax = 60.dp
private val NowPlayingMetadataToControlsCompact = 6.dp
private val NowPlayingMetadataToControlsRegular = 10.dp
private val NowPlayingSeekToControlsCompact = 10.dp
private val NowPlayingSeekToControlsRegular = 14.dp
private val NowPlayingControlsBottomCompact = 8.dp
private val NowPlayingControlsBottomRegular = 12.dp
private val NowPlayingControlsToSecondaryCompact = 0.dp
private val NowPlayingControlsToSecondaryRegular = 3.dp
private val NowPlayingMetadataTopPadding = 22.dp
private val NowPlayingMetadataBottomPadding = 6.dp
private val NowPlayingMetadataHorizontalPadding = 24.dp
private val NowPlayingMetadataActionLaneWidth = 56.dp
private val NowPlayingSliderTouchHeight = 44.dp
private val NowPlayingTitleFontSize = 22.sp
private val NowPlayingTitleLineHeight = 28.sp
private val NowPlayingArtistFontSize = 15.sp
private val NowPlayingAlbumFontSize = 13.sp
private val NowPlayingTimelineFontSize = 12.sp
private val NowPlayingHelperFontSize = 13.sp
private val NowPlayingLyricsFontSize = 14.sp

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

    // Refresh playback state when screen becomes visible to get accurate position
    LaunchedEffect(Unit) {
        viewModel.playbackRepository.refreshPlaybackState()
    }

    // Get accent color from user profile (not dynamic from album art)
    val accentTokens = accent()
    val accentColor = accentTokens.primary
    val palette = rememberAlbumPalette(playbackState.currentSong?.albumArtUri)
    val collapseThresholdPx = with(LocalDensity.current) { 96.dp.toPx() }

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
                    .pointerInput(collapseThresholdPx) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                // Collapse only after a deliberate downward drag distance.
                                if (dragOffset > collapseThresholdPx) {
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
                        onRepeatClick = { viewModel.toggleRepeat() },
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.22f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
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
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.98f)
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
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.98f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    // Overlay states
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showQueueModal by remember { mutableStateOf(false) }
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
            val topContentPadding = (maxHeight * 0.08f).coerceIn(56.dp, 84.dp)
            val albumHeight = (maxHeight * 0.46f).coerceIn(250.dp, 430.dp)
            val controlCardBottomPadding = if (maxHeight < 700.dp) 10.dp else 14.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = topContentPadding,
                        bottom = controlCardBottomPadding
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AlbumArtSection(
                    song = song,
                    onAlbumArtClick = {},
                    onNextClick = onNextClick,
                    onPreviousClick = onPreviousClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(albumHeight)
                )

                Spacer(modifier = Modifier.weight(1f))

                NowPlayingControlCard(
                    song = song,
                    playbackState = playbackState,
                    accentColor = accentColor,
                    sliderColor = sliderColor,
                    currentPosition = currentPosition,
                    isSeeking = isSeeking,
                    seekPosition = seekPosition,
                    onLikeClick = onLikeClick,
                    onSeekStart = {
                        isSeeking = true
                        seekPosition = currentPosition
                    },
                    onSeekChange = { seekPosition = it },
                    onSeekEnd = {
                        onSeekTo(seekPosition)
                        isSeeking = false
                    },
                    onPreviousClick = onPreviousClick,
                    onPlayPauseClick = onPlayPauseClick,
                    onNextClick = onNextClick,
                    onShuffleClick = onShuffleClick,
                    onRepeatClick = onRepeatClick,
                    onLyricsClick = { showLyricsSheet = true },
                    onQueueClick = {
                        showQueueModal = true
                        onNavigateToQueue()
                    },
                    onShareClick = { shareHandler(song) },
                    isLyricsActive = showLyricsSheet
                )
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
    if (showQueueModal) {
        QueueModalSheet(
            queue = playbackState.queue,
            currentIndex = playbackState.currentQueueIndex,
            accentColor = accentColor,
            onDismiss = { showQueueModal = false },
            onSongClick = { index -> viewModel.jumpToQueueIndex(index) },
            onRemoveClick = { index -> viewModel.removeFromQueue(index) }
        )
    }

    if (showLyricsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLyricsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 6.dp,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            LyricsBottomSheetContent(
                lyricsState = lyricsState,
                currentPosition = currentPosition,
                accentColor = accentColor
            )
        }
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
private fun NowPlayingControlCard(
    song: Song,
    playbackState: PlaybackState,
    accentColor: Color,
    sliderColor: Color,
    currentPosition: Long,
    isSeeking: Boolean,
    seekPosition: Long,
    onLikeClick: () -> Unit,
    onSeekStart: () -> Unit,
    onSeekChange: (Long) -> Unit,
    onSeekEnd: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit,
    onShareClick: () -> Unit,
    isLyricsActive: Boolean
) {
    val cardContainerColor = if (MaterialTheme.colorScheme.background.red > 0.5f) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
        color = cardContainerColor,
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            TrackMetadataSection(
                song = song,
                onLikeClick = onLikeClick
            )

            SeekBarSection(
                currentPosition = currentPosition,
                duration = playbackState.duration,
                sliderColor = sliderColor,
                isSeeking = isSeeking,
                seekPosition = seekPosition,
                onSeekStart = onSeekStart,
                onSeekChange = onSeekChange,
                onSeekEnd = onSeekEnd
            )

            Spacer(modifier = Modifier.height(14.dp))

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

            SecondaryActionsSection(
                accentColor = accentColor,
                onLyricsClick = onLyricsClick,
                isLyricsActive = isLyricsActive,
                onShareClick = onShareClick,
                onQueueClick = onQueueClick
            )
        }
    }
}

@Composable
private fun AlbumArtSection(
    song: Song,
    onAlbumArtClick: () -> Unit,
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Track whether album art loaded successfully
    var hasAlbumArt by remember { mutableStateOf(false) }
    // Track horizontal swipe for next/previous navigation
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    val context = LocalContext.current
    val albumArtRequest = remember(song.id, song.albumArtUri) {
        ImageRequest.Builder(context)
            .data(song.albumArtUri)
            .crossfade(220)
            .allowHardware(true)
            .precision(Precision.EXACT)
            .memoryCacheKey("now_playing_album_${song.id}")
            .diskCacheKey(song.albumArtUri?.toString() ?: "song_${song.id}")
            .build()
    }

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
            .pointerInput(swipeThresholdPx) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        // Swipe left (negative): next track
                        if (horizontalDragOffset < -swipeThresholdPx) {
                            onNextClick()
                        }
                        // Swipe right (positive): previous track
                        else if (horizontalDragOffset > swipeThresholdPx) {
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
                model = albumArtRequest,
                contentDescription = "Album art for ${song.album}",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                loading = {
                    hasAlbumArt = false
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
                    hasAlbumArt = false
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                },
                onSuccess = { hasAlbumArt = true }
            )

            // Only apply scrim effects when actual album art is loaded
            if (hasAlbumArt) {
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
            }

        }
    }
}

/**
 * Lyrics content rendered inside a modal bottom sheet.
 */
@Composable
private fun LyricsBottomSheetContent(
    lyricsState: LyricsState,
    currentPosition: Long,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.86f)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Lyrics",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxSize()) {
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
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = NowPlayingHelperFontSize
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                }

                is LyricsState.Success -> {
                    val parsedLines = lyricsState.parsedLines
                    if (parsedLines.isNotEmpty()) {
                        CompactSyncedLyricsView(
                            lines = parsedLines,
                            currentPosition = currentPosition,
                            accentColor = accentColor
                        )
                    } else if (!lyricsState.lyrics.plainLyrics.isNullOrBlank()) {
                        CompactPlainLyricsView(text = lyricsState.lyrics.plainLyrics)
                    } else {
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
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = NowPlayingHelperFontSize
                            ),
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
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = NowPlayingLyricsFontSize
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
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
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = NowPlayingHelperFontSize
            ),
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
    val likeStateDescription = if (song.isLiked) "Liked" else "Not liked"

    // Metadata directly on screen background - calm and subordinate
    // Proper spacing to prevent visual overlap with album art
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 0.dp,
                bottom = 8.dp
            ),
        horizontalAlignment = Alignment.Start
    ) {
        // Spotify-style metadata: text stack on left, like/favorite pinned right.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = song.title.ifBlank { "Unknown Song" },
                    style = MaterialTheme.typography.songTitleLarge.copy(
                        fontSize = NowPlayingTitleFontSize,
                        lineHeight = NowPlayingTitleLineHeight,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 1f),
                    textAlign = TextAlign.Start
                )

                if (song.artist.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = NowPlayingArtistFontSize,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.80f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }

                if (song.album.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.album,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = NowPlayingAlbumFontSize
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
            }

            Box(
                modifier = Modifier.width(NowPlayingMetadataActionLaneWidth),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Favorite"
                            stateDescription = likeStateDescription
                        }
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
                .height(NowPlayingSliderTouchHeight),
            colors = SliderDefaults.colors(
                thumbColor = sliderColor,
                activeTrackColor = sliderColor.copy(alpha = activeTrackOpacity),
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
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
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
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
                style = MaterialTheme.typography.compactLabel.copy(
                    fontSize = NowPlayingTimelineFontSize,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.64f)
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.compactLabel.copy(
                    fontSize = NowPlayingTimelineFontSize,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.64f)
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
    val shuffleStateDescription = if (playbackState.shuffleEnabled) "On" else "Off"
    val repeatStateDescription = when (playbackState.repeatMode) {
        RepeatMode.OFF -> "Off"
        RepeatMode.ALL -> "All songs"
        RepeatMode.ONE -> "Current song"
    }

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
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = "Shuffle"
                    stateDescription = shuffleStateDescription
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (playbackState.shuffleEnabled)
                        accentColor
                    else
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f),
                    modifier = Modifier.size(22.dp)
                )

                if (playbackState.shuffleEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 8.dp)
                            .size(4.dp)
                            .background(accentColor, CircleShape)
                    )
                }
            }
        }

        // Previous Button
        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
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
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                modifier = Modifier.size(30.dp)
            )
        }

        // Repeat Button (right)
        IconButton(
            onClick = onRepeatClick,
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = "Repeat"
                    stateDescription = repeatStateDescription
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when (playbackState.repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = when (playbackState.repeatMode) {
                        RepeatMode.OFF -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f)
                        else -> accentColor
                    },
                    modifier = Modifier.size(22.dp)
                )

                if (playbackState.repeatMode != RepeatMode.OFF) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 8.dp)
                            .size(4.dp)
                            .background(accentColor, CircleShape)
                    )
                }
            }
        }
    }
}

/**
 * E. SecondaryActionsSection - Secondary actions with labels.
 */
@Composable
private fun SecondaryActionsSection(
    accentColor: Color,
    onLyricsClick: () -> Unit,
    isLyricsActive: Boolean,
    onShareClick: () -> Unit,
    onQueueClick: () -> Unit
) {
    val lyricsStateDescription = if (isLyricsActive) "Shown" else "Hidden"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        ActionWithLabel(
            label = "Lyrics",
            icon = Icons.Default.Lyrics,
            tint = if (isLyricsActive) accentColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            onClick = onLyricsClick,
            modifier = Modifier
                .weight(1f)
                .semantics {
                    role = Role.Button
                    contentDescription = "Lyrics"
                    stateDescription = lyricsStateDescription
                }
        )

        ActionWithLabel(
            label = "Queue",
            icon = Icons.Default.QueueMusic,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            onClick = onQueueClick,
            modifier = Modifier.weight(1f)
        )

        ActionWithLabel(
            label = "Share",
            icon = Icons.Default.Share,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            onClick = onShareClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionWithLabel(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint.copy(alpha = 0.95f)
        )
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



