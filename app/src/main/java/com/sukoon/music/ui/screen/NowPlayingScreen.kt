package com.sukoon.music.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sukoon.music.R
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
// Album Art
private val NowPlayingAlbumArtRoundedCorners = 16.dp
private val NowPlayingAlbumArtHorizontalPadding = 24.dp
private val NowPlayingAlbumArtShadow = 12.dp

// Spacing
private val NowPlayingTopPaddingMin = 0.dp
private val NowPlayingTopPaddingMax = 2.dp
private val NowPlayingAlbumToMetadataMin = 12.dp
private val NowPlayingAlbumToMetadataMax = 24.dp
private val NowPlayingMetadataToControlsCompact = 2.dp
private val NowPlayingMetadataToControlsRegular = 4.dp
private val NowPlayingSeekToControlsCompact = 6.dp
private val NowPlayingSeekToControlsRegular = 8.dp
private val NowPlayingControlsBottomCompact = 6.dp
private val NowPlayingControlsBottomRegular = 6.dp
private val NowPlayingControlsToSecondaryCompact = 0.dp
private val NowPlayingControlsToSecondaryRegular = 0.dp
private val NowPlayingMetadataTopPadding = 4.dp
private val NowPlayingMetadataBottomPadding = 0.dp
private val NowPlayingMetadataHorizontalPadding = 24.dp
private val NowPlayingMetadataActionLaneWidth = 56.dp

// Seek Bar
private val NowPlayingSliderTouchHeight = 44.dp
private val NowPlayingSeekBarTrackHeight = 2.dp
private val NowPlayingSeekBarActiveHeight = 3.dp
private val NowPlayingSeekBarThumbMinSize = 8.dp
private val NowPlayingSeekBarThumbMaxSize = 14.dp

// Typography
private val NowPlayingTitleFontSize = 22.sp
private val NowPlayingTitleLineHeight = 28.sp
private val NowPlayingArtistFontSize = 15.sp
private val NowPlayingTimelineFontSize = 11.sp
private val NowPlayingHelperFontSize = 13.sp
private val NowPlayingLyricsFontSize = 14.sp
private val NowPlayingLyricsActiveFontSize = 26.sp
private val NowPlayingLyricsInactiveFontSize = 18.sp
private val NowPlayingLyricsLineSpacing = 20.dp

// Playback Controls
private val NowPlayingPlayButtonSize = 68.dp
private val NowPlayingPlayButtonIconSize = 36.dp
private val NowPlayingSkipButtonSize = 52.dp
private val NowPlayingSkipButtonIconSize = 32.dp
private val NowPlayingToggleButtonSize = 48.dp
private val NowPlayingToggleButtonIconSize = 22.dp

