package com.sukoon.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.SubcomposeAsyncImage
import androidx.compose.material.icons.automirrored.filled.Sort
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.AnimatedEqualizer
import com.sukoon.music.ui.components.BannerAdView
import com.sukoon.music.ui.components.SongContextMenu
import com.sukoon.music.ui.components.SongMenuHandler
import com.sukoon.music.ui.components.rememberSongMenuHandler
import com.sukoon.music.ui.components.MiniPlayer
import com.sukoon.music.ui.components.SongInfoDialog
import com.sukoon.music.ui.components.DeleteConfirmationDialog
import com.sukoon.music.ui.components.AddToPlaylistDialog
import com.sukoon.music.ui.components.MultiSelectActionBottomBar
import com.sukoon.music.ui.components.PlaceholderAlbumArt
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.theme.*
// SukoonOrange removed - using MaterialTheme.colorScheme.primary instead
import com.sukoon.music.ui.viewmodel.AlbumDetailViewModel
import com.sukoon.music.ui.components.rememberShareHandler
import com.sukoon.music.ui.theme.*

/**
 * Album Detail Screen - Shows songs in a specific album.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    onBackClick: () -> Unit,
    navController: NavController,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToArtist: (Long) -> Unit = {},
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    // Load album data
    LaunchedEffect(albumId) {
        viewModel.loadAlbum(albumId)
    }

    val album by viewModel.album.collectAsStateWithLifecycle()
    val songs by viewModel.albumSongs.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedSongIds by viewModel.selectedSongIds.collectAsStateWithLifecycle()

    var showSortDialog by remember { mutableStateOf(false) }
    var songForInfo by remember { mutableStateOf<Song?>(null) }
    var songPendingDeletion by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var showPlaylistDialogForSelection by remember { mutableStateOf(false) }
    var songsPendingPlaylistAdd by remember { mutableStateOf<List<Song>>(emptyList()) }
    var songsPendingDeletion by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val playlistViewModel: com.sukoon.music.ui.viewmodel.PlaylistViewModel = hiltViewModel()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()

    // Delete result launcher
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_song_deleted_successfully), Toast.LENGTH_SHORT).show()
            viewModel.loadAlbum(albumId)
        } else {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_delete_cancelled), Toast.LENGTH_SHORT).show()
        }
        songPendingDeletion = null
    }

    // Delete launcher for multi-select
    val multiSelectDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_songs_deleted_successfully), Toast.LENGTH_SHORT).show()
            viewModel.loadAlbum(albumId)
            songsPendingDeletion = false
        } else {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_delete_cancelled), Toast.LENGTH_SHORT).show()
            songsPendingDeletion = false
        }
    }

    // Share handler
    val shareHandler = rememberShareHandler()

    // Create menu handler for song context menu
    val menuHandler = rememberSongMenuHandler(
        playbackRepository = viewModel.playbackRepository,
        onNavigateToAlbum = { /* Already on album detail, skip navigation */ },
        onNavigateToArtist = onNavigateToArtist,
        onShowPlaylistSelector = { song ->
            songToAddToPlaylist = song
            showAddToPlaylistDialog = true
        },
        onShowSongInfo = { song ->
            songForInfo = song
        },
        onShowDeleteConfirmation = { song ->
            songPendingDeletion = song
        },
        onToggleLike = { songId, isLiked ->
            viewModel.toggleLike(songId, isLiked)
        },
        onShare = shareHandler
    )

    // Playlist Dialog for selected songs
    if (showPlaylistDialogForSelection) {
        AddToPlaylistDialog(
            playlists = playlists,
            onPlaylistSelected = { playlistId ->
                songsPendingPlaylistAdd.forEach { song ->
                    playlistViewModel.addSongToPlaylist(playlistId, song.id)
                }
                showPlaylistDialogForSelection = false
                Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_songs_added_to_playlist), Toast.LENGTH_SHORT).show()
                viewModel.toggleSelectionMode(false)
            },
            onDismiss = { showPlaylistDialogForSelection = false }
        )
    }

    // Delete confirmation dialog for selected songs
    if (songsPendingDeletion) {
        AlertDialog(
            onDismissRequest = { songsPendingDeletion = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_delete_selected_songs_title, selectedSongIds.size))
            },
            text = {
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_delete_songs_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedSongsWithResult(songs, context) { deleteResult ->
                            when (deleteResult) {
                                is DeleteHelper.DeleteResult.RequiresPermission -> {
                                    multiSelectDeleteLauncher.launch(
                                        IntentSenderRequest.Builder(deleteResult.intentSender).build()
                                    )
                                }
                                is DeleteHelper.DeleteResult.Success -> {
                                    Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_songs_deleted_successfully), Toast.LENGTH_SHORT).show()
                                    songsPendingDeletion = false
                                }
                                is DeleteHelper.DeleteResult.Error -> {
                                    Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_error_with_message, deleteResult.message), Toast.LENGTH_SHORT).show()
                                    songsPendingDeletion = false
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
                TextButton(onClick = { songsPendingDeletion = false }) {
                    Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_selected_count, selectedSongIds.size)) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleSelectionMode(false) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_exit_selection)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            } else {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* TODO: Search in album */ }) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_search))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        bottomBar = {
            if (isSelectionMode && selectedSongIds.isNotEmpty()) {
                MultiSelectActionBottomBar(
                    onPlay = { viewModel.playSelectedSongs(songs) },
                    onAddToPlaylist = {
                        songsPendingPlaylistAdd = songs.filter { selectedSongIds.contains(it.id) }
                        showPlaylistDialogForSelection = true
                    },
                    onDelete = { songsPendingDeletion = true },
                    onPlayNext = { viewModel.playSelectedSongsNext(songs) },
                    onAddToQueue = { viewModel.addSelectedSongsToQueueBatch(songs) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (album != null) {
            AlbumDetailContent(
                album = album!!,
                songs = songs,
                currentSongId = playbackState.currentSong?.id,
                isPlayingGlobally = playbackState.isPlaying,
                isSelectionMode = isSelectionMode,
                selectedSongIds = selectedSongIds,
                menuHandler = menuHandler,
                onPlayAll = { viewModel.playAlbum(songs) },
                onShuffle = { viewModel.shuffleAlbum(songs) },
                onSongClick = { song ->
                    if (isSelectionMode) {
                        viewModel.toggleSongSelection(song.id)
                    } else {
                        if (playbackState.currentSong?.id != song.id) {
                            viewModel.playSong(song, songs)
                        } else {
                            onNavigateToNowPlaying()
                        }
                    }
                },
                onToggleLike = { songId, isLiked ->
                    viewModel.toggleLike(songId, isLiked)
                },
                onSortClick = { showSortDialog = true },
                onSelectionClick = { viewModel.toggleSelectionMode(true) },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }

    // Sort Dialog
    if (showSortDialog) {
        AlbumSongSortDialog(
            currentMode = sortMode,
            onDismiss = { showSortDialog = false },
            onModeSelect = { mode ->
                viewModel.setSortMode(mode)
                showSortDialog = false
            }
        )
    }

    // Song Info Dialog
    songForInfo?.let { song ->
        SongInfoDialog(
            song = song,
            onDismiss = { songForInfo = null }
        )
    }

    // Add to Playlist Dialog
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

    // Delete confirmation dialog
    songPendingDeletion?.let { song ->
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
                        Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_song_deleted_successfully), Toast.LENGTH_SHORT).show()
                        viewModel.loadAlbum(albumId)
                        songPendingDeletion = null
                    }
                    is DeleteHelper.DeleteResult.Error -> {
                        Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_error_with_message, result.message), Toast.LENGTH_SHORT).show()
                        songPendingDeletion = null
                    }
                }
            },
            onDismiss = { songPendingDeletion = null }
        )
    }
}

