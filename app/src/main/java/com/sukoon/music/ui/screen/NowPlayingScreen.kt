package com.sukoon.music.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.unit.Dp
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
import com.sukoon.music.ui.components.PlaceholderAlbumArt
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.util.hasUsableAlbumArt
import com.sukoon.music.ui.util.rememberAlbumPalette
import com.sukoon.music.ui.util.resolveNowPlayingAccentColors
import com.sukoon.music.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.draw.blur
import com.sukoon.music.ui.components.LiquidMeshBackground
import com.sukoon.music.ui.components.AddToPlaylistDialog
import com.sukoon.music.ui.components.SleepTimerDialog
import com.sukoon.music.ui.animation.rememberPlaybackMotionClock
import com.sukoon.music.ui.animation.toMotionDirective
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import com.sukoon.music.ui.theme.*
import kotlin.math.abs
import kotlin.math.sin

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
private val NowPlayingAlbumArtRoundedCorners = 8.dp
private val NowPlayingAlbumArtHorizontalPadding = 16.dp
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

private enum class NowPlayingHeightBucket {
    COMPACT,
    REGULAR,
    TALL
}

private data class NowPlayingSpacingSpec(
    val topContentPadding: Dp,
    val albumToMetadataSpacing: Dp,
    val metadataToControlsSpacing: Dp,
    val seekToPrimaryControlsSpacing: Dp,
    val primaryControlsBottomSpacing: Dp,
    val controlsToSecondarySpacing: Dp,
    val bottomAfterSecondarySpacing: Dp,
    val albumArtHorizontalPadding: Dp,
    val metadataHorizontalPadding: Dp,
    val controlsHorizontalPadding: Dp,
    val secondaryHorizontalPadding: Dp,
    val secondaryVerticalPadding: Dp,
    val titleToArtistSpacing: Dp,
    val artistToAlbumSpacing: Dp,
    val seekToTimelineSpacing: Dp,
    val lyricsHeaderVerticalPadding: Dp,
    val lyricsHeaderToDividerSpacing: Dp,
    val lyricsContentVerticalPadding: Dp,
    val lyricsLineSpacing: Dp
)