// Animations
private val NowPlayingSongCrossfadeDuration = 800
private val NowPlayingLikeBounceDuration = 300
private val NowPlayingScreenEntryDuration = 500

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
        // Liquid Mesh Background - animated aurora with smooth color transitions
        LiquidMeshBackground(
            palette = palette,
            songId = playbackState.currentSong?.id,
            isPlaying = playbackState.isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        // Premium blur/overlay effect for polish
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(alpha = 0.06f)
                )
        )

        // Content without parent-level Crossfade (transitions moved to individual components)
        Scaffold(
                containerColor = Color.Transparent
            ) { paddingValues ->
            // Swipe down gesture to collapse Now Playing screen
            var dragOffset by remember { mutableFloatStateOf(0f) }

            val density = LocalDensity.current
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .offset(y = with(density) { dragOffset.toDp() })
                    .alpha(1f - (dragOffset / (collapseThresholdPx * 2f)).coerceIn(0f, 0.3f))
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

/**
 * A. TopUtilityBar - Icon-only utility bar with collapse button and 3-dot menu.
 * Fixed height: 48dp, Icon size: 24dp, Touch target: 48dp
 */
@Composable
private fun TopUtilityBar(
    song: Song,
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = stringResource(R.string.now_playing_collapse),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 1f)
            )
        }

        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.now_playing_more_options),
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

    // Modal states
    var showLyricsModal by remember { mutableStateOf(false) }
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

    var showQuickActionMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha)
    ) {
        val statusBarTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val topBarOffset = (statusBarTopInset - 8.dp).coerceAtLeast(0.dp)

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val topBarHeight = 44.dp
            val topContentPadding = 0.dp
            val albumToMetadataSpacing = (maxHeight * 0.03f).coerceIn(NowPlayingAlbumToMetadataMin, NowPlayingAlbumToMetadataMax)
            val metadataToControlsSpacing = if (maxHeight < 700.dp) 8.dp else 12.dp
            val seekToPrimaryControlsSpacing = if (maxHeight < 700.dp) NowPlayingSeekToControlsCompact else NowPlayingSeekToControlsRegular
            val primaryControlsBottomSpacing = if (maxHeight < 700.dp) NowPlayingControlsBottomCompact else NowPlayingControlsBottomRegular
            val controlsToSecondarySpacing = if (maxHeight < 700.dp) NowPlayingControlsToSecondaryCompact else NowPlayingControlsToSecondaryRegular
            val albumArtWeight = when {
                maxHeight < 700.dp -> 0.52f
                maxHeight < 840.dp -> 0.56f
                else -> 0.60f
            }

            // Content uses adaptive spacing so controls remain balanced on small and tall screens.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(
                        top = topBarHeight + topContentPadding,
                        bottom = ContentBottomPadding
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // B. Album Art - Prominent, album-first design
                AlbumArtSection(
                    song = song,
                    onAlbumArtClick = { isImmersiveMode = !isImmersiveMode },
                    showLyricsModal = showLyricsModal,
                    lyricsState = lyricsState,
                    currentPosition = currentPosition,
                    accentColor = accentColor,
                    onNextClick = onNextClick,
                    onPreviousClick = onPreviousClick,
                    onLikeClick = {
                        playbackState.currentSong?.let { s ->
                            onLikeClick()
                        }
                    },
                    modifier = Modifier.weight(albumArtWeight)
                )

                Spacer(modifier = Modifier.height(albumToMetadataSpacing))

                // Top scrim gradient for metadata readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.08f)
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )

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

                Spacer(modifier = Modifier.height(metadataToControlsSpacing))

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
                        modifier = Modifier.padding(vertical = 0.dp, horizontal = 24.dp)
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

                        Spacer(modifier = Modifier.height(seekToPrimaryControlsSpacing))

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

                        Spacer(modifier = Modifier.height(primaryControlsBottomSpacing))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom scrim gradient for secondary actions readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )

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
                        onLyricsClick = { showLyricsModal = true },
                        onShareClick = { shareHandler(song) },
                        onQueueClick = { showQueueModal = true }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Floating quick action button
                    FloatingActionButton(
                        onClick = { showQuickActionMenu = true },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 8.dp),
                        containerColor = accentColor.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = "Add to playlist",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
        }
        }

        // A. Pinned TopUtilityBar - Fixed position outside scrollable content
        TopUtilityBar(
            song = song,
            onBackClick = onBackClick,
            onMoreClick = { showSongContextMenu = true },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topBarOffset)
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

    // Lyrics Modal Sheet
    if (showLyricsModal) {
        LyricsModalSheet(
            song = song,
            lyricsState = lyricsState,
            currentPosition = currentPosition,
            accentColor = accentColor,
            onDismiss = { showLyricsModal = false },
            onSeekTo = { position -> onSeekTo(position) }
        )
    }
}

