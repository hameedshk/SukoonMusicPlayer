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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import android.widget.Toast
import com.sukoon.music.data.mediastore.DeleteHelper
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
    onNavigateToArtistDetail: (Long) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    onNavigateToAlbumSelection: () -> Unit = {},
    onNavigateToArtistSelection: () -> Unit = {},
    onNavigateToGenreSelection: () -> Unit = {},
    onNavigateToPlaylistDetail: (Long) -> Unit = {},
    onNavigateToSmartPlaylist: (SmartPlaylistType) -> Unit = {},
    onNavigateToRestorePlaylist: () -> Unit = {},
    onNavigateToFolderDetail: (Long) -> Unit = {},
    onNavigateToAlbumDetail: (Long) -> Unit = {},
    onNavigateToGenreDetail: (Long) -> Unit = {},
    username: String = "",
    viewModel: HomeViewModel = hiltViewModel(),
    playlistViewModel: com.sukoon.music.ui.viewmodel.PlaylistViewModel = hiltViewModel()
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val rediscoverAlbums by viewModel.rediscoverAlbums.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

    // Get managers from Hilt entry points for use in non-injected contexts
    val appContext = LocalContext.current
    val tag = "HomeScreen"

    // Resolve app-level managers once per context to avoid repeated lookup on recomposition.
    val adMobManager = remember(appContext) {
        runCatching {
            EntryPointAccessors.fromApplication(
                appContext,
                com.sukoon.music.ui.navigation.AdMobManagerEntryPoint::class.java
            ).adMobManager()
        }.getOrElse { error ->
            Log.w(tag, "Failed to resolve AdMobManager entry point", error)
            null
        }
    }

    val preferencesManager = remember(appContext) {
        runCatching {
            EntryPointAccessors.fromApplication(
                appContext,
                com.sukoon.music.ui.navigation.PreferencesManagerEntryPoint::class.java
            ).preferencesManager()
        }.getOrElse { error ->
            Log.w(tag, "Failed to resolve PreferencesManager entry point", error)
            null
        }
    }

    val remoteConfigManager = remember(appContext) {
        runCatching {
            EntryPointAccessors.fromApplication(
                appContext,
                com.sukoon.music.ui.navigation.RemoteConfigManagerEntryPoint::class.java
            ).remoteConfigManager()
        }.getOrElse { error ->
            Log.w(tag, "Failed to resolve RemoteConfigManager entry point", error)
            null
        }
    }

    val displayUsername = username.trim()

    // Use ViewModel's tab state (persisted to DataStore, survives app restart)
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    val handleTabSelection: (HomeTabKey) -> Unit = { tab ->
        viewModel.setSelectedTab(tab)
    }

    val tabs = remember(appContext, displayUsername) {
        val homeLabel = if (displayUsername.isNotBlank()) {
            appContext.getString(com.sukoon.music.R.string.home_tab_greeting, displayUsername)
        } else {
            appContext.getString(com.sukoon.music.R.string.home_tab_for_you)
        }
        listOf(
            HomeTabSpec(
                HomeTabKey.HOME,
                homeLabel,
                Icons.Default.Home
            ),
            HomeTabSpec(HomeTabKey.SONGS, appContext.getString(com.sukoon.music.R.string.home_tab_songs), Icons.Default.MusicNote),
            HomeTabSpec(HomeTabKey.PLAYLISTS, appContext.getString(com.sukoon.music.R.string.home_tab_playlists), Icons.AutoMirrored.Filled.List),
            HomeTabSpec(HomeTabKey.FOLDERS, appContext.getString(com.sukoon.music.R.string.home_tab_folders), Icons.Default.Folder),
            HomeTabSpec(HomeTabKey.ALBUMS, appContext.getString(com.sukoon.music.R.string.home_tab_albums), Icons.Default.Album),
            HomeTabSpec(HomeTabKey.ARTISTS, appContext.getString(com.sukoon.music.R.string.home_tab_artists), Icons.Default.Person),
            HomeTabSpec(HomeTabKey.GENRES, appContext.getString(com.sukoon.music.R.string.home_tab_genres), Icons.Default.Star)
        )
    }

    val normalizedSelectedTab = remember(selectedTab, tabs) {
        if (tabs.any { it.key == selectedTab }) selectedTab else HomeTabKey.HOME
    }

    LaunchedEffect(selectedTab, normalizedSelectedTab) {
        if (selectedTab != normalizedSelectedTab) {
            handleTabSelection(normalizedSelectedTab)
        }
    }


    val permissionState = rememberAudioPermissionState(
        onPermissionGranted = {
            viewModel.scanLocalMusic()
        }
    )

    // Auto-scan on startup is now handled by HomeViewModel.tryStartupScan()
    // This prevents duplicate scans and respects the scanOnStartup preference + 30-min deduplication

    // Show toast for explicit manual scan result events only.
    LaunchedEffect(viewModel, appContext) {
        viewModel.manualScanResults.collect { result ->
            when (result) {
                is HomeViewModel.ManualScanResult.Success -> {
                    val totalSongs = result.totalSongs
                    Toast.makeText(
                        appContext,
                        appContext.resources.getQuantityString(
                            com.sukoon.music.R.plurals.home_scan_completed_songs,
                            totalSongs,
                            totalSongs
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
                is HomeViewModel.ManualScanResult.Error -> {
                    val scanFailedPrefix = appContext.getString(com.sukoon.music.R.string.home_scan_failed_title)
                    val toastMessage = if (result.message.startsWith(scanFailedPrefix, ignoreCase = true)) {
                        result.message
                    } else {
                        appContext.getString(com.sukoon.music.R.string.home_scan_failed_with_reason, result.message)
                    }
                    Toast.makeText(appContext, toastMessage, Toast.LENGTH_LONG).show()
                }
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
        is ScanState.Scanning -> appContext.getString(
            com.sukoon.music.R.string.home_topbar_scanning_context,
            (scanState as ScanState.Scanning).scannedCount
        )
        else -> {
            if (sessionState.isActive) {
                appContext.getString(com.sukoon.music.R.string.home_topbar_private_session_active)
            } else {
                null
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onHomeVisible()
    }

    LaunchedEffect(normalizedSelectedTab) {
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
                            onPremiumClick = onNavigateToPremium,
                            onGlobalSearchClick = onNavigateToSearch,
                            onSettingsClick = onNavigateToSettings,
                            sessionState = sessionState
                        )
                    }
                }
                TabPills(
                    tabs = tabs,
                    selectedTab = normalizedSelectedTab,
                    onTabSelected = { handleTabSelection(it) }
                )
            }
        },
        bottomBar = {
            val shouldAddPlaylistMiniGap =
                normalizedSelectedTab == HomeTabKey.PLAYLISTS && playbackState.currentSong != null
            val playlistBannerTopGap = if (normalizedSelectedTab == HomeTabKey.PLAYLISTS) SpacingSmall else 0.dp
            val playlistBottomGap = if (shouldAddPlaylistMiniGap) SpacingMedium else 0.dp
            Column {
                // Banner ad
                if (adMobManager != null && preferencesManager != null && remoteConfigManager != null) {
                    SimpleBannerAd(
                        adMobManager = adMobManager,
                        preferencesManager = preferencesManager,
                        remoteConfigManager = remoteConfigManager,
                        modifier = Modifier.padding(top = playlistBannerTopGap)
                    )
                    if (playlistBottomGap > 0.dp) {
                        Spacer(modifier = Modifier.height(playlistBottomGap))
                    }
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
                        initialPage = tabs.indexOfFirst { it.key == normalizedSelectedTab }.coerceAtLeast(0),
                        pageCount = { tabs.size }
                    )

                    // Sync pager state to ViewModel (only after scroll settles)
                    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                        if (!pagerState.isScrollInProgress) {
                            val newTab = tabs.getOrNull(pagerState.currentPage)?.key
                            if (newTab != null && newTab != normalizedSelectedTab) {
                                handleTabSelection(newTab)
                            }
                        }
                    }

                    // Sync ViewModel state to pager (for pill clicks)
                    LaunchedEffect(normalizedSelectedTab) {
                        val targetPage = tabs.indexOfFirst { it.key == normalizedSelectedTab }
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
                                    onNavigateToSmartPlaylist = onNavigateToSmartPlaylist
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
                                    onBackClick = { },
                                    onNavigateToGenreSelection = onNavigateToGenreSelection
                                )
                            }
                            HomeTabKey.PLAYLISTS -> {
                                PlaylistsScreen(
                                    onNavigateToPlaylist = onNavigateToPlaylistDetail,
                                   onNavigateToSmartPlaylist = onNavigateToSmartPlaylist,
                                    onNavigateToRestore = onNavigateToRestorePlaylist,
                                    onBackClick = { },
                                    additionalBottomInset = if (playbackState.currentSong != null) SpacingMedium else 0.dp
                                )
                            }
                            HomeTabKey.FOLDERS -> {
                                FoldersScreen(
                                    onNavigateToFolder = onNavigateToFolderDetail,
                                    onNavigateToNowPlaying = onNavigateToNowPlaying,
                                    onNavigateToAlbum = onNavigateToAlbumDetail,
                                    onNavigateToArtist = onNavigateToArtistDetail,
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
    val scanProgressLabel = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.home_scan_progress_title)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SectionSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(64.dp)
                .semantics {
                    contentDescription = scanProgressLabel
                },
            color = accentTokens.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = scanProgressLabel,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(SpacingSmall))

        Text(
            text = androidx.compose.ui.res.pluralStringResource(
                com.sukoon.music.R.plurals.home_scan_progress_found_count,
                scanState.scannedCount,
                scanState.scannedCount
            ),
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
                !hasPermission -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.permission_required_title)
                scanState is ScanState.Error -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.home_scan_failed_title)
                scanState is ScanState.Success -> if (scanState.totalSongs == 0) {
                    androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.home_empty_no_music_found_title)
                } else {
                    androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.home_empty_no_songs_title)
                }
                else -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.home_empty_no_music_found_title)
            },
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = when {
                !hasPermission -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.home_empty_permission_message)
                scanState is ScanState.Error -> scanState.error
                scanState is ScanState.Success -> if (scanState.totalSongs == 0) {
                    androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.home_empty_scan_prompt)
                } else {
                    androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.home_empty_tap_scan_refresh)
                }
                else -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.home_empty_scan_prompt)
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
            Text(
                text = if (hasPermission) {
                    androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_scan_for_music)
                } else {
                    androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.permission_required_action_grant)
                }
            )
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
                label = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_all)) },
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
                label = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_smart_playlists)) },
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
                label = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_my_playlists)) },
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
                        text = androidx.compose.ui.res.pluralStringResource(
                            com.sukoon.music.R.plurals.common_song_count,
                            smartPlaylist.songCount,
                            smartPlaylist.songCount
                        ),
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
                        contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.home_cd_playlist_cover),
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
                            contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_options),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_delete)) },
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
                    text = androidx.compose.ui.res.pluralStringResource(com.sukoon.music.R.plurals.common_song_count, playlist.songCount, playlist.songCount),
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
                text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.albums_empty_title),
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




