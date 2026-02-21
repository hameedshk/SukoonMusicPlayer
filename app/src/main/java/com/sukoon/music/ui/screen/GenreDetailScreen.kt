package com.sukoon.music.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukoon.music.R
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Genre
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.AddToPlaylistDialog
import com.sukoon.music.ui.components.AnimatedEqualizer
import com.sukoon.music.ui.components.DeleteConfirmationDialog
import com.sukoon.music.ui.components.GenreIcon
import com.sukoon.music.ui.components.MultiSelectActionBottomBar
import com.sukoon.music.ui.components.SimpleBannerAd
import com.sukoon.music.ui.components.SongContextMenu
import com.sukoon.music.ui.components.SongInfoDialog
import com.sukoon.music.ui.components.SongMenuHandler
import com.sukoon.music.ui.components.rememberShareHandler
import com.sukoon.music.ui.components.rememberSongMenuHandler
import com.sukoon.music.ui.navigation.PreferencesManagerEntryPoint
import com.sukoon.music.ui.navigation.RemoteConfigManagerEntryPoint
import com.sukoon.music.ui.theme.ContentBottomPadding
import com.sukoon.music.ui.theme.ContentTopPadding
import com.sukoon.music.ui.viewmodel.GenreDetailViewModel
import com.sukoon.music.ui.viewmodel.GenreSongSortMode
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

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

    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var showSongInfo by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var showPlaylistDialogForSelection by remember { mutableStateOf(false) }
    var songsPendingPlaylistAdd by remember { mutableStateOf<List<Song>>(emptyList()) }
    var songsPendingDeletion by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val tag = "GenreDetailScreen"

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

    val shareHandler = rememberShareHandler()
    val genreScope = rememberCoroutineScope()
    val menuHandler = rememberSongMenuHandler(
        playbackRepository = viewModel.playbackRepository,
        onNavigateToAlbum = onNavigateToAlbum,
        onNavigateToAlbumBySong = { song ->
            genreScope.launch {
                val resolvedAlbumId = viewModel.resolveAlbumIdForSong(song)
                if (resolvedAlbumId != null) {
                    onNavigateToAlbum(resolvedAlbumId)
                } else {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.toast_error_with_message,
                            context.getString(R.string.library_album_detail_unknown_album)
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        },
        onNavigateToArtist = onNavigateToArtist,
        onShowSongInfo = { song -> showSongInfo = song },
        onShowDeleteConfirmation = { song -> songToDelete = song },
        onShowPlaylistSelector = { song ->
            songToAddToPlaylist = song
            showAddToPlaylistDialog = true
        },
        onToggleLike = { songId, isLiked -> viewModel.toggleLike(songId, isLiked) },
        onShare = shareHandler
    )

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, context.getString(R.string.toast_song_deleted_successfully), Toast.LENGTH_SHORT).show()
            viewModel.loadGenre(genreId)
        } else {
            Toast.makeText(context, context.getString(R.string.toast_delete_cancelled), Toast.LENGTH_SHORT).show()
        }
        songToDelete = null
    }

    val multiSelectDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, context.getString(R.string.toast_songs_deleted_successfully), Toast.LENGTH_SHORT).show()
            viewModel.loadGenre(genreId)
            songsPendingDeletion = false
        } else {
            Toast.makeText(context, context.getString(R.string.toast_delete_cancelled), Toast.LENGTH_SHORT).show()
            songsPendingDeletion = false
        }
    }

    if (showPlaylistDialogForSelection) {
        AddToPlaylistDialog(
            playlists = playlists,
            onPlaylistSelected = { playlistId ->
                songsPendingPlaylistAdd.forEach { song ->
                    playlistViewModel.addSongToPlaylist(playlistId, song.id)
                }
                showPlaylistDialogForSelection = false
                Toast.makeText(context, context.getString(R.string.toast_songs_added_to_playlist), Toast.LENGTH_SHORT).show()
                viewModel.toggleSelectionMode(false)
            },
            onDismiss = { showPlaylistDialogForSelection = false }
        )
    }

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
                Text(androidx.compose.ui.res.stringResource(R.string.dialog_delete_selected_songs_title, selectedSongIds.size))
            },
            text = {
                Text(androidx.compose.ui.res.stringResource(R.string.dialog_delete_songs_message))
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
                                    Toast.makeText(context, context.getString(R.string.toast_songs_deleted_successfully), Toast.LENGTH_SHORT).show()
                                    viewModel.loadGenre(genreId)
                                    songsPendingDeletion = false
                                }

                                is DeleteHelper.DeleteResult.Error -> {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_error_with_message, deleteResult.message),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    songsPendingDeletion = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(androidx.compose.ui.res.stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { songsPendingDeletion = false }) {
                    Text(androidx.compose.ui.res.stringResource(R.string.common_cancel))
                }
            }
        )
    }
    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text(androidx.compose.ui.res.stringResource(R.string.label_selected_count, selectedSongIds.size)) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleSelectionMode(false) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = androidx.compose.ui.res.stringResource(R.string.common_exit_selection)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            genre?.name ?: androidx.compose.ui.res.stringResource(R.string.library_screens_b_genre_title)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = androidx.compose.ui.res.stringResource(R.string.common_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
        if (genre == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            GenreDetailContent(
                genre = genre!!,
                songs = songs,
                currentSongId = playbackState.currentSong?.id,
                isPlayingGlobally = playbackState.isPlaying,
                isSelectionMode = isSelectionMode,
                selectedSongIds = selectedSongIds,
                sortMode = sortMode,
                menuHandler = menuHandler,
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
                onSortClick = { showSortDialog = true },
                onSelectionClick = { viewModel.toggleSelectionMode(true) },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }

    songToDelete?.let { song ->
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
                        Toast.makeText(context, context.getString(R.string.toast_song_deleted_successfully), Toast.LENGTH_SHORT).show()
                        viewModel.loadGenre(genreId)
                        songToDelete = null
                    }

                    is DeleteHelper.DeleteResult.Error -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_error_with_message, result.message),
                            Toast.LENGTH_SHORT
                        ).show()
                        songToDelete = null
                    }
                }
            },
            onDismiss = { songToDelete = null }
        )
    }

    showSongInfo?.let { song ->
        SongInfoDialog(
            song = song,
            onDismiss = { showSongInfo = null }
        )
    }

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

