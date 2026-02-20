package com.sukoon.music.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.ui.components.SelectionBottomBarItem
import com.sukoon.music.ui.components.SortOption
import com.sukoon.music.ui.viewmodel.ArtistSortMode
import com.sukoon.music.ui.viewmodel.ArtistsViewModel
import androidx.compose.material3.Scaffold
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.ui.components.MultiSelectActionBottomBar
import com.sukoon.music.ui.components.AddToPlaylistDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.animation.*
import androidx.compose.foundation.lazy.rememberLazyListState
import com.sukoon.music.ui.theme.*

@Composable
fun ArtistsScreen(
    onNavigateToArtistDetail: (Long) -> Unit,
    onNavigateToArtistSelection: () -> Unit = {},
    onBackClick: () -> Unit,
    viewModel: ArtistsViewModel = hiltViewModel(),
    playlistViewModel: com.sukoon.music.ui.viewmodel.PlaylistViewModel = hiltViewModel()
) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val recentlyPlayedArtists by viewModel.recentlyPlayedArtists.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedArtistIds by viewModel.selectedArtistIds.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()

    var showPlaylistDialog by remember { mutableStateOf(false) }
    var artistSongsForPlaylist by remember { mutableStateOf<List<com.sukoon.music.domain.model.Song>>(emptyList()) }
    var pendingArtistForPlaylist by remember { mutableStateOf<Long?>(null) }
    var artistsPendingDeletion by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Delete launcher for handling file deletion permissions
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_artist_deleted_successfully), Toast.LENGTH_SHORT).show()
            artistsPendingDeletion = false
        } else {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_delete_cancelled), Toast.LENGTH_SHORT).show()
        }
        artistsPendingDeletion = false
    }

    // Handle pending artist(s) for playlist - support multi-artist selection
    LaunchedEffect(pendingArtistForPlaylist, selectedArtistIds) {
        if (pendingArtistForPlaylist != null) {
            if (pendingArtistForPlaylist == -1L) {
                // Multi-select: get all selected artists' songs
                if (selectedArtistIds.isNotEmpty()) {
                    val allSongs = selectedArtistIds.flatMap { artistId ->
                        viewModel.getSongsForArtist(artistId)
                    }
                    artistSongsForPlaylist = allSongs
                    showPlaylistDialog = true
                    pendingArtistForPlaylist = null
                }
            } else {
                // Single artist from context menu
                artistSongsForPlaylist = viewModel.getSongsForArtist(pendingArtistForPlaylist!!)
                showPlaylistDialog = true
                pendingArtistForPlaylist = null
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (isSelectionMode) {
                ArtistSelectionTopBar(
                    selectedCount = selectedArtistIds.size,
                    onBackClick = {
                        viewModel.clearSelection()
                        viewModel.toggleSelectionMode(false)
                    }
                )
            }
        },
        bottomBar = {
            if (isSelectionMode && selectedArtistIds.isNotEmpty()) {
                MultiSelectActionBottomBar(
                    onPlay = { viewModel.playSelectedArtists() },
                    onAddToPlaylist = {
                        // Trigger LaunchedEffect to load all selected artists' songs
                        pendingArtistForPlaylist = -1L // Use sentinel value to trigger
                    },
                    onDelete = { artistsPendingDeletion = true },
                    onPlayNext = { viewModel.playSelectedArtistsNext() },
                    onAddToQueue = { viewModel.addSelectedArtistsToQueue() }
                )
            }
        }
    ) { padding ->
        ArtistsContent(
            artists = artists,
            recentlyPlayedArtists = recentlyPlayedArtists,
            onArtistClick = onNavigateToArtistDetail,
            onNavigateToArtistSelection = onNavigateToArtistSelection,
            viewModel = viewModel,
            modifier = if (isSelectionMode) Modifier.padding(padding) else Modifier,
            playlistViewModel = playlistViewModel,
            showPlaylistDialog = showPlaylistDialog,
            artistSongsForPlaylist = artistSongsForPlaylist,
            onPlaylistDialogDismiss = { showPlaylistDialog = false },
            onPlaylistSelected = { playlistId ->
                artistSongsForPlaylist.forEach { song ->
                    playlistViewModel.addSongToPlaylist(playlistId, song.id)
                }
                showPlaylistDialog = false
                Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_songs_added_to_playlist), Toast.LENGTH_SHORT).show()
                viewModel.toggleSelectionMode(false)
            },
            artistsPendingDeletion = artistsPendingDeletion,
            onDeleteConfirmed = {
                viewModel.deleteSelectedArtistsWithResult { deleteResult ->
                    when (deleteResult) {
                        is DeleteHelper.DeleteResult.RequiresPermission -> {
                            deleteLauncher.launch(
                                IntentSenderRequest.Builder(deleteResult.intentSender).build()
                            )
                        }
                        is DeleteHelper.DeleteResult.Success -> {
                            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_artist_deleted_successfully), Toast.LENGTH_SHORT).show()
                            artistsPendingDeletion = false
                        }
                        is DeleteHelper.DeleteResult.Error -> {
                            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_error_with_message, deleteResult.message), Toast.LENGTH_SHORT).show()
                            artistsPendingDeletion = false
                        }
                    }
                }
            },
            onDeleteDismissed = { artistsPendingDeletion = false }
        )
    }
}

