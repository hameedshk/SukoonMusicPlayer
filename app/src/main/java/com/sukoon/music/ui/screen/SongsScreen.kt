package com.sukoon.music.ui.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sukoon.music.R
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.ui.navigation.Routes
import com.sukoon.music.data.premium.PremiumManager
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.AddToPlaylistDialog
import com.sukoon.music.ui.components.AlphabetScrollBar
import com.sukoon.music.ui.components.DeleteConfirmationDialog
import com.sukoon.music.ui.components.SongContextMenu
import com.sukoon.music.ui.components.SongInfoDialog
import com.sukoon.music.ui.components.SongSortDialog
import com.sukoon.music.ui.components.SongsSortHeader
import com.sukoon.music.ui.components.StandardSongListItem
import com.sukoon.music.ui.components.rememberShareHandler
import com.sukoon.music.ui.components.rememberSongMenuHandler
import com.sukoon.music.ui.theme.ContentBottomPadding
import com.sukoon.music.ui.theme.ContentTopPadding
import com.sukoon.music.ui.theme.MiniPlayerHeight
import com.sukoon.music.ui.theme.SpacingSmall
import com.sukoon.music.ui.viewmodel.HomeViewModel
import com.sukoon.music.ui.viewmodel.SongsViewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private const val ALPHABET_SCROLL_THRESHOLD = 150

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun SongsScreen(
    onBackClick: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToArtist: (Long) -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    onNavigateToAudioEditor: (Long) -> Unit = {},
    navController: NavController? = null,
    premiumManager: PremiumManager? = null,
    homeViewModel: HomeViewModel = hiltViewModel(),
    playlistViewModel: com.sukoon.music.ui.viewmodel.PlaylistViewModel = hiltViewModel(),
    songsViewModel: SongsViewModel = hiltViewModel()
) {
    val songs by songsViewModel.songs.collectAsStateWithLifecycle()
    val visibleSongs by songsViewModel.visibleSongs.collectAsStateWithLifecycle()
    val playbackState by songsViewModel.playbackState.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()
    val sortMode by songsViewModel.sortMode.collectAsStateWithLifecycle()
    val sortOrder by songsViewModel.sortOrder.collectAsStateWithLifecycle()
    val showSortDialog by songsViewModel.showSortDialog.collectAsStateWithLifecycle()
    val selectedSongId by songsViewModel.selectedSongId.collectAsStateWithLifecycle()
    val showMenu by songsViewModel.showMenu.collectAsStateWithLifecycle()
    val songToDelete by songsViewModel.songToDelete.collectAsStateWithLifecycle()
    val showInfoForSong by songsViewModel.showInfoForSong.collectAsStateWithLifecycle()
    val deleteError by songsViewModel.deleteError.collectAsStateWithLifecycle()
    val isPremium by (premiumManager?.isPremiumUser ?: flowOf(false)).collectAsStateWithLifecycle(false)
    val isPremiumState = rememberUpdatedState(isPremium)
    val context = LocalContext.current

    DisposableEffect(Unit) {
        songsViewModel.setScreenActive(true)
        onDispose { songsViewModel.setScreenActive(false) }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(
                context,
                context.getString(R.string.toast_song_deleted_successfully),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.toast_delete_cancelled),
                Toast.LENGTH_SHORT
            ).show()
        }
        songsViewModel.finalizeDeletion()
    }

    deleteError?.let { error ->
        LaunchedEffect(error) {
            Toast.makeText(
                context,
                context.getString(R.string.toast_error_with_message, error),
                Toast.LENGTH_SHORT
            ).show()
            songsViewModel.clearDeleteError()
        }
    }

    LaunchedEffect(Unit) {
        songsViewModel.setSearchQuery("")
        songsViewModel.clearFilters()
    }

    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

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
        onShowEditAudio = { song ->
            if (isPremiumState.value) {
                if (navController != null) {
                    navController.navigate(Routes.AudioEditor.createRoute(song.id))
                } else {
                    onNavigateToAudioEditor(song.id)
                }
            } else {
                if (navController != null) {
                    navController.navigate(Routes.Settings.createRoute(openPremiumDialog = true))
                } else {
                    onNavigateToPremium()
                }
            }
        },
        onToggleLike = { songId, isLiked -> homeViewModel.toggleLike(songId, isLiked) },
        onShare = rememberShareHandler()
    )

    val songStartIndex = 1
    val letterToIndexMap = remember(visibleSongs, songStartIndex) {
        val map = mutableMapOf<String, Int>()
        visibleSongs.forEachIndexed { songIndex, song ->
            val firstChar = song.title.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
            val key = if (firstChar.firstOrNull()?.isLetter() == true) firstChar else "#"
            if (!map.containsKey(key)) {
                map[key] = songStartIndex + songIndex
            }
        }
        map
    }
    val activeLetter by remember(scrollState, visibleSongs, songStartIndex) {
        derivedStateOf {
            val firstSongItem = scrollState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index >= songStartIndex }
            firstSongItem?.let {
                visibleSongs.getOrNull(it.index - songStartIndex)?.title?.firstOrNull()?.let { ch ->
                    if (ch.isLetter()) ch.uppercaseChar().toString() else "#"
                }
            }
        }
    }

    when {
        songs.isEmpty() -> {
            EmptySongsState()
        }

        visibleSongs.isEmpty() -> {
            NoSongsResultsState(onClear = { songsViewModel.clearSearchAndFilters() })
        }

        else -> {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = MiniPlayerHeight + SpacingSmall)
                ) {
                    stickyHeader(key = "header") {
                        SongsSortHeader(
                            songCount = visibleSongs.size,
                            hasActiveFilters = false,
                            onSortClick = { songsViewModel.showSortDialog() },
                            onFilterClick = {},
                            showFilterAction = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                        )
                    }

                    itemsIndexed(
                        items = visibleSongs,
                        key = { _, song -> song.id },
                        contentType = { _, _ -> "song_row" }
                    ) { index, song ->
                        val isCurrentSong = playbackState.currentSong?.id == song.id
                        val isPlaybackActive = isCurrentSong && playbackState.isPlaying
                        StandardSongListItem(
                            song = song,
                            isCurrentSong = isCurrentSong,
                            isPlaybackActive = isPlaybackActive,
                            showArtistAlbumMeta = false,
                            showLikeButton = false,
                            onClick = { songsViewModel.playQueue(visibleSongs, index) },
                            onMenuClick = { songsViewModel.openMenuForSong(song.id) }
                        )
                    }
                }

                if (visibleSongs.size >= ALPHABET_SCROLL_THRESHOLD) {
                    AlphabetScrollBar(
                        onLetterClick = { letter ->
                            letterToIndexMap[letter]?.let { index ->
                                scope.launch { scrollState.animateScrollToItem(index) }
                            }
                        },
                        activeLetter = activeLetter,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(
                                top = 56.dp,
                                end = 4.dp,
                                bottom = MiniPlayerHeight + SpacingSmall + 8.dp
                            )
                    )
                }
            }
        }
    }

    if (showSortDialog) {
        SongSortDialog(
            currentSortMode = sortMode,
            currentOrder = sortOrder,
            onDismiss = { songsViewModel.hideSortDialog() },
            onConfirm = { mode, order ->
                songsViewModel.setSortMode(mode, order)
                songsViewModel.hideSortDialog()
            }
        )
    }

    if (showMenu && selectedSongId != null) {
        val freshSong = songs.firstOrNull { it.id == selectedSongId }
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
                            deleteLauncher.launch(IntentSenderRequest.Builder(result.intentSender).build())
                        }

                        is DeleteHelper.DeleteResult.Success -> {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_song_deleted_successfully),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is DeleteHelper.DeleteResult.Error -> Unit
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
private fun EmptySongsState() {
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
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.home_empty_no_songs_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.common_no_songs_found),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NoSongsResultsState(
    onClear: () -> Unit
) {
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
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.liked_songs_no_filter_results_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.liked_songs_no_filter_results_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onClear) {
            Text(stringResource(R.string.liked_songs_filter_chip_clear))
        }
    }
}
