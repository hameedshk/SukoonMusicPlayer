package com.sukoon.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Playlist
import com.sukoon.music.domain.model.ScanState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.BannerAdView
import com.sukoon.music.ui.permissions.rememberAudioPermissionState
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.theme.SukoonOrange
import com.sukoon.music.ui.theme.SpacingLarge
import com.sukoon.music.ui.theme.SpacingMedium
import com.sukoon.music.ui.theme.SpacingSmall
import com.sukoon.music.ui.theme.SpacingXLarge
import com.sukoon.music.ui.theme.DarkCard
import com.sukoon.music.ui.theme.DarkCardElevated
import com.sukoon.music.ui.theme.ActionButtonShape
import com.sukoon.music.ui.theme.PillShape
import com.sukoon.music.ui.theme.TextPrimary
import com.sukoon.music.ui.theme.TextSecondary
import com.sukoon.music.ui.theme.MiniPlayerAlbumArtSize
import com.sukoon.music.ui.theme.MiniPlayerHeight
import com.sukoon.music.ui.theme.MiniPlayerShape
import com.sukoon.music.ui.theme.LastAddedCardWidth
import com.sukoon.music.ui.theme.LastAddedCardHeight
import com.sukoon.music.ui.util.accentColor
import com.sukoon.music.ui.util.rememberAlbumPalette
import com.sukoon.music.ui.viewmodel.HomeViewModel
import com.sukoon.music.ui.viewmodel.PlaylistViewModel

/**
 * Home Screen - Main entry point of the app.
 *
 * Features:
 * - Scan button to discover local music
 * - Progress indicator during scanning
 * - Song list with play action
 * - Bottom playback controls (mini player)
 * - Navigation to Search, Liked Songs, Albums, and Settings
 *
 * Follows Material 3 design with Spotify-inspired dark theme.
 *
 * @param onNavigateToNowPlaying Callback to navigate to Now Playing screen
 * @param onNavigateToSearch Callback to navigate to Search screen
 * @param onNavigateToLikedSongs Callback to navigate to Liked Songs screen
 * @param onNavigateToAlbums Callback to navigate to Albums screen
 * @param onNavigateToArtists Callback to navigate to Artists screen
 * @param onNavigateToSettings Callback to navigate to Settings screen
 * @param viewModel ViewModel for home screen state and actions
 */
