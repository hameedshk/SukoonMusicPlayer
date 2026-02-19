package com.sukoon.music.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material.icons.filled.Home
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
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.animation.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
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
import com.sukoon.music.ui.model.HomeTabKey
import com.sukoon.music.ui.model.HomeTabSpec
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
import androidx.compose.ui.platform.LocalDensity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.data.premium.PremiumManager
import com.sukoon.music.data.ads.AdMobManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import com.sukoon.music.ui.theme.*
import kotlin.math.roundToInt
import kotlin.math.abs
/**
 * Home Screen - Main entry point of the app.
 */
@OptIn(ExperimentalFoundationApi::class)
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
    onNavigateToAlbumSelection: () -> Unit = {},
    onNavigateToArtistSelection: () -> Unit = {},
    onNavigateToGenreSelection: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    onNavigateToFolders: () -> Unit = {},
    onNavigateToGenres: () -> Unit = {},
    onNavigateToPlaylistDetail: (Long) -> Unit = {},
    onNavigateToSmartPlaylist: (SmartPlaylistType) -> Unit = {},
    onNavigateToRestorePlaylist: () -> Unit = {},
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

    val preferencesManager = try {
        EntryPointAccessors.fromApplication(appContext, com.sukoon.music.ui.navigation.PreferencesManagerEntryPoint::class.java).preferencesManager()
    } catch (e: Exception) {
        null
    }

    val remoteConfigManager = try {
        EntryPointAccessors.fromApplication(appContext, com.sukoon.music.ui.navigation.RemoteConfigManagerEntryPoint::class.java).remoteConfigManager()
    } catch (e: Exception) {
        null
    }

    // Use provided username or default greeting
    val displayUsername = username.ifBlank { "there" }

    // Use ViewModel's tab state (persisted to DataStore, survives app restart)
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    val handleTabSelection: (HomeTabKey) -> Unit = { tab ->
        viewModel.setSelectedTab(tab)
    }

    val tabs = listOf(
        HomeTabSpec(HomeTabKey.HOME, "Hi $displayUsername", Icons.Default.Home),
        HomeTabSpec(HomeTabKey.SONGS, "Songs", Icons.Default.MusicNote),
        HomeTabSpec(HomeTabKey.PLAYLISTS, "Playlists", Icons.AutoMirrored.Filled.List),
        HomeTabSpec(HomeTabKey.FOLDERS, "Folders", Icons.Default.Folder),
        HomeTabSpec(HomeTabKey.ALBUMS, "Albums", Icons.Default.Album),
        HomeTabSpec(HomeTabKey.ARTISTS, "Artists", Icons.Default.Person),
        HomeTabSpec(HomeTabKey.GENRES, "Genres", Icons.Default.Star)
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

    var collapsibleTopBarHeightPx by remember { mutableIntStateOf(0) }
    var collapsibleTopBarOffsetPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    val consumeTopBarDelta: (Float) -> Float = { deltaY ->
        if (collapsibleTopBarHeightPx == 0) {
            0f
        } else {
            val previous = collapsibleTopBarOffsetPx
            val updated = (previous + deltaY).coerceIn(-collapsibleTopBarHeightPx.toFloat(), 0f)
            collapsibleTopBarOffsetPx = updated
            updated - previous
        }
    }

    val topBarScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y == 0f) return Offset.Zero
                if (abs(available.y) <= abs(available.x)) return Offset.Zero
                return Offset(0f, consumeTopBarDelta(available.y))
            }
        }
    }

    val topBarContextText = when (scanState) {
        is ScanState.Scanning -> "Scanning ${(scanState as ScanState.Scanning).scannedCount} songs..."
        else -> {
            if (sessionState.isActive) {
                "Private session is active"
            } else {
                null
            }
        }
    }

    LaunchedEffect(selectedTab) {
        collapsibleTopBarOffsetPx = 0f
    }

    LaunchedEffect(scanState is ScanState.Scanning, songs.isEmpty()) {
        if (scanState is ScanState.Scanning || songs.isEmpty()) {
            collapsibleTopBarOffsetPx = 0f
        }
    }

    Scaffold(
        modifier = Modifier
            .gradientBackground()
            .nestedScroll(topBarScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                val hasMeasuredTopBar = collapsibleTopBarHeightPx > 0
                val visibleTopBarHeightDp = with(density) {
                    (collapsibleTopBarHeightPx.toFloat() + collapsibleTopBarOffsetPx).coerceAtLeast(0f).toDp()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (!hasMeasuredTopBar) {
                                Modifier
                            } else {
                                Modifier
                                    .height(visibleTopBarHeightDp)
                                    .clipToBounds()
                            }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(0, collapsibleTopBarOffsetPx.roundToInt()) }
                            .onSizeChanged { size ->
                                val isExpanded = abs(collapsibleTopBarOffsetPx) < 0.5f
                                val shouldUpdateMeasuredHeight = size.height > 0 && (
                                    collapsibleTopBarHeightPx == 0 ||
                                        (isExpanded && collapsibleTopBarHeightPx != size.height)
                                    )
                                if (shouldUpdateMeasuredHeight) {
                                    collapsibleTopBarHeightPx = size.height
                                    collapsibleTopBarOffsetPx = collapsibleTopBarOffsetPx
                                        .coerceIn(-size.height.toFloat(), 0f)
                                }
                            }
                    ) {
                        RedesignedTopBar(
                            onPremiumClick = { },
                            onGlobalSearchClick = onNavigateToSearch,
                            onSettingsClick = onNavigateToSettings,
                            sessionState = sessionState,
                            contextText = topBarContextText
                        )
                    }
                }
                TabPills(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = { handleTabSelection(it) }
                )
            }
        },
        bottomBar = {
            Column {
                // Banner ad
                if (adMobManager != null && preferencesManager != null && remoteConfigManager != null) {
                    SimpleBannerAd(
                        adMobManager = adMobManager,
                        preferencesManager = preferencesManager,
                        remoteConfigManager = remoteConfigManager
                    )
                }
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
                    val pagerState = rememberPagerState(
                        initialPage = tabs.indexOfFirst { it.key == selectedTab }.coerceAtLeast(0),
                        pageCount = { tabs.size }
                    )

                    // Sync pager state to ViewModel (only after scroll settles)
                    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                        if (!pagerState.isScrollInProgress) {
                            val newTab = tabs.getOrNull(pagerState.currentPage)?.key
                            if (newTab != null && newTab != selectedTab) {
                                handleTabSelection(newTab)
                            }
                        }
                    }

                    // Sync ViewModel state to pager (for pill clicks)
                    LaunchedEffect(selectedTab) {
                        val targetPage = tabs.indexOfFirst { it.key == selectedTab }
                        if (targetPage != -1 && targetPage != pagerState.currentPage) {
                            pagerState.scrollToPage(targetPage)
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        when (tabs[pageIndex].key) {
                            HomeTabKey.HOME -> {
                                HomeTabScreen(
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
                            HomeTabKey.SONGS -> {
                                SongsScreen(
                                    onBackClick = { },
                                    onNavigateToAlbum = onNavigateToAlbumDetail,
                                    onNavigateToArtist = onNavigateToArtistDetail,
                                    homeViewModel = viewModel,
                                    playlistViewModel = playlistViewModel
                                )
                            }
                            HomeTabKey.ALBUMS -> {
                                AlbumsScreen(
                                    onNavigateToAlbum = onNavigateToAlbumDetail,
                                    onBackClick = { },
                                    onNavigateToAlbumSelection = onNavigateToAlbumSelection
                                )
                            }
                            HomeTabKey.ARTISTS -> {
                                ArtistsScreen(
                                    onNavigateToArtistDetail = onNavigateToArtistDetail,
                                    onNavigateToArtistSelection = onNavigateToArtistSelection,
                                    onBackClick = { }
                                )
                            }
                            HomeTabKey.GENRES -> {
                                GenresScreen(
                                    onNavigateToGenre = onNavigateToGenreDetail,
                                    onBackClick = { /* optional: navController.popBackStack() */ },
                                    onNavigateToGenreSelection = onNavigateToGenreSelection
                                )
                            }
                            HomeTabKey.PLAYLISTS -> {
                                PlaylistsScreen(
                                   onNavigateToPlaylist = onNavigateToPlaylistDetail,
                                   onNavigateToSmartPlaylist = onNavigateToSmartPlaylist,
                                    onNavigateToRestore = onNavigateToRestorePlaylist,
                                    onBackClick = { }
                                )
                            }
                            HomeTabKey.FOLDERS -> {
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
            .padding(SectionSpacing),
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