@Composable
private fun AlbumArtSection(
    song: Song,
    onAlbumArtClick: () -> Unit,
    showLyricsModal: Boolean = false,
    lyricsState: LyricsState = LyricsState.NotFound,
    currentPosition: Long = 0L,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onLikeClick: () -> Unit = {}
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

    // Album art container - floating with rounded corners and shadow
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NowPlayingAlbumArtHorizontalPadding)
    ) {
        // Crossfade on song change
        AnimatedContent(
            targetState = song.id,
            transitionSpec = {
                fadeIn(animationSpec = tween(NowPlayingSongCrossfadeDuration)) togetherWith
                        fadeOut(animationSpec = tween(NowPlayingSongCrossfadeDuration))
            },
            label = "album_art_crossfade"
        ) { songId ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(NowPlayingAlbumArtShadow, RoundedCornerShape(NowPlayingAlbumArtRoundedCorners))
                    .clip(RoundedCornerShape(NowPlayingAlbumArtRoundedCorners))
                    .pointerInput(swipeThresholdPx) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                // Swipe left (negative): next track
                                if (horizontalDragOffset < -swipeThresholdPx) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onNextClick()
                                }
                                // Swipe right (positive): previous track
                                else if (horizontalDragOffset > swipeThresholdPx) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onAlbumArtClick() },
                            onDoubleTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLikeClick()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Album Art Image with gradient overlay
                Box(modifier = Modifier.fillMaxSize()) {
                    SubcomposeAsyncImage(
                        model = albumArtRequest,
                        contentDescription = stringResource(R.string.now_playing_album_art, song.title),
                        modifier = Modifier.fillMaxSize(),
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

                    // Subtle gradient overlay for premium look
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.15f)
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
}


/**
 * C. TrackMetadataSection - Displays song title and artist name with like bounce.
 * Title: 22sp Bold with marquee for long titles
 * Artist: 15sp, 75% opacity
 * Like button with spring bounce animation (1.0 → 1.3 → 1.0)
 * Crossfade on song change
 */
@Composable
private fun TrackMetadataSection(
    song: Song,
    onLikeClick: () -> Unit
) {
    val likeStateDescription = if (song.isLiked) stringResource(R.string.now_playing_liked) else stringResource(R.string.now_playing_not_liked)

    // Crossfade on song change
    AnimatedContent(
        targetState = song.id,
        transitionSpec = {
            fadeIn(animationSpec = tween(NowPlayingSongCrossfadeDuration)) togetherWith
                    fadeOut(animationSpec = tween(NowPlayingSongCrossfadeDuration))
        },
        label = "metadata_crossfade"
    ) { songId ->
        // Metadata without scrim background (aurora provides contrast)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = NowPlayingMetadataTopPadding,
                    bottom = NowPlayingMetadataBottomPadding,
                    start = NowPlayingMetadataHorizontalPadding,
                    end = NowPlayingMetadataHorizontalPadding
                ),
            horizontalAlignment = Alignment.Start
        ) {
            // Text stack on left, like/favorite pinned right
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
                    // Title with marquee for long names
                    Text(
                        text = song.title.ifBlank { stringResource(R.string.now_playing_unknown_song) },
                        style = MaterialTheme.typography.songTitleLarge.copy(
                            fontSize = NowPlayingTitleFontSize,
                            lineHeight = NowPlayingTitleLineHeight,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            repeatDelayMillis = 3000,
                            velocity = 30.dp
                        )
                    )

                    if (song.artist.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = NowPlayingArtistFontSize,
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                    }
                }

                // Like button with bounce animation
                LikeButton(
                    isLiked = song.isLiked,
                    onLikeClick = onLikeClick,
                    stateDescription = likeStateDescription
                )
            }
        }
    }
}

/**
 * Like button with spring bounce animation and color pulse
 */