@Composable
fun HomeScreen(
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToLikedSongs: () -> Unit = {},
    onNavigateToAlbums: () -> Unit = {},
    onNavigateToArtists: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSongs: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    onNavigateToFolders: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val adMobManager = viewModel.adMobManager

    // Tab selection state
    var selectedTab by remember { mutableStateOf("For you") }

    // Handle tab selection - navigates for Playlist and Folders, changes content for others
    val handleTabSelection: (String) -> Unit = { tab ->
        when (tab) {
            "Playlist" -> onNavigateToPlaylists()
            "Folders" -> onNavigateToFolders()
            else -> selectedTab = tab
        }
    }

    // Permission handling
    val permissionState = rememberAudioPermissionState(
        onPermissionGranted = {
            // Auto-scan when permission is granted
            viewModel.scanLocalMusic()
        }
    )

    Scaffold(
        topBar = {
            Column {
                RedesignedTopBar(
                    onPremiumClick = { /* TODO: Navigate to Premium screen */ },
                    onGlobalSearchClick = onNavigateToSearch,
                    onSettingsClick = onNavigateToSettings
                )
                // Global Tabs - always visible
                TabPills(
                    selectedTab = selectedTab,
                    onTabSelected = handleTabSelection
                )
            }
        },
        bottomBar = {
            Column {
                // Redesigned Mini Player
                if (playbackState.currentSong != null) {
                    RedesignedMiniPlayer(
                        playbackState = playbackState,
                        onPlayPauseClick = { viewModel.playPause() },
                        onNextClick = { viewModel.seekToNext() },
                        onClick = onNavigateToNowPlaying
                    )
                }

                // Global Banner Ad - always visible
                BannerAdView(
                    adMobManager = adMobManager,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Show scan progress
                scanState is ScanState.Scanning -> {
                    ScanProgressView(scanState as ScanState.Scanning)
                }
                // Show empty state
                songs.isEmpty() -> {
                    EmptyState(
                        scanState = scanState,
                        hasPermission = permissionState.hasPermission,
                        onScanClick = {
                            if (permissionState.hasPermission) {
                                viewModel.scanLocalMusic()
                            } else {
                                permissionState.requestPermission()
                            }
                        }
                    )
                }
                // Show content based on selected tab
                else -> {
                    when (selectedTab) {
                        "For you" -> {
                            ForYouContent(
                                songs = songs,
                                recentlyPlayed = recentlyPlayed,
                                playbackState = playbackState,
                                onSongClick = { song, index ->
                                    viewModel.playQueue(songs, index)
                                },
                                onRecentlyPlayedClick = { song ->
                                    viewModel.playSong(song)
                                },
                                onLikeClick = { song ->
                                    viewModel.toggleLike(song.id, song.isLiked)
                                },
                                onShuffleAllClick = { viewModel.shuffleAll() },
                                onPlayAllClick = { viewModel.playAll() },
                                onScanClick = {
                                    if (permissionState.hasPermission) {
                                        viewModel.scanLocalMusic()
                                    } else {
                                        permissionState.requestPermission()
                                    }
                                },
                                onSettingsClick = onNavigateToSettings
                            )
                        }
                        "Songs" -> {
                            SongsContent(
                                songs = songs,
                                playbackState = playbackState,
                                onSongClick = { song, index ->
                                    viewModel.playQueue(songs, index)
                                },
                                onLikeClick = { song ->
                                    viewModel.toggleLike(song.id, song.isLiked)
                                }
                            )
                        }
                        "Folders" -> {
                            FoldersPlaceholder()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onScanClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Sukoon Music",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            }
            IconButton(onClick = onScanClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Scan for music"
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun ScanProgressView(
    scanState: ScanState.Scanning
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Scanning for music...",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Found ${scanState.scannedCount} songs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        scanState.message?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyState(
    scanState: ScanState,
    hasPermission: Boolean,
    onScanClick: () -> Unit
) {
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = when {
                !hasPermission -> "Permission Required"
                scanState is ScanState.Error -> "Scan failed"
                scanState is ScanState.Success -> if (scanState.totalSongs == 0) "No music found" else "No songs"
                else -> "No music found"
            },
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when {
                !hasPermission -> "Grant permission to access your music library and discover local songs"
                scanState is ScanState.Error -> scanState.error
                scanState is ScanState.Success -> if (scanState.totalSongs == 0) "Tap the button below to scan for local music" else "Tap scan to refresh"
                else -> "Tap the button below to scan for local music"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onScanClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(
                imageVector = if (hasPermission) Icons.Default.Refresh else Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (hasPermission) "Scan for Music" else "Grant Permission")
        }
    }
}

/**
 * "For you" tab content - shows personalized content with widget banner, action buttons, last added, and all songs.
 */
@Composable
private fun ForYouContent(
    songs: List<Song>,
    recentlyPlayed: List<Song>,
    playbackState: PlaybackState,
    onSongClick: (Song, Int) -> Unit,
    onRecentlyPlayedClick: (Song) -> Unit,
    onLikeClick: (Song) -> Unit,
    onShuffleAllClick: () -> Unit,
    onPlayAllClick: () -> Unit,
    onScanClick: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Widget Banner
        item {
            WidgetBanner(onClick = { /* TODO: Open widget configuration */ })
        }

        // Action Button Grid (2x2)
        item {
            ActionButtonGrid(
                onShuffleClick = onShuffleAllClick,
                onPlayClick = onPlayAllClick,
                onScanClick = onScanClick,
                onSettingsClick = onSettingsClick
            )
        }

        // Last Added Section
        if (songs.isNotEmpty()) {
            item {
                LastAddedSection(
                    songs = songs,
                    onSongClick = { song ->
                        val index = songs.indexOf(song)
                        onSongClick(song, index)
                    }
                )
            }
        }

        // "All Songs" header
        item {
            Text(
                text = "All Songs",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp)
            )
        }

        // Song List
        items(
            items = songs,
            key = { it.id }
        ) { song ->
            val isPlaying = playbackState.currentSong?.id == song.id && playbackState.isPlaying
            val index = songs.indexOf(song)

            SongItem(
                song = song,
                isPlaying = isPlaying,
                onClick = { onSongClick(song, index) },
                onLikeClick = { onLikeClick(song) }
            )
        }
    }
}

@Composable
private fun RecentlyPlayedGrid(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    // Take only the first 6 songs for the 2x3 grid
    val gridSongs = songs.take(6)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // First row (3 items)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            gridSongs.take(3).forEach { song ->
                RecentlyPlayedItem(
                    song = song,
                    onClick = { onSongClick(song) },
                    modifier = Modifier.weight(1f)
                )
            }
            // Fill empty slots if less than 3 songs
            repeat(3 - minOf(gridSongs.size, 3)) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Second row (3 items)
        if (gridSongs.size > 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gridSongs.drop(3).take(3).forEach { song ->
                    RecentlyPlayedItem(
                        song = song,
                        onClick = { onSongClick(song) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty slots if less than 6 songs total
                repeat(6 - gridSongs.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RecentlyPlayedItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(56.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            Card(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        error = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Song Title
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SongItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                error = {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Song details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${song.artist} • ${song.album}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Like button
        IconButton(onClick = onLikeClick) {
            Icon(
                imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (song.isLiked) "Unlike" else "Like",
                tint = if (song.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MiniPlayer(
    playbackState: PlaybackState,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onClick: () -> Unit
) {
    // Extract colors from album art for dynamic theming
    val palette = rememberAlbumPalette(playbackState.currentSong?.albumArtUri)
    val accentColor = palette.accentColor

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (playbackState.currentSong?.albumArtUri != null) {
                    SubcomposeAsyncImage(
                        model = playbackState.currentSong.albumArtUri,
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        error = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playbackState.currentSong?.title ?: "No song",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playbackState.currentSong?.artist ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Playback controls
            IconButton(onClick = onPreviousClick) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(accentColor, CircleShape)
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(onClick = onNextClick) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SukoonMusicPlayerTheme {
        // Preview requires theme setup
        Surface(modifier = Modifier.fillMaxSize()) {
            EmptyState(
                scanState = ScanState.Idle,
                hasPermission = false,
                onScanClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SongItemPreview() {
    SukoonMusicPlayerTheme {
        Surface {
            SongItem(
                song = Song(
                    id = 1,
                    title = "Test Song",
                    artist = "Test Artist",
                    album = "Test Album",
                    duration = 180000,
                    uri = "",
                    albumArtUri = null,
                    dateAdded = 0,
                    isLiked = false
                ),
                isPlaying = false,
                onClick = {},
                onLikeClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MiniPlayerPreview() {
    SukoonMusicPlayerTheme {
        MiniPlayer(
            playbackState = PlaybackState(
                isPlaying = true,
                currentSong = Song(
                    id = 1,
                    title = "Test Song",
                    artist = "Test Artist",
                    album = "Test Album",
                    duration = 180000,
                    uri = "",
                    albumArtUri = null,
                    dateAdded = 0,
                    isLiked = false
                )
            ),
            onPlayPauseClick = {},
            onNextClick = {},
            onPreviousClick = {},
            onClick = {}
        )
    }
}

// ============================================================================
// REDESIGNED COMPONENTS (New Design)
// ============================================================================

/**
 * Redesigned top bar with SukoonMusic logo and action icons.
 */
@Composable
private fun RedesignedTopBar(
    onPremiumClick: () -> Unit,
    onGlobalSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingLarge, vertical = SpacingMedium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sukoon Music Logo
        Text(
            text = "Sukoon Music",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = SukoonOrange
        )

        // Action Icons Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Premium/Diamond Icon
            IconButton(onClick = onPremiumClick) {
                Icon(
                    imageVector = Icons.Default.Star, // Using Star for premium
                    contentDescription = "Premium",
                    tint = SukoonOrange
                )
            }

            // Global Search Icon
            IconButton(onClick = onGlobalSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            // Settings Icon
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

/**
 * Horizontal scrollable tab pills (For you, Songs, Playlist, Folders).
 */
@Composable
private fun TabPills(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf("For you", "Songs", "Playlist", "Folders")

    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingLarge, vertical = SpacingMedium),
        horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
    ) {
        items(tabs.size) { index ->
            val tab = tabs[index]
            val isSelected = tab == selectedTab

            Surface(
                modifier = Modifier
                    .height(44.dp)
                    .clickable { onTabSelected(tab) },
                shape = PillShape,
                color = if (isSelected) SukoonOrange else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = if (isSelected) 4.dp else 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) androidx.compose.ui.graphics.Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Widget promotion banner.
 */
@Composable
private fun WidgetBanner(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingLarge, vertical = SpacingMedium)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SpacingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Album, // Using Album as grid icon
                    contentDescription = null,
                    tint = SukoonOrange,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Add widgets to your home screen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SukoonOrange
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Go",
                tint = SukoonOrange,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 2x2 Action button grid (Shuffle, Play, Scan, Settings).
 */
@Composable
private fun ActionButtonGrid(
    onShuffleClick: () -> Unit,
    onPlayClick: () -> Unit,
    onScanClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingLarge, vertical = SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(SpacingMedium)
    ) {
        // First Row: Shuffle, Play
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            ActionButton(
                text = "Shuffle",
                icon = Icons.Default.Shuffle,
                onClick = onShuffleClick,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = "Play",
                icon = Icons.Default.PlayArrow,
                onClick = onPlayClick,
                modifier = Modifier.weight(1f)
            )
        }

        // Second Row: Scan music, Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            ActionButton(
                text = "Scan music",
                icon = Icons.Default.Refresh,
                onClick = onScanClick,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = "Settings",
                icon = Icons.Default.Settings,
                onClick = onSettingsClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual action button.
 */
@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick),
        shape = ActionButtonShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(SpacingLarge),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = SukoonOrange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(SpacingMedium))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * "Last Added" section with horizontal scrolling song cards.
 */
@Composable
private fun LastAddedSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SpacingMedium)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Last added",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "See all",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(SpacingMedium))

        // Horizontal scrolling song cards
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = SpacingLarge),
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            items(songs.size.coerceAtMost(10)) { index ->
                LastAddedCard(
                    song = songs[index],
                    onClick = { onSongClick(songs[index]) }
                )
            }
        }
    }
}

/**
 * Individual card for Last Added section.
 */
@Composable
private fun LastAddedCard(
    song: Song,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(160.dp)
            .height(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Album art or music note icon
            if (song.albumArtUri != null) {
                SubcomposeAsyncImage(
                    model = song.albumArtUri,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                )
            } else {
                // No album art - show music note
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            // Play button overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
                            )
                        )
                    ),
                contentAlignment = Alignment.BottomEnd
            ) {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = SukoonOrange,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Redesigned Mini Player with larger, centered play button.
 */
@Composable
private fun RedesignedMiniPlayer(
    playbackState: PlaybackState,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onClick: () -> Unit
) {
    val palette = rememberAlbumPalette(playbackState.currentSong?.albumArtUri)
    val accentColor = palette.accentColor

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .padding(horizontal = SpacingLarge, vertical = SpacingMedium)
            .clickable(onClick = onClick),
        shape = MiniPlayerShape,
        color = DarkCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            // Album Art (56dp)
            if (playbackState.currentSong != null) {
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
            } else {
                Box(
                    modifier = Modifier
                        .size(MiniPlayerAlbumArtSize)
                        .clip(RoundedCornerShape(8.dp))
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

            // Song Info (Title + Artist)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = playbackState.currentSong?.title ?: "No song playing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playbackState.currentSong?.artist ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Large Play/Pause Button (48dp, centered)
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
    }
}

/**
 * "Songs" tab content - shows all songs in alphabetical order with search.
 */
@Composable
private fun SongsContent(
    songs: List<Song>,
    playbackState: PlaybackState,
    onSongClick: (Song, Int) -> Unit,
    onLikeClick: (Song) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Sort songs alphabetically and filter by search query
    val filteredSongs = remember(songs, searchQuery) {
        songs
            .sortedBy { it.title.lowercase() }
            .filter { song ->
                searchQuery.isEmpty() ||
                song.title.contains(searchQuery, ignoreCase = true) ||
                song.artist.contains(searchQuery, ignoreCase = true) ||
                song.album.contains(searchQuery, ignoreCase = true)
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingLarge, vertical = SpacingMedium),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingLarge, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Search songs...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                    singleLine = true
                )

                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search"
                        )
                    }
                }
            }
        }

        // Songs List
        if (filteredSongs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isEmpty()) "No songs" else "No results",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = filteredSongs,
                    key = { it.id }
                ) { song ->
                    val isPlaying = playbackState.currentSong?.id == song.id && playbackState.isPlaying
                    val index = songs.indexOf(song)

                    SongItem(
                        song = song,
                        isPlaying = isPlaying,
                        onClick = { onSongClick(song, index) },
                        onLikeClick = { onLikeClick(song) }
                    )
                }
            }
        }
    }
}

/**
 * "Folders" tab content - placeholder for future implementation.
 */
@Composable
private fun FoldersPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Folders",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Coming soon",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Playlist card component used in the playlists grid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenameClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Cover Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (playlist.coverImageUri != null) {
                    SubcomposeAsyncImage(
                        model = playlist.coverImageUri,
                        contentDescription = "Playlist cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        error = {
                            DefaultPlaylistCover()
                        }
                    )
                } else {
                    DefaultPlaylistCover()
                }

                // Options menu button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                onRenameClick()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                onDeleteClick()
                                showMenu = false
                            }
                        )
                    }
                }
            }

            // Playlist Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${playlist.songCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Default playlist cover when no album art is available.
 */
@Composable
private fun DefaultPlaylistCover() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Song selection dialog for adding songs to a playlist.
 * Shows all available songs with checkboxes for multi-selection.
 */
@Composable
fun SongSelectionDialog(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    var selectedSongIds by remember { mutableStateOf(setOf<Long>()) }
    var searchQuery by remember { mutableStateOf("") }

    // Filter songs by search query
    val filteredSongs = remember(songs, searchQuery) {
        songs.filter { song ->
            searchQuery.isEmpty() ||
            song.title.contains(searchQuery, ignoreCase = true) ||
            song.artist.contains(searchQuery, ignoreCase = true) ||
            song.album.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Songs to Playlist",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search songs...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selected count
                Text(
                    text = "${selectedSongIds.size} songs selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Song list with checkboxes
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(
                        items = filteredSongs,
                        key = { it.id }
                    ) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSongIds = if (selectedSongIds.contains(song.id)) {
                                        selectedSongIds - song.id
                                    } else {
                                        selectedSongIds + song.id
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedSongIds.contains(song.id),
                                onCheckedChange = { checked ->
                                    selectedSongIds = if (checked) {
                                        selectedSongIds + song.id
                                    } else {
                                        selectedSongIds - song.id
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedSongIds.toList())
                },
                enabled = selectedSongIds.isNotEmpty()
            ) {
                Text("Add to Playlist")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
