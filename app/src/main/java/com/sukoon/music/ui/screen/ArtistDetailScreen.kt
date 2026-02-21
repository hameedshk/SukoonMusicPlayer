package com.sukoon.music.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.material.icons.automirrored.filled.Sort
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.*
import com.sukoon.music.ui.components.PlaceholderAlbumArt
import com.sukoon.music.ui.viewmodel.ArtistDetailViewModel
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import com.sukoon.music.data.mediastore.DeleteHelper
import androidx.compose.material3.ButtonDefaults
import com.sukoon.music.ui.theme.*


/**
 * Artist Detail Screen - Shows artist info with Songs and Albums tabs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistId: Long,
    onBackClick: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToNowPlaying: () -> Unit = {},
    viewModel: ArtistDetailViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val artist by viewModel.artist.collectAsStateWithLifecycle()
    val artistSongs by viewModel.artistSongs.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedSongIds by viewModel.selectedSongIds.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()

    var songToDelete by rememberSaveable { mutableStateOf<Song?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var showInfoForSong by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var showPlaylistDialogForSelection by remember { mutableStateOf(false) }
    var songsPendingPlaylistAdd by remember { mutableStateOf<List<Song>>(emptyList()) }
    var songsPendingDeletion by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val shareHandler = rememberShareHandler()

    val menuHandler = rememberSongMenuHandler(
        playbackRepository = viewModel.playbackRepository,
        onNavigateToAlbum = onNavigateToAlbum,
        onShowDeleteConfirmation = { song ->
            if (!isDeleting) songToDelete = song
        },
        onShowSongInfo = { song -> showInfoForSong = song },
        onShowPlaylistSelector = { song ->
            songToAddToPlaylist = song
            showAddToPlaylistDialog = true
        },
        onToggleLike = { id, isLiked -> viewModel.toggleLike(id, isLiked) },
        onShare = shareHandler
    )

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_song_deleted_successfully), Toast.LENGTH_SHORT).show()
            viewModel.loadArtist(artistId)
        } else {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_delete_cancelled), Toast.LENGTH_SHORT).show()
        }
        songToDelete = null
        isDeleting = false
    }

    // Delete launcher for multi-select
    val multiSelectDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_songs_deleted_successfully), Toast.LENGTH_SHORT).show()
            viewModel.loadArtist(artistId)
            songsPendingDeletion = false
        } else {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_delete_cancelled), Toast.LENGTH_SHORT).show()
            songsPendingDeletion = false
        }
    }

    // Load artist when screen opens
    LaunchedEffect(artistId) {
        viewModel.loadArtist(artistId)
    }

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
                        viewModel.deleteSelectedSongsWithResult(artistSongs, context) { deleteResult ->
                            when (deleteResult) {
                                is DeleteHelper.DeleteResult.RequiresPermission -> {
                                    multiSelectDeleteLauncher.launch(
                                        IntentSenderRequest.Builder(deleteResult.intentSender).build()
                                    )
                                    songsPendingDeletion = false
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
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            } else {
                TopAppBar(
                    title = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_artist_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (isSelectionMode && selectedSongIds.isNotEmpty()) {
                MultiSelectActionBottomBar(
                    onPlay = { viewModel.playSelectedSongs(artistSongs) },
                    onAddToPlaylist = {
                        songsPendingPlaylistAdd = artistSongs.filter { selectedSongIds.contains(it.id) }
                        showPlaylistDialogForSelection = true
                    },
                    onDelete = { songsPendingDeletion = true },
                    onPlayNext = { viewModel.playSelectedSongsNext(artistSongs) },
                    onAddToQueue = { viewModel.addSelectedSongsToQueueBatch(artistSongs) }
                )
            }
        }
    ) { paddingValues ->
        if (artist == null) {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(
                        top = ContentTopPadding,
                        bottom = ContentBottomPadding
                    )
            ) {
                // Artist Header
                ArtistHeader(
                    artist = artist!!,
                    onPlayClick = { viewModel.playArtist(artistSongs) },
                    onShuffleClick = { viewModel.shuffleArtist(artistSongs) }
                )

                // Songs List
                if (artistSongs.isEmpty()) {
                    EmptyStateMessage(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_no_songs_found))
                } else {
                    SongsList(
                        songs = artistSongs,
                        currentSongId = playbackState.currentSong?.id,
                        isPlaying = playbackState.isPlaying,
                        menuHandler = menuHandler,
                        onSongClick = { song ->
                            if (isSelectionMode) {
                                viewModel.toggleSongSelection(song.id)
                            } else {
                                if (playbackState.currentSong?.id != song.id) {
                                    viewModel.playSong(song, artistSongs)
                                } else {
                                    onNavigateToNowPlaying()
                                }
                            }
                        },
                        onLikeClick = { song ->
                            viewModel.toggleLike(song.id, song.isLiked)
                        },
                        isSelectionMode = isSelectionMode,
                        selectedSongIds = selectedSongIds,
                        sortMode = sortMode,
                        onSortClick = { showSortDialog = true },
                        onSelectionModeClick = { viewModel.toggleSelectionMode(true) }
                    )
                }
            }
        }

        // Delete confirmation dialog
        songToDelete?.let { song ->
            DeleteConfirmationDialog(
                song = song,
                onConfirm = {
                    if (!isDeleting) {
                        isDeleting = true

                        when (val result = DeleteHelper.deleteSongs(context, listOf(song))) {
                            is DeleteHelper.DeleteResult.RequiresPermission -> {
                                deleteLauncher.launch(
                                    IntentSenderRequest.Builder(result.intentSender).build()
                                )
                                songToDelete = null
                                isDeleting = false
                            }
                            is DeleteHelper.DeleteResult.Success -> {
                                Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_song_deleted_successfully), Toast.LENGTH_SHORT).show()
                                songToDelete = null
                                isDeleting = false
                            }
                            is DeleteHelper.DeleteResult.Error -> {
                                Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_error_with_message, result.message), Toast.LENGTH_SHORT).show()
                                songToDelete = null
                                isDeleting = false
                            }
                        }
                    }
                },
                onDismiss = { songToDelete = null }
            )
        }

        // Song info dialog
        showInfoForSong?.let { song ->
            SongInfoDialog(
                song = song,
                onDismiss = { showInfoForSong = null }
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

        // Sort dialog
        if (showSortDialog) {
            ArtistSongSortDialog(
                currentMode = sortMode,
                onDismiss = { showSortDialog = false },
                onModeSelect = { mode ->
                    viewModel.setSortMode(mode)
                    showSortDialog = false
                }
            )
        }
    }
}

@Composable
private fun ArtistHeader(
    artist: Artist,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circular Artwork
        if (artist.artworkUri != null) {
            SubcomposeAsyncImage(
                model = artist.artworkUri,
                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_artist_artwork),
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                },
                error = {
                    DefaultArtistIcon()
                }
            )
        } else {
            DefaultArtistIcon()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Artist Name
        Text(
            text = artist.name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Stats
        Text(
            text = androidx.compose.ui.res.stringResource(
                com.sukoon.music.R.string.library_screens_b_artist_stats,
                artist.albumCount,
                artist.songCount
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Play Button
            Button(
                onClick = onPlayClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play))
            }

            // Shuffle Button
            OutlinedButton(
                onClick = onShuffleClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_shuffle))
            }
        }
    }
}

@Composable
private fun DefaultArtistIcon() {
    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun SongsList(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    menuHandler: SongMenuHandler,
    onSongClick: (Song) -> Unit,
    onLikeClick: (Song) -> Unit,
    isSelectionMode: Boolean = false,
    selectedSongIds: Set<Long> = emptySet(),
    sortMode: com.sukoon.music.ui.viewmodel.ArtistSongSortMode = com.sukoon.music.ui.viewmodel.ArtistSongSortMode.TITLE,
    onSortClick: () -> Unit = {},
    onSelectionModeClick: () -> Unit = {}
) {
    val sortedSongs = when (sortMode) {
        com.sukoon.music.ui.viewmodel.ArtistSongSortMode.TITLE -> songs.sortedBy { it.title.lowercase() }
        com.sukoon.music.ui.viewmodel.ArtistSongSortMode.ARTIST -> songs.sortedBy { it.artist.lowercase() }
        com.sukoon.music.ui.viewmodel.ArtistSongSortMode.ALBUM -> songs.sortedBy { it.album.lowercase() }
        com.sukoon.music.ui.viewmodel.ArtistSongSortMode.DURATION -> songs.sortedByDescending { it.duration }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Sort header with count and buttons
        if (!isSelectionMode) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = androidx.compose.ui.res.pluralStringResource(
                            com.sukoon.music.R.plurals.library_screens_b_song_count,
                            sortedSongs.size,
                            sortedSongs.size
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        IconButton(onClick = onSortClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort)
                            )
                        }
                        IconButton(onClick = onSelectionModeClick) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_select_songs)
                            )
                        }
                    }
                }
            }
        }

        items(
            items = sortedSongs,
            key = { song -> song.id }
        ) { song ->
            ArtistSongItemRow(
                song = song,
                isCurrentlyPlaying = song.id == currentSongId,
                isPlayingGlobally = isPlaying,
                isSelectionMode = isSelectionMode,
                isSelected = selectedSongIds.contains(song.id),
                menuHandler = menuHandler,
                onClick = { onSongClick(song) },
                onToggleLike = { onLikeClick(song) }
            )
        }
    }
}

@Composable
private fun ArtistSongItemRow(
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
        // Checkbox, Playing Indicator, or Dash
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
                text = song.album,
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
private fun AlbumsGrid(
    albums: List<Album>,
    onAlbumClick: (Long) -> Unit,
    onPlayAlbum: (Long) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = albums,
            key = { album -> album.id }
        ) { album ->
            AlbumCard(
                album = album,
                onClick = { onAlbumClick(album.id) },
                onPlayClick = { onPlayAlbum(album.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),   // Square card = bigger art
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // BIG Album Art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.6f),   // 👈 Give MORE space to image
                contentAlignment = Alignment.Center
            ) {
                if (album.albumArtUri != null) {
                    SubcomposeAsyncImage(
                        model = album.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
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

            // Compact Text (less space → bigger art)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.compactCardTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${album.songCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.MusicOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ArtistSongSortDialog(
    currentMode: com.sukoon.music.ui.viewmodel.ArtistSongSortMode,
    onDismiss: () -> Unit,
    onModeSelect: (com.sukoon.music.ui.viewmodel.ArtistSongSortMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_sort_by)) },
        text = {
            Column {
                com.sukoon.music.ui.viewmodel.ArtistSongSortMode.values().forEach { mode ->
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
                                com.sukoon.music.ui.viewmodel.ArtistSongSortMode.TITLE -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_option_title_az)
                                com.sukoon.music.ui.viewmodel.ArtistSongSortMode.ARTIST -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_option_artist_az)
                                com.sukoon.music.ui.viewmodel.ArtistSongSortMode.ALBUM -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_option_album_az)
                                com.sukoon.music.ui.viewmodel.ArtistSongSortMode.DURATION -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_option_duration)
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