@Composable
private fun LikeButton(
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    stateDescription: String
) {
    var bouncing by remember(isLiked) { mutableStateOf(isLiked) }

    val scale by animateFloatAsState(
        targetValue = if (bouncing) 1.35f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.35f,
            stiffness = 700f
        ),
        label = "like_bounce"
    )

    val likeColor by animateColorAsState(
        targetValue = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
        animationSpec = tween(durationMillis = 300),
        label = "like_color"
    )

    val likeDescriptionText = stringResource(
        if (isLiked) R.string.now_playing_unlike else R.string.now_playing_like
    )

    LaunchedEffect(isLiked) {
        if (isLiked) {
            bouncing = true
            delay(NowPlayingLikeBounceDuration.toLong())
            bouncing = false
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
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale
                )
                .semantics {
                    role = Role.Button
                    contentDescription = likeDescriptionText
                }
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = likeColor.copy(alpha = if (isLiked) 1f else 0.5f),
                modifier = Modifier.size(24.dp)
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
    // Progress bar: 2dp height, soft neutral inactive track, accent active portion
    // Thumb: 6dp circle, expands to 12dp on drag
    // Use seekPosition when seeking, otherwise use currentPosition
    val rawDisplayPosition = if (isSeeking) seekPosition.toFloat() else currentPosition.toFloat()
    val displayPosition by animateFloatAsState(
        targetValue = rawDisplayPosition,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "smooth_progress"
    )

    // Dedicated interaction source to isolate slider touch events
    val sliderInteractionSource = remember { MutableInteractionSource() }

    // Track active opacity boost during scrubbing
    val activeTrackOpacity by animateFloatAsState(
        targetValue = if (isSeeking) 1f else 0.9f,
        animationSpec = tween(durationMillis = 150),
        label = "active_track_opacity"
    )

    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Slider(
            value = displayPosition,
            onValueChange = {
                if (!isSeeking) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSeekStart()
                }
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
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)
            ),
            thumb = {
                val thumbSize by animateDpAsState(
                    targetValue = if (isSeeking) NowPlayingSeekBarThumbMaxSize else NowPlayingSeekBarThumbMinSize,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                    label = "thumb_size"
                )
                val thumbShadow by animateDpAsState(
                    targetValue = if (isSeeking) 8.dp else 2.dp,
                    animationSpec = tween(150),
                    label = "thumb_shadow"
                )
                Box(
                    modifier = Modifier
                        .size(thumbSize)
                        .shadow(thumbShadow, CircleShape)
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
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f),
                                RoundedCornerShape(1.dp)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(sliderColor.copy(alpha = activeTrackOpacity), RoundedCornerShape(1.dp))
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
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            // Spotify convention: show remaining time with minus sign
            val remainingMs = (duration - displayPosition.toLong()).coerceAtLeast(0L)
            Text(
                text = "-${formatDuration(remainingMs)}",
                style = MaterialTheme.typography.compactLabel.copy(
                    fontSize = NowPlayingTimelineFontSize,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * PressableIconButton - Reusable wrapper that adds press-scale feedback
 * Scales from 1.0 to 0.92 on press with spring animation
 */
@Composable
private fun PressableIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "press_scale"
    )

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
    ) {
        content()
    }
}

