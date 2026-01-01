package com.sukoon.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.sukoon.music.ui.viewmodel.AlbumsViewModel
import com.sukoon.music.ui.viewmodel.AlbumSortMode
import com.sukoon.music.ui.viewmodel.ArtistsViewModel
import com.sukoon.music.ui.viewmodel.GenresViewModel
import com.sukoon.music.ui.viewmodel.GenreSortMode
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

/**
 * Home Screen - Main entry point of the app.
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
    onNavigateToGenres: () -> Unit = {},
    onNavigateToPlaylistDetail: (Long) -> Unit = {},
    onNavigateToSmartPlaylist: (SmartPlaylistType) -> Unit = {},
    onNavigateToFolderDetail: (Long) -> Unit = {},
    onNavigateToAlbumDetail: (Long) -> Unit = {},
    onNavigateToGenreDetail: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    playlistViewModel: com.sukoon.music.ui.viewmodel.PlaylistViewModel = hiltViewModel(),
    folderViewModel: com.sukoon.music.ui.viewmodel.FolderViewModel = hiltViewModel(),
    albumsViewModel: AlbumsViewModel = hiltViewModel(),
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

    // Albums state
    val albums by albumsViewModel.albums.collectAsStateWithLifecycle()
    val recentlyPlayedAlbums by albumsViewModel.recentlyPlayedAlbums.collectAsStateWithLifecycle()
    val isAlbumSelectionMode by albumsViewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedAlbumIds by albumsViewModel.selectedAlbumIds.collectAsStateWithLifecycle()

    // Artists state
    val artists by artistsViewModel.artists.collectAsStateWithLifecycle()
    val recentlyPlayedArtists by artistsViewModel.recentlyPlayedArtists.collectAsStateWithLifecycle()

    // Genres state
    val genres by genresViewModel.genres.collectAsStateWithLifecycle()
    val genreSortMode by genresViewModel.sortMode.collectAsStateWithLifecycle()

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
            if (selectedTab == "Albums" && isAlbumSelectionMode) {
                AlbumSelectionTopBar(
                    selectedCount = selectedAlbumIds.size,
                    onBackClick = { albumsViewModel.toggleSelectionMode(false) }
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
                if (selectedTab == "Albums" && isAlbumSelectionMode) {
                    AlbumSelectionBottomBar(
                        onPlay = { albumsViewModel.playSelectedAlbums() },
                        onAddToPlaylist = { /* TODO */ },
                        onDelete = { /* TODO */ },
                        onMore = { /* TODO */ }
                    )
                } else if (playbackState.currentSong != null) {
                    MiniPlayer(
                        playbackState = playbackState,
                        onPlayPauseClick = { viewModel.playPause() },
                        onNextClick = { viewModel.seekToNext() },
                        onClick = onNavigateToNowPlaying
                    )
                }

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
                            AlbumsContent(
                                albums = albums,
                                recentlyPlayedAlbums = recentlyPlayedAlbums,
                                isSelectionMode = isAlbumSelectionMode,
                                selectedIds = selectedAlbumIds,
                                onAlbumClick = { albumId ->
                                    if (isAlbumSelectionMode) {
                                        albumsViewModel.toggleAlbumSelection(albumId)
                                    } else {
                                        onNavigateToAlbumDetail(albumId)
                                    }
                                },
                                onAlbumLongClick = { albumId ->
                                    albumsViewModel.toggleSelectionMode(true)
                                    albumsViewModel.toggleAlbumSelection(albumId)
                                },
                                onPlayAlbum = { albumsViewModel.playAlbum(it) },
                                onShuffleAlbum = { albumsViewModel.shuffleAlbum(it) },
                                viewModel = albumsViewModel
                            )
                        }
                        "Artists" -> {
                            ArtistsContent(
                                artists = artists,
                                recentlyPlayedArtists = recentlyPlayedArtists,
                                onArtistClick = onNavigateToArtists,
                                viewModel = artistsViewModel
                            )
                        }
                        "Genres" -> {
                            GenresContent(
                                genres = genres,
                                sortMode = genreSortMode,
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
                                folderViewModel = folderViewModel
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
                    songs = recentlyPlayed,
                    onSongClick = onRecentlyPlayedClick,
                    onHeaderClick = {
                        onNavigateToSmartPlaylist(SmartPlaylistType.RECENTLY_PLAYED)
                    }
                )
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
private fun SongItemWithMenu(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
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

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongMenuBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onGoToAlbum: () -> Unit,
    onSetRingtone: () -> Unit,
    onChangeCover: () -> Unit,
    onEditTags: () -> Unit,
    onEditAudio: () -> Unit,
    onUpdateLyrics: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Song header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = {
                            Icon(Icons.Default.MusicNote, contentDescription = null)
                        }
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                IconButton(onClick = { /* TODO: Info */ }) {
                    Icon(Icons.Default.Info, contentDescription = "Info")
                }
                IconButton(onClick = { /* TODO: Share */ }) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
            }

            // Pill buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PillButton(
                    text = "Set as ringtone",
                    icon = Icons.Default.Notifications,
                    onClick = { onSetRingtone(); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
                PillButton(
                    text = "Change cover",
                    icon = Icons.Default.Image,
                    onClick = { onChangeCover(); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
                PillButton(
                    text = "Edit tags",
                    icon = Icons.Default.Edit,
                    onClick = { onEditTags(); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Menu items
            MenuOption("Play next", Icons.Default.SkipNext) { onPlayNext(); onDismiss() }
            MenuOption("Add to queue", Icons.Default.PlaylistAdd) { onAddToQueue(); onDismiss() }
            MenuOption("Add to playlist", Icons.Default.PlaylistAdd) { onAddToPlaylist(); onDismiss() }
            MenuOption("Go to album", Icons.Default.Album) { onGoToAlbum(); onDismiss() }
            MenuOption("Edit audio", Icons.Default.MusicNote) { onEditAudio(); onDismiss() }
            MenuOption("Update lyrics", Icons.Default.Edit) { onUpdateLyrics(); onDismiss() }
            MenuOption("Delete from device", Icons.Default.Delete, isDestructive = true) { onDelete(); onDismiss() }
        }
    }
}

@Composable
private fun PillButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MenuOption(
    text: String,
    icon: ImageVector,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SongItemSelectable(
    song: Song,
    isSelected: Boolean,
    onClick: () -> Unit
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
            SubcomposeAsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = { Icon(Icons.Default.MusicNote, contentDescription = null) }
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(song.artist, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        RadioButton(selected = isSelected, onClick = null)
    }
}

@Composable
private fun SelectionActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongSortDialog(
    currentSortMode: String,
    currentOrder: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentSortMode) }
    var selectedOrder by remember { mutableStateOf(currentOrder) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text("Sort by", style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))

            listOf("Song name", "Artist name", "Album name", "Folder name",
                "Time added", "Play count", "Year", "Duration", "Size").forEach { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { selectedMode = mode }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(mode, style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedMode == mode) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface)
                    if (selectedMode == mode) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            listOf("A to Z", "Z to A").forEach { order ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { selectedOrder = order }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("From $order", style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedOrder == order) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface)
                    if (selectedOrder == order) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onDismiss, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurface)
                }
                Button(onClick = { onConfirm(selectedMode, selectedOrder) }, modifier = Modifier.weight(1f)) {
                    Text("OK")
                }
            }
        }
    }
}

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
        Text(
            text = "Sukoon Music",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPremiumClick) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Premium",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onGlobalSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
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