@Composable
private fun AlbumDetailContent(
    album: Album,
    songs: List<Song>,
    currentSongId: Long?,
    isPlayingGlobally: Boolean,
    isSelectionMode: Boolean,
    selectedSongIds: Set<Long>,
    menuHandler: SongMenuHandler,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onSongClick: (Song) -> Unit,
    onToggleLike: (Long, Boolean) -> Unit,
    onSortClick: () -> Unit,
    onSelectionClick: () -> Unit,
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
        // Album Header
        item {
            AlbumHeader(
                album = album,
                onPlayAll = onPlayAll,
                onShuffle = onShuffle
            )
        }

        // Stats Header (e.g. "1 song") - hide in selection mode
        if (!isSelectionMode) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = androidx.compose.ui.res.pluralStringResource(
                            com.sukoon.music.R.plurals.common_song_count,
                            album.songCount,
                            album.songCount
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        IconButton(onClick = onSortClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_common_sort)
                            )
                        }
                        IconButton(onClick = onSelectionClick) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_select_songs)
                            )
                        }
                    }
                }
            }
        }

        // Play/Shuffle Buttons Row - hide in selection mode
        if (!isSelectionMode) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onShuffle,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_shuffle), color = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = onPlayAll,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Song List
        if (songs.isEmpty()) {
            item {
                EmptyAlbumState()
            }
        } else {
            itemsIndexed(
                items = songs,
                key = { _, song -> song.id }
            ) { index, song ->
                AlbumSongItemRow(
                    song = song,
                    isCurrentlyPlaying = song.id == currentSongId,
                    isPlayingGlobally = isPlayingGlobally,
                    isSelectionMode = isSelectionMode,
                    isSelected = selectedSongIds.contains(song.id),
                    menuHandler = menuHandler,
                    onClick = { onSongClick(song) },
                    onToggleLike = { onToggleLike(song.id, song.isLiked) }
                )
            }
        }
    }
}

