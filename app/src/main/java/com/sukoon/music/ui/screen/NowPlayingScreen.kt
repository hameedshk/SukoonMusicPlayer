package com.sukoon.music.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.data.lyrics.LrcParser
import com.sukoon.music.domain.model.LyricLine
import com.sukoon.music.domain.model.LyricsState
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.RepeatMode
import com.sukoon.music.domain.model.Song
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

    // Extract color palette from album art
    val palette = rememberAlbumPalette(playbackState.currentSong?.albumArtUri)
    val accentColor = palette.accentColor

    // Dynamic background gradient colors using album art palette
    val backgroundColor = MaterialTheme.colorScheme.background
    val gradientColors = listOf(
        backgroundColor.copy(alpha = 0.8f),
        accentColor.copy(alpha = 0.15f),
        backgroundColor
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
                        // Queue button
                        IconButton(onClick = onNavigateToQueue) {
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Album Art
        AlbumArtSection(song = song)

        Spacer(modifier = Modifier.height(32.dp))

        // Song Info and Like Button
        SongInfoSection(
            song = song,
            accentColor = accentColor,
            onLikeClick = onLikeClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Seek Bar
        SeekBarSection(
            currentPosition = currentPosition,
            duration = playbackState.duration,
            accentColor = accentColor,
            onSeekTo = onSeekTo
        )

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(32.dp))

        // Lyrics/Queue Tabs Section
        LyricsQueueSection(
            song = song,
            currentPosition = currentPosition,
            accentColor = accentColor,
            viewModel = viewModel
        )
    }
}

@Composable
private fun AlbumArtSection(song: Song) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        IconButton(onClick = onLikeClick) {
            Icon(
                imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (song.isLiked) "Unlike" else "Like",
                tint = if (song.isLiked) accentColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

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
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentPosition),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle button
        IconButton(onClick = onShuffleClick) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (playbackState.shuffleEnabled) {
                    accentColor
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(28.dp)
            )
        }

        // Previous button
        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(40.dp)
            )
        }

        // Play/Pause button (Dynamic accent color)
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .size(72.dp)
                .background(accentColor, CircleShape)
        ) {
            Icon(
                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(40.dp)
            )
        }

        // Next button
        IconButton(
            onClick = onNextClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(40.dp)
            )
        }

        // Repeat button with mode indicator
        IconButton(onClick = onRepeatClick) {
            Icon(
                imageVector = when (playbackState.repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                contentDescription = "Repeat",
                tint = when (playbackState.repeatMode) {
                    RepeatMode.OFF -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    else -> accentColor
                },
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun LyricsQueueSection(
    song: Song,
    currentPosition: Long,
    accentColor: Color,
    viewModel: HomeViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
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
                    onClick = { selectedTab = index },
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
