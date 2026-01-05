package com.sukoon.music.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.sukoon.music.ui.viewmodel.ArtistsViewModel
import com.sukoon.music.ui.viewmodel.ArtistSortMode
import com.sukoon.music.ui.viewmodel.GenresViewModel
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import com.sukoon.music.ui.screen.artists.ArtistsScreen

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
    viewModel: HomeViewModel = hiltViewModel(),
    playlistViewModel: com.sukoon.music.ui.viewmodel.PlaylistViewModel = hiltViewModel(),
    folderViewModel: com.sukoon.music.ui.viewmodel.FolderViewModel = hiltViewModel(),
    artistsViewModel: ArtistsViewModel = hiltViewModel(),
    genresViewModel: GenresViewModel = hiltViewModel()
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val adMobManager = viewModel.adMobManager

    // Playlists state
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()
    val smartPlaylists by playlistViewModel.smartPlaylists.collectAsStateWithLifecycle()
    val availableSongs by playlistViewModel.availableSongs.collectAsStateWithLifecycle()

    // Folders state
    val folders by folderViewModel.folders.collectAsStateWithLifecycle()
    val hiddenFolders by folderViewModel.hiddenFolders.collectAsStateWithLifecycle()
    val folderViewMode by folderViewModel.folderViewMode.collectAsStateWithLifecycle()
    val folderSortMode by folderViewModel.sortMode.collectAsStateWithLifecycle()

    // Artists state
    val artists by artistsViewModel.artists.collectAsStateWithLifecycle()
    val recentlyPlayedArtists by artistsViewModel.recentlyPlayedArtists.collectAsStateWithLifecycle()
    val isArtistSelectionMode by artistsViewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedArtistIds by artistsViewModel.selectedArtistIds.collectAsStateWithLifecycle()

    // Genres state
    val genres by genresViewModel.genres.collectAsStateWithLifecycle()
    val isGenreSelectionMode by genresViewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedGenreIds by genresViewModel.selectedGenreIds.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf("For you") }

    val handleTabSelection: (String) -> Unit = { tab ->
        selectedTab = tab
    }

    // Create menu handler for song context menu
    val menuHandler = rememberSongMenuHandler(
        playbackRepository = viewModel.playbackRepository
    )

    val permissionState = rememberAudioPermissionState(
        onPermissionGranted = {
            viewModel.scanLocalMusic()
        }
    )

    Scaffold(
        topBar = {
                if (selectedTab == "Genres" && isGenreSelectionMode) {
                GenreSelectionTopBar(
                    selectedCount = selectedGenreIds.size,
                    onBackClick = { genresViewModel.toggleSelectionMode(false) }
                )
            } else {
                Column {
                    RedesignedTopBar(
                        onPremiumClick = { /* TODO: Navigate to Premium screen */ },
                        onGlobalSearchClick = onNavigateToSearch,
                        onSettingsClick = onNavigateToSettings
                    )
                    TabPills(
                        selectedTab = selectedTab,
                        onTabSelected = handleTabSelection
                    )
                }
            }
        },
        bottomBar = {
            Column {
                if (selectedTab == "Genres" && isGenreSelectionMode && selectedGenreIds.isNotEmpty()) {
                    GenreSelectionBottomBar(
                        selectedCount = selectedGenreIds.size,
                        onPlay = { genresViewModel.playSelectedGenres() },
                        onAddToPlaylist = { /* TODO */ },
                        onDelete = { genresViewModel.deleteSelectedGenres() },
                        onPlayNext = { genresViewModel.playSelectedNext() },
                        onAddToQueue = { genresViewModel.addSelectedToQueue() }
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
                                onSettingsClick = onNavigateToSettings,
                                onNavigateToSmartPlaylist = onNavigateToSmartPlaylist
                            )
                        }
                        "Songs" -> {
                            SongsContent(
                                songs = songs,
                                playbackState = playbackState,
                                onSongClick = { song, index ->
                                    viewModel.playQueue(songs, index)
                                },
                                onShuffleAllClick = { viewModel.shuffleAll() },
                                onPlayAllClick = { viewModel.playAll() },
                                viewModel = viewModel,
                                playlistViewModel = playlistViewModel
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
                                onNavigateToArtistDetail = onNavigateToArtistDetail

                            )
                        }
                        "Genres" -> {
                            GenresContent(
                                genres = genres,
                                onGenreClick = onNavigateToGenreDetail,
                                viewModel = genresViewModel
                            )
                        }
                        "Playlist" -> {
                            PlaylistsContent(
                                playlists = playlists,
                                smartPlaylists = smartPlaylists,
                                availableSongs = availableSongs,
                                playbackState = playbackState,
                                onPlaylistClick = onNavigateToPlaylistDetail,
                                onSmartPlaylistClick = onNavigateToSmartPlaylist,
                                playlistViewModel = playlistViewModel
                            )
                        }
                        "Folders" -> {
                            FoldersContent(
                                folders = folders,
                                hiddenFolders = hiddenFolders,
                                folderViewMode = folderViewMode,
                                sortMode = folderSortMode,
                                playbackState = playbackState,
                                onFolderClick = onNavigateToFolderDetail,
                                folderViewModel = folderViewModel,
                                menuHandler = menuHandler,
                                onNavigateToNowPlaying = onNavigateToNowPlaying
                            )
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
    onSettingsClick: () -> Unit = {},
    onNavigateToSmartPlaylist: (SmartPlaylistType) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item { WidgetBanner(onClick = { /* TODO: Open widget configuration */ }) }
        item {
            ActionButtonGrid(
                onShuffleAllClick = onShuffleAllClick,
                onPlayAllClick = onPlayAllClick,
                onScanClick = onScanClick,
                onSettingsClick = onSettingsClick
            )
        }
        if (songs.isNotEmpty()) {
            item {
                LastAddedSection(
                    songs = songs,
                    onSongClick = { song ->
                        val index = songs.indexOf(song)
                        onSongClick(song, index)
                    },
                    onHeaderClick = {
                        onNavigateToSmartPlaylist(SmartPlaylistType.LAST_ADDED)
                    }
                )
            }
        }
        if (recentlyPlayed.isNotEmpty()) {
            item {
                RecentlyPlayedSection(
                    items = recentlyPlayed,
                    onItemClick = onRecentlyPlayedClick,
                    onHeaderClick = {
                        onNavigateToSmartPlaylist(SmartPlaylistType.RECENTLY_PLAYED)
                    }
                ) { song, onClick ->
                    RecentlyPlayedSongCard(song = song, onClick = onClick)
                }
            }
        }
        item {
            Text(
                text = "All Songs",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp)
            )
        }
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
private fun SongsContent(
    songs: List<Song>,
    playbackState: PlaybackState,
    onSongClick: (Song, Int) -> Unit,
    onShuffleAllClick: () -> Unit,
    onPlayAllClick: () -> Unit,
    viewModel: HomeViewModel,
    playlistViewModel: com.sukoon.music.ui.viewmodel.PlaylistViewModel
) {
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var sortMode by rememberSaveable { mutableStateOf("Song name") }
    var sortOrder by rememberSaveable { mutableStateOf("A to Z") }
    var showSortDialog by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedSongIds by remember { mutableStateOf(setOf<Long>()) }
    var showMenuForSong by remember { mutableStateOf<Song?>(null) }
    var showInfoForSong by remember { mutableStateOf<Song?>(null) }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Create menu handler for song context menu
    val menuHandler = rememberSongMenuHandler(
        playbackRepository = viewModel.playbackRepository,
        onShowSongInfo = { song -> showInfoForSong = song }
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
        Column(modifier = Modifier.fillMaxSize()) {
            if (isSelectionMode) {
                // Selection mode header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "${selectedSongIds.size} selected",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                // Search bar in selection mode (placeholder)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Text("Search songs",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Select all row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedSongIds = if (selectedSongIds.size == sortedSongs.size)
                                emptySet() else sortedSongs.map { it.id }.toSet()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Select all", style = MaterialTheme.typography.bodyLarge)
                    RadioButton(
                        selected = selectedSongIds.size == sortedSongs.size,
                        onClick = null
                    )
                }
            } else {
                // Normal mode header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        IconButton(onClick = { isSelectionMode = true }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Select")
                        }
                    }
                }
            }

            // Shuffle and Play buttons (hide in selection mode)
            if (!isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable(onClick = onShuffleAllClick),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Shuffle", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable(onClick = onPlayAllClick),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Play", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            }

            // Song list
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    items = sortedSongs,
                    key = { it.id }
                ) { song ->
                    val isPlaying = playbackState.currentSong?.id == song.id && playbackState.isPlaying
                    val index = songs.indexOf(song)
                    val isSelected = selectedSongIds.contains(song.id)

                    if (isSelectionMode) {
                        SongItemSelectable(
                            song = song,
                            isSelected = isSelected,
                            onClick = {
                                selectedSongIds = if (isSelected)
                                    selectedSongIds - song.id
                                else
                                    selectedSongIds + song.id
                            }
                        )
                    } else {
                        SongItemWithMenu(
                            song = song,
                            isPlaying = isPlaying,
                            onClick = { onSongClick(song, index) },
                            onMenuClick = { showMenuForSong = song }
                        )
                    }
                }
            }
        }

        // Alphabet scroller (hide in selection mode)
        if (!isSelectionMode) {
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

        // Selection mode bottom action bar
        if (isSelectionMode && selectedSongIds.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SelectionActionButton(
                        icon = Icons.Default.PlayArrow,
                        label = "Play",
                        onClick = { /* TODO: Play selected */ }
                    )
                    SelectionActionButton(
                        icon = Icons.Default.PlaylistAdd,
                        label = "Playlist",
                        onClick = { /* TODO: Add to playlist */ }
                    )
                    SelectionActionButton(
                        icon = Icons.Default.Add,
                        label = "Add",
                        onClick = { /* TODO: Add to queue */ }
                    )
                    SelectionActionButton(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        onClick = { /* TODO: Delete */ }
                    )
                    SelectionActionButton(
                        icon = Icons.Default.MoreVert,
                        label = "More",
                        onClick = { /* TODO: More */ }
                    )
                }
            }
        }
    }

    // Sort dialog
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

    // Song menu bottom sheet
    showMenuForSong?.let { song ->
        SongContextMenu(
            song = song,
            menuHandler = menuHandler,
            onDismiss = { showMenuForSong = null }
        )
    }

    // Add to playlist dialog
    if (showAddToPlaylistDialog && songToAddToPlaylist != null) {
        AddToPlaylistDialog(
            playlists = playlists,
            onPlaylistSelected = { playlistId ->
                playlistViewModel.addSongToPlaylist(playlistId, songToAddToPlaylist!!.id)
                showAddToPlaylistDialog = false
                songToAddToPlaylist = null
            },
            onDismiss = {
                showAddToPlaylistDialog = false
                songToAddToPlaylist = null
            }
        )
    }

    // Song info dialog
    showInfoForSong?.let { song ->
        SongInfoDialog(
            song = song,
            onDismiss = { showInfoForSong = null }
        )
    }
}

