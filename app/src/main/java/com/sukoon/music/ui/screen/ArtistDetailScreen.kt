package com.sukoon.music.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.R
import com.sukoon.music.ui.navigation.Routes
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.data.premium.PremiumManager
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.AddToPlaylistDialog
import com.sukoon.music.ui.components.AnimatedEqualizer
import com.sukoon.music.ui.components.DeleteConfirmationDialog
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
import com.sukoon.music.ui.util.rememberAlbumPalette
import com.sukoon.music.ui.viewmodel.ArtistDetailViewModel
import com.sukoon.music.ui.viewmodel.ArtistSongSortMode
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistId: Long,
    onBackClick: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToNowPlaying: () -> Unit = {},
    navController: NavController? = null,
    premiumManager: PremiumManager? = null,
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
    val isPremium by (premiumManager?.isPremiumUser ?: flowOf(false)).collectAsStateWithLifecycle(false)

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
    val tag = "ArtistDetailScreen"

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
    val menuHandler = rememberSongMenuHandler(
        playbackRepository = viewModel.playbackRepository,
        onNavigateToAlbum = onNavigateToAlbum,
        onShowDeleteConfirmation = { song -> if (!isDeleting) songToDelete = song },
        onShowSongInfo = { song -> showInfoForSong = song },
        onShowPlaylistSelector = { song ->
            songToAddToPlaylist = song
            showAddToPlaylistDialog = true
        },
        onShowEditAudio = { song ->
            if (isPremium) {
                navController?.navigate("audio_editor/${song.id}")
            } else {
                navController?.navigate(Routes.Settings.createRoute(openPremiumDialog = true))
            }
        },
        onToggleLike = { id, isLiked -> viewModel.toggleLike(id, isLiked) },
        onShare = shareHandler
    )

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, context.getString(R.string.toast_song_deleted_successfully), Toast.LENGTH_SHORT).show()
            viewModel.loadArtist(artistId)
        } else {
            Toast.makeText(context, context.getString(R.string.toast_delete_cancelled), Toast.LENGTH_SHORT).show()
        }
        songToDelete = null
        isDeleting = false
    }

    val multiSelectDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, context.getString(R.string.toast_songs_deleted_successfully), Toast.LENGTH_SHORT).show()
            viewModel.loadArtist(artistId)
            songsPendingDeletion = false
        } else {
            Toast.makeText(context, context.getString(R.string.toast_delete_cancelled), Toast.LENGTH_SHORT).show()
            songsPendingDeletion = false
        }
    }

    LaunchedEffect(artistId) {
        viewModel.loadArtist(artistId)
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
                        viewModel.deleteSelectedSongsWithResult(artistSongs, context) { deleteResult ->
                            when (deleteResult) {
                                is DeleteHelper.DeleteResult.RequiresPermission -> {
                                    multiSelectDeleteLauncher.launch(
                                        IntentSenderRequest.Builder(deleteResult.intentSender).build()
                                    )
                                    songsPendingDeletion = false
                                }

                                is DeleteHelper.DeleteResult.Success -> {
                                    Toast.makeText(context, context.getString(R.string.toast_songs_deleted_successfully), Toast.LENGTH_SHORT).show()
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
                    title = {
                        Text(androidx.compose.ui.res.stringResource(R.string.label_selected_count, selectedSongIds.size))
                    },
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
                            text = artist?.name?.takeIf { it.isNotBlank() }
                                ?: androidx.compose.ui.res.stringResource(R.string.library_screens_b_artist_title)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                androidx.compose.ui.res.stringResource(R.string.common_back)
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
        if (artist == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            ArtistDetailContent(
                artist = artist!!,
                songs = artistSongs,
                currentSongId = playbackState.currentSong?.id,
                isPlayingGlobally = playbackState.isPlaying,
                isSelectionMode = isSelectionMode,
                selectedSongIds = selectedSongIds,
                sortMode = sortMode,
                menuHandler = menuHandler,
                onPlayAll = { viewModel.playArtist(artistSongs) },
                onShuffle = { viewModel.shuffleArtist(artistSongs) },
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
                            Toast.makeText(context, context.getString(R.string.toast_song_deleted_successfully), Toast.LENGTH_SHORT).show()
                            songToDelete = null
                            isDeleting = false
                        }

                        is DeleteHelper.DeleteResult.Error -> {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_error_with_message, result.message),
                                Toast.LENGTH_SHORT
                            ).show()
                            songToDelete = null
                            isDeleting = false
                        }
                    }
                }
            },
            onDismiss = { songToDelete = null }
        )
    }

    showInfoForSong?.let { song ->
        SongInfoDialog(
            song = song,
            onDismiss = { showInfoForSong = null }
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
@Composable
private fun ArtistDetailContent(
    artist: Artist,
    songs: List<Song>,
    currentSongId: Long?,
    isPlayingGlobally: Boolean,
    isSelectionMode: Boolean,
    selectedSongIds: Set<Long>,
    sortMode: ArtistSongSortMode,
    menuHandler: SongMenuHandler,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onSongClick: (Song) -> Unit,
    onSortClick: () -> Unit,
    onSelectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedSongs = when (sortMode) {
        ArtistSongSortMode.TITLE -> songs.sortedBy { it.title.lowercase() }
        ArtistSongSortMode.ARTIST -> songs.sortedBy { it.artist.lowercase() }
        ArtistSongSortMode.ALBUM -> songs.sortedBy { it.album.lowercase() }
        ArtistSongSortMode.DURATION -> songs.sortedByDescending { it.duration }
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
            ArtistHeader(
                artist = artist,
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
                EmptyArtistState()
            }
        } else {
            itemsIndexed(
                items = sortedSongs,
                key = { _, song -> song.id }
            ) { index, song ->
                ArtistSongItemRow(
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
private fun ArtistHeader(
    artist: Artist,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val artworkSize = (screenWidthDp * 0.72f).coerceIn(216.dp, 320.dp)
    val palette = rememberAlbumPalette(artist.artworkUri)

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (artist.artworkUri != null) {
                    SubcomposeAsyncImage(
                        model = artist.artworkUri,
                        contentDescription = androidx.compose.ui.res.stringResource(R.string.library_screens_b_artist_artwork),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = { DefaultArtistPlaceholderLarge() },
                        error = { DefaultArtistPlaceholderLarge() }
                    )
                } else {
                    DefaultArtistPlaceholderLarge()
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(
                R.string.library_screens_b_artist_stats,
                artist.albumCount,
                artist.songCount
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
                enabled = artist.songCount > 0,
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
                enabled = artist.songCount > 0,
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
private fun DefaultArtistPlaceholderLarge() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun ArtistSongItemRow(
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
    val unknownAlbumLabel = androidx.compose.ui.res.stringResource(R.string.library_album_detail_unknown_album)
    val trimmedAlbum = song.album.trim()
    val displayAlbum = when {
        trimmedAlbum.isEmpty() -> unknownAlbumLabel
        trimmedAlbum.equals("<unknown>", ignoreCase = true) -> unknownAlbumLabel
        else -> trimmedAlbum
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
                text = displayAlbum,
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
private fun EmptyArtistState() {
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
            text = androidx.compose.ui.res.stringResource(R.string.library_screens_b_no_songs_found),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ArtistSongSortDialog(
    currentMode: ArtistSongSortMode,
    onDismiss: () -> Unit,
    onModeSelect: (ArtistSongSortMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(R.string.common_sort_by)) },
        text = {
            Column {
                ArtistSongSortMode.values().forEach { mode ->
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
                                ArtistSongSortMode.TITLE -> androidx.compose.ui.res.stringResource(R.string.library_screens_b_sort_option_title_az)
                                ArtistSongSortMode.ARTIST -> androidx.compose.ui.res.stringResource(R.string.library_screens_b_sort_option_artist_az)
                                ArtistSongSortMode.ALBUM -> androidx.compose.ui.res.stringResource(R.string.library_screens_b_sort_option_album_az)
                                ArtistSongSortMode.DURATION -> androidx.compose.ui.res.stringResource(R.string.library_screens_b_sort_option_duration)
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
