package com.sukoon.music.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.*
import com.sukoon.music.ui.theme.*
import com.sukoon.music.ui.viewmodel.HomeViewModel
import com.sukoon.music.ui.viewmodel.SongsViewModel
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.sukoon.music.data.mediastore.DeleteHelper

/**
 * Songs Screen - Displays all songs with sorting and playback controls.
 * Owns SongsViewModel lifecycle and coordinates state flow to UI.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SongsScreen(
    onBackClick: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToArtist: (Long) -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    playlistViewModel: com.sukoon.music.ui.viewmodel.PlaylistViewModel = hiltViewModel(),
    songsViewModel: SongsViewModel = hiltViewModel()
) {
    val songs by songsViewModel.songs.collectAsStateWithLifecycle()
    val playbackState by songsViewModel.playbackState.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()
    val sortMode by songsViewModel.sortMode.collectAsStateWithLifecycle()
    val sortOrder by songsViewModel.sortOrder.collectAsStateWithLifecycle()
    val showSortDialog by songsViewModel.showSortDialog.collectAsStateWithLifecycle()
    val selectedSongId by songsViewModel.selectedSongId.collectAsStateWithLifecycle()
    val showMenu by songsViewModel.showMenu.collectAsStateWithLifecycle()
    val songToDelete by songsViewModel.songToDelete.collectAsStateWithLifecycle()
    val isDeleting by songsViewModel.isDeleting.collectAsStateWithLifecycle()
    val showInfoForSong by songsViewModel.showInfoForSong.collectAsStateWithLifecycle()
    val deleteError by songsViewModel.deleteError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Handle lifecycle
    DisposableEffect(Unit) {
        songsViewModel.setScreenActive(true)
        onDispose {
            songsViewModel.setScreenActive(false)
        }
    }

    // Delete result launcher
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_song_deleted_successfully), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_delete_cancelled), Toast.LENGTH_SHORT).show()
        }
        songsViewModel.finalizeDeletion()
    }

    // Show error toast
    deleteError?.let { error ->
        LaunchedEffect(error) {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_error_with_message, error), Toast.LENGTH_SHORT).show()
            songsViewModel.clearDeleteError()
        }
    }

    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val shareHandler = rememberShareHandler()
    val menuHandler = rememberSongMenuHandler(
        playbackRepository = homeViewModel.playbackRepository,
        onNavigateToAlbum = onNavigateToAlbum,
        onNavigateToArtist = onNavigateToArtist,
        onShowPlaylistSelector = { song ->
            songToAddToPlaylist = song
            showAddToPlaylistDialog = true
        },
        onShowSongInfo = { song -> songsViewModel.showSongInfo(song) },
        onShowDeleteConfirmation = { song -> songsViewModel.showDeleteConfirmation(song) },
        onToggleLike = { songId, isLiked -> homeViewModel.toggleLike(songId, isLiked) },
        onShare = shareHandler
    )

    // Sort songs
    val sortedSongs = remember(songs, sortMode, sortOrder) {
        val sorted = when (sortMode) {
            "Song name" -> songs.sortedBy { it.title.lowercase() }
            "Artist name" -> songs.sortedBy { it.artist.lowercase() }
            "Album name" -> songs.sortedBy { it.album.lowercase() }
            "Folder name" -> songs.sortedBy { it.path.lowercase() }
            "Time added" -> songs.sortedByDescending { it.dateAdded }
            "Play count" -> songs.sortedByDescending { it.playCount }
            "Year" -> songs.sortedByDescending { it.year }
            "Duration" -> songs.sortedByDescending { it.duration }
            "Size" -> songs.sortedByDescending { it.size }
            else -> songs.sortedBy { it.title.lowercase() }
        }
        if (sortOrder == "Z to A") sorted.reversed() else sorted
    }

    if (sortedSongs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SpacingMedium)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_no_songs_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 0.dp,
                    bottom = MiniPlayerHeight + SpacingSmall
                )
            ) {
                stickyHeader(key = "header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = androidx.compose.ui.res.pluralStringResource(
                                com.sukoon.music.R.plurals.common_song_count,
                                sortedSongs.size,
                                sortedSongs.size
                            ),
                            style = MaterialTheme.typography.sectionHeader,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(SpacingSmall)) {
                            IconButton(onClick = { songsViewModel.showSortDialog() }) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.songs_cd_sort),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                stickyHeader(key = "chips") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(CompactButtonShape)
                                .surfaceLevel2Gradient()
                                .clickable(onClick = { songsViewModel.shuffleAll() }),
                            shape = CompactButtonShape,
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                                Spacer(Modifier.width(SpacingSmall))
                                Text(
                                    androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_shuffle_all),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(CompactButtonShape)
                                .surfaceLevel2Gradient()
                                .clickable(onClick = { songsViewModel.playAll() }),
                            shape = CompactButtonShape,
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                                Spacer(Modifier.width(SpacingSmall))
                                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                            }
                        }
                    }
                }

                items(
                    items = sortedSongs,
                    key = { it.id }
                ) { song ->
                    val isCurrentSong = playbackState.currentSong?.id == song.id
                    val isPlaybackActive = isCurrentSong && playbackState.isPlaying
                    val index = songs.indexOf(song)

                    SongListItem(
                        song = song,
                        isCurrentSong = isCurrentSong,
                        isPlaybackActive = isPlaybackActive,
                        menuHandler = menuHandler,
                        onClick = { songsViewModel.playQueue(songs, index) },
                        onLikeClick = { homeViewModel.toggleLike(song.id, song.isLiked) },
                        onMenuClick = { songsViewModel.openMenuForSong(song.id) }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showSortDialog) {
        SongSortDialog(
            currentSortMode = sortMode,
            currentOrder = sortOrder,
            onDismiss = { songsViewModel.hideSortDialog() },
            onConfirm = { newMode, newOrder ->
                songsViewModel.setSortMode(newMode, newOrder)
                songsViewModel.hideSortDialog()
            }
        )
    }

    if (showMenu && selectedSongId != null) {
        val freshSong = sortedSongs.find { it.id == selectedSongId }
        if (freshSong != null) {
            SongContextMenu(
                song = freshSong,
                menuHandler = menuHandler,
                onDismiss = { songsViewModel.closeMenu() }
            )
        }
    }

    showInfoForSong?.let { song ->
        SongInfoDialog(
            song = song,
            onDismiss = { songsViewModel.hideSongInfo() }
        )
    }

    songToDelete?.let { song ->
        DeleteConfirmationDialog(
            song = song,
            onConfirm = {
                songsViewModel.confirmDelete(song) { result ->
                    when (result) {
                        is DeleteHelper.DeleteResult.RequiresPermission -> {
                            deleteLauncher.launch(
                                IntentSenderRequest.Builder(result.intentSender).build()
                            )
                        }
                        is DeleteHelper.DeleteResult.Success -> {
                            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_song_deleted_successfully), Toast.LENGTH_SHORT).show()
                        }
                        is DeleteHelper.DeleteResult.Error -> {}
                    }
                }
            },
            onDismiss = { songsViewModel.hideDeleteConfirmation() }
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
}

@Composable
private fun SongListItem(
    song: Song,
    isCurrentSong: Boolean,
    isPlaybackActive: Boolean,
    menuHandler: SongMenuHandler,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (isCurrentSong) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingLarge, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                SubcomposeAsyncImage(
                    model = song.albumArtUri,
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_album_art_for_song, song.title),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        PlaceholderAlbumArt.Placeholder(
                            seed = PlaceholderAlbumArt.generateSeed(
                                albumName = song.album,
                                artistName = song.artist,
                                songId = song.id
                            )
                        )
                    },
                    error = {
                        PlaceholderAlbumArt.Placeholder(
                            seed = PlaceholderAlbumArt.generateSeed(
                                albumName = song.album,
                                artistName = song.artist,
                                songId = song.id
                            )
                        )
                    }
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCurrentSong) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AnimatedEqualizer(
                            isAnimating = isPlaybackActive,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.now_playing_more_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}




