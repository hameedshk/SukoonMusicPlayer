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
import kotlinx.coroutines.flow.flowOf
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
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.data.ads.AdMobDecisionAgent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import com.sukoon.music.ui.theme.*
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
    onNavigateToSongSelection: () -> Unit = {},
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

    // Get managers from Hilt entry points for use in non-injected contexts
    val appContext = LocalContext.current

    // Get PremiumManager to check if user is premium (for ad injection)
    val premiumManager = try {
        EntryPointAccessors.fromApplication(appContext, com.sukoon.music.ui.navigation.PremiumManagerEntryPoint::class.java).premiumManager()
    } catch (e: Exception) {
        null
    }
    val premiumFlow = premiumManager?.isPremiumUser ?: flowOf(false)
    val isPremium by premiumFlow.collectAsStateWithLifecycle(false)

    // Get AdMobManager for native ad loading
    val adMobManager = try {
        EntryPointAccessors.fromApplication(appContext, com.sukoon.music.ui.navigation.AdMobManagerEntryPoint::class.java).adMobManager()
    } catch (e: Exception) {
        null
    }

    // Get AdMobDecisionAgent for intelligent ad delivery
    val adMobDecisionAgent = try {
        EntryPointAccessors.fromApplication(appContext, com.sukoon.music.ui.navigation.AdMobDecisionAgentEntryPoint::class.java).adMobDecisionAgent()
    } catch (e: Exception) {
        null
    }

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

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(appContext, "Song deleted successfully", Toast.LENGTH_SHORT).show()
            viewModel.scanLocalMusic()
        } else {
            Toast.makeText(appContext, "Delete cancelled", Toast.LENGTH_SHORT).show()
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
                    appContext,
                    "Scan completed: $totalSongs songs found",
                    Toast.LENGTH_LONG
                ).show()
                // Reset flag after showing toast
                viewModel.resetUserInitiatedScanFlag()
            } else if (scanState is ScanState.Error) {
                val errorMsg = (scanState as ScanState.Error).error
                Toast.makeText(appContext, "Scan failed: $errorMsg", Toast.LENGTH_LONG).show()
                // Reset flag after showing toast
                viewModel.resetUserInitiatedScanFlag()
            }
        }
    }

    Scaffold(
        modifier = Modifier.gradientBackground(),
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
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
                            if (adMobManager != null && adMobDecisionAgent != null) {
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
                                    adMobManager = adMobManager,
                                    adMobDecisionAgent = adMobDecisionAgent,
                                    isPremium = isPremium,
                                    onNavigateToArtistDetail = onNavigateToArtistDetail,
                                    onNavigateToAlbumDetail = onNavigateToAlbumDetail,
                                    onNavigateToSongSelection = onNavigateToSongSelection
                                )
                            }
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
                when (val result = DeleteHelper.deleteSongs(appContext, listOf(song))) {
                    is DeleteHelper.DeleteResult.RequiresPermission -> {
                        deleteLauncher.launch(
                            IntentSenderRequest.Builder(result.intentSender).build()
                        )
                    }
                    is DeleteHelper.DeleteResult.Success -> {
                        Toast.makeText(appContext, "Song deleted successfully", Toast.LENGTH_SHORT).show()
                        viewModel.scanLocalMusic()
                        songToDelete = null
                    }
                    is DeleteHelper.DeleteResult.Error -> {
                        Toast.makeText(appContext, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
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
    val accentTokens = accent()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = accentTokens.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Scanning for music...",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(SpacingSmall))

        Text(
            text = "Found ${scanState.scannedCount} songs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        scanState.message?.let { message ->
            Spacer(modifier = Modifier.height(SpacingSmall))
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
            Spacer(modifier = Modifier.width(SpacingSmall))
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
    val accentTokens = accent()
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterChange(null) },
                label = { Text("All") },
                modifier = if (selectedFilter != null) {
                    Modifier
                        .clip(CompactButtonShape)
                        .surfaceLevel2Gradient()
                } else {
                    Modifier
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (selectedFilter != null) Color.Transparent else accentTokens.primary,
                    labelColor = if (selectedFilter != null) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                )
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == true,
                onClick = { onFilterChange(true) },
                label = { Text("Smart Playlists") },
                leadingIcon = if (selectedFilter == true) {
                    { Icon(Icons.Default.Star, null, Modifier.size(18.dp)) }
                } else null,
                modifier = if (selectedFilter != true) {
                    Modifier
                        .clip(CompactButtonShape)
                        .surfaceLevel2Gradient()
                } else {
                    Modifier
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (selectedFilter != true) Color.Transparent else accentTokens.primary,
                    labelColor = if (selectedFilter != true) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                )
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == false,
                onClick = { onFilterChange(false) },
                label = { Text("My Playlists") },
                leadingIcon = if (selectedFilter == false) {
                    { Icon(Icons.Default.Folder, null, Modifier.size(18.dp)) }
                } else null,
                modifier = if (selectedFilter != false) {
                    Modifier
                        .clip(CompactButtonShape)
                        .surfaceLevel2Gradient()
                } else {
                    Modifier
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (selectedFilter != false) Color.Transparent else accentTokens.primary,
                    labelColor = if (selectedFilter != false) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                )
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
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .surfaceLevel1Gradient(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
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
                        style = MaterialTheme.typography.cardTitle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${smartPlaylist.songCount} songs",
                        style = MaterialTheme.typography.cardSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = getSmartPlaylistIcon(smartPlaylist.type),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(SpacingMedium)
                    .size(32.dp),
                tint = accent().primary
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

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .clip(RoundedCornerShape(12.dp))
            .surfaceLevel1Gradient(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
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
                            tint = MaterialTheme.colorScheme.onBackground
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
                    .padding(SpacingMedium)
                    .surfaceLevel1Gradient()
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.cardTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${playlist.songCount} songs",
                    style = MaterialTheme.typography.cardSubtitle,
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
    adMobManager: AdMobManager,
    adMobDecisionAgent: AdMobDecisionAgent,
    isPremium: Boolean = false,
    onNavigateToArtistDetail: (Long) -> Unit = {},
    onNavigateToAlbumDetail: (Long) -> Unit = {},
    onNavigateToSongSelection: () -> Unit = {}
) {
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf("Song name") }
    var sortOrder by rememberSaveable { mutableStateOf("A to Z") }
    var showSortDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedSongId by remember { mutableStateOf<Long?>(null) }
    var showInfoForSong by remember { mutableStateOf<Song?>(null) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Track if user has scrolled (for native ad engagement requirement)
    val hasUserScrolled = remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "Songs deleted successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Delete cancelled", Toast.LENGTH_SHORT).show()
        }
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

    Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 0.dp,  // No extra top padding - handled by parent Scaffold
                    bottom = MiniPlayerHeight + SpacingSmall  // Space for mini player (64dp + 8dp)
                )
            ) {
                stickyHeader(key = "header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${sortedSongs.size} songs",
                                style = MaterialTheme.typography.sectionHeader,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(SpacingSmall)) {
                                IconButton(onClick = { showSortDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Sort,
                                        contentDescription = "Sort",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = onNavigateToSongSelection) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Select",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    stickyHeader(key = "chips") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(CompactButtonShape)
                                    .surfaceLevel2Gradient()
                                    .clickable(onClick = onShuffleAllClick),
                                shape = CompactButtonShape,
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                                    Spacer(Modifier.width(SpacingSmall))
                                    Text("Shuffle All", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(CompactButtonShape)
                                    .surfaceLevel2Gradient()
                                    .clickable(onClick = onPlayAllClick),
                                shape = CompactButtonShape,
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                                    Spacer(Modifier.width(SpacingSmall))
                                    Text("Play", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                                }
                            }
                        }
                    }

                // Inject native ads every 25 songs (only if user is not premium)
                val displayItems = if (!isPremium) {
                    injectNativeAds(sortedSongs, interval = 25)
                } else {
                    sortedSongs.map { ListItem.SongItem(it) }
                }

                // Helper to get album info for an ad (based on previous song)
                val getAlbumForAdItem: (Int) -> Pair<Long, Int>? = { adIndex ->
                    // Find the previous song in displayItems
                    var result: Pair<Long, Int>? = null
                    for (i in adIndex - 1 downTo 0) {
                        val item = displayItems.getOrNull(i)
                        if (item is ListItem.SongItem<*>) {
                            val song = item.item as Song
                            val albumId = song.album.hashCode().toLong()
                            // Count songs with same album in sortedSongs
                            val albumTrackCount = sortedSongs.count { it.album == song.album }
                            result = Pair(albumId, albumTrackCount)
                            break
                        }
                    }
                    result
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
                        is ListItem.AdItem<*> -> {
                            // Native ad with decision agent integration
                            val adIndex = displayItems.indexOf(item)
                            val albumInfo = getAlbumForAdItem(adIndex)

                            if (albumInfo != null) {
                                val (albumId, albumTrackCount) = albumInfo
                                NativeAdLoader(
                                    adMobManager = adMobManager,
                                    decisionAgent = adMobDecisionAgent,
                                    albumId = albumId,
                                    albumTrackCount = albumTrackCount,
                                    hasUserScrolled = hasUserScrolled.value
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.1f), CircleShape),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                    alphabet.forEach { char ->
                        val isHighlighted = char == currentHighlightChar.value
                        Text(
                            text = char.toString(),
                            style = MaterialTheme.typography.alphabetLabel.copy(
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
                            color = if (isHighlighted) accent().primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