/**
 * Genres Content - Displays genres in a list.
 */
@Composable
private fun GenresContent(
    genres: List<Genre>,
    onGenreClick: (Long) -> Unit,
    viewModel: GenresViewModel
) {
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedGenreIds by viewModel.selectedGenreIds.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        if (genres.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No genres found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Genre Header with Selection Button
                if (!isSelectionMode) {
                    item {
                        GenreHeader(
                            genreCount = genres.size,
                            onSelectionClick = { viewModel.toggleSelectionMode(true) }
                        )
                    }
                }

                // Select All Checkbox
                if (isSelectionMode) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedGenreIds.size == genres.size && genres.isNotEmpty()) {
                                        viewModel.clearSelection()
                                    } else {
                                        viewModel.selectAllGenres()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedGenreIds.size == genres.size && genres.isNotEmpty(),
                                onCheckedChange = {
                                    if (it) viewModel.selectAllGenres() else viewModel.clearSelection()
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Select all",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                items(
                    items = genres,
                    key = { it.id }
                ) { genre ->
                    GenreRow(
                        genre = genre,
                        isSelected = selectedGenreIds.contains(genre.id),
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) viewModel.toggleGenreSelection(genre.id)
                            else onGenreClick(genre.id)
                        },
                        onSelectionToggle = { viewModel.toggleGenreSelection(genre.id) },
                        onPlayClick = { viewModel.playGenre(genre.id) },
                        onPlayNextClick = { viewModel.playGenreNext(genre.id) },
                        onAddToQueueClick = { viewModel.addGenreToQueue(genre.id) },
                        onAddToPlaylistClick = { viewModel.showAddToPlaylistDialog(listOf(genre.id)) },
                        onDeleteClick = { /* Handled via selection mode */ }
                    )
                }
            }
        }
    }
}