@Composable
private fun GenreDetailContent(
    genre: Genre,
    songs: List<Song>,
    currentSongId: Long?,
    isPlayingGlobally: Boolean,
    isSelectionMode: Boolean,
    selectedSongIds: Set<Long>,
    sortMode: GenreSongSortMode,
    menuHandler: SongMenuHandler,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onSongClick: (Song) -> Unit,
    onSortClick: () -> Unit,
    onSelectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val albumCount = remember(songs) { songs.map { it.album }.distinct().size }
    val sortedSongs = when (sortMode) {
        GenreSongSortMode.TITLE -> songs.sortedBy { it.title.lowercase() }
        GenreSongSortMode.ARTIST -> songs.sortedBy { it.artist.lowercase() }
        GenreSongSortMode.ALBUM -> songs.sortedBy { it.album.lowercase() }
        GenreSongSortMode.DURATION -> songs.sortedByDescending { it.duration }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = ContentTopPadding,
            bottom = 8.dp + ContentBottomPadding,
            start = 0.dp,
            end = 0.dp
        )
    ) {
        item {
            GenreHeader(
                genre = genre,
                songCount = songs.size,
                albumCount = albumCount,
                onPlayAll = onPlayAll,
                onShuffle = onShuffle
            )
        }

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
                        text = androidx.compose.ui.res.stringResource(R.string.home_tab_songs),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        IconButton(onClick = onSortClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = androidx.compose.ui.res.stringResource(R.string.library_common_sort)
                            )
                        }
                        IconButton(onClick = onSelectionClick) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = androidx.compose.ui.res.stringResource(R.string.library_screens_b_select_songs)
                            )
                        }
                    }
                }
            }
        }

        if (sortedSongs.isEmpty()) {
            item {
                EmptyGenreState()
            }
        } else {
            itemsIndexed(
                items = sortedSongs,
                key = { _, song -> song.id }
            ) { index, song ->
                GenreSongItemRow(
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
private fun GenreHeader(
    genre: Genre,
    songCount: Int,
    albumCount: Int,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val artworkSize = (screenWidthDp * 0.72f).coerceIn(216.dp, 320.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.size(artworkSize),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            GenreIcon(
                genreName = genre.name,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = genre.name,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = androidx.compose.ui.res.pluralStringResource(
                R.plurals.common_song_count,
                songCount,
                songCount
            ) + " • " + androidx.compose.ui.res.pluralStringResource(
                R.plurals.library_screens_b_album_count,
                albumCount,
                albumCount
            ),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onShuffle,
                enabled = songCount > 0,
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
                enabled = songCount > 0,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
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
private fun GenreSongItemRow(
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
                        androidx.compose.ui.res.stringResource(R.string.library_screens_b_checked)
                    } else {
                        androidx.compose.ui.res.stringResource(R.string.library_screens_b_unchecked)
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

        Column(modifier = Modifier.weight(1f)) {
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
                    AnimatedEqualizer(
                        isAnimating = isPlayingGlobally,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.common_artist_album_pair, song.artist, song.album),
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
                    contentDescription = androidx.compose.ui.res.stringResource(R.string.now_playing_more_options),
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
private fun EmptyGenreState() {
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
            text = androidx.compose.ui.res.stringResource(R.string.library_screens_b_no_songs_in_genre),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun GenreSongSortDialog(
    currentMode: GenreSongSortMode,
    onDismiss: () -> Unit,
    onModeSelect: (GenreSongSortMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(R.string.common_sort_by)) },
        text = {
            Column {
                GenreSongSortMode.values().forEach { mode ->
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
                                GenreSongSortMode.TITLE -> androidx.compose.ui.res.stringResource(R.string.library_screens_b_sort_option_title_az)
                                GenreSongSortMode.ARTIST -> androidx.compose.ui.res.stringResource(R.string.library_screens_b_sort_option_artist_az)
                                GenreSongSortMode.ALBUM -> androidx.compose.ui.res.stringResource(R.string.library_screens_b_sort_option_album_az)
                                GenreSongSortMode.DURATION -> androidx.compose.ui.res.stringResource(R.string.library_screens_b_sort_option_duration)
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
                Text(androidx.compose.ui.res.stringResource(R.string.common_close))
            }
        }
    )
}
