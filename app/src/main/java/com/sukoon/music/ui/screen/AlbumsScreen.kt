package com.sukoon.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Album
import com.sukoon.music.ui.components.AddToPlaylistDialog
import com.sukoon.music.ui.components.AlphabetScrollBar
import com.sukoon.music.ui.components.MultiSelectActionBottomBar
import com.sukoon.music.ui.components.DeleteConfirmationDialog
import com.sukoon.music.ui.components.PlaceholderAlbumArt
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.AlbumsViewModel
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.sukoon.music.ui.components.AlphabetScroller
import com.sukoon.music.ui.components.RecentlyPlayedAlbumCard
import com.sukoon.music.ui.components.RecentlyPlayedSection
import kotlinx.coroutines.launch
import com.sukoon.music.ui.screen.albums.*
import com.sukoon.music.ui.theme.*

/**
 * Albums Screen - Shows all albums in a grid view.
 *
 * Features:
 * - 2-column grid layout (S-style)
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
    onNavigateToAlbumSelection: () -> Unit = {},
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
    var showSortDialog by remember { mutableStateOf(false) }
    var albumsPendingDeletion by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Delete launcher for handling file deletion permissions
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "Albums deleted successfully", Toast.LENGTH_SHORT).show()
            albumsPendingDeletion = false
        } else {
            Toast.makeText(context, "Delete cancelled", Toast.LENGTH_SHORT).show()
        }
        albumsPendingDeletion = false
    }


    // Handle pending album(s) for playlist - support multi-album selection
    LaunchedEffect(pendingAlbumForPlaylist, selectedAlbumIds) {
        if (pendingAlbumForPlaylist != null) {
            if (pendingAlbumForPlaylist == -1L) {
                // Multi-select: get all selected albums' songs
                if (selectedAlbumIds.isNotEmpty()) {
                    val allSongs = selectedAlbumIds.flatMap { albumId ->
                        viewModel.getSongsForAlbum(albumId).toList()
                    }
                    albumSongsForPlaylist = allSongs
                    showPlaylistDialog = true
                }
                pendingAlbumForPlaylist = null
            } else {
                // Single album from context menu
                albumSongsForPlaylist = viewModel.getSongsForAlbum(pendingAlbumForPlaylist!!)
                showPlaylistDialog = true
                pendingAlbumForPlaylist = null
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text("${selectedAlbumIds.size} selected")
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                viewModel.toggleSelectionMode(false)
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
            if (isSelectionMode && selectedAlbumIds.isNotEmpty()) {
                MultiSelectActionBottomBar(
                    onPlay = { viewModel.playSelectedAlbums() },
                    onAddToPlaylist = {
                        // Trigger LaunchedEffect to load all selected albums' songs
                        pendingAlbumForPlaylist = -1L // Use sentinel value to trigger
                    },
                    onDelete = { albumsPendingDeletion = true },
                    onPlayNext = { viewModel.playSelectedAlbumsNext() },
                    onAddToQueue = { viewModel.addSelectedAlbumsToQueue() }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isSelectionMode) Modifier.padding(paddingValues)
                    else Modifier
                )
        ) {
            if (albums.isEmpty()) {
                EmptyAlbumsState()
            } else {
                if (showSortDialog) {
                    AlbumSortDialog(
                        currentSortMode = viewModel.sortMode.collectAsStateWithLifecycle().value,
                        isAscending = viewModel.isAscending.collectAsStateWithLifecycle().value,
                        onDismiss = { showSortDialog = false },
                        onSortModeChange = { viewModel.setSortMode(it) },
                        onOrderChange = { viewModel.setAscending(it) }
                    )
                }

// Main Albums List
                val scrollState = rememberLazyListState()

                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = MiniPlayerHeight + SpacingSmall)
                ) {

                    // ✅ Recently Played (wrapped correctly)
                    if (!isSelectionMode && recentlyPlayedAlbums.isNotEmpty()) {
                        item {
                            RecentlyPlayedAlbumsSection(
                                albums = recentlyPlayedAlbums,
                                onAlbumClick = onNavigateToAlbum,
                                onHeaderClick = {
                                    onNavigateToSmartPlaylist(
                                        com.sukoon.music.domain.model.SmartPlaylistType.RECENTLY_PLAYED
                                    )
                                }
                            )
                        }
                    }

                    if (!isSelectionMode) {
                        stickyHeader(key = "header") {
                            AlbumSortHeader(
                                albumCount = albums.size,
                                onSortClick = {
                                    showSortDialog = true
                                },
                                onSelectionClick = {
                                    onNavigateToAlbumSelection()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                            )
                        }
                    }

                    items(albums, key = { it.id }) { album ->
                        AlbumRow(
                            album = album,
                            isSelected = selectedAlbumIds.contains(album.id),
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleAlbumSelection(album.id)
                                } else {
                                    onNavigateToAlbum(album.id)
                                }
                            },
                            onMoreClick = {
                                selectedAlbumForMenu = album
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    viewModel.toggleSelectionMode(true)
                                    viewModel.toggleAlbumSelection(album.id)
                                }
                            }
                        )
                    }
                }

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
                    Toast.makeText(context, "Songs added to playlist", Toast.LENGTH_SHORT).show()
                    viewModel.toggleSelectionMode(false)
                },
                onDismiss = { showPlaylistDialog = false }
            )
        }

        // Delete confirmation dialog
        if (albumsPendingDeletion) {
            AlertDialog(
                onDismissRequest = { albumsPendingDeletion = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text("Delete ${selectedAlbumIds.size} album(s)?")
                },
                text = {
                    Text("All songs in these albums will be permanently deleted from your device. This cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // Get all songs from selected albums and delete them
                            val selectedAlbumsList = selectedAlbumIds.toList()
                            viewModel.deleteSelectedAlbumsWithResult { deleteResult ->
                                when (deleteResult) {
                                    is DeleteHelper.DeleteResult.RequiresPermission -> {
                                        deleteLauncher.launch(
                                            IntentSenderRequest.Builder(deleteResult.intentSender).build()
                                        )
                                    }
                                    is DeleteHelper.DeleteResult.Success -> {
                                        Toast.makeText(context, "Albums deleted successfully", Toast.LENGTH_SHORT).show()
                                        albumsPendingDeletion = false
                                    }
                                    is DeleteHelper.DeleteResult.Error -> {
                                        Toast.makeText(context, "Error: ${deleteResult.message}", Toast.LENGTH_SHORT).show()
                                        albumsPendingDeletion = false
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
                    TextButton(onClick = { albumsPendingDeletion = false }) {
                        Text("Cancel")
                    }
                }
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
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .clickable(onClick = onClick),
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
                            PlaceholderAlbumArt.Placeholder(
                                seed = PlaceholderAlbumArt.generateSeed(
                                    albumName = album.title,
                                    artistName = album.artist,
                                    albumId = album.id
                                )
                            )
                        }
                    )
                } else {
                    PlaceholderAlbumArt.Placeholder(
                        seed = PlaceholderAlbumArt.generateSeed(
                            albumName = album.title,
                            artistName = album.artist,
                            albumId = album.id
                        )
                    )
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
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .zIndex(1f)
                            .padding(4.dp)
                    ) {
                        IconButton(
                            onClick = onShowContextMenu,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(50)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
                    style = MaterialTheme.typography.cardTitle,
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
private fun EmptyAlbumsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 32.dp,
                end = 32.dp,
                top = ContentTopPadding,
                bottom = ContentBottomPadding + 16.dp
            ),
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
            .padding(bottom = 16.dp)
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
                    style = MaterialTheme.typography.screenHeader,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
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
                    PlaceholderAlbumArt.Placeholder(
                        seed = PlaceholderAlbumArt.generateSeed(
                            albumName = album.title,
                            artistName = album.artist,
                            albumId = album.id
                        )
                    )
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.listItemTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.listItemSubtitle,
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
                            PlaceholderAlbumArt.Placeholder(
                                seed = PlaceholderAlbumArt.generateSeed(
                                    albumName = album.title,
                                    artistName = album.artist,
                                    albumId = album.id
                                )
                            )
                        },
                        error = {
                            PlaceholderAlbumArt.Placeholder(
                                seed = PlaceholderAlbumArt.generateSeed(
                                    albumName = album.title,
                                    artistName = album.artist,
                                    albumId = album.id
                                )
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.cardTitle
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
private fun AlbumSortHeader(
    albumCount: Int,
    onSortClick: () -> Unit,
    onSelectionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$albumCount albums",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row {
            IconButton(onClick = onSortClick) {
                Icon(Icons.Default.Sort, contentDescription = "Sort")
            }
            IconButton(onClick = onSelectionClick) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Select")
            }
            /*IconButton(onClick = { /* TODO: Toggle Grid/List */ }) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "View Mode")
            }*/
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AlbumsScreenPreview() {
    SukoonMusicPlayerTheme(theme = AppTheme.DARK) {
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