private fun spacingForHeight(maxHeight: Dp): NowPlayingSpacingSpec {
    val bucket = when {
        maxHeight < 700.dp -> NowPlayingHeightBucket.COMPACT
        maxHeight < 840.dp -> NowPlayingHeightBucket.REGULAR
        else -> NowPlayingHeightBucket.TALL
    }

    return when (bucket) {
        NowPlayingHeightBucket.COMPACT -> NowPlayingSpacingSpec(
            topContentPadding = 4.dp,
            albumToMetadataSpacing = 8.dp,
            metadataToControlsSpacing = 6.dp,
            seekToPrimaryControlsSpacing = 8.dp,
            primaryControlsBottomSpacing = 6.dp,
            controlsToSecondarySpacing = 4.dp,
            bottomAfterSecondarySpacing = 8.dp,
            albumArtHorizontalPadding = 8.dp,
            metadataHorizontalPadding = 20.dp,
            controlsHorizontalPadding = 8.dp,
            secondaryHorizontalPadding = 16.dp,
            secondaryVerticalPadding = 4.dp,
            titleToArtistSpacing = 4.dp,
            artistToAlbumSpacing = 2.dp,
            seekToTimelineSpacing = 2.dp,
            lyricsHeaderVerticalPadding = 12.dp,
            lyricsHeaderToDividerSpacing = 8.dp,
            lyricsContentVerticalPadding = 40.dp,
            lyricsLineSpacing = 14.dp
        )

        NowPlayingHeightBucket.REGULAR -> NowPlayingSpacingSpec(
            topContentPadding = 8.dp,
            albumToMetadataSpacing = 10.dp,
            metadataToControlsSpacing = 10.dp,
            seekToPrimaryControlsSpacing = 12.dp,
            primaryControlsBottomSpacing = 8.dp,
            controlsToSecondarySpacing = 6.dp,
            bottomAfterSecondarySpacing = 12.dp,
            albumArtHorizontalPadding = 10.dp,
            metadataHorizontalPadding = 24.dp,
            controlsHorizontalPadding = 12.dp,
            secondaryHorizontalPadding = 20.dp,
            secondaryVerticalPadding = 6.dp,
            titleToArtistSpacing = 6.dp,
            artistToAlbumSpacing = 2.dp,
            seekToTimelineSpacing = 4.dp,
            lyricsHeaderVerticalPadding = 16.dp,
            lyricsHeaderToDividerSpacing = 12.dp,
            lyricsContentVerticalPadding = 56.dp,
            lyricsLineSpacing = 18.dp
        )

        NowPlayingHeightBucket.TALL -> NowPlayingSpacingSpec(
            topContentPadding = 12.dp,
            albumToMetadataSpacing = 12.dp,
            metadataToControlsSpacing = 14.dp,
            seekToPrimaryControlsSpacing = 14.dp,
            primaryControlsBottomSpacing = 10.dp,
            controlsToSecondarySpacing = 8.dp,
            bottomAfterSecondarySpacing = 16.dp,
            albumArtHorizontalPadding = 12.dp,
            metadataHorizontalPadding = 28.dp,
            controlsHorizontalPadding = 16.dp,
            secondaryHorizontalPadding = 24.dp,
            secondaryVerticalPadding = 8.dp,
            titleToArtistSpacing = 6.dp,
            artistToAlbumSpacing = 2.dp,
            seekToTimelineSpacing = 4.dp,
            lyricsHeaderVerticalPadding = 16.dp,
            lyricsHeaderToDividerSpacing = 12.dp,
            lyricsContentVerticalPadding = 64.dp,
            lyricsLineSpacing = 20.dp
        )
    }
}

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

    // Base accent from user profile (used as fallback and for secondary actions)
    val accentTokens = accent()
    val accentColor = accentTokens.primary
    val palette = rememberAlbumPalette(playbackState.currentSong?.albumArtUri)
    val hasAlbumArt = remember(playbackState.currentSong?.albumArtUri) {
        hasUsableAlbumArt(playbackState.currentSong?.albumArtUri)
    }
    val resolvedAccentColors = remember(palette, accentColor, hasAlbumArt) {
        resolveNowPlayingAccentColors(
            palette = palette,
            hasAlbumArt = hasAlbumArt,
            fallbackAccent = accentColor
        )
    }
    val controlsAccentColor by animateColorAsState(
        targetValue = resolvedAccentColors.controlsColor,
        animationSpec = tween(durationMillis = 220),
        label = "now_playing_controls_accent"
    )
    val sliderAccentColor by animateColorAsState(
        targetValue = resolvedAccentColors.sliderColor,
        animationSpec = tween(durationMillis = 220),
        label = "now_playing_slider_accent"
    )
    val motionDirective = playbackState.toMotionDirective(isVisible = true)
    val motionPhase by rememberPlaybackMotionClock(motionDirective)
    val collapseThresholdPx = with(LocalDensity.current) { 96.dp.toPx() }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Liquid Mesh Background - animated aurora with smooth color transitions
        LiquidMeshBackground(
            palette = palette,
            songId = playbackState.currentSong?.id,
            motion = motionDirective,
            phase = motionPhase,
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
                        motionDirective = motionDirective,
                        motionPhase = motionPhase,
                        accentColor = accentColor,
                        controlsAccentColor = controlsAccentColor,
                        sliderColor = sliderAccentColor,
                        albumOverlayAlpha = (0.22f - (palette.dominant.luminance() * 0.16f)).coerceIn(0.06f, 0.18f),
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
    queueName: String?,
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

        // Center: Playing From context (if available)
        if (!queueName.isNullOrBlank()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.now_playing_playing_from),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Text(
                    text = queueName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
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
    motionDirective: com.sukoon.music.ui.animation.MotionDirective,
    motionPhase: Float,
    accentColor: Color,
    controlsAccentColor: Color = accentColor,
    sliderColor: Color = accentColor,
    albumOverlayAlpha: Float = 0.12f,
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
    viewModel: HomeViewModel,
    playlistViewModel: PlaylistViewModel = hiltViewModel()
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
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSongContextMenu by remember { mutableStateOf(false) }
    var showInfoForSong by remember { mutableStateOf<Song?>(null) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }

    val context = LocalContext.current

    // Delete result launcher
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_song_deleted_successfully), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_delete_cancelled), Toast.LENGTH_SHORT).show()
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

    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()
    val isSleepTimerActive by viewModel.isSleepTimerActive.collectAsStateWithLifecycle()

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
            .alpha(screenAlpha)
    ) {
        val statusBarTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val topBarOffset = (statusBarTopInset - 4.dp).coerceAtLeast(0.dp)

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val spacingSpec = spacingForHeight(maxHeight)
            val topBarHeight = 44.dp
            val topContentPadding = spacingSpec.topContentPadding
            val albumToMetadataSpacing = spacingSpec.albumToMetadataSpacing
            val metadataToControlsSpacing = spacingSpec.metadataToControlsSpacing
            val seekToPrimaryControlsSpacing = spacingSpec.seekToPrimaryControlsSpacing
            val primaryControlsBottomSpacing = spacingSpec.primaryControlsBottomSpacing
            val controlsToSecondarySpacing = spacingSpec.controlsToSecondarySpacing
            val albumArtWeight = when {
                maxHeight < 700.dp -> 0.73f
                maxHeight < 840.dp -> 0.79f
                else -> 0.85f
            }
            val albumArtShadowElevation = when {
                maxHeight < 700.dp -> 2.dp
                maxHeight < 840.dp -> 3.dp
                else -> 4.dp
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
                    motionDirective = motionDirective,
                    motionPhase = motionPhase,
                    onAlbumArtClick = { isImmersiveMode = !isImmersiveMode },
                    onNextClick = onNextClick,
                    onPreviousClick = onPreviousClick,
                    overlayAlpha = albumOverlayAlpha,
                    shadowElevation = albumArtShadowElevation,
                    onLikeClick = {
                        playbackState.currentSong?.let { s ->
                            onLikeClick()
                        }
                    },
                    horizontalPadding = spacingSpec.albumArtHorizontalPadding,
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
                        onLikeClick = onLikeClick,
                        onArtistClick = { 
                            val artistId = song.artist.hashCode().toLong()
                            onNavigateToArtist(artistId)
                        },
                        onAlbumClick = {
                            val albumId = song.album.hashCode().toLong()
                            onNavigateToAlbum(albumId)
                        },
                        horizontalPadding = spacingSpec.metadataHorizontalPadding,
                        titleToArtistSpacing = spacingSpec.titleToArtistSpacing,
                        artistToAlbumSpacing = spacingSpec.artistToAlbumSpacing
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
                        modifier = Modifier.padding(vertical = 0.dp, horizontal = spacingSpec.controlsHorizontalPadding)
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
                            },
                            timelineTopSpacing = spacingSpec.seekToTimelineSpacing
                        )

                        Spacer(modifier = Modifier.height(seekToPrimaryControlsSpacing))

                        // D. Primary Playback Controls (Shuffle - Previous - Play - Next - Repeat)
                        PlaybackControlsSection(
                            playbackState = playbackState,
                            accentColor = controlsAccentColor,
                            currentPosition = currentPosition,
                            onPreviousClick = onPreviousClick,
                            onPlayPauseClick = onPlayPauseClick,
                            onNextClick = onNextClick,
                            onShuffleClick = onShuffleClick,
                            onRepeatClick = onRepeatClick
                        )

                        Spacer(modifier = Modifier.height(primaryControlsBottomSpacing))
                    }
                }

                Spacer(modifier = Modifier.height(controlsToSecondarySpacing))

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
                        onQueueClick = { showQueueModal = true },
                        onAddToPlaylistClick = { showPlaylistDialog = true },
                        onTimerClick = { showSleepTimerDialog = true },
                        isTimerActive = isSleepTimerActive,
                        horizontalPadding = spacingSpec.secondaryHorizontalPadding,
                        verticalPadding = spacingSpec.secondaryVerticalPadding
                    )

                    Spacer(modifier = Modifier.height(spacingSpec.bottomAfterSecondarySpacing))
                }
        }
        }

        // A. Pinned TopUtilityBar - Fixed position outside scrollable content
        TopUtilityBar(
            song = song,
            queueName = playbackState.currentQueueName,
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
            isPlayingGlobally = playbackState.isPlaying,
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

    if (showPlaylistDialog) {
        AddToPlaylistDialog(
            playlists = playlists,
            onPlaylistSelected = { playlistId ->
                playlistViewModel.addSongToPlaylist(playlistId, song.id)
                showPlaylistDialog = false
                Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_added_to_playlist), Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showPlaylistDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            onTimerSelected = { minutes ->
                viewModel.setSleepTimer(minutes)
                showSleepTimerDialog = false
                if (minutes > 0) {
                    Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_timer_set_minutes, minutes), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_timer_cancelled), Toast.LENGTH_SHORT).show()
                }
            },
            onEndOfTrackSelected = {
                val remainingMs = (playbackState.duration - currentPosition).coerceAtLeast(0L)
                if (remainingMs > 0L) {
                    viewModel.setSleepTimerTargetTime(System.currentTimeMillis() + remainingMs)
                    Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_timer_set_end_of_track), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_track_end_not_available), Toast.LENGTH_SHORT).show()
                }
                showSleepTimerDialog = false
            },
            onDismiss = { showSleepTimerDialog = false }
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
                        Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_song_deleted_successfully), Toast.LENGTH_SHORT).show()
                        songToDelete = null
                    }
                    is DeleteHelper.DeleteResult.Error -> {
                        Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_error_with_message, result.message), Toast.LENGTH_SHORT).show()
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
    motionDirective: com.sukoon.music.ui.animation.MotionDirective,
    motionPhase: Float,
    onAlbumArtClick: () -> Unit,
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    horizontalPadding: Dp = NowPlayingAlbumArtHorizontalPadding,
    shadowElevation: Dp = NowPlayingAlbumArtShadow,
    overlayAlpha: Float = 0.12f,
    modifier: Modifier = Modifier,
    onLikeClick: () -> Unit = {}
) {
    // Track horizontal swipe for next/previous navigation
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }
    var transitionDirection by remember { mutableIntStateOf(0) } // 1 = next, -1 = previous
    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    val swipeVelocityThreshold = 1200f
    val context = LocalContext.current
    val albumArtCacheKey = song.albumArtUri?.takeIf { it.isNotBlank() } ?: "song_${song.id}"
    val albumArtRequest = remember(song.id, song.albumArtUri) {
        ImageRequest.Builder(context)
            .data(song.albumArtUri)
            .crossfade(false)
            .allowHardware(true)
            .precision(Precision.EXACT)
            .memoryCacheKey("now_playing_album_$albumArtCacheKey")
            .diskCacheKey(albumArtCacheKey)
            .build()
    }
    val defaultFallbackColor = MaterialTheme.colorScheme.primary
    var fallbackDominantColor by remember(song.id, defaultFallbackColor) {
        mutableStateOf(defaultFallbackColor)
    }

    // Album art container - floating with rounded corners and shadow
    val haptic = LocalHapticFeedback.current

    val breathScale = 1f + (0.015f * ((sin(motionPhase) + 1f) / 2f))
    val isMotionActive = motionDirective.state == com.sukoon.music.ui.animation.MotionPlayState.RUNNING
    val isDragging = abs(horizontalDragOffset) > 0.5f
    val animatedScale by animateFloatAsState(
        targetValue = when {
            motionDirective.state == com.sukoon.music.ui.animation.MotionPlayState.REST -> 1.0f
            isDragging -> 1.0f
            isMotionActive -> breathScale
            else -> breathScale
        },
        animationSpec = tween(durationMillis = 1000),
        label = "smooth_breath_transition"
    )
    val animatedDragOffset by animateFloatAsState(
        targetValue = horizontalDragOffset,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 450f),
        label = "album_drag_offset"
    )
    val dragVisualTranslationX = (animatedDragOffset * 0.22f).coerceIn(-48f, 48f)
    val dragVisualRotation = (animatedDragOffset / 42f).coerceIn(-3f, 3f)

    LaunchedEffect(song.id) {
        transitionDirection = 0
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
    ) {
        // Album art transition on song change (directional for swipe actions)
        AnimatedContent(
            targetState = song.id,
            transitionSpec = {
                when (transitionDirection) {
                    1 -> {
                        (slideInHorizontally(
                            animationSpec = tween(260, easing = FastOutSlowInEasing),
                            initialOffsetX = { it / 3 }
                        ) + fadeIn(animationSpec = tween(220))) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                                    targetOffsetX = { -it / 3 }
                                ) + fadeOut(animationSpec = tween(200)))
                    }
                    -1 -> {
                        (slideInHorizontally(
                            animationSpec = tween(260, easing = FastOutSlowInEasing),
                            initialOffsetX = { -it / 3 }
                        ) + fadeIn(animationSpec = tween(220))) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                                    targetOffsetX = { it / 3 }
                                ) + fadeOut(animationSpec = tween(200)))
                    }
                    else -> {
                        fadeIn(animationSpec = tween(NowPlayingSongCrossfadeDuration)) togetherWith
                                fadeOut(animationSpec = tween(NowPlayingSongCrossfadeDuration))
                    }
                }
            },
            label = "album_art_transition"
        ) { _ ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer {
                        translationX = dragVisualTranslationX
                        rotationZ = dragVisualRotation
                    }
                    .shadow(shadowElevation, RoundedCornerShape(NowPlayingAlbumArtRoundedCorners))
                    .clip(RoundedCornerShape(NowPlayingAlbumArtRoundedCorners))
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            horizontalDragOffset = (horizontalDragOffset + delta).coerceIn(-320f, 320f)
                        },
                        onDragStopped = { velocity ->
                            val shouldGoNext = horizontalDragOffset < -swipeThresholdPx || velocity < -swipeVelocityThreshold
                            val shouldGoPrevious = horizontalDragOffset > swipeThresholdPx || velocity > swipeVelocityThreshold
                            when {
                                shouldGoNext -> {
                                    transitionDirection = 1
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onNextClick()
                                }
                                shouldGoPrevious -> {
                                    transitionDirection = -1
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPreviousClick()
                                }
                            }
                            horizontalDragOffset = 0f
                        }
                    )
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAlbumArtClick,
                        onDoubleClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLikeClick()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Album Art Image with gradient overlay
                Box(modifier = Modifier.fillMaxSize()) {
                    SubcomposeAsyncImage(
                        model = albumArtRequest,
                        contentDescription = stringResource(R.string.now_playing_album_art, song.title),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.Medium,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                // Content-aware gradient background from song metadata
                                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                                val seed = PlaceholderAlbumArt.generateSeed(
                                    albumName = song.album,
                                    artistName = song.artist,
                                    songId = song.id
                                )

                                // Extract dominant color for UI tinting
                                LaunchedEffect(seed, isDark) {
                                    val hash = PlaceholderAlbumArt.hashString(seed)
                                    val colors = PlaceholderAlbumArt.selectColors(hash, isDark)
                                    fallbackDominantColor = PlaceholderAlbumArt.extractDominantColor(
                                        color1 = colors[0],
                                        color2 = colors.getOrElse(1) { colors[0] },
                                        isDark = isDark
                                    )
                                }

                                PlaceholderAlbumArt.Placeholder(
                                    seed = seed,
                                    modifier = Modifier.fillMaxSize(),
                                    iconOpacity = 0f
                                )

                                // Loading spinner overlay
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = fallbackDominantColor.copy(alpha = 0.7f),
                                    strokeWidth = 3.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                // Content-aware gradient background from song metadata
                                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                                val seed = PlaceholderAlbumArt.generateSeed(
                                    albumName = song.album,
                                    artistName = song.artist,
                                    songId = song.id
                                )

                                // Extract dominant color for UI tinting
                                LaunchedEffect(seed, isDark) {
                                    val hash = PlaceholderAlbumArt.hashString(seed)
                                    val colors = PlaceholderAlbumArt.selectColors(hash, isDark)
                                    fallbackDominantColor = PlaceholderAlbumArt.extractDominantColor(
                                        color1 = colors[0],
                                        color2 = colors.getOrElse(1) { colors[0] },
                                        isDark = isDark
                                    )
                                }

                                PlaceholderAlbumArt.Placeholder(
                                    seed = seed,
                                    modifier = Modifier.fillMaxSize(),
                                    icon = Icons.Default.MusicNote,
                                    iconSize = 64,
                                    iconOpacity = 0.8f
                                )
                            }
                        }
                    )

                    // Subtle gradient overlay for premium look
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = overlayAlpha)
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
    onLikeClick: () -> Unit,
    onArtistClick: () -> Unit,
    onAlbumClick: () -> Unit,
    horizontalPadding: Dp = NowPlayingMetadataHorizontalPadding,
    titleToArtistSpacing: Dp = 6.dp,
    artistToAlbumSpacing: Dp = 2.dp
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
                    start = horizontalPadding,
                    end = horizontalPadding
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
                        Spacer(modifier = Modifier.height(titleToArtistSpacing))
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = NowPlayingArtistFontSize,
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null, // No ripple for cleaner look
                                onClick = onArtistClick
                            )
                        )
                    }

                    if (song.album.isNotBlank()) {
                        Spacer(modifier = Modifier.height(artistToAlbumSpacing))
                        Text(
                            text = song.album,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onAlbumClick
                            )
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
    onSeekEnd: () -> Unit,
    timelineTopSpacing: Dp = 4.dp
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
        // Timestamp preview during seeking
        AnimatedVisibility(
            visible = isSeeking,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = formatDuration(seekPosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = sliderColor,
                    fontSize = 12.sp
                )
            }
        }

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = timelineTopSpacing),
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
 * Play Button with Circular Progress Ring
 * Shows progress around the play button as song plays
 */
