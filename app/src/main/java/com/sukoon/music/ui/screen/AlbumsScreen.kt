package com.sukoon.music.ui.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.components.AddToPlaylistDialog
import com.sukoon.music.ui.components.MultiSelectActionBottomBar
import com.sukoon.music.ui.components.PlaceholderAlbumArt
import com.sukoon.music.ui.components.RecentlyPlayedAlbumCard
import com.sukoon.music.ui.screen.albums.*
import com.sukoon.music.ui.theme.*
import com.sukoon.music.ui.util.albumArtContentDescription
import com.sukoon.music.ui.viewmodel.AlbumsViewModel

/**
 * Albums Screen - Shows all albums in a fast, scrollable list.
 *
 * Features:
 * - Recently played albums section
 * - Sticky controls for sorting and multi-select
 * - Album rows with cover art, title, artist, and song count
 * - Context menu for play/shuffle options
 * - Empty state when no albums exist
 * - Tap to navigate to album detail screen
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Suppress("UNUSED_PARAMETER")
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
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_album_deleted_successfully), Toast.LENGTH_SHORT).show()
            albumsPendingDeletion = false
        } else {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_delete_cancelled), Toast.LENGTH_SHORT).show()
        }
        albumsPendingDeletion = false
    }


    // Handle pending album(s) for playlist - support multi-album selection
    LaunchedEffect(pendingAlbumForPlaylist, selectedAlbumIds) {
        if (pendingAlbumForPlaylist != null) {
            if (pendingAlbumForPlaylist == -1L) {
                // Multi-select: get all selected albums' songs
                if (selectedAlbumIds.isNotEmpty()) {
                    val allSongs = viewModel.getSongsForAlbums(selectedAlbumIds)
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
                        Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_selected_count, selectedAlbumIds.size))
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                viewModel.toggleSelectionMode(false)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_exit_selection)
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.home_tab_albums))
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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

                // Main albums list
                val scrollState = rememberLazyListState()

                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = MiniPlayerHeight + SpacingSmall)
                ) {

                    // Recently played section
                    if (!isSelectionMode && recentlyPlayedAlbums.isNotEmpty()) {
                        item(
                            key = "recently_played_albums",
                            contentType = "recently_played_albums"
                        ) {
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

                    items(
                        items = albums,
                        key = { it.id },
                        contentType = { "album_row" }
                    ) { album ->
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
                    Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_songs_added_to_playlist), Toast.LENGTH_SHORT).show()
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
                    Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_delete_selected_albums_title, selectedAlbumIds.size))
                },
                text = {
                    Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_delete_album_songs_message))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteSelectedAlbumsWithResult { deleteResult ->
                                when (deleteResult) {
                                    is DeleteHelper.DeleteResult.RequiresPermission -> {
                                        deleteLauncher.launch(
                                            IntentSenderRequest.Builder(deleteResult.intentSender).build()
                                        )
                                    }
                                    is DeleteHelper.DeleteResult.Success -> {
                                        Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_album_deleted_successfully), Toast.LENGTH_SHORT).show()
                                        albumsPendingDeletion = false
                                    }
                                    is DeleteHelper.DeleteResult.Error -> {
                                        Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_error_with_message, deleteResult.message), Toast.LENGTH_SHORT).show()
                                        albumsPendingDeletion = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { albumsPendingDeletion = false }) {
                        Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_cancel))
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
                }
            )
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
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.albums_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_albums_empty_message),
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
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_common_recently_played),
                    style = MaterialTheme.typography.screenHeader,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_see_all),
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
            items(
                items = albums,
                key = { it.id },
                contentType = { "recent_album_card" }
            ) { album ->
                RecentlyPlayedAlbumCard(album = album, onClick = { onAlbumClick(album.id) })
            }
        }
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
    onAddToPlaylist: () -> Unit
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
                    modifier = Modifier.size(64.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    SubcomposeAsyncImage(
                        model = album.albumArtUri,
                        contentDescription = albumArtContentDescription(
                            albumTitle = album.title,
                            artistName = album.artist
                        ),
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
                        text = androidx.compose.ui.res.stringResource(
                            com.sukoon.music.R.string.library_albums_artist_song_count_format,
                            album.artist,
                            androidx.compose.ui.res.pluralStringResource(
                                com.sukoon.music.R.plurals.common_song_count,
                                album.songCount,
                                album.songCount
                            )
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Menu Items
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play)) },
                leadingContent = { Icon(Icons.Default.PlayArrow, null) },
                modifier = Modifier.clickable(onClick = onPlay)
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play_next)) },
                leadingContent = { Icon(Icons.Default.SkipNext, null) },
                modifier = Modifier.clickable(onClick = onPlayNext)
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_queue)) },
                leadingContent = { Icon(Icons.Default.Queue, null) },
                modifier = Modifier.clickable(onClick = onAddToQueue)
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_playlist)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                modifier = Modifier.clickable(onClick = onAddToPlaylist)
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
    val accentTokens = accent()
    val sortInteractionSource = remember { MutableInteractionSource() }
    val selectInteractionSource = remember { MutableInteractionSource() }
    val isSortPressed by sortInteractionSource.collectIsPressedAsState()
    val isSelectPressed by selectInteractionSource.collectIsPressedAsState()

    val pressedContainerColor = accentTokens.softBg.copy(alpha = 0.30f)
    val actionIconColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = androidx.compose.ui.res.pluralStringResource(
                    com.sukoon.music.R.plurals.library_albums_count,
                    albumCount,
                    albumCount
                ),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSortPressed) pressedContainerColor else Color.Transparent)
                        .clickable(
                            interactionSource = sortInteractionSource,
                            indication = null,
                            onClick = onSortClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_common_sort),
                        tint = if (isSortPressed) accentTokens.active else actionIconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelectPressed) pressedContainerColor else Color.Transparent)
                        .clickable(
                            interactionSource = selectInteractionSource,
                            indication = null,
                            onClick = onSelectionClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_select),
                        tint = if (isSelectPressed) accentTokens.active else actionIconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
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