/**
 * Artists Content - Displays artists in a list view.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ArtistsContent(
    artists: List<Artist>,
    recentlyPlayedArtists: List<Artist>,
    onArtistClick: (Long) -> Unit,
    onNavigateToArtistSelection: () -> Unit = {},
    viewModel: ArtistsViewModel,
    modifier: Modifier = Modifier,
    playlistViewModel: com.sukoon.music.ui.viewmodel.PlaylistViewModel = hiltViewModel(),
    showPlaylistDialog: Boolean = false,
    artistSongsForPlaylist: List<com.sukoon.music.domain.model.Song> = emptyList(),
    onPlaylistDialogDismiss: () -> Unit = {},
    onPlaylistSelected: (Long) -> Unit = {},
    artistsPendingDeletion: Boolean = false,
    onDeleteConfirmed: () -> Unit = {},
    onDeleteDismissed: () -> Unit = {}
) {
    var showMenuForArtist by remember { mutableStateOf<Artist?>(null) }
    var showSortDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val selectedArtistIds by viewModel.selectedArtistIds.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val isAscending by viewModel.isAscending.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedArtistIds.collectAsStateWithLifecycle()

    val scrollState = rememberLazyListState()

    if (artists.isEmpty() && searchQuery.isEmpty()) {
        EmptyArtistsContentState()
    } else {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = MiniPlayerHeight + ContentBottomPadding,
                start = 0.dp,
                end = 0.dp
            )
        ) {
            // Recently played section
            if (!isSelectionMode && recentlyPlayedArtists.isNotEmpty() && searchQuery.isEmpty()) {
                item {
                    Column {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_common_recently_played),
                            style = MaterialTheme.typography.sectionHeader,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentlyPlayedArtists.take(10), key = { it.id }) { artist ->
                                ArtistCard(
                                    artist = artist,
                                    onClick = { onArtistClick(artist.id) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp
                        )
                    }
                }
            }

            // Sort Header (sticky)
            if (!isSelectionMode) {
                stickyHeader(key = "header") {
                    ArtistSortHeader(
                        artistCount = artists.size,
                        onSortClick = { showSortDialog = true },
                        onSelectionClick = onNavigateToArtistSelection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
            }

            // Artists list
            items(artists, key = { it.id }) { artist ->
                ArtistListItem(
                    artist = artist,
                    isSelected = selectedIds.contains(artist.id),
                    isSelectionMode = isSelectionMode,
                    onClick = {
                        if (isSelectionMode) {
                            viewModel.toggleArtistSelection(artist.id)
                        } else {
                            onArtistClick(artist.id)
                        }
                    },
                    onLongClick = { viewModel.toggleSelectionMode(true); viewModel.toggleArtistSelection(artist.id) },
                    onMenuClick = { showMenuForArtist = artist }
                )
            }
        }
    }

    // Artist context menu
    showMenuForArtist?.let { artist ->
        ArtistContextMenuBottomSheet(
            artist = artist,
            onDismiss = { showMenuForArtist = null },
            onPlay = {
                viewModel.playArtist(artist.id)
                showMenuForArtist = null
            },
            onShuffle = {
                viewModel.shuffleArtist(artist.id)
                showMenuForArtist = null
            },
            onPlayNext = {
                viewModel.playArtistNext(artist.id)
                showMenuForArtist = null
            },
            onAddToQueue = {
                viewModel.addArtistToQueue(artist.id)
                showMenuForArtist = null
            },
            onAddToPlaylist = {
                viewModel.showAddToPlaylistDialog(artist.id)
                showMenuForArtist = null
            }
        )
    }

    // Playlist Dialog
    if (showPlaylistDialog) {
        AddToPlaylistDialog(
            playlists = playlists,
            onPlaylistSelected = { playlistId ->
                artistSongsForPlaylist.forEach { song ->
                    playlistViewModel.addSongToPlaylist(playlistId, song.id)
                }
                Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_songs_added_to_playlist), Toast.LENGTH_SHORT).show()
                onPlaylistSelected(playlistId)
                onPlaylistDialogDismiss()
            },
            onDismiss = { onPlaylistDialogDismiss() }
        )
    }

    // Delete confirmation dialog
    if (artistsPendingDeletion) {
        AlertDialog(
            onDismissRequest = { onDeleteDismissed() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_delete_selected_artists_title, selectedArtistIds.size))
            },
            text = {
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_delete_artist_songs_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteConfirmed()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onDeleteDismissed() }) {
                    Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_cancel))
                }
            }
        )
    }

    // Sort dialog
    if (showSortDialog) {
        ArtistSortDialog(
            currentSortMode = sortMode,
            isAscending = isAscending,
            onDismiss = { showSortDialog = false },
            onSortModeChange = { viewModel.setSortMode(it) },
            onOrderChange = { viewModel.setAscending(it) }
        )
    }
}

@Composable
private fun ArtistCard(
    artist: Artist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (artist.artworkUri != null) {
                SubcomposeAsyncImage(
                    model = artist.artworkUri,
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.cardSubtitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistListItem(
    artist: Artist,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (artist.artworkUri != null) {
                SubcomposeAsyncImage(
                    model = artist.artworkUri,
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.listItemTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(
                    com.sukoon.music.R.string.library_artists_item_stats_format,
                    artist.albumCount,
                    androidx.compose.ui.res.stringResource(
                        if (artist.albumCount == 1) com.sukoon.music.R.string.library_artists_word_album_singular
                        else com.sukoon.music.R.string.library_artists_word_album_plural
                    ),
                    artist.songCount,
                    androidx.compose.ui.res.stringResource(
                        if (artist.songCount == 1) com.sukoon.music.R.string.library_artists_word_song_singular
                        else com.sukoon.music.R.string.library_artists_word_song_plural
                    )
                ),
                style = MaterialTheme.typography.listItemSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isSelectionMode) {
            if (isSelected) {
                Checkbox(
                    checked = true,
                    onCheckedChange = { onClick() }
                )
            } else {
                RadioButton(
                    selected = false,
                    onClick = { onClick() }
                )
            }
        } else {
            IconButton(onClick = { onMenuClick() }, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.now_playing_more_options),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ArtistSortHeader(
    artistCount: Int,
    onSortClick: () -> Unit,
    onSelectionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(
                com.sukoon.music.R.string.library_artists_header_count_format,
                artistCount,
                androidx.compose.ui.res.stringResource(
                    if (artistCount == 1) com.sukoon.music.R.string.library_artists_word_artist_singular
                    else com.sukoon.music.R.string.library_artists_word_artist_plural
                )
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row {
            IconButton(onClick = onSortClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Sort,
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_common_sort),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onSelectionClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_select),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
@Composable
private fun ArtistSortDialog(
    currentSortMode: ArtistSortMode,
    isAscending: Boolean,
    onDismiss: () -> Unit,
    onSortModeChange: (ArtistSortMode) -> Unit,
    onOrderChange: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_ok), color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_cancel)) }
        },
        title = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_sort_by)) },
        text = {
            Column {
                SortOption(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_artists_sort_artist_name),
                    isSelected = currentSortMode == ArtistSortMode.ARTIST_NAME,
                    onClick = { onSortModeChange(ArtistSortMode.ARTIST_NAME) }
                )
                SortOption(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_artists_sort_number_of_albums),
                    isSelected = currentSortMode == ArtistSortMode.ALBUM_COUNT,
                    onClick = { onSortModeChange(ArtistSortMode.ALBUM_COUNT) }
                )
                SortOption(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_artists_sort_number_of_songs),
                    isSelected = currentSortMode == ArtistSortMode.SONG_COUNT,
                    onClick = { onSortModeChange(ArtistSortMode.SONG_COUNT) }
                )
                SortOption(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_common_random),
                    isSelected = currentSortMode == ArtistSortMode.RANDOM,
                    onClick = { onSortModeChange(ArtistSortMode.RANDOM) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SortOption(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_common_sort_order_a_to_z),
                    isSelected = isAscending,
                    onClick = { onOrderChange(true) }
                )
                SortOption(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_common_sort_order_z_to_a),
                    isSelected = !isAscending,
                    onClick = { onOrderChange(false) }
                )
            }
        }
    )
}

@Composable
fun EmptyArtistsContentState() {
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
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_artists_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_artists_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistContextMenuBottomSheet(
    artist: Artist,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
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
            // Header with artist info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (artist.artworkUri != null) {
                        SubcomposeAsyncImage(
                            model = artist.artworkUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.cardTitle
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(
                            com.sukoon.music.R.string.library_artists_context_stats_format,
                            artist.albumCount,
                            artist.songCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Menu items
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play)) },
                leadingContent = { Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.clickable(onClick = onPlay)
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_shuffle)) },
                leadingContent = { Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.clickable(onClick = onShuffle)
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play_next)) },
                leadingContent = { Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.clickable(onClick = onPlayNext)
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_queue)) },
                leadingContent = { Icon(Icons.Default.PlaylistAdd, null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.clickable(onClick = onAddToQueue)
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_playlist)) },
                leadingContent = { Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.clickable(onClick = onAddToPlaylist)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistSelectionTopBar(
    selectedCount: Int,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                androidx.compose.ui.res.stringResource(
                    com.sukoon.music.R.string.label_selected_count,
                    selectedCount
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_back))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@Composable
internal fun ArtistSelectionBottomBar(
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            SelectionBottomBarItem(
                icon = Icons.Default.PlayArrow,
                label = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play),
                onClick = onPlay
            )
            SelectionBottomBarItem(
                icon = Icons.Default.PlaylistAdd,
                label = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_artists_selection_add_to_play),
                onClick = onAddToPlaylist
            )
            SelectionBottomBarItem(
                icon = Icons.Default.Delete,
                label = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_delete),
                onClick = onDelete
            )
            /*SelectionBottomBarItem(icon = Icons.Default.MoreVert, label = "More", onClick = onMore)*/
        }
    }
}




