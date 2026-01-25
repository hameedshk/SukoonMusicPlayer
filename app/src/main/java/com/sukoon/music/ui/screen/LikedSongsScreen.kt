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
                title = { Text("Liked Songs") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Sort menu
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, "Sort")
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
                modifier = Modifier.padding(paddingValues)
            )

            // Artist filter menu
            if (showArtistMenu) {
                FilterMenu(
                    title = "Filter by Artist",
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
                    title = "Filter by Album",
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
            LikedSongItem(
                song = song,
                menuHandler = menuHandler,
                onClick = { onSongClick(song) },
                onLikeClick = { onLikeClick(song) }
            )
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
            text = "Liked Songs",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$songCount ${if (songCount == 1) "song" else "songs"}",
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
            Icon(Icons.Default.PlayArrow, "Play All", Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Play All")
        }

        OutlinedButton(
            onClick = onShuffleAll,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Shuffle, "Shuffle", Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Shuffle")
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
                Text(selectedArtist ?: "Artist")
            },
            leadingIcon = if (selectedArtist != null) {
                { Icon(Icons.Default.Person, null, Modifier.size(18.dp)) }
            } else null
        )

        // Album filter chip
        FilterChip(
            selected = selectedAlbum != null,
            onClick = onAlbumFilterClick,
            label = {
                Text(selectedAlbum ?: "Album")
            },
            leadingIcon = if (selectedAlbum != null) {
                { Icon(Icons.Default.Album, null, Modifier.size(18.dp)) }
            } else null
        )

        // Clear filters button
        if (selectedArtist != null || selectedAlbum != null) {
            FilterChip(
                selected = false,
                onClick = onClearFilters,
                label = { Text("Clear") },
                leadingIcon = {
                    Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                }
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
                            Text("Clear Filter", color = MaterialTheme.colorScheme.primary)
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
                Text("Close")
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

private fun getSortModeLabel(mode: LikedSongsSortMode): String = when (mode) {
    LikedSongsSortMode.TITLE -> "Title (A-Z)"
    LikedSongsSortMode.ARTIST -> "Artist (A-Z)"
    LikedSongsSortMode.ALBUM -> "Album (A-Z)"
    LikedSongsSortMode.DATE_ADDED -> "Recently Added"
}

@Composable
private fun LikedSongItem(
    song: Song,
    menuHandler: SongMenuHandler,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            Card(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (song.albumArtUri != null) {
                        SubcomposeAsyncImage(
                            model = song.albumArtUri,
                            contentDescription = "Album art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            loading = {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            error = {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Song Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // More options menu
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Like Button
            IconButton(onClick = onLikeClick) {
                AnimatedFavoriteIcon(
                    isLiked = song.isLiked,
                    songId = song.id,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 24.dp
                )
            }
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
            text = "No liked songs yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Songs you like will appear here",
            style = MaterialTheme.typography.bodyMedium,
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
            text = "No songs match filters",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Try clearing filters",
            style = MaterialTheme.typography.bodySmall,
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