/**
 * D. PlaybackControlsSection - Primary playback controls with press feedback
 * Layout: Shuffle : Previous : Play/Pause : Next : Repeat
 * Play/Pause: 68dp circle, 36dp icon with morphing animation
 * Skip buttons: 52dp touch, 32dp icon with press scale
 * Toggle buttons: 48dp touch, 22dp icon with press scale
 * Indicator dots with smooth appear/disappear animation
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
    val haptic = LocalHapticFeedback.current
    val shuffleDesc = stringResource(R.string.now_playing_shuffle)
    val previousDesc = stringResource(R.string.now_playing_previous)
    val playDesc = stringResource(R.string.now_playing_play)
    val pauseDesc = stringResource(R.string.now_playing_pause)
    val nextDesc = stringResource(R.string.now_playing_next)
    val repeatDesc = stringResource(R.string.now_playing_repeat)
    val shuffleStateDesc = stringResource(
        if (playbackState.shuffleEnabled) R.string.now_playing_shuffle_on else R.string.now_playing_shuffle_off
    )
    val repeatStateDesc = stringResource(
        when (playbackState.repeatMode) {
            RepeatMode.OFF -> R.string.now_playing_repeat_off
            RepeatMode.ALL -> R.string.now_playing_repeat_all
            RepeatMode.ONE -> R.string.now_playing_repeat_one
        }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle Button (left) with press scale
        PressableIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onShuffleClick()
            },
            modifier = Modifier
                .size(NowPlayingToggleButtonSize)
                .semantics {
                    role = Role.Button
                    contentDescription = shuffleDesc
                }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                val shuffleColor by animateColorAsState(
                    targetValue = if (playbackState.shuffleEnabled) accentColor else MaterialTheme.colorScheme.onBackground,
                    animationSpec = tween(300),
                    label = "shuffle_color"
                )
                val shuffleAlpha by animateFloatAsState(
                    targetValue = if (playbackState.shuffleEnabled) 1f else 0.65f,
                    animationSpec = tween(300),
                    label = "shuffle_alpha"
                )
                val shuffleRotation by animateFloatAsState(
                    targetValue = if (playbackState.shuffleEnabled) 360f else 0f,
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                    label = "shuffle_rotation"
                )

                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    tint = shuffleColor.copy(alpha = shuffleAlpha),
                    modifier = Modifier
                        .size(NowPlayingToggleButtonIconSize)
                        .graphicsLayer(rotationZ = shuffleRotation)
                )

                // Animated indicator dot with smooth appear/disappear
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

        // Previous Button with press scale
        PressableIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onPreviousClick()
            },
            modifier = Modifier.size(NowPlayingSkipButtonSize)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = previousDesc,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(NowPlayingSkipButtonIconSize)
            )
        }

        // Play/Pause Button (center, dominant) with press scale
        PressableIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onPlayPauseClick()
            },
            modifier = Modifier
                .size(NowPlayingPlayButtonSize)
                .background(accentColor, CircleShape)
        ) {
            AnimatedContent(
                targetState = playbackState.isPlaying,
                transitionSpec = {
                    scaleIn(initialScale = 0.8f) + fadeIn(animationSpec = tween(150)) togetherWith
                            scaleOut(targetScale = 0.8f) + fadeOut(animationSpec = tween(150))
                },
                label = "play_pause_icon"
            ) { isPlaying ->
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) pauseDesc else playDesc,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(NowPlayingPlayButtonIconSize)
                )
            }
        }

        // Next Button with press scale
        PressableIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onNextClick()
            },
            modifier = Modifier.size(NowPlayingSkipButtonSize)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = nextDesc,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(NowPlayingSkipButtonIconSize)
            )
        }

        // Repeat Button (right) with press scale
        PressableIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onRepeatClick()
            },
            modifier = Modifier
                .size(NowPlayingToggleButtonSize)
                .semantics {
                    role = Role.Button
                    contentDescription = repeatDesc
                }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                val repeatColor by animateColorAsState(
                    targetValue = when (playbackState.repeatMode) {
                        RepeatMode.OFF -> MaterialTheme.colorScheme.onBackground
                        else -> accentColor
                    },
                    animationSpec = tween(300),
                    label = "repeat_color"
                )
                val repeatAlpha by animateFloatAsState(
                    targetValue = when (playbackState.repeatMode) {
                        RepeatMode.OFF -> 0.65f
                        else -> 1f
                    },
                    animationSpec = tween(300),
                    label = "repeat_alpha"
                )
                val repeatScale by animateFloatAsState(
                    targetValue = if (playbackState.repeatMode != RepeatMode.OFF) 1.15f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
                    label = "repeat_scale"
                )

                Icon(
                    imageVector = when (playbackState.repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    },
                    contentDescription = null,
                    tint = repeatColor.copy(alpha = repeatAlpha),
                    modifier = Modifier
                        .size(NowPlayingToggleButtonIconSize)
                        .graphicsLayer(scaleX = repeatScale, scaleY = repeatScale)
                )

                // Mode indicator badge for better clarity
                if (playbackState.repeatMode != RepeatMode.OFF) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(14.dp)
                            .background(accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (playbackState.repeatMode) {
                                RepeatMode.ONE -> "1"
                                else -> "∞"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * E. SecondaryActionsSection - Secondary actions (Lyrics | Share | Queue)
 * 3-button row with 24dp icons, 48dp touch targets, 60% alpha default
 * Lyrics button opens modal bottom sheet
 */
