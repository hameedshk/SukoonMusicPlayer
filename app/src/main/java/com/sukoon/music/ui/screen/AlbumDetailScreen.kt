package com.sukoon.music.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.R
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.AddToPlaylistDialog
import com.sukoon.music.ui.components.AnimatedEqualizer
import com.sukoon.music.ui.components.DeleteConfirmationDialog
import com.sukoon.music.ui.components.MultiSelectActionBottomBar
import com.sukoon.music.ui.components.PlaceholderAlbumArt
import com.sukoon.music.ui.components.SimpleBannerAd
import com.sukoon.music.ui.components.SongContextMenu
import com.sukoon.music.ui.components.SongInfoDialog
import com.sukoon.music.ui.components.SongMenuHandler
import com.sukoon.music.ui.components.rememberShareHandler
import com.sukoon.music.ui.components.rememberSongMenuHandler
import com.sukoon.music.ui.navigation.PreferencesManagerEntryPoint
import com.sukoon.music.ui.navigation.RemoteConfigManagerEntryPoint
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.theme.ContentBottomPadding
import com.sukoon.music.ui.theme.ContentTopPadding
import com.sukoon.music.ui.util.AlbumSourceType
import com.sukoon.music.ui.util.albumArtContentDescription
import com.sukoon.music.ui.util.buildAlbumHeaderModel
import com.sukoon.music.ui.util.rememberAlbumPalette
import com.sukoon.music.ui.viewmodel.AlbumDetailViewModel
import dagger.hilt.android.EntryPointAccessors

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
    val tag = "AlbumDetailScreen"
    val playlistViewModel: com.sukoon.music.ui.viewmodel.PlaylistViewModel = hiltViewModel()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()

    val preferencesManager = remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context,
                PreferencesManagerEntryPoint::class.java
            ).preferencesManager()
        }.getOrElse { error ->
            Log.w(tag, "Failed to resolve PreferencesManager entry point", error)
            null
        }
    }

    val remoteConfigManager = remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context,
                RemoteConfigManagerEntryPoint::class.java
            ).remoteConfigManager()
        }.getOrElse { error ->
            Log.w(tag, "Failed to resolve RemoteConfigManager entry point", error)
            null
        }
    }

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
                    title = {
                        Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_album_detail_header_title))
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
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        bottomBar = {
            when {
                isSelectionMode && selectedSongIds.isNotEmpty() -> {
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
                preferencesManager != null && remoteConfigManager != null -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 1.dp
                    ) {
                        SimpleBannerAd(
                            adMobManager = viewModel.adMobManager,
                            preferencesManager = preferencesManager,
                            remoteConfigManager = remoteConfigManager
                        )
                    }
                }
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
    onSortClick: () -> Unit,
    onSelectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(
            top = ContentTopPadding,
            bottom = 8.dp + ContentBottomPadding,
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
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.home_tab_songs),
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
                    index = index + 1,
                    song = song,
                    isCurrentlyPlaying = song.id == currentSongId,
                    isPlayingGlobally = isPlayingGlobally,
                    isSelectionMode = isSelectionMode,
                    isSelected = selectedSongIds.contains(song.id),
                    menuHandler = menuHandler,
                    onClick = { onSongClick(song) }
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
    val palette = rememberAlbumPalette(album.albumArtUri)
    val songCountLabel = androidx.compose.ui.res.pluralStringResource(
        R.plurals.common_song_count,
        album.songCount,
        album.songCount
    )
    val headerModel = buildAlbumHeaderModel(
        album = album,
        songCountLabel = songCountLabel,
        unknownAlbumLabel = androidx.compose.ui.res.stringResource(R.string.library_album_detail_unknown_album),
        unknownArtistLabel = androidx.compose.ui.res.stringResource(R.string.now_playing_unknown_artist)
    )

    val sourceLabel = when (headerModel.sourceType) {
        AlbumSourceType.TAGGED_ALBUM -> androidx.compose.ui.res.stringResource(R.string.library_album_detail_source_album)
        AlbumSourceType.FOLDER_INFERRED -> androidx.compose.ui.res.stringResource(R.string.library_album_detail_source_folder)
        AlbumSourceType.ARTIST_SCOPED -> androidx.compose.ui.res.stringResource(R.string.library_album_detail_source_artist)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.size(216.dp),
            shape = RoundedCornerShape(20.dp),
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
                        contentDescription = albumArtContentDescription(
                            albumTitle = album.title,
                            artistName = album.artist
                        ),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
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

        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = headerModel.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = headerModel.metadataLine,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            color = palette.mutedLight.copy(alpha = 0.38f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = sourceLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onShuffle,
                enabled = album.songCount > 0,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(androidx.compose.ui.res.stringResource(R.string.common_shuffle))
            }

            Button(
                onClick = onPlayAll,
                enabled = album.songCount > 0,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.vibrant,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.common_play_all),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AlbumSongItemRow(
    index: Int,
    song: Song,
    isCurrentlyPlaying: Boolean,
    isPlayingGlobally: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    menuHandler: SongMenuHandler,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val unknownArtistLabel = androidx.compose.ui.res.stringResource(R.string.now_playing_unknown_artist)
    val trimmedArtist = song.artist.trim()
    val displayArtist = when {
        trimmedArtist.isEmpty() -> unknownArtistLabel
        trimmedArtist.equals(song.title.trim(), ignoreCase = true) -> unknownArtistLabel
        trimmedArtist.equals("<unknown>", ignoreCase = true) -> unknownArtistLabel
        else -> trimmedArtist
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(28.dp),
            contentAlignment = Alignment.CenterStart
        ) {
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
                text = index.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        }

        Spacer(modifier = Modifier.width(12.dp))

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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = displayArtist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = song.durationFormatted(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, end = 4.dp)
        )

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

