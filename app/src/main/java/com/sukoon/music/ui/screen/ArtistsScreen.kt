package com.sukoon.music.ui.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.ui.components.AddToPlaylistDialog
import com.sukoon.music.ui.components.MultiSelectActionBottomBar
import com.sukoon.music.ui.theme.ContentBottomPadding
import com.sukoon.music.ui.theme.ContentTopPadding
import com.sukoon.music.ui.theme.MiniPlayerHeight
import com.sukoon.music.ui.theme.SpacingLarge
import com.sukoon.music.ui.theme.SpacingMedium
import com.sukoon.music.ui.theme.SpacingSmall
import com.sukoon.music.ui.theme.SpacingTiny
import com.sukoon.music.ui.theme.SpacingXSmall
import com.sukoon.music.ui.theme.accent
import com.sukoon.music.ui.theme.*
import com.sukoon.music.ui.viewmodel.ArtistSortMode
import com.sukoon.music.ui.viewmodel.ArtistsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Suppress("UNUSED_PARAMETER")
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

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(
                context,
                context.getString(com.sukoon.music.R.string.toast_artist_deleted_successfully),
                Toast.LENGTH_SHORT
            ).show()
            artistsPendingDeletion = false
        } else {
            Toast.makeText(
                context,
                context.getString(com.sukoon.music.R.string.toast_delete_cancelled),
                Toast.LENGTH_SHORT
            ).show()
        }
        artistsPendingDeletion = false
    }

    LaunchedEffect(pendingArtistForPlaylist, selectedArtistIds) {
        if (pendingArtistForPlaylist != null) {
            if (pendingArtistForPlaylist == -1L) {
                if (selectedArtistIds.isNotEmpty()) {
                    val allSongs = selectedArtistIds.flatMap { artistId ->
                        viewModel.getSongsForArtist(artistId)
                    }
                    artistSongsForPlaylist = allSongs
                    showPlaylistDialog = true
                    pendingArtistForPlaylist = null
                }
            } else {
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
                TopAppBar(
                    title = {
                        Text(
                            androidx.compose.ui.res.stringResource(
                                com.sukoon.music.R.string.label_selected_count,
                                selectedArtistIds.size
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.clearSelection()
                            viewModel.toggleSelectionMode(false)
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = androidx.compose.ui.res.stringResource(
                                    com.sukoon.music.R.string.common_exit_selection
                                )
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.home_tab_artists))
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
            if (isSelectionMode && selectedArtistIds.isNotEmpty()) {
                MultiSelectActionBottomBar(
                    onPlay = { viewModel.playSelectedArtists() },
                    onAddToPlaylist = { pendingArtistForPlaylist = -1L },
                    onDelete = { artistsPendingDeletion = true },
                    onPlayNext = { viewModel.playSelectedArtistsNext() },
                    onAddToQueue = { viewModel.addSelectedArtistsToQueue() }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isSelectionMode) Modifier.padding(paddingValues) else Modifier)
        ) {
            if (artists.isEmpty()) {
                EmptyArtistsContentState()
            } else {
                ArtistsContent(
                    artists = artists,
                    recentlyPlayedArtists = recentlyPlayedArtists,
                    onArtistClick = onNavigateToArtistDetail,
                    onNavigateToArtistSelection = onNavigateToArtistSelection,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (showPlaylistDialog) {
            AddToPlaylistDialog(
                playlists = playlists,
                onPlaylistSelected = { playlistId ->
                    artistSongsForPlaylist.forEach { song ->
                        playlistViewModel.addSongToPlaylist(playlistId, song.id)
                    }
                    showPlaylistDialog = false
                    Toast.makeText(
                        context,
                        context.getString(com.sukoon.music.R.string.toast_songs_added_to_playlist),
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.toggleSelectionMode(false)
                },
                onDismiss = { showPlaylistDialog = false }
            )
        }

        if (artistsPendingDeletion) {
            AlertDialog(
                onDismissRequest = { artistsPendingDeletion = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text(
                        androidx.compose.ui.res.stringResource(
                            com.sukoon.music.R.string.dialog_delete_selected_artists_title,
                            selectedArtistIds.size
                        )
                    )
                },
                text = {
                    Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_delete_artist_songs_message))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteSelectedArtistsWithResult { deleteResult ->
                                when (deleteResult) {
                                    is DeleteHelper.DeleteResult.RequiresPermission -> {
                                        deleteLauncher.launch(
                                            IntentSenderRequest.Builder(deleteResult.intentSender).build()
                                        )
                                    }

                                    is DeleteHelper.DeleteResult.Success -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(com.sukoon.music.R.string.toast_artist_deleted_successfully),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        artistsPendingDeletion = false
                                    }

                                    is DeleteHelper.DeleteResult.Error -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(
                                                com.sukoon.music.R.string.toast_error_with_message,
                                                deleteResult.message
                                            ),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        artistsPendingDeletion = false
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
                    TextButton(onClick = { artistsPendingDeletion = false }) {
                        Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_cancel))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ArtistsContent(
    artists: List<Artist>,
    recentlyPlayedArtists: List<Artist>,
    onArtistClick: (Long) -> Unit,
    onNavigateToArtistSelection: () -> Unit,
    viewModel: ArtistsViewModel,
    modifier: Modifier = Modifier
) {
    var showMenuForArtist by remember { mutableStateOf<Artist?>(null) }
    var showSortDialog by remember { mutableStateOf(false) }
    val selectedIds by viewModel.selectedArtistIds.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val isAscending by viewModel.isAscending.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val scrollState = rememberLazyListState()

    if (showSortDialog) {
        ArtistSortDialog(
            currentSortMode = sortMode,
            isAscending = isAscending,
            onDismiss = { showSortDialog = false },
            onSortModeChange = { viewModel.setSortMode(it) },
            onOrderChange = { viewModel.setAscending(it) }
        )
    }

    LazyColumn(
        state = scrollState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = MiniPlayerHeight + SpacingSmall)
    ) {
        if (!isSelectionMode && recentlyPlayedArtists.isNotEmpty() && searchQuery.isEmpty()) {
            item(key = "recently_played_artists", contentType = "recently_played_artists") {
                RecentlyPlayedArtistsSection(
                    artists = recentlyPlayedArtists.take(10),
                    onArtistClick = onArtistClick
                )
            }
        }

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

        items(
            items = artists,
            key = { it.id },
            contentType = { "artist_row" }
        ) { artist ->
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
                onLongClick = {
                    if (!isSelectionMode) {
                        viewModel.toggleSelectionMode(true)
                        viewModel.toggleArtistSelection(artist.id)
                    }
                },
                onMenuClick = { showMenuForArtist = artist }
            )
        }
    }

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
}
@Composable
private fun RecentlyPlayedArtistsSection(
    artists: List<Artist>,
    onArtistClick: (Long) -> Unit,
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
                items = artists,
                key = { it.id },
                contentType = { "recent_artist_card" }
            ) { artist ->
                ArtistCard(
                    artist = artist,
                    onClick = { onArtistClick(artist.id) }
                )
            }
        }
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
        Card(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp)
        ) {
            ArtistArtwork(
                artist = artist,
                modifier = Modifier.fillMaxSize(),
                iconSize = 44.dp
            )
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
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val rowShape = RoundedCornerShape(12.dp)
    val rowSelected = isSelectionMode && isSelected
    val rowContainerColor = if (rowSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        Color.Transparent
    }
    val rowBorderWidth = if (rowSelected) 1.dp else 0.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingMedium, vertical = SpacingTiny)
            .clip(rowShape)
            .background(rowContainerColor)
            .border(
                width = rowBorderWidth,
                color = if (rowSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = rowShape
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = SpacingLarge, vertical = SpacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtistArtwork(
            artist = artist,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp)),
            iconSize = 28.dp
        )

        Spacer(modifier = Modifier.width(SpacingMedium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.compactCardTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(SpacingXSmall))
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
                style = MaterialTheme.typography.cardSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = androidx.compose.ui.res.stringResource(
                        if (isSelected) {
                            com.sukoon.music.R.string.library_screens_b_checked
                        } else {
                            com.sukoon.music.R.string.library_screens_b_unchecked
                        }
                    ),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            IconButton(onClick = onMenuClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_more),
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
    onSelectionClick: () -> Unit,
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
                text = androidx.compose.ui.res.stringResource(
                    com.sukoon.music.R.string.library_artists_header_count_format,
                    artistCount,
                    androidx.compose.ui.res.stringResource(
                        if (artistCount == 1) com.sukoon.music.R.string.library_artists_word_artist_singular
                        else com.sukoon.music.R.string.library_artists_word_artist_plural
                    )
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
            TextButton(onClick = onDismiss) {
                Text(
                    androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_ok),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_cancel))
            }
        },
        title = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_sort_by)) },
        text = {
            Column {
                SortOptionRow(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_artists_sort_artist_name),
                    selected = currentSortMode == ArtistSortMode.ARTIST_NAME,
                    onClick = { onSortModeChange(ArtistSortMode.ARTIST_NAME) }
                )
                SortOptionRow(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_artists_sort_number_of_albums),
                    selected = currentSortMode == ArtistSortMode.ALBUM_COUNT,
                    onClick = { onSortModeChange(ArtistSortMode.ALBUM_COUNT) }
                )
                SortOptionRow(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_artists_sort_number_of_songs),
                    selected = currentSortMode == ArtistSortMode.SONG_COUNT,
                    onClick = { onSortModeChange(ArtistSortMode.SONG_COUNT) }
                )
                SortOptionRow(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_common_random),
                    selected = currentSortMode == ArtistSortMode.RANDOM,
                    onClick = { onSortModeChange(ArtistSortMode.RANDOM) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SortOptionRow(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_common_sort_order_a_to_z),
                    selected = isAscending,
                    onClick = { onOrderChange(true) }
                )
                SortOptionRow(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_common_sort_order_z_to_a),
                    selected = !isAscending,
                    onClick = { onOrderChange(false) }
                )
            }
        }
    )
}

@Composable
private fun SortOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
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
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_artists_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.size(64.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    ArtistArtwork(
                        artist = artist,
                        modifier = Modifier.fillMaxSize(),
                        iconSize = 28.dp
                    )
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

            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play)) },
                leadingContent = { Icon(Icons.Default.PlayArrow, null) },
                modifier = Modifier.clickable(onClick = onPlay)
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_shuffle)) },
                leadingContent = { Icon(Icons.Default.Shuffle, null) },
                modifier = Modifier.clickable(onClick = onShuffle)
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
private fun ArtistArtwork(
    artist: Artist,
    modifier: Modifier,
    iconSize: Dp
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (artist.artworkUri != null) {
            SubcomposeAsyncImage(
                model = artist.artworkUri,
                contentDescription = artist.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                error = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