@Composable
private fun TabPills(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf("For you", "Songs", "Albums", "Artists", "Genres", "Playlist", "Folders")

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SpacingMedium),
        horizontalArrangement = Arrangement.spacedBy(SpacingMedium),
        contentPadding = PaddingValues(horizontal = SpacingLarge)
    ) {
        items(
            count = tabs.size,
            key = { index -> tabs[index] }
        ) { index ->
            val tab = tabs[index]
            val isSelected = tab == selectedTab

            Surface(
                modifier = Modifier
                    .height(44.dp)
                    .clickable { onTabSelected(tab) },
                shape = PillShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
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
                        color = if (isSelected) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

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
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Add widgets to your home screen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Go",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ActionButtonGrid(
    onShuffleAllClick: () -> Unit,
    onPlayAllClick: () -> Unit,
    onScanClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingLarge, vertical = SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(SpacingMedium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            ActionButton(
                text = "Shuffle",
                icon = Icons.Default.Shuffle,
                onClick = onShuffleAllClick,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = "Play",
                icon = Icons.Default.PlayArrow,
                onClick = onPlayAllClick,
                modifier = Modifier.weight(1f)
            )
        }
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

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
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
                tint = MaterialTheme.colorScheme.primary,
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

@Composable
private fun LastAddedSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onHeaderClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SpacingMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onHeaderClick)
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

        LazyRow(
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f)
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
                            tint = MaterialTheme.colorScheme.primary,
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

@Composable
private fun RecentlyPlayedSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onHeaderClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SpacingMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onHeaderClick)
                .padding(horizontal = SpacingLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Recently played",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "See all",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(SpacingMedium))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = SpacingLarge),
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            items(songs.size.coerceAtMost(10)) { index ->
                RecentlyPlayedCard(
                    song = songs[index],
                    onClick = { onSongClick(songs[index]) }
                )
            }
        }
    }
}