@Composable
private fun PlayButtonWithProgress(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    accentColor: Color,
    onPlayPauseClick: () -> Unit,
    haptic: HapticFeedback,
    playDesc: String,
    pauseDesc: String
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    val density = LocalDensity.current
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "progress_ring"
    )

    Box(
        modifier = Modifier.size(76.dp),  // 68dp + 8dp for ring
        contentAlignment = Alignment.Center
    ) {
        // Circular progress ring drawn on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = with(density) { 4.dp.toPx() }
            val radius = (size.minDimension - strokeWidth) / 2

            // Background track (subtle, always visible)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active progress arc (fills as song plays)
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Play button (centered in the ring)
        PressableIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onPlayPauseClick()
            },
            modifier = Modifier
                .size(68.dp)
                .background(accentColor, CircleShape)
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    scaleIn(initialScale = 0.8f) + fadeIn(animationSpec = tween(150)) togetherWith
                            scaleOut(targetScale = 0.8f) + fadeOut(animationSpec = tween(150))
                },
                label = "play_pause_icon"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) pauseDesc else playDesc,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
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
    currentPosition: Long,
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
    val primaryTransportTint = accentColor.copy(alpha = 0.92f)

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
                tint = primaryTransportTint,
                modifier = Modifier.size(NowPlayingSkipButtonIconSize)
            )
        }

        // Play/Pause Button with Progress Ring (center, dominant)
        PlayButtonWithProgress(
            isPlaying = playbackState.isPlaying,
            currentPosition = currentPosition,
            duration = playbackState.duration,
            accentColor = accentColor,
            onPlayPauseClick = onPlayPauseClick,
            haptic = haptic,
            playDesc = playDesc,
            pauseDesc = pauseDesc
        )

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
                tint = primaryTransportTint,
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
    onQueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onTimerClick: () -> Unit,
    isTimerActive: Boolean,
    horizontalPadding: Dp = 20.dp,
    verticalPadding: Dp = 6.dp
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding, horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Add to Playlist
        PressableIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onAddToPlaylistClick()
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlaylistAdd,
                contentDescription = stringResource(R.string.common_add_to_playlist),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                modifier = Modifier.size(24.dp)
            )
        }

        // 2. Lyrics Button (with accent color to highlight premium feature)
        // PressableIconButton(
        //     onClick = {
        //         haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        //         onLyricsClick()
        //     },
        //     modifier = Modifier.size(48.dp)
        // ) {
        //     Icon(
        //         imageVector = Icons.Default.Lyrics,
        //         contentDescription = stringResource(R.string.now_playing_lyrics),
        //         tint = accentColor.copy(alpha = 0.8f),
        //         modifier = Modifier.size(24.dp)
        //     )
        // }

        // 3. Share Button
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

        // 4. Queue Button
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

        // 5. Sleep Timer
        PressableIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onTimerClick()
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = stringResource(R.string.dialog_sleep_timer_title),
                tint = if (isTimerActive) accentColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
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
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val spacingSpec = spacingForHeight(screenHeight)

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
                    .padding(horizontal = 24.dp, vertical = spacingSpec.lyricsHeaderVerticalPadding),
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
                    text = song.artist.ifBlank { stringResource(R.string.now_playing_unknown_artist) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Divider
            HorizontalDivider(modifier = Modifier.padding(vertical = spacingSpec.lyricsHeaderToDividerSpacing))

            // Lyrics content - fills remaining space
            SpotifyStyleSyncedLyrics(
                lyricsState = lyricsState,
                currentPosition = currentPosition,
                accentColor = accentColor,
                onSeekTo = onSeekTo,
                lyricsLineSpacing = spacingSpec.lyricsLineSpacing,
                lyricsContentVerticalPadding = spacingSpec.lyricsContentVerticalPadding,
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
    lyricsLineSpacing: Dp = NowPlayingLyricsLineSpacing,
    lyricsContentVerticalPadding: Dp = 60.dp,
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
                    lyricsLineSpacing = lyricsLineSpacing,
                    lyricsContentVerticalPadding = lyricsContentVerticalPadding,
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
    lyricsLineSpacing: Dp = NowPlayingLyricsLineSpacing,
    lyricsContentVerticalPadding: Dp = 60.dp,
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
            verticalArrangement = Arrangement.spacedBy(lyricsLineSpacing),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = lyricsContentVerticalPadding)
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
        val previewPlaybackState = PlaybackState(
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
        )
        NowPlayingContent(
            playbackState = previewPlaybackState,
            motionDirective = previewPlaybackState.toMotionDirective(isVisible = true),
            motionPhase = 1.2f,
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



