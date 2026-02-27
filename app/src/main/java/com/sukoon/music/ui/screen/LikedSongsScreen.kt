package com.sukoon.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.data.premium.PremiumManager
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.*
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.LikedSongsViewModel
import com.sukoon.music.ui.viewmodel.LikedSongsSortMode
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import com.sukoon.music.ui.theme.*

/**
 * Enhanced Liked Songs Screen - Shows all user-favorited songs with filtering and sorting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikedSongsScreen(
    onBackClick: () -> Unit,
    onNavigateToNowPlaying: () -> Unit = {},
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToArtist: (Long) -> Unit = {},
    navController: NavController? = null,
    premiumManager: PremiumManager? = null,
    viewModel: LikedSongsViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val likedSongs by viewModel.likedSongs.collectAsStateWithLifecycle()
    val selectedArtist by viewModel.selectedArtist.collectAsStateWithLifecycle()
    val selectedAlbum by viewModel.selectedAlbum.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val availableArtists by viewModel.availableArtists.collectAsStateWithLifecycle()
    val availableAlbums by viewModel.availableAlbums.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    var showArtistMenu by remember { mutableStateOf(false) }
    var showAlbumMenu by remember { mutableStateOf(false) }
    var showSongInfo by remember { mutableStateOf<Song?>(null) }
    var showPlaylistSelector by remember { mutableStateOf<Song?>(null) }

    // Share handler
    val shareHandler = rememberShareHandler()

    // Create menu handler for song context menu
    val menuHandler = rememberSongMenuHandler(
        playbackRepository = viewModel.playbackRepository,
        onNavigateToAlbum = onNavigateToAlbum,
        onNavigateToArtist = onNavigateToArtist,
        onShowSongInfo = { song -> showSongInfo = song },
        onShowPlaylistSelector = { song -> showPlaylistSelector = song },
        onToggleLike = { songId, isLiked -> viewModel.toggleLike(songId, isLiked) },
        onShare = shareHandler
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_liked_songs)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_back)
                        )
                    }
                },
                actions = {
                    // Sort menu
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                Icons.Default.Sort,
                                androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.liked_songs_cd_sort)
                            )
                        }
                        SortModeMenu(
                            expanded = showSortMenu,
                            currentMode = sortMode,
                            onDismiss = { showSortMenu = false },
                            onModeSelect = { mode ->
                                viewModel.updateSortMode(mode)
                                showSortMenu = false
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (likedSongs.isEmpty() && selectedArtist == null && selectedAlbum == null) {
            EmptyLikedSongsState(
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LikedSongsContent(
                songs = likedSongs,
                selectedArtist = selectedArtist,
                selectedAlbum = selectedAlbum,
                availableArtists = availableArtists,
                availableAlbums = availableAlbums,
                menuHandler = menuHandler,
                onSongClick = { song ->
                    if (playbackState.currentSong?.id != song.id) {
                        viewModel.playSong(song)
                    } else {
                        onNavigateToNowPlaying()
                    }
                },
                onLikeClick = { song -> viewModel.toggleLike(song.id, song.isLiked) },
                onPlayAll = { viewModel.playAll() },
                onShuffleAll = { viewModel.shuffleAll() },
                onArtistFilterClick = { showArtistMenu = true },
                onAlbumFilterClick = { showAlbumMenu = true },
                onClearFilters = { viewModel.clearFilters() },
                currentSongId = playbackState.currentSong?.id,
                isPlayingGlobally = playbackState.isPlaying,
                modifier = Modifier.padding(paddingValues)
            )

            // Artist filter menu
            if (showArtistMenu) {
                FilterMenu(
                    title = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.liked_songs_filter_artist_title),
                    items = availableArtists,
                    selectedItem = selectedArtist,
                    onDismiss = { showArtistMenu = false },
                    onItemSelect = { artist ->
                        viewModel.filterByArtist(artist)
                        showArtistMenu = false
                    },
                    onClear = {
                        viewModel.filterByArtist(null)
                        showArtistMenu = false
                    }
                )
            }

            // Album filter menu
            if (showAlbumMenu) {
                FilterMenu(
                    title = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.liked_songs_filter_album_title),
                    items = availableAlbums,
                    selectedItem = selectedAlbum,
                    onDismiss = { showAlbumMenu = false },
                    onItemSelect = { album ->
                        viewModel.filterByAlbum(album)
                        showAlbumMenu = false
                    },
                    onClear = {
                        viewModel.filterByAlbum(null)
                        showAlbumMenu = false
                    }
                )
            }
        }
    }

    // Song info dialog
    showSongInfo?.let { song ->
        SongInfoDialog(
            song = song,
            onDismiss = { showSongInfo = null }
        )
    }

    // Add to playlist dialog
    showPlaylistSelector?.let { song ->
        AddToPlaylistDialog(
            playlists = playlistViewModel.playlists.collectAsStateWithLifecycle().value,
            onPlaylistSelected = { playlistId ->
                playlistViewModel.addSongToPlaylist(playlistId, song.id)
                showPlaylistSelector = null
            },
            onDismiss = { showPlaylistSelector = null }
        )
    }
}

@Composable
private fun LikedSongsContent(
    songs: List<Song>,
    selectedArtist: String?,
    selectedAlbum: String?,
    availableArtists: List<String>,
    availableAlbums: List<String>,
    menuHandler: SongMenuHandler,
    onSongClick: (Song) -> Unit,
    onLikeClick: (Song) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onArtistFilterClick: () -> Unit,
    onAlbumFilterClick: () -> Unit,
    onClearFilters: () -> Unit,
    currentSongId: Long?,
    isPlayingGlobally: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(
            top = ContentTopPadding,
            bottom = 16.dp + ContentBottomPadding,
            start = 0.dp,
            end = 0.dp
        )
    ) {
        // Header with icon and count
        item {
            LikedSongsHeader(songCount = songs.size)
        }

        // Play All / Shuffle buttons
        if (songs.isNotEmpty()) {
            item {
                PlayControlButtons(
                    onPlayAll = onPlayAll,
                    onShuffleAll = onShuffleAll,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Filter chips
        if (availableArtists.isNotEmpty() || availableAlbums.isNotEmpty()) {
            item {
                FilterChips(
                    selectedArtist = selectedArtist,
                    selectedAlbum = selectedAlbum,
                    onArtistFilterClick = onArtistFilterClick,
                    onAlbumFilterClick = onAlbumFilterClick,
                    onClearFilters = onClearFilters,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Empty state when filters return no results
        if (songs.isEmpty() && (selectedArtist != null || selectedAlbum != null)) {
            item {
                NoFilterResultsState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            }
        }

        // Song List
        items(
            items = songs,
            key = { it.id }
        ) { song ->
            var showMenu by remember { mutableStateOf(false) }
            val isCurrentSong = currentSongId == song.id

            StandardSongListItem(
                song = song,
                isCurrentSong = isCurrentSong,
                isPlaybackActive = isCurrentSong && isPlayingGlobally,
                isSelectionMode = false,
                isSelected = false,
                onClick = { onSongClick(song) },
                onCheckboxClick = {},
                onLongPress = {},
                onLikeClick = { onLikeClick(song) },
                onMenuClick = { showMenu = true }
            )

            if (showMenu) {
                SongContextMenu(
                    song = song,
                    menuHandler = menuHandler,
                    onDismiss = { showMenu = false }
                )
            }
        }
    }
}

@Composable
private fun LikedSongsHeader(songCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_liked_songs),
            style = MaterialTheme.typography.screenHeader,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = androidx.compose.ui.res.pluralStringResource(
                com.sukoon.music.R.plurals.liked_songs_count,
                songCount,
                songCount
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun PlayControlButtons(
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onPlayAll,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                Icons.Default.PlayArrow,
                androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play_all),
                Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play_all))
        }

        OutlinedButton(
            onClick = onShuffleAll,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.Shuffle,
                androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_shuffle),
                Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_shuffle))
        }
    }
}

@Composable
private fun FilterChips(
    selectedArtist: String?,
    selectedAlbum: String?,
    onArtistFilterClick: () -> Unit,
    onAlbumFilterClick: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Artist filter chip
        FilterChip(
            selected = selectedArtist != null,
            onClick = onArtistFilterClick,
            label = {
                Text(
                    selectedArtist
                        ?: androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.liked_songs_filter_chip_artist)
                )
            },
            leadingIcon = if (selectedArtist != null) {
                { Icon(Icons.Default.Person, null, Modifier.size(18.dp)) }
            } else null,
            modifier = if (selectedArtist == null) {
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .surfaceLevel2Gradient()
            } else {
                Modifier
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = if (selectedArtist == null) Color.Transparent else MaterialTheme.colorScheme.primary,
                labelColor = if (selectedArtist == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
            )
        )

        // Album filter chip
        FilterChip(
            selected = selectedAlbum != null,
            onClick = onAlbumFilterClick,
            label = {
                Text(
                    selectedAlbum
                        ?: androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.liked_songs_filter_chip_album)
                )
            },
            leadingIcon = if (selectedAlbum != null) {
                { Icon(Icons.Default.Album, null, Modifier.size(18.dp)) }
            } else null,
            modifier = if (selectedAlbum == null) {
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .surfaceLevel2Gradient()
            } else {
                Modifier
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = if (selectedAlbum == null) Color.Transparent else MaterialTheme.colorScheme.primary,
                labelColor = if (selectedAlbum == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
            )
        )

        // Clear filters button
        if (selectedArtist != null || selectedAlbum != null) {
            FilterChip(
                selected = false,
                onClick = onClearFilters,
                label = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.liked_songs_filter_chip_clear)) },
                leadingIcon = {
                    Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .surfaceLevel2Gradient(),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun FilterMenu(
    title: String,
    items: List<String>,
    selectedItem: String?,
    onDismiss: () -> Unit,
    onItemSelect: (String) -> Unit,
    onClear: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                if (selectedItem != null) {
                    item {
                        TextButton(
                            onClick = onClear,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.liked_songs_clear_filter),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        HorizontalDivider()
                    }
                }

                items(items) { item ->
                    TextButton(
                        onClick = { onItemSelect(item) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (item == selectedItem) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_close))
            }
        }
    )
}

@Composable
private fun SortModeMenu(
    expanded: Boolean,
    currentMode: LikedSongsSortMode,
    onDismiss: () -> Unit,
    onModeSelect: (LikedSongsSortMode) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        LikedSongsSortMode.entries.forEach { mode ->
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(getSortModeLabel(mode))
                        if (mode == currentMode) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                onClick = { onModeSelect(mode) }
            )
        }
    }
}

@Composable
private fun getSortModeLabel(mode: LikedSongsSortMode): String = when (mode) {
    LikedSongsSortMode.TITLE -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.liked_songs_sort_title_asc)
    LikedSongsSortMode.ARTIST -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.liked_songs_sort_artist_asc)
    LikedSongsSortMode.ALBUM -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.liked_songs_sort_album_asc)
    LikedSongsSortMode.DATE_ADDED -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.liked_songs_sort_recently_added)
}



@Composable
private fun EmptyLikedSongsState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.liked_songs_empty_title),
            style = MaterialTheme.typography.emptyStateTitle,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.liked_songs_empty_message),
            style = MaterialTheme.typography.emptyStateDescription,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NoFilterResultsState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.FilterAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.liked_songs_no_filter_results_title),
            style = MaterialTheme.typography.emptyStateTitle,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.liked_songs_no_filter_results_message),
            style = MaterialTheme.typography.emptyStateDescription,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun LikedSongsScreenPreview() {
    SukoonMusicPlayerTheme(theme = AppTheme.DARK) {
        LikedSongsScreen(onBackClick = {})
    }
}