@Composable
private fun RecentlyPlayedCard(
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

            // Gradient overlay with play button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f)
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                }
            }

            // Recently played badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
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
 * Albums Content - Displays albums as per the provided design.
 */
@Composable
private fun AlbumsContent(
    albums: List<Album>,
    recentlyPlayedAlbums: List<Album>,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onAlbumClick: (Long) -> Unit,
    onAlbumLongClick: (Long) -> Unit,
    onPlayAlbum: (Long) -> Unit,
    onShuffleAlbum: (Long) -> Unit,
    viewModel: AlbumsViewModel
) {
    var showSortDialog by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val isAscending by viewModel.isAscending.collectAsStateWithLifecycle()

    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#".toList()
    val currentHighlightChar = remember { derivedStateOf {
        if (isSelectionMode || albums.isEmpty()) null
        else {
            val firstVisibleIndex = scrollState.firstVisibleItemIndex
            // Basic logic: find first visible album's title first char
            // Account for header items (Recently Played section and Sort Header)
            var actualAlbumIndex = firstVisibleIndex
            if (recentlyPlayedAlbums.isNotEmpty() && searchQuery.isEmpty()) actualAlbumIndex--
            actualAlbumIndex-- // Sort header

            if (actualAlbumIndex in albums.indices) {
                val char = albums[actualAlbumIndex].title.firstOrNull()?.uppercaseChar()
                if (char != null && char in 'A'..'Z') char else '#'
            } else null
        }
    }}

    if (albums.isEmpty() && searchQuery.isEmpty()) {
        EmptyAlbumsState()
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Search Bar in Selection Mode
                if (isSelectionMode) {
                    item {
                        AlbumSearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) }
                        )
                    }
                    item {
                        SelectAllRow(
                            isAllSelected = selectedIds.size == albums.size && albums.isNotEmpty(),
                            onToggleSelectAll = {
                                if (selectedIds.size == albums.size) viewModel.clearSelection()
                                else viewModel.selectAllAlbums()
                            }
                        )
                    }
                }

                // Recently Played Section
                if (!isSelectionMode && recentlyPlayedAlbums.isNotEmpty() && searchQuery.isEmpty()) {
                    item {
                        RecentlyPlayedAlbumsSection(
                            albums = recentlyPlayedAlbums,
                            onAlbumClick = onAlbumClick,
                            onHeaderClick = {
                                // TODO: Navigate to recently played screen
                            }
                        )
                    }
                }

                // Sort Header
                if (!isSelectionMode) {
                    item {
                        AlbumSortHeader(
                            albumCount = albums.size,
                            onSortClick = { showSortDialog = true },
                            onSelectionClick = { viewModel.toggleSelectionMode(true) }
                        )
                    }
                }

                // Main Album List
                items(albums, key = { it.id }) { album ->
                    AlbumRow(
                        album = album,
                        isSelected = selectedIds.contains(album.id),
                        isSelectionMode = isSelectionMode,
                        onClick = { onAlbumClick(album.id) },
                        onLongClick = { onAlbumLongClick(album.id) }
                    )
                }
            }

            // Alphabet Scroller
            if (!isSelectionMode) {
                AlphabetScroller(
                    highlightChar = currentHighlightChar.value,
                    onCharClick = { char ->
                        coroutineScope.launch {
                            val targetIndex = albums.indexOfFirst { 
                                val firstChar = it.title.firstOrNull()?.uppercaseChar()
                                if (char == '#') firstChar == null || firstChar !in 'A'..'Z'
                                else firstChar == char
                            }
                            if (targetIndex != -1) {
                                // Account for headers
                                var scrollIndex = targetIndex + 1 // Sort header
                                if (recentlyPlayedAlbums.isNotEmpty() && searchQuery.isEmpty()) scrollIndex++
                                scrollState.animateScrollToItem(scrollIndex)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                )
            }
        }
    }

    if (showSortDialog) {
        AlbumSortDialog(
            currentSortMode = sortMode,
            isAscending = isAscending,
            onDismiss = { showSortDialog = false },
            onSortModeChange = { viewModel.setSortMode(it) },
            onOrderChange = { viewModel.setAscending(it) }
        )
    }
}