@Composable
private fun AlbumHeader(
    album: Album,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Album Cover
        Card(
            modifier = Modifier.size(220.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (album.albumArtUri != null) {
                    SubcomposeAsyncImage(
                        model = album.albumArtUri,
                        contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_album_art),
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
                } else {
                    PlaceholderAlbumArt.Placeholder(
                        seed = PlaceholderAlbumArt.generateSeed(
                            albumName = album.title,
                            artistName = album.artist,
                            albumId = album.id
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Album Title
        Text(
            text = album.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Artist
        Text(
            text = album.artist,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun AlbumSongItemRow(
    song: Song,
    isCurrentlyPlaying: Boolean,
    isPlayingGlobally: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    menuHandler: SongMenuHandler,
    onClick: () -> Unit,
    onToggleLike: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dash, Playing Indicator, or Checkbox
        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = if (isSelected) {
                    androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_checked)
                } else {
                    androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_unchecked)
                },
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onClick() },
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        } else if (isCurrentlyPlaying) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = "-",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Song Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isCurrentlyPlaying) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AnimatedEqualizer(isAnimating = isPlayingGlobally, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                text = song.artist,
                style = MaterialTheme.typography.listItemSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // More options button - hide in selection mode
        if (!isSelectionMode) {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.now_playing_more_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showMenu && !isSelectionMode) {
        SongContextMenu(
            song = song,
            menuHandler = menuHandler,
            onDismiss = { showMenu = false }
        )
    }
}

@Composable
private fun EmptyAlbumState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Album,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_album_detail_no_songs),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun AlbumSongSortDialog(
    currentMode: com.sukoon.music.ui.viewmodel.AlbumSongSortMode,
    onDismiss: () -> Unit,
    onModeSelect: (com.sukoon.music.ui.viewmodel.AlbumSongSortMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_sort_by)) },
        text = {
            Column {
                com.sukoon.music.ui.viewmodel.AlbumSongSortMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelect(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when (mode) {
                                com.sukoon.music.ui.viewmodel.AlbumSongSortMode.TRACK_NUMBER -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_album_detail_sort_track_number)
                                com.sukoon.music.ui.viewmodel.AlbumSongSortMode.TITLE -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_option_title_az)
                                com.sukoon.music.ui.viewmodel.AlbumSongSortMode.ARTIST -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_option_artist_az)
                                com.sukoon.music.ui.viewmodel.AlbumSongSortMode.DURATION -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_option_duration)
                            }
                        )
                        if (mode == currentMode) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
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
private fun SongSelectionBottomBar(
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit
) {
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
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
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onAddToQueue)
            ) {
                Icon(
                    imageVector = Icons.Default.Queue,
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_queue),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_queue),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onDelete)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_delete),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_delete),
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
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_more),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_more),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AlbumDetailScreenPreview() {
    SukoonMusicPlayerTheme(theme = AppTheme.DARK) {
        AlbumDetailScreen(
            albumId = 1,
            onBackClick = {},
            navController = rememberNavController(),
            onNavigateToNowPlaying = {}
        )
    }
}