/**
 * Playlists Content - Displays smart playlists and user playlists inline within HomeScreen.
 */
@Composable
private fun PlaylistsContent(
    playlists: List<Playlist>,
    smartPlaylists: List<SmartPlaylist>,
    availableSongs: List<Song>,
    playbackState: PlaybackState,
    onPlaylistClick: (Long) -> Unit,
    onSmartPlaylistClick: (SmartPlaylistType) -> Unit,
    playlistViewModel: com.sukoon.music.ui.viewmodel.PlaylistViewModel
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistFilter by remember { mutableStateOf<Boolean?>(null) }
    var newPlaylistId by remember { mutableStateOf<Long?>(null) }
    var showAddSongsDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filter Chips Section
        item {
            PlaylistFilterChips(
                selectedFilter = playlistFilter,
                onFilterChange = { playlistFilter = it }
            )
        }

        // Smart Playlists Section (show only if filter allows)
        if (playlistFilter == null || playlistFilter == true) {
            item {
                SmartPlaylistsSection(
                    smartPlaylists = smartPlaylists,
                    onSmartPlaylistClick = onSmartPlaylistClick
                )
            }
        }
        // Actions and User Playlists (show only if filter allows)
        if (playlistFilter == null || playlistFilter == false) {
            item {
                PlaylistActionsSection(
                    playlistCount = playlists.size,
                    onCreateClick = { showCreateDialog = true }
                )
            }

            // User Playlists Grid
            if (playlists.isNotEmpty()) {
                item {
                    Text(
                        text = "My playlists (${playlists.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(
                    items = playlists.chunked(2),
                    key = { row: List<Playlist> -> row.first().id }
                ) { rowPlaylists: List<Playlist> ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowPlaylists.forEach { playlist ->
                            Box(modifier = Modifier.weight(1f)) {
                                PlaylistCard(
                                    playlist = playlist,
                                    onClick = { onPlaylistClick(playlist.id) },
                                    onDeleteClick = { playlistToDelete = playlist }
                                )
                            }
                        }
                        if (rowPlaylists.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No playlists yet. Create your first playlist!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Create Playlist Dialog
    if (showCreateDialog) {
        HomeScreenCreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description ->
                playlistViewModel.createPlaylist(name, description) { playlistId ->
                    newPlaylistId = playlistId
                    playlistViewModel.loadPlaylist(playlistId)
                    showAddSongsDialog = true
                }
                showCreateDialog = false
            }
        )
    }

    // Add Songs Dialog
    if (showAddSongsDialog && newPlaylistId != null) {
        HomeScreenAddSongsToNewPlaylistDialog(
            availableSongs = availableSongs,
            onDismiss = {
                showAddSongsDialog = false
                newPlaylistId?.let { onPlaylistClick(it) }
                newPlaylistId = null
            },
            onAddSongs = { selectedSongs ->
                selectedSongs.forEach { song ->
                    playlistViewModel.addSongToPlaylist(newPlaylistId!!, song.id)
                }
                showAddSongsDialog = false
                newPlaylistId?.let { onPlaylistClick(it) }
                newPlaylistId = null
            }
        )
    }

    // Delete Confirmation
    playlistToDelete?.let { playlist ->
        HomeScreenDeletePlaylistConfirmationDialog(
            playlistName = playlist.name,
            onDismiss = { playlistToDelete = null },
            onConfirm = {
                playlistViewModel.deletePlaylist(playlist.id)
                playlistToDelete = null
            }
        )
    }
}

/**
 * Folders Content - Displays music folders with management options inline within HomeScreen.
 */
@Composable
private fun FoldersContent(
    folders: List<Folder>,
    hiddenFolders: List<Folder>,
    folderViewMode: FolderViewMode,
    sortMode: FolderSortMode,
    playbackState: PlaybackState,
    onFolderClick: (Long) -> Unit,
    folderViewModel: com.sukoon.music.ui.viewmodel.FolderViewModel,
    menuHandler: SongMenuHandler,
    onNavigateToNowPlaying: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf<Folder?>(null) }
    var folderToDeleteId by remember { mutableStateOf<Long?>(null) }

    val browsingContent by folderViewModel.browsingContent.collectAsStateWithLifecycle()
    val currentPath by folderViewModel.currentPath.collectAsStateWithLifecycle()

    val displayFolders = if (folderViewMode == FolderViewMode.DIRECTORIES) {
        folders
    } else {
        hiddenFolders
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Back button when navigating in folders
        if (folderViewMode == FolderViewMode.DIRECTORIES && currentPath != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { folderViewModel.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate up"
                    )
                }
                Text(
                    text = currentPath ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider()
        }

        // Category pill row (only show when at root)
        if (currentPath == null) {
            CategoryPillRow(
                itemCount = displayFolders.size,
                itemLabel = if (displayFolders.size == 1) "folder" else "folders",
                sortOptions = FolderSortMode.entries,
                currentSortMode = sortMode,
                onSortModeChanged = { folderViewModel.setSortMode(it) },
                sortModeToDisplayName = { it.toDisplayName() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Context header
            FolderContextHeader(
                selectedMode = folderViewMode,
                onModeChanged = { folderViewModel.setFolderViewMode(it) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Folder list or empty state
        Box(modifier = Modifier.weight(1f)) {
            if (folderViewMode == FolderViewMode.DIRECTORIES && browsingContent.isNotEmpty()) {
                // Hierarchical browsing mode
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(browsingContent.size, key = { index ->
                        when (val item = browsingContent[index]) {
                            is com.sukoon.music.ui.viewmodel.FolderBrowserItem.SubFolder -> "folder_${item.path}"
                            is com.sukoon.music.ui.viewmodel.FolderBrowserItem.SongItem -> "song_${item.song.id}"
                        }
                    }) { index ->
                        when (val item = browsingContent[index]) {
                            is com.sukoon.music.ui.viewmodel.FolderBrowserItem.SubFolder -> {
                                FolderBrowserSubFolderRow(
                                    subFolder = item,
                                    onClick = { folderViewModel.navigateToFolder(item.path) },
                                    onPlay = { folderViewModel.playFolder(item.path) }
                                )
                            }
                            is com.sukoon.music.ui.viewmodel.FolderBrowserItem.SongItem -> {
                                FolderBrowserSongRow(
                                    song = item.song,
                                    isPlaying = item.song.id == playbackState.currentSong?.id,
                                    menuHandler = menuHandler,
                                    onClick = {
                                        val contextSongs = browsingContent.filterIsInstance<com.sukoon.music.ui.viewmodel.FolderBrowserItem.SongItem>()
                                            .map { it.song }
                                        folderViewModel.playSong(item.song, contextSongs)
                                        onNavigateToNowPlaying()
                                    },
                                    onLikeClick = { folderViewModel.toggleLike(item.song.id, item.song.isLiked) }
                                )
                            }
                        }
                    }
                }
            } else if (displayFolders.isEmpty()) {
                EmptyFoldersState(
                    isHiddenView = folderViewMode == FolderViewMode.HIDDEN
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(displayFolders, key = { it.id }) { folder ->
                        FolderRow(
                            folder = folder,
                            isHidden = folderViewMode == FolderViewMode.HIDDEN,
                            onFolderClick = { onFolderClick(folder.id) },
                            onPlay = { folderViewModel.playFolder(folder.path) },
                            onPlayNext = { folderViewModel.playNext(folder.path) },
                            onAddToQueue = { folderViewModel.addToQueue(folder.path) },
                            onAddToPlaylist = { folderViewModel.showAddToPlaylistDialog(folder.id) },
                            onHide = { folderViewModel.excludeFolder(folder.path) },
                            onUnhide = { folderViewModel.unhideFolder(folder.path) },
                            onDelete = {
                                showDeleteConfirmation = folder
                                folderToDeleteId = folder.id
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteConfirmation?.let { folder ->
        DeleteFolderConfirmationDialog(
            folder = folder,
            onConfirm = {
                folderToDeleteId?.let { folderViewModel.deleteFolder(it) }
                showDeleteConfirmation = null
                folderToDeleteId = null
            },
            onDismiss = {
                showDeleteConfirmation = null
                folderToDeleteId = null
            }
        )
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

@Composable
private fun SmartPlaylistsSection(
    smartPlaylists: List<SmartPlaylist>,
    onSmartPlaylistClick: (SmartPlaylistType) -> Unit
) {
    Column {
        Text(
            text = "4 playlists",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val rows = smartPlaylists.chunked(2)
        rows.forEach { rowPlaylists ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowPlaylists.forEach { smartPlaylist ->
                    Box(modifier = Modifier.weight(1f)) {
                        SmartPlaylistCard(
                            smartPlaylist = smartPlaylist,
                            onClick = { onSmartPlaylistClick(smartPlaylist.type) }
                        )
                    }
                }
            }
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

@Composable
private fun PlaylistActionsSection(
    playlistCount: Int,
    onCreateClick: () -> Unit
) {
    Column {
        Text(
            text = "My playlists ($playlistCount)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Surface(
            onClick = onCreateClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Create new playlist",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenAddSongsToNewPlaylistDialog(
    availableSongs: List<Song>,
    onDismiss: () -> Unit,
    onAddSongs: (List<Song>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSongs by remember { mutableStateOf(setOf<Long>()) }

    val filteredSongs = remember(availableSongs, searchQuery) {
        if (searchQuery.isBlank()) {
            availableSongs
        } else {
            availableSongs.filter { song ->
                song.title.contains(searchQuery, ignoreCase = true) ||
                song.artist.contains(searchQuery, ignoreCase = true) ||
                song.album.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Songs to Playlist",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedSongs.isNotEmpty()) {
                        Text(
                            text = "${selectedSongs.size} selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Search songs...") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (filteredSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No songs available" else "No songs found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        itemsIndexed(
                            items = filteredSongs,
                            key = { _: Int, song: Song -> song.id }
                        ) { _: Int, song: Song ->
                            val isSelected = selectedSongs.contains(song.id)

                            Surface(
                                onClick = {
                                    selectedSongs = if (isSelected) {
                                        selectedSongs - song.id
                                    } else {
                                        selectedSongs + song.id
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                } else {
                                    Color.Transparent
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (song.albumArtUri != null) {
                                            SubcomposeAsyncImage(
                                                model = song.albumArtUri,
                                                contentDescription = "Album art",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                error = {
                                                    Icon(
                                                        imageVector = Icons.Default.MusicNote,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                }
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = song.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Skip")
                    }

                    Button(
                        onClick = {
                            val songsToAdd = availableSongs.filter { it.id in selectedSongs }
                            onAddSongs(songsToAdd)
                        },
                        enabled = selectedSongs.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (selectedSongs.isEmpty()) "Add" else "Add (${selectedSongs.size})"
                        )
                    }
                }
            }
        }
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


@Composable
private fun EmptyFoldersState(
    isHiddenView: Boolean = false
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isHiddenView) Icons.Default.VisibilityOff else Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isHiddenView) "No hidden folders" else "No folders found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isHiddenView)
                    "Folders you hide will appear here"
                else
                    "Scan your media library to see your music folders",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DeleteFolderConfirmationDialog(
    folder: Folder,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete folder?") },
        text = {
            Column {
                Text("This will permanently delete ${folder.songCount} songs from your device:")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = folder.path,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun FolderSortMode.toDisplayName(): String = when (this) {
    FolderSortMode.NAME_ASC -> "Name (A-Z)"
    FolderSortMode.NAME_DESC -> "Name (Z-A)"
    FolderSortMode.TRACK_COUNT -> "Most Songs"
    FolderSortMode.RECENTLY_MODIFIED -> "Recently Added"
    FolderSortMode.DURATION -> "Total Duration"
}

@Composable
private fun HomeScreenCreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Playlist") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description.ifBlank { null }) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = { name = ""; description = ""; onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun HomeScreenDeletePlaylistConfirmationDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Playlist") },
        text = { Text("Are you sure you want to delete '$playlistName'?") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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

@Composable
private fun GenreHeader(
    genreCount: Int,
    onSelectionClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$genreCount ${if (genreCount == 1) "genre" else "genres"}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onSelectionClick) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Select")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenreSelectionTopBar(
    selectedCount: Int,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@Composable
private fun FolderBrowserSubFolderRow(
    subFolder: com.sukoon.music.ui.viewmodel.FolderBrowserItem.SubFolder,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (subFolder.albumArtUri != null) {
                SubcomposeAsyncImage(
                    model = subFolder.albumArtUri,
                    contentDescription = "Folder cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    error = {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = subFolder.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${subFolder.songCount} songs",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Enter folder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FolderBrowserSongRow(
    song: Song,
    isPlaying: Boolean,
    menuHandler: SongMenuHandler,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onLikeClick) {
            Icon(
                imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (song.isLiked) "Unlike" else "Like",
                tint = if (song.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showMenu) {
        SongContextMenu(
            song = song,
            menuHandler = menuHandler,
            onDismiss = { showMenu = false }
        )
    }
}