/**
 * Artists Content - Displays artists in a list view.
 */
@Composable
private fun ArtistsContent(
    artists: List<Artist>,
    recentlyPlayedArtists: List<Artist>,
    onArtistClick: () -> Unit,
    viewModel: ArtistsViewModel
) {
    if (artists.isEmpty()) {
        EmptyArtistsContentState()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Recently played section
            if (recentlyPlayedArtists.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Recently played",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentlyPlayedArtists.take(10), key = { it.id }) { artist ->
                                ArtistCard(
                                    artist = artist,
                                    onClick = { onArtistClick() }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp
                        )
                    }
                }
            }

            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${artists.size} ${if (artists.size == 1) "artist" else "artists"}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Artists list
            items(artists, key = { it.id }) { artist ->
                ArtistListItem(
                    artist = artist,
                    onClick = { onArtistClick() }
                )
            }
        }
    }
}

@Composable
private fun ArtistCard(
    artist: Artist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (artist.artworkUri != null) {
                SubcomposeAsyncImage(
                    model = artist.artworkUri,
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ArtistListItem(
    artist: Artist,
    onClick: () -> Unit
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
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (artist.artworkUri != null) {
                SubcomposeAsyncImage(
                    model = artist.artworkUri,
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${artist.albumCount} ${if (artist.albumCount == 1) "album" else "albums"} · ${artist.songCount} ${if (artist.songCount == 1) "song" else "songs"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyArtistsContentState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No artists found",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Scan your library to discover artists",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Genres Content - Displays genres in a list.
 */
@Composable
private fun GenresContent(
    genres: List<Genre>,
    sortMode: GenreSortMode,
    onGenreClick: (Long) -> Unit,
    viewModel: GenresViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CategoryPillRow(
            itemCount = genres.size,
            itemLabel = if (genres.size == 1) "genre" else "genres",
            sortOptions = GenreSortMode.entries.toList(),
            currentSortMode = sortMode,
            onSortModeChanged = { viewModel.setSortMode(it) },
            sortModeToDisplayName = { mode ->
                when (mode) {
                    GenreSortMode.NAME -> "Name"
                    GenreSortMode.SONG_COUNT -> "Songs"
                    GenreSortMode.RANDOM -> "Random"
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

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
                items(
                    items = genres,
                    key = { it.id }
                ) { genre ->
                    GenreRow(
                        genre = genre,
                        onClick = { onGenreClick(genre.id) },
                        onPlayClick = { viewModel.playGenre(genre.id) },
                        onPlayNextClick = { viewModel.playGenreNext(genre.id) },
                        onAddToQueueClick = { viewModel.addGenreToQueue(genre.id) },
                        onAddToPlaylistClick = { viewModel.showAddToPlaylistDialog(listOf(genre.id)) },
                        onDeleteClick = { /* Handled via detail or selection mode */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentlyPlayedAlbumsSection(
    albums: List<Album>,
    onAlbumClick: (Long) -> Unit,
    onHeaderClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onHeaderClick)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Recently played",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "See all",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(albums) { album ->
                RecentlyPlayedAlbumCard(album = album, onClick = { onAlbumClick(album.id) })
            }
        }
    }
}

@Composable
private fun RecentlyPlayedAlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = album.albumArtUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AlbumSortHeader(
    albumCount: Int,
    onSortClick: () -> Unit,
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
            text = "$albumCount albums",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row {
            IconButton(onClick = onSelectionClick) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Select")
            }
            IconButton(onClick = onSortClick) {
                Icon(Icons.Default.Sort, contentDescription = "Sort")
            }
            /*IconButton(onClick = { /* TODO: Toggle Grid/List */ }) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "View Mode")
            }*/
        }
    }
}

@Composable
private fun AlbumRow(
    album: Album,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = album.albumArtUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${album.artist} • ${album.songCount} song${if (album.songCount > 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            IconButton(onClick = { /* More options */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
        }
    }
}

@Composable
private fun AlphabetScroller(
    highlightChar: Char?,
    onCharClick: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#".toList()
    Column(
        modifier = modifier.background(Color.Black.copy(alpha = 0.1f), CircleShape),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        alphabet.forEach { char ->
            val isHighlighted = char == highlightChar
            Text(
                text = char.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                ),
                modifier = Modifier
                    .padding(vertical = 1.dp, horizontal = 4.dp)
                    .clickable { onCharClick(char) },
                color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumSortDialog(
    currentSortMode: AlbumSortMode,
    isAscending: Boolean,
    onDismiss: () -> Unit,
    onSortModeChange: (AlbumSortMode) -> Unit,
    onOrderChange: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK", color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
        title = { Text("Sort by") },
        text = {
            Column {
                SortOption(
                    text = "Album name",
                    isSelected = currentSortMode == AlbumSortMode.ALBUM_NAME,
                    onClick = { onSortModeChange(AlbumSortMode.ALBUM_NAME) }
                )
                SortOption(
                    text = "Artist name",
                    isSelected = currentSortMode == AlbumSortMode.ARTIST_NAME,
                    onClick = { onSortModeChange(AlbumSortMode.ARTIST_NAME) }
                )
                SortOption(
                    text = "Number of songs",
                    isSelected = currentSortMode == AlbumSortMode.SONG_COUNT,
                    onClick = { onSortModeChange(AlbumSortMode.SONG_COUNT) }
                )
                SortOption(
                    text = "Year",
                    isSelected = currentSortMode == AlbumSortMode.YEAR,
                    onClick = { onSortModeChange(AlbumSortMode.YEAR) }
                )
                SortOption(
                    text = "Random",
                    isSelected = currentSortMode == AlbumSortMode.RANDOM,
                    onClick = { onSortModeChange(AlbumSortMode.RANDOM) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SortOption(
                    text = "From A to Z",
                    isSelected = isAscending,
                    onClick = { onOrderChange(true) }
                )
                SortOption(
                    text = "From Z to A",
                    isSelected = !isAscending,
                    onClick = { onOrderChange(false) }
                )
            }
        }
    )
}

@Composable
private fun SortOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumSelectionTopBar(
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
private fun AlbumSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search albums", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        }
    }
}

@Composable
private fun SelectAllRow(
    isAllSelected: Boolean,
    onToggleSelectAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelectAll)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Select all",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (isAllSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AlbumSelectionBottomBar(
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            SelectionBottomBarItem(icon = Icons.Default.PlayArrow, label = "Play", onClick = onPlay)
            SelectionBottomBarItem(icon = Icons.Default.PlaylistAdd, label = "Add to play", onClick = onAddToPlaylist)
            SelectionBottomBarItem(icon = Icons.Default.Delete, label = "Delete", onClick = onDelete)
            SelectionBottomBarItem(icon = Icons.Default.MoreVert, label = "More", onClick = onMore)
        }
    }
}

@Composable
private fun SelectionBottomBarItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label)
        Text(label, style = MaterialTheme.typography.labelSmall)
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
    folderViewModel: com.sukoon.music.ui.viewmodel.FolderViewModel
) {
    var showDeleteConfirmation by remember { mutableStateOf<Folder?>(null) }
    var folderToDeleteId by remember { mutableStateOf<Long?>(null) }

    val displayFolders = if (folderViewMode == FolderViewMode.DIRECTORIES) {
        folders
    } else {
        hiddenFolders
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Category pill row
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

        // Folder list or empty state
        Box(modifier = Modifier.weight(1f)) {
            if (displayFolders.isEmpty()) {
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
                            onUnhide = { folderViewModel.unhideFolder(folder.id) },
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

private fun getSmartPlaylistIcon(type: SmartPlaylistType): ImageVector {
    return when (type) {
        SmartPlaylistType.MY_FAVOURITE -> Icons.Default.Favorite
        SmartPlaylistType.LAST_ADDED -> Icons.Default.Add
        SmartPlaylistType.RECENTLY_PLAYED -> Icons.Default.History
        SmartPlaylistType.MOST_PLAYED -> Icons.Default.PlayArrow
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
private fun AlbumDefaultCover() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Album,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
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