@Composable
private fun SecondaryActionsSection(
    song: Song,
    playbackState: PlaybackState,
    accentColor: Color,
    onLyricsClick: () -> Unit,
    onShareClick: () -> Unit,
    onQueueClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lyrics Button (opens modal sheet) - with accent color to highlight premium feature
        PressableIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLyricsClick()
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lyrics,
                contentDescription = stringResource(R.string.now_playing_lyrics),
                tint = accentColor.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }

        // Share Button - standard secondary action color
        PressableIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onShareClick()
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = stringResource(R.string.now_playing_share),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                modifier = Modifier.size(24.dp)
            )
        }

        // Queue Button - standard secondary action color
        PressableIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onQueueClick()
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Default.QueueMusic,
                contentDescription = stringResource(R.string.now_playing_queue),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                modifier = Modifier.size(24.dp)
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
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.now_playing_no_song),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.now_playing_select_song),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * LyricsModalSheet - Full-screen modal bottom sheet for synced lyrics
 * Displays large, readable lyrics with active line highlighting
 * Supports tap-to-seek on lyric lines
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsModalSheet(
    song: Song,
    lyricsState: LyricsState,
    currentPosition: Long,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Song info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.now_playing_lyrics),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = song.title.ifBlank { stringResource(R.string.now_playing_unknown_song) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist.ifBlank { "Unknown Artist" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Divider
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Lyrics content - fills remaining space
            SpotifyStyleSyncedLyrics(
                lyricsState = lyricsState,
                currentPosition = currentPosition,
                accentColor = accentColor,
                onSeekTo = onSeekTo,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * SpotifyStyleSyncedLyrics - Premium large-text synced lyrics display
 * Active line: 26sp Bold, white (full opacity)
 * Inactive lines: 18sp Normal, 35% opacity
 * Auto-scrolls to keep active line in upper third of screen
 * Tap any line to seek to that timestamp
 * Gradient fade edges for polish
 */
@Composable
private fun SpotifyStyleSyncedLyrics(
    lyricsState: LyricsState,
    currentPosition: Long,
    accentColor: Color,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when (lyricsState) {
        is LyricsState.Loading -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = accentColor,
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.now_playing_loading_lyrics),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        is LyricsState.Success -> {
            val parsedLines = lyricsState.parsedLines
            if (parsedLines.isNotEmpty()) {
                SyncedLyricsView(
                    lines = parsedLines,
                    currentPosition = currentPosition,
                    accentColor = accentColor,
                    onSeekTo = onSeekTo,
                    modifier = modifier
                )
            } else if (!lyricsState.lyrics.plainLyrics.isNullOrBlank()) {
                PlainLyricsView(
                    text = lyricsState.lyrics.plainLyrics,
                    modifier = modifier
                )
            } else {
                NoLyricsAvailable(modifier = modifier)
            }
        }

        is LyricsState.Error -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.now_playing_lyrics_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        is LyricsState.NotFound -> {
            NoLyricsAvailable(modifier = modifier)
        }
    }
}

/**
 * SyncedLyricsView - LazyColumn with large Spotify-style text
 */
@Composable
private fun SyncedLyricsView(
    lines: List<LyricLine>,
    currentPosition: Long,
    accentColor: Color,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val activeLine = remember(currentPosition) {
        LrcParser.findActiveLine(lines, currentPosition)
    }

    // Auto-scroll to keep active line in upper third
    LaunchedEffect(activeLine) {
        if (activeLine >= 0 && activeLine < lines.size) {
            listState.animateScrollToItem(
                index = activeLine.coerceAtLeast(0),
                scrollOffset = -(150).dp.value.toInt()  // Keep in upper third
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NowPlayingLyricsLineSpacing),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 60.dp)
        ) {
            itemsIndexed(
                items = lines,
                key = { index, _ -> index }
            ) { index, line ->
                val isActive = index == activeLine
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = if (isActive) NowPlayingLyricsActiveFontSize else NowPlayingLyricsInactiveFontSize,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = 32.sp
                    ),
                    color = if (isActive) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onSeekTo(line.timestamp)
                        }
                        .padding(vertical = 8.dp)
                )
            }
        }

        // Gradient fade edges
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )
    }
}

/**
 * PlainLyricsView - For unsynced lyrics without timestamps
 */
@Composable
private fun PlainLyricsView(
    text: String,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 28.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * NoLyricsAvailable - Empty state for missing lyrics
 */
@Composable
private fun NoLyricsAvailable(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.now_playing_no_lyrics),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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


