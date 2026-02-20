package com.sukoon.music.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.automirrored.filled.Sort
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Genre
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.*
import com.sukoon.music.ui.viewmodel.GenreDetailViewModel
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.ButtonDefaults
import com.sukoon.music.ui.theme.*

/**
 * Genre Detail Screen - Shows songs in a specific genre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreDetailScreen(
    genreId: Long,
    onBackClick: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToArtist: (Long) -> Unit = {},
    viewModel: GenreDetailViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    // Load genre data
    LaunchedEffect(genreId) {
        viewModel.loadGenre(genreId)
    }

    val genre by viewModel.genre.collectAsStateWithLifecycle()
    val songs by viewModel.genreSongs.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedSongIds by viewModel.selectedSongIds.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()

    var showSongContextSheet by remember { mutableStateOf<Song?>(null) }
    var songPendingDeletion by remember { mutableStateOf<Song?>(null) }
    var showSongInfo by remember { mutableStateOf<Song?>(null) }
    var showPlaylistSelector by remember { mutableStateOf<Song?>(null) }
    var showError by remember { mutableStateOf(false) }
    var showPlaylistDialogForSelection by remember { mutableStateOf(false) }
    var songsPendingPlaylistAdd by remember { mutableStateOf<List<Song>>(emptyList()) }
    var songsPendingDeletion by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Share handler
    val shareHandler = rememberShareHandler()

    // Delete result launcher
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_song_deleted_successfully), Toast.LENGTH_SHORT).show()
            viewModel.loadGenre(genreId)
        }
        songPendingDeletion = null
    }

    // Delete launcher for multi-select
    val multiSelectDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_songs_deleted_successfully), Toast.LENGTH_SHORT).show()
            viewModel.loadGenre(genreId)
            songsPendingDeletion = false
        } else {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_delete_cancelled), Toast.LENGTH_SHORT).show()
            songsPendingDeletion = false
        }
    }

    // Create menu handler for song context menu
    val menuHandler = rememberSongMenuHandler(
        playbackRepository = viewModel.playbackRepository,
        onNavigateToAlbum = onNavigateToAlbum,
        onNavigateToArtist = onNavigateToArtist,
        onShowSongInfo = { song -> showSongInfo = song; showSongContextSheet = null },
        onShowDeleteConfirmation = { song -> songPendingDeletion = song; showSongContextSheet = null },
        onShowPlaylistSelector = { song -> showPlaylistSelector = song; showSongContextSheet = null },
        onToggleLike = { songId, isLiked -> viewModel.toggleLike(songId, isLiked) },
        onShare = shareHandler
    )

    // Error state handling - show error if genre not found after timeout
    LaunchedEffect(genreId, genre) {
        if (genre == null) {
            kotlinx.coroutines.delay(3000) // 3 second timeout
            if (genre == null) {
                showError = true
            }
        } else {
            showError = false
        }
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
                        viewModel.deleteSelectedSongsWithResult(songs) { deleteResult ->
                            when (deleteResult) {
                                is DeleteHelper.DeleteResult.RequiresPermission -> {
                                    multiSelectDeleteLauncher.launch(
                                        IntentSenderRequest.Builder(deleteResult.intentSender).build()
                                    )
                                }
                                is DeleteHelper.DeleteResult.Success -> {
                                    Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_songs_deleted_successfully), Toast.LENGTH_SHORT).show()
                                    viewModel.loadGenre(genreId)
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
                    title = {
                        Text(
                            genre?.name
                                ?: androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_genre_title)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_back)
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                showError -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_genre_not_found_title),
                            style = MaterialTheme.typography.emptyStateTitle
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_genre_not_found_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(onClick = onBackClick) {
                            Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_go_back))
                        }
                    }
                }
                genre == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    GenreDetailContent(
                        genre = genre!!,
                        songs = songs,
                        currentSongId = playbackState.currentSong?.id,
                        isPlaying = playbackState.isPlaying,
                        onPlayAll = { viewModel.playGenre(songs) },
                        onShuffle = { viewModel.shuffleGenre(songs) },
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
                        onSongLongClick = { song ->
                            if (!isSelectionMode) {
                                viewModel.toggleSelectionMode(true)
                                viewModel.toggleSongSelection(song.id)
                            } else {
                                showSongContextSheet = song
                            }
                        },
                        onLikeClick = { song -> viewModel.toggleLike(song.id, song.isLiked) },
                        isSelectionMode = isSelectionMode,
                        selectedSongIds = selectedSongIds,
                        onSelectionChange = { viewModel.toggleSongSelection(it) },
                        sortMode = sortMode,
                        onSortClick = { showSortDialog = true },
                        onSelectionModeClick = { viewModel.toggleSelectionMode(true) },
                        menuHandler = menuHandler
                    )
                }
            }

            // Song Context Menu
            showSongContextSheet?.let { selectedSong ->
                // Get the latest song state from the songs list to reflect like status changes
                val currentSong = songs.find { it.id == selectedSong.id } ?: selectedSong
                SongContextMenu(
                    song = currentSong,
                    menuHandler = menuHandler,
                    onDismiss = { showSongContextSheet = null }
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
                                viewModel.loadGenre(genreId)
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

            // Sort dialog
            if (showSortDialog) {
                GenreSongSortDialog(
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
}

@Composable
private fun GenreDetailContent(
    genre: Genre,
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onLikeClick: (Song) -> Unit,
    isSelectionMode: Boolean = false,
    selectedSongIds: Set<Long> = emptySet(),
    onSelectionChange: (Long) -> Unit = {},
    sortMode: com.sukoon.music.ui.viewmodel.GenreSongSortMode = com.sukoon.music.ui.viewmodel.GenreSongSortMode.TITLE,
    onSortClick: () -> Unit = {},
    onSelectionModeClick: () -> Unit = {},
    menuHandler: SongMenuHandler
) {
    val albumCount = remember(songs) { songs.map { it.album }.distinct().size }

    val sortedSongs = when (sortMode) {
        com.sukoon.music.ui.viewmodel.GenreSongSortMode.TITLE -> songs.sortedBy { it.title.lowercase() }
        com.sukoon.music.ui.viewmodel.GenreSongSortMode.ARTIST -> songs.sortedBy { it.artist.lowercase() }
        com.sukoon.music.ui.viewmodel.GenreSongSortMode.ALBUM -> songs.sortedBy { it.album.lowercase() }
        com.sukoon.music.ui.viewmodel.GenreSongSortMode.DURATION -> songs.sortedByDescending { it.duration }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GenreIcon(
                    genreName = genre.name,
                    modifier = Modifier.size(220.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = genre.name,
                    style = MaterialTheme.typography.screenHeader,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = androidx.compose.ui.res.pluralStringResource(
                            com.sukoon.music.R.plurals.common_song_count,
                            genre.songCount,
                            genre.songCount
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = androidx.compose.ui.res.pluralStringResource(
                            com.sukoon.music.R.plurals.library_screens_b_album_count,
                            albumCount,
                            albumCount
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onShuffle,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_shuffle))
                    }

                    Button(
                        onClick = onPlayAll,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play_all))
                    }
                }
            }
        }

        // Sort header with count and buttons
        if (!isSelectionMode && sortedSongs.isNotEmpty()) {
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

        // Songs List
        if (sortedSongs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_no_songs_in_genre),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            itemsIndexed(
                items = sortedSongs,
                key = { _, song -> song.id }
            ) { _, song ->
                val isCurrentSong = song.id == currentSongId

                GenreSongItemRow(
                    song = song,
                    isCurrentlyPlaying = isCurrentSong,
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
}

@Composable
private fun GenreSongItemRow(
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
                text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_artist_album_pair, song.artist, song.album),
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
private fun GenreSongSortDialog(
    currentMode: com.sukoon.music.ui.viewmodel.GenreSongSortMode,
    onDismiss: () -> Unit,
    onModeSelect: (com.sukoon.music.ui.viewmodel.GenreSongSortMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_sort_by)) },
        text = {
            Column {
                com.sukoon.music.ui.viewmodel.GenreSongSortMode.values().forEach { mode ->
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
                                com.sukoon.music.ui.viewmodel.GenreSongSortMode.TITLE -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_option_title_az)
                                com.sukoon.music.ui.viewmodel.GenreSongSortMode.ARTIST -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_option_artist_az)
                                com.sukoon.music.ui.viewmodel.GenreSongSortMode.ALBUM -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_option_album_az)
                                com.sukoon.music.ui.viewmodel.GenreSongSortMode.DURATION -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_option_duration)
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




