package com.sukoon.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.*
import com.sukoon.music.ui.components.*
import com.sukoon.music.ui.components.FolderViewMode
import com.sukoon.music.ui.components.CategoryPillRow
import com.sukoon.music.ui.components.FolderContextHeader
import com.sukoon.music.ui.components.FolderRow
import com.sukoon.music.ui.permissions.rememberAudioPermissionState
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.theme.SpacingLarge
import com.sukoon.music.ui.theme.SpacingMedium
import com.sukoon.music.ui.theme.ActionButtonShape
import com.sukoon.music.ui.theme.PillShape
import com.sukoon.music.ui.viewmodel.HomeViewModel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.data.premium.PremiumManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
/**
 * Home Screen - Main entry point of the app.
 */
@Composable
fun HomeScreen(
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToLikedSongs: () -> Unit = {},
    onNavigateToArtists: () -> Unit = {},
    onNavigateToArtistDetail: (Long) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSongs: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    onNavigateToFolders: () -> Unit = {},
    onNavigateToGenres: () -> Unit = {},
    onNavigateToPlaylistDetail: (Long) -> Unit = {},
    onNavigateToSmartPlaylist: (SmartPlaylistType) -> Unit = {},
    onNavigateToFolderDetail: (Long) -> Unit = {},
    onNavigateToAlbumDetail: (Long) -> Unit = {},
    onNavigateToGenreDetail: (Long) -> Unit = {},
    username: String = "",
    viewModel: HomeViewModel = hiltViewModel(),
    playlistViewModel: com.sukoon.music.ui.viewmodel.PlaylistViewModel = hiltViewModel(),
    folderViewModel: com.sukoon.music.ui.viewmodel.FolderViewModel = hiltViewModel()
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val rediscoverAlbums by viewModel.rediscoverAlbums.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val isUserInitiatedScan by viewModel.isUserInitiatedScan.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

    // Get PremiumManager to check if user is premium (for ad injection)
    val context = LocalContext.current
    val premiumManager = try {
        EntryPointAccessors.fromApplication(context, com.sukoon.music.ui.navigation.PremiumManagerEntryPoint::class.java).premiumManager()
    } catch (e: Exception) {
        null
    }
    val isPremium by (premiumManager?.isPremiumUser?.collectAsStateWithLifecycle(false) ?: remember { mutableStateOf(false) })

    // Use provided username or default greeting
    val displayUsername = username.ifBlank { "there" }
    val defaultTab = "Hi $username"

    // Use ViewModel's tab state (persisted to DataStore, survives app restart)
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    val handleTabSelection: (String) -> Unit = { tab ->
        viewModel.setSelectedTab(tab)
    }

    val tabs = listOf(
        "Hi $username",
        "Songs",
        "Playlist",
        "Folders",
        "Albums",
        "Artists",
        "Genres"
    )

    // Delete state and launcher
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    val context = LocalContext.current

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "Song deleted successfully", Toast.LENGTH_SHORT).show()
            viewModel.scanLocalMusic()
        } else {
            Toast.makeText(context, "Delete cancelled", Toast.LENGTH_SHORT).show()
        }
        songToDelete = null
    }

    // Share handler
    val shareHandler = rememberShareHandler()

    // Create menu handler for song context menu
    val menuHandler = rememberSongMenuHandler(
        playbackRepository = viewModel.playbackRepository,
        onNavigateToAlbum = onNavigateToAlbumDetail,
        onNavigateToArtist = onNavigateToArtistDetail,
        onShowDeleteConfirmation = { song -> songToDelete = song },
        onToggleLike = { songId, isLiked -> viewModel.toggleLike(songId, isLiked) },
        onShare = shareHandler
    )

    val permissionState = rememberAudioPermissionState(
        onPermissionGranted = {
            viewModel.scanLocalMusic()
        }
    )

    // Auto-scan on startup is now handled by HomeViewModel.tryStartupScan()
    // This prevents duplicate scans and respects the scanOnStartup preference + 30-min deduplication

    // Show toast only for user-initiated scans (not startup scans)
    LaunchedEffect(scanState, isUserInitiatedScan) {
        if (isUserInitiatedScan) {
            if (scanState is ScanState.Success) {
                val totalSongs = (scanState as ScanState.Success).totalSongs
                Toast.makeText(
                    context,
                    "Scan completed: $totalSongs songs found",
                    Toast.LENGTH_LONG
                ).show()
                // Reset flag after showing toast
                viewModel.resetUserInitiatedScanFlag()
            } else if (scanState is ScanState.Error) {
                val errorMsg = (scanState as ScanState.Error).error
                Toast.makeText(context, "Scan failed: $errorMsg", Toast.LENGTH_LONG).show()
                // Reset flag after showing toast
                viewModel.resetUserInitiatedScanFlag()
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                RedesignedTopBar(
                    onPremiumClick = { },
                    onGlobalSearchClick = onNavigateToSearch,
                    onSettingsClick = onNavigateToSettings,
                    sessionState = sessionState
                )
                TabPills(
                    tabs = tabs,
                    selectedTab = selectedTab ?: defaultTab,
                    onTabSelected = { handleTabSelection(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

        },
        bottomBar = {
            Column {
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                scanState is ScanState.Scanning -> {
                    ScanProgressView(scanState as ScanState.Scanning)
                }
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
                else -> {
                    when (selectedTab ?: defaultTab) {
                        "Hi $username" -> {
                            HomeTab(
                                songs = songs,
                                recentlyPlayed = recentlyPlayed,
                                rediscoverAlbums = rediscoverAlbums,
                                playbackState = playbackState,
                                viewModel = viewModel,
                                onNavigateToNowPlaying = onNavigateToNowPlaying,
                                onNavigateToAlbumDetail = onNavigateToAlbumDetail,
                                onNavigateToSmartPlaylist = onNavigateToSmartPlaylist,
                                onSettingsClick = onNavigateToSettings
                            )
                        }
                        "Songs" -> {
                            SongsContent(
                                songs = songs,
                                playbackState = playbackState,
                                onSongClick = { song, index ->
                                    if (playbackState.currentSong?.id != song.id) {
                                        viewModel.playQueue(songs, index)
                                    } else {
                                        onNavigateToNowPlaying()
                                    }
                                },
                                onShuffleAllClick = { viewModel.shuffleAll() },
                                onPlayAllClick = { viewModel.playAll() },
                                viewModel = viewModel,
                                playlistViewModel = playlistViewModel,
                                onNavigateToArtistDetail = onNavigateToArtistDetail,
                                onNavigateToAlbumDetail = onNavigateToAlbumDetail
                            )
                        }
                        "Albums" -> {
                            AlbumsScreen(
                                onNavigateToAlbum = onNavigateToAlbumDetail,
                                onBackClick = { }
                            )
                        }
                        "Artists" -> {
                            ArtistsScreen(
                                onNavigateToArtistDetail = onNavigateToArtistDetail,
                                onBackClick = { }
                            )
                        }
                        "Genres" -> {
                            GenresScreen(
                                onNavigateToGenre = onNavigateToGenreDetail,
                                onBackClick = { /* optional: navController.popBackStack() */ }
                            )
                        }
                        "Playlist" -> {
                            PlaylistsScreen(
                               onNavigateToPlaylist = onNavigateToPlaylistDetail,
                               onNavigateToSmartPlaylist = onNavigateToSmartPlaylist,
                                onBackClick = { }
                            )
                        }
                        "Folders" -> {
                            FoldersScreen(
                                onNavigateToFolder = onNavigateToFolderDetail,
                                onNavigateToNowPlaying = onNavigateToNowPlaying,
                                onBackClick = {}

                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    songToDelete?.let { song ->
        DeleteConfirmationDialog(
            song = song,
            onConfirm = {
                when (val result = DeleteHelper.deleteSongs(context, listOf(song))) {
                    is DeleteHelper.DeleteResult.RequiresPermission -> {
                        deleteLauncher.launch(
                            IntentSenderRequest.Builder(result.intentSender).build()
                        )
                    }
                    is DeleteHelper.DeleteResult.Success -> {
                        Toast.makeText(context, "Song deleted successfully", Toast.LENGTH_SHORT).show()
                        viewModel.scanLocalMusic()
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

        Spacer(modifier = Modifier.height(24.dp))

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



// Supporting Functions for PlaylistsContent

@Composable
private fun PlaylistFilterChips(
    selectedFilter: Boolean?,
    onFilterChange: (Boolean?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterChange(null) },
                label = { Text("All") }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == true,
                onClick = { onFilterChange(true) },
                label = { Text("Smart Playlists") },
                leadingIcon = if (selectedFilter == true) {
                    { Icon(Icons.Default.Star, null, Modifier.size(18.dp)) }
                } else null
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == false,
                onClick = { onFilterChange(false) },
                label = { Text("My Playlists") },
                leadingIcon = if (selectedFilter == false) {
                    { Icon(Icons.Default.Folder, null, Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartPlaylistCard(
    smartPlaylist: SmartPlaylist,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = smartPlaylist.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${smartPlaylist.songCount} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = getSmartPlaylistIcon(smartPlaylist.type),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
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
                        error = { DefaultPlaylistCover() }
                    )
                } else {
                    DefaultPlaylistCover()
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun EmptyAlbumsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Album,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No albums found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongsContent(
    songs: List<Song>,
    playbackState: PlaybackState,
    onSongClick: (Song, Int) -> Unit,
    onShuffleAllClick: () -> Unit,
    onPlayAllClick: () -> Unit,
    viewModel: HomeViewModel,
    playlistViewModel: com.sukoon.music.ui.viewmodel.PlaylistViewModel,
    onNavigateToArtistDetail: (Long) -> Unit = {},
    onNavigateToAlbumDetail: (Long) -> Unit = {}
) {
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()
    val isSongSelectionMode by viewModel.isSongSelectionMode.collectAsStateWithLifecycle()
    val selectedSongIds by viewModel.selectedSongIds.collectAsStateWithLifecycle()
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var songSongsForPlaylist by remember { mutableStateOf<List<Song>>(emptyList()) }
    var pendingSongsForPlaylist by remember { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf("Song name") }
    var sortOrder by rememberSaveable { mutableStateOf("A to Z") }
    var showSortDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedSongId by remember { mutableStateOf<Long?>(null) }
    var showInfoForSong by remember { mutableStateOf<Song?>(null) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var songsPendingDeletion by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "Songs deleted successfully", Toast.LENGTH_SHORT).show()
            songsPendingDeletion = false
        } else {
            Toast.makeText(context, "Delete cancelled", Toast.LENGTH_SHORT).show()
        }
        songsPendingDeletion = false
    }

    val shareHandler = rememberShareHandler()

    val menuHandler = rememberSongMenuHandler(
        playbackRepository = viewModel.playbackRepository,
        onNavigateToAlbum = onNavigateToAlbumDetail,
        onNavigateToArtist = onNavigateToArtistDetail,
        onShowPlaylistSelector = { song ->
            showAddToPlaylistDialog = true
        },
        onShowSongInfo = { song -> showInfoForSong = song },
        onShowDeleteConfirmation = { song -> songToDelete = song },
        onToggleLike = { songId, isLiked -> viewModel.toggleLike(songId, isLiked) },
        onShare = shareHandler
    )

    val sortedSongs = remember(songs, sortMode, sortOrder) {
        val sorted = when (sortMode) {
            "Song name" -> songs.sortedBy { it.title.lowercase() }
            "Artist name" -> songs.sortedBy { it.artist.lowercase() }
            "Album name" -> songs.sortedBy { it.album.lowercase() }
            "Folder name" -> songs.sortedBy { it.path.lowercase() }
            "Time added" -> songs.sortedByDescending { it.dateAdded }
            "Play count" -> songs.sortedByDescending { it.playCount }
            "Year" -> songs.sortedByDescending { it.year }
            "Duration" -> songs.sortedByDescending { it.duration }
            "Size" -> songs.sortedByDescending { it.size }
            else -> songs.sortedBy { it.title.lowercase() }
        }
        if (sortOrder == "Z to A") sorted.reversed() else sorted
    }

    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#@".toList()
    val currentHighlightChar = remember {
        derivedStateOf {
            if (sortedSongs.isEmpty()) null
            else {
                val firstVisibleIndex = scrollState.firstVisibleItemIndex
                if (firstVisibleIndex in sortedSongs.indices) {
                    val char = sortedSongs[firstVisibleIndex].title.firstOrNull()?.uppercaseChar()
                    if (char != null && char in 'A'..'Z') char else '#'
                } else null
            }
        }
    }

    // Handle pending songs for playlist
    LaunchedEffect(pendingSongsForPlaylist, selectedSongIds) {
        if (pendingSongsForPlaylist) {
            if (selectedSongIds.isNotEmpty()) {
                val selectedSongs = sortedSongs.filter { it.id in selectedSongIds }
                songSongsForPlaylist = selectedSongs
                showAddToPlaylistDialog = true
            }
            pendingSongsForPlaylist = false
        }
    }

    Scaffold(
        topBar = {
            if (isSongSelectionMode) {
                TopAppBar(
                    title = {
                        Text("${selectedSongIds.size} selected")
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                viewModel.toggleSongSelectionMode(false)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit selection"
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isSongSelectionMode && selectedSongIds.isNotEmpty()) {
                MultiSelectActionBottomBar(
                    onPlay = { viewModel.playSelectedSongs() },
                    onAddToPlaylist = { pendingSongsForPlaylist = true },
                    onDelete = { songsPendingDeletion = true },
                    onPlayNext = { viewModel.playSelectedSongsNext() },
                    onAddToQueue = { viewModel.addSelectedSongsToQueue() }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (!isSongSelectionMode) {
                    stickyHeader(key = "header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${sortedSongs.size} songs",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = { showSortDialog = true }) {
                                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                                }
                                IconButton(onClick = { viewModel.toggleSongSelectionMode(true) }) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Select")
                                }
                            }
                        }
                    }
                    stickyHeader(key = "chips") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clickable(onClick = onShuffleAllClick),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Shuffle All", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clickable(onClick = onPlayAllClick),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Play", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
                // Inject native ads every 15 songs (only if user is not premium)
                val displayItems = if (!isPremium && !isSongSelectionMode) {
                    injectNativeAds(sortedSongs, interval = 15)
                } else {
                    sortedSongs.map { ListItem.SongItem(it) }
                }

                items(
                    items = displayItems,
                    key = { item ->
                        when (item) {
                            is ListItem.SongItem<*> -> "song_${(item.item as Song).id}"
                            is ListItem.AdItem<*> -> "ad_${item.hashCode()}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is ListItem.SongItem<*> -> {
                            val song = item.item as Song
                            val isPlaying = playbackState.currentSong?.id == song.id && playbackState.isPlaying
                            val index = songs.indexOf(song)
                            val isSelected = selectedSongIds.contains(song.id)

                            if (isSongSelectionMode) {
                                SongItemSelectable(
                                    song = song,
                                    isSelected = isSelected,
                                    onClick = {
                                        viewModel.toggleSongSelection(song.id)
                                    }
                                )
                            } else {
                                SongItemWithMenu(
                                    song = song,
                                    isPlaying = isPlaying,
                                    onClick = { onSongClick(song, index) },
                                    onMenuClick = {
                                        showMenu = true
                                        selectedSongId = song.id
                                    }
                                )
                            }
                        }
                        is ListItem.AdItem<*> -> {
                            // Native ad card (only shown if not premium)
                            NativeAdCard(
                                onAdClick = { /* Open advertiser */ }
                            )
                        }
                    }
                }
            }

            if (!isSongSelectionMode) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .background(Color.Black.copy(alpha = 0.1f), CircleShape),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    alphabet.forEach { char ->
                        val isHighlighted = char == currentHighlightChar.value
                        Text(
                            text = char.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                            ),
                            modifier = Modifier
                                .padding(vertical = 1.dp, horizontal = 4.dp)
                                .clickable {
                                    coroutineScope.launch {
                                        val targetIndex = sortedSongs.indexOfFirst {
                                            val firstChar = it.title.firstOrNull()?.uppercaseChar()
                                            when (char) {
                                                '#' -> firstChar == null || firstChar !in 'A'..'Z'
                                                '@' -> false
                                                else -> firstChar == char
                                            }
                                        }
                                        if (targetIndex != -1) scrollState.animateScrollToItem(targetIndex)
                                    }
                                },
                            color = if (isHighlighted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showSortDialog) {
        SongSortDialog(
            currentSortMode = sortMode,
            currentOrder = sortOrder,
            onDismiss = { showSortDialog = false },
            onConfirm = { newMode, newOrder ->
                sortMode = newMode
                sortOrder = newOrder
                showSortDialog = false
            }
        )
    }

    if (showMenu && selectedSongId != null) {
        val freshSong = sortedSongs.find { it.id == selectedSongId }
        if (freshSong != null) {
            SongContextMenu(
                song = freshSong,
                menuHandler = menuHandler,
                onDismiss = { showMenu = false }
            )
        }
    }

    // Add to playlist dialog for multi-select
    if (showAddToPlaylistDialog && songSongsForPlaylist.isNotEmpty()) {
        AddToPlaylistDialog(
            playlists = playlists,
            onPlaylistSelected = { playlistId ->
                songSongsForPlaylist.forEach { song ->
                    playlistViewModel.addSongToPlaylist(playlistId, song.id)
                }
                showAddToPlaylistDialog = false
                Toast.makeText(context, "Songs added to playlist", Toast.LENGTH_SHORT).show()
                viewModel.toggleSongSelectionMode(false)
            },
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }

    // Delete confirmation dialog for multi-select
    if (songsPendingDeletion) {
        AlertDialog(
            onDismissRequest = { songsPendingDeletion = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Delete ${selectedSongIds.size} song(s)?")
            },
            text = {
                Text("These songs will be permanently deleted from your device. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedSongsWithResult { deleteResult ->
                            when (deleteResult) {
                                is DeleteHelper.DeleteResult.RequiresPermission -> {
                                    deleteLauncher.launch(
                                        IntentSenderRequest.Builder(deleteResult.intentSender).build()
                                    )
                                }
                                is DeleteHelper.DeleteResult.Success -> {
                                    Toast.makeText(context, "Songs deleted successfully", Toast.LENGTH_SHORT).show()
                                    songsPendingDeletion = false
                                    viewModel.toggleSongSelectionMode(false)
                                }
                                is DeleteHelper.DeleteResult.Error -> {
                                    Toast.makeText(context, "Error: ${deleteResult.message}", Toast.LENGTH_SHORT).show()
                                    songsPendingDeletion = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { songsPendingDeletion = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMenu && selectedSongId != null) {
        val freshSong = sortedSongs.find { it.id == selectedSongId }
        if (freshSong != null) {
            SongContextMenu(
                song = freshSong,
                menuHandler = menuHandler,
                onDismiss = { showMenu = false }
            )
        }
    }

    showInfoForSong?.let { song ->
        SongInfoDialog(
            song = song,
            onDismiss = { showInfoForSong = null }
        )
    }

    songToDelete?.let { song ->
        DeleteConfirmationDialog(
            song = song,
            onConfirm = {
                when (val result = menuHandler.performDelete(song)) {
                    is DeleteHelper.DeleteResult.RequiresPermission -> {
                        deleteLauncher.launch(
                            IntentSenderRequest.Builder(result.intentSender).build()
                        )
                    }
                    is DeleteHelper.DeleteResult.Success -> {
                        Toast.makeText(context, "Song deleted successfully", Toast.LENGTH_SHORT).show()
                        viewModel.scanLocalMusic()
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

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SukoonMusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            EmptyState(
                scanState = ScanState.Idle,
                hasPermission = false,
                onScanClick = {}
            )
        }
    }
}