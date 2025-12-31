package com.sukoon.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Album
import com.sukoon.music.ui.components.AddToPlaylistDialog
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.AlbumsViewModel

/**
 * Albums Screen - Shows all albums in a grid view.
 *
 * Features:
 * - 2-column grid layout (Spotify-style)
 * - Album cards with cover art, title, artist, and song count
 * - Context menu for play/shuffle options
 * - Empty state when no albums exist
 * - Tap to navigate to album detail screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    onNavigateToAlbum: (Long) -> Unit,
    onBackClick: () -> Unit,
    onNavigateToSmartPlaylist: (com.sukoon.music.domain.model.SmartPlaylistType) -> Unit = {},
    viewModel: AlbumsViewModel = hiltViewModel(),
    playlistViewModel: com.sukoon.music.ui.viewmodel.PlaylistViewModel = hiltViewModel()
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val recentlyPlayedAlbums by viewModel.recentlyPlayedAlbums.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedAlbumIds by viewModel.selectedAlbumIds.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()

    var showPlaylistDialog by remember { mutableStateOf(false) }
    var selectedAlbumForMenu by remember { mutableStateOf<Album?>(null) }
    var albumSongsForPlaylist by remember { mutableStateOf<List<com.sukoon.music.domain.model.Song>>(emptyList()) }
    var pendingAlbumForPlaylist by remember { mutableStateOf<Long?>(null) }


    // Handle pending album for playlist
    LaunchedEffect(pendingAlbumForPlaylist) {
        pendingAlbumForPlaylist?.let { albumId ->
            albumSongsForPlaylist = viewModel.getSongsForAlbum(albumId)
            showPlaylistDialog = true
            pendingAlbumForPlaylist = null
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedAlbumIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleSelectionMode(false) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit selection"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("Albums") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (isSelectionMode && selectedAlbumIds.isNotEmpty()) {
                AlbumSelectionBottomBar(
                    onPlay = { viewModel.playSelectedAlbums() },
                    onAddToPlaylist = {
                        // TODO: Handle multiple albums for playlist
                        val firstAlbumId = selectedAlbumIds.firstOrNull()
                        if (firstAlbumId != null) {
                            pendingAlbumForPlaylist = firstAlbumId
                        }
                    },
                    onDelete = { viewModel.deleteSelectedAlbums() },
                    onMore = { /* TODO: Show more options */ }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (albums.isEmpty()) {
                EmptyAlbumsState()
            } else {
                // Recently Played Section
                if (!isSelectionMode && recentlyPlayedAlbums.isNotEmpty()) {
                    RecentlyPlayedAlbumsSection(
                        albums = recentlyPlayedAlbums,
                        onAlbumClick = onNavigateToAlbum,
                        onHeaderClick = {
                            onNavigateToSmartPlaylist(com.sukoon.music.domain.model.SmartPlaylistType.RECENTLY_PLAYED)
                        }
                    )
                }

                // Main Albums Grid
                AlbumsGrid(
                    albums = albums,
                    isSelectionMode = isSelectionMode,
                    selectedAlbumIds = selectedAlbumIds,
                    onAlbumClick = { album ->
                        if (isSelectionMode) {
                            viewModel.toggleAlbumSelection(album.id)
                        } else {
                            onNavigateToAlbum(album.id)
                        }
                    },
                    onAlbumLongClick = { album ->
                        if (!isSelectionMode) {
                            viewModel.toggleSelectionMode(true)
                            viewModel.toggleAlbumSelection(album.id)
                        }
                    },
                    onPlayAlbum = { albumId ->
                        viewModel.playAlbum(albumId)
                    },
                    onShuffleAlbum = { albumId ->
                        viewModel.shuffleAlbum(albumId)
                    },
                    onShowContextMenu = { album ->
                        selectedAlbumForMenu = album
                    },
                    onSortClick = { /* TODO: Show sort dialog */ },
                    onSelectionClick = { viewModel.toggleSelectionMode(true) }
                )
            }
        }

        // Playlist Dialog
        if (showPlaylistDialog) {
            AddToPlaylistDialog(
                playlists = playlists,
                onPlaylistSelected = { playlistId ->
                    albumSongsForPlaylist.forEach { song ->
                        playlistViewModel.addSongToPlaylist(playlistId, song.id)
                    }
                    showPlaylistDialog = false
                },
                onDismiss = { showPlaylistDialog = false }
            )
        }

        // Album Context Menu
        selectedAlbumForMenu?.let { album ->
            AlbumContextMenuBottomSheet(
                album = album,
                onDismiss = { selectedAlbumForMenu = null },
                onPlay = {
                    viewModel.playAlbum(album.id)
                    selectedAlbumForMenu = null
                },
                onPlayNext = {
                    viewModel.playAlbumNext(album.id)
                    selectedAlbumForMenu = null
                },
                onAddToQueue = {
                    viewModel.addAlbumToQueue(album.id)
                    selectedAlbumForMenu = null
                },
                onAddToPlaylist = {
                    pendingAlbumForPlaylist = album.id
                    selectedAlbumForMenu = null
                },
                onEditTags = {
                    // TODO: Show edit tags dialog
                    selectedAlbumForMenu = null
                },
                onChangeCover = {
                    // TODO: Show change cover dialog
                    selectedAlbumForMenu = null
                },
                onDelete = {
                    // TODO: Show delete confirmation
                    selectedAlbumForMenu = null
                }
            )
        }
    }
}

@Composable
private fun AlbumsGrid(
    albums: List<Album>,
    isSelectionMode: Boolean,
    selectedAlbumIds: Set<Long>,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
    onPlayAlbum: (Long) -> Unit,
    onShuffleAlbum: (Long) -> Unit,
    onShowContextMenu: (Album) -> Unit,
    onSortClick: () -> Unit,
    onSelectionClick: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Sort Header spanning 2 columns - only show when NOT in selection mode
        if (!isSelectionMode) {
            item(
                span = { GridItemSpan(2) }
            ) {
                AlbumGridSortHeader(
                    albumCount = albums.size,
                    onSortClick = onSortClick,
                    onSelectionClick = onSelectionClick
                )
            }
        }

        items(
            items = albums,
            key = { album -> album.id }
        ) { album ->
            AlbumCard(
                album = album,
                isSelectionMode = isSelectionMode,
                isSelected = selectedAlbumIds.contains(album.id),
                onClick = {
                    onAlbumClick(album) },
                onLongClick = { onAlbumLongClick(album) },
                onPlayClick = { onPlayAlbum(album.id) },
                onShuffleClick = { onShuffleAlbum(album.id) },
                onShowContextMenu = { onShowContextMenu(album) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumCard(
    album: Album,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onShowContextMenu: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Album Art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (album.albumArtUri != null) {
                    SubcomposeAsyncImage(
                        model = album.albumArtUri,
                        contentDescription = "Album cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        error = {
                            DefaultAlbumCover()
                        }
                    )
                } else {
                    DefaultAlbumCover()
                }

                // Selection checkbox or context menu button
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onClick() }
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            onShowContextMenu()
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(50)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White
                        )
                    }
                }
            }

            // Album Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${album.songCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DefaultAlbumCover() {
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
private fun EmptyAlbumsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Album,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No albums found",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Scan your library to discover albums from your music collection",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
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
            fontWeight = FontWeight.Medium,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumContextMenuBottomSheet(
    album: Album,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEditTags: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header with album details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art
                Card(
                    modifier = Modifier.size(56.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    SubcomposeAsyncImage(
                        model = album.albumArtUri,
                        contentDescription = null,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Album,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Album,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${album.artist} · ${album.songCount} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Menu Items
            ListItem(
                headlineContent = { Text("Play") },
                leadingContent = { Icon(Icons.Default.PlayArrow, null) },
                modifier = Modifier.clickable(onClick = onPlay)
            )
            ListItem(
                headlineContent = { Text("Play next") },
                leadingContent = { Icon(Icons.Default.SkipNext, null) },
                modifier = Modifier.clickable(onClick = onPlayNext)
            )
            ListItem(
                headlineContent = { Text("Add to queue") },
                leadingContent = { Icon(Icons.Default.Queue, null) },
                modifier = Modifier.clickable(onClick = onAddToQueue)
            )
            ListItem(
                headlineContent = { Text("Add to playlist") },
                leadingContent = { Icon(Icons.Default.PlaylistAdd, null) },
                modifier = Modifier.clickable(onClick = onAddToPlaylist)
            )
            ListItem(
                headlineContent = { Text("Edit tags") },
                leadingContent = { Icon(Icons.Default.Edit, null) },
                modifier = Modifier.clickable(onClick = onEditTags)
            )
            ListItem(
                headlineContent = { Text("Change cover") },
                leadingContent = { Icon(Icons.Default.Image, null) },
                modifier = Modifier.clickable(onClick = onChangeCover)
            )
            ListItem(
                headlineContent = { Text("Delete from device", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable(onClick = onDelete)
            )
        }
    }
}

@Composable
private fun AlbumSelectionBottomBar(
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onPlay)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Play",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onAddToPlaylist)
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = "Add to playlist",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add to playlist",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onDelete)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onMore)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "More",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AlbumGridSortHeader(
    albumCount: Int,
    onSortClick: () -> Unit,
    onSelectionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$albumCount album${if (albumCount != 1) "s" else ""}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row {
            IconButton(onClick = onSortClick) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Sort"
                )
            }
            IconButton(onClick = onSelectionClick) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Select albums"
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AlbumsScreenPreview() {
    SukoonMusicPlayerTheme(darkTheme = true) {
        AlbumsScreen(
            onNavigateToAlbum = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AlbumCardPreview() {
    SukoonMusicPlayerTheme {
        AlbumCard(
            album = Album(
                id = 1,
                title = "Abbey Road",
                artist = "The Beatles",
                songCount = 17,
                totalDuration = 2874000,
                albumArtUri = null,
                songIds = emptyList()
            ),
            isSelectionMode = false,
            isSelected = false,
            onClick = {},
            onLongClick = {},
            onPlayClick = {},
            onShuffleClick = {},
            onShowContextMenu = {}
        )
    }
}
