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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
fun ArtistsScreen(
    onNavigateToArtistDetail: (Long) -> Unit,
    onBackClick: () -> Unit,
    viewModel: ArtistsViewModel = hiltViewModel()
) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val recentlyPlayedArtists by viewModel.recentlyPlayedArtists.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedArtistIds by viewModel.selectedArtistIds.collectAsStateWithLifecycle()

    Scaffold(
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
                ArtistSelectionBottomBar(
                    onPlay = { viewModel.playSelectedArtists() },
                    onAddToPlaylist = {
                        // keep existing logic (if any)
                    },
                    onDelete = { viewModel.clearDeleteResult() }
                )
            }
        }
    ) { padding ->
        ArtistsContent(
            artists = artists,
            recentlyPlayedArtists = recentlyPlayedArtists,
            onArtistClick = onNavigateToArtistDetail,
            viewModel = viewModel,
            modifier = Modifier.padding(padding)
        )
    }
}

/**
 * Artists Content - Displays artists in a list view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistsContent(
    artists: List<Artist>,
    recentlyPlayedArtists: List<Artist>,
    onArtistClick: (Long) -> Unit,
    viewModel: ArtistsViewModel,
    modifier: Modifier = Modifier
) {
    var showMenuForArtist by remember { mutableStateOf<Artist?>(null) }
    var showSortDialog by remember { mutableStateOf(false) }

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val isAscending by viewModel.isAscending.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedArtistIds.collectAsStateWithLifecycle()

    if (artists.isEmpty() && searchQuery.isEmpty()) {
        EmptyArtistsContentState()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Search Bar in Selection Mode
            if (isSelectionMode) {
                item {
                    ArtistSearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) }
                    )
                }
                item {
                    SelectAllArtistsRow(
                        isAllSelected = selectedIds.size == artists.size && artists.isNotEmpty(),
                        onToggleSelectAll = {
                            if (selectedIds.size == artists.size) viewModel.clearSelection()
                            else viewModel.selectAllArtists()
                        }
                    )
                }
            }

            // Recently played section
            if (!isSelectionMode && recentlyPlayedArtists.isNotEmpty() && searchQuery.isEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Recently played",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
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
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp
                        )
                    }
                }
            }

            // Sort Header
            if (!isSelectionMode) {
                item {
                    ArtistSortHeader(
                        artistCount = artists.size,
                        onSortClick = { showSortDialog = true },
                        onSelectionClick = { viewModel.toggleSelectionMode(true) }
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
            .width(120.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
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
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${artist.albumCount} ${if (artist.albumCount == 1) "album" else "albums"} · ${artist.songCount} ${if (artist.songCount == 1) "song" else "songs"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() }
            )
        } else {
            IconButton(onClick = { onMenuClick() }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
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
    onSelectionClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$artistCount ${if (artistCount == 1) "artist" else "artists"}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row {
            IconButton(onClick = onSelectionClick) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Select")
            }
            IconButton(onClick = onSortClick) {
                Icon(Icons.Default.Sort, contentDescription = "Sort")
            }
        }
    }
}

@Composable
private fun ArtistSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search artists...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
private fun SelectAllArtistsRow(
    isAllSelected: Boolean,
    onToggleSelectAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelectAll)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isAllSelected) "Deselect all" else "Select all",
            style = MaterialTheme.typography.bodyLarge
        )
        Checkbox(
            checked = isAllSelected,
            onCheckedChange = null
        )
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
            TextButton(onClick = onDismiss) { Text("OK", color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
        title = { Text("Sort by") },
        text = {
            Column {
                SortOption(
                    text = "Artist name",
                    isSelected = currentSortMode == ArtistSortMode.ARTIST_NAME,
                    onClick = { onSortModeChange(ArtistSortMode.ARTIST_NAME) }
                )
                SortOption(
                    text = "Number of albums",
                    isSelected = currentSortMode == ArtistSortMode.ALBUM_COUNT,
                    onClick = { onSortModeChange(ArtistSortMode.ALBUM_COUNT) }
                )
                SortOption(
                    text = "Number of songs",
                    isSelected = currentSortMode == ArtistSortMode.SONG_COUNT,
                    onClick = { onSortModeChange(ArtistSortMode.SONG_COUNT) }
                )
                SortOption(
                    text = "Random",
                    isSelected = currentSortMode == ArtistSortMode.RANDOM,
                    onClick = { onSortModeChange(ArtistSortMode.RANDOM) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SortOption(
                    text = "From A to Z",
                    isSelected = isAscending,
                    onClick = { onOrderChange(true) }
                )
                SortOption(
                    text = "From Z to A",
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
            .padding(32.dp),
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
            text = "No artists found",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Scan your library to discover artists",
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${artist.albumCount} albums · ${artist.songCount} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Menu items
            ListItem(
                headlineContent = { Text("Play") },
                leadingContent = { Icon(Icons.Default.PlayArrow, null) },
                modifier = Modifier.clickable(onClick = onPlay)
            )
            ListItem(
                headlineContent = { Text("Shuffle") },
                leadingContent = { Icon(Icons.Default.Shuffle, null) },
                modifier = Modifier.clickable(onClick = onShuffle)
            )
            ListItem(
                headlineContent = { Text("Play next") },
                leadingContent = { Icon(Icons.Default.SkipNext, null) },
                modifier = Modifier.clickable(onClick = onPlayNext)
            )
            ListItem(
                headlineContent = { Text("Add to queue") },
                leadingContent = { Icon(Icons.Default.PlaylistAdd, null) },
                modifier = Modifier.clickable(onClick = onAddToQueue)
            )
            ListItem(
                headlineContent = { Text("Add to playlist") },
                leadingContent = { Icon(Icons.Default.Add, null) },
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
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            SelectionBottomBarItem(icon = Icons.Default.PlayArrow, label = "Play", onClick = onPlay)
            SelectionBottomBarItem(icon = Icons.Default.PlaylistAdd, label = "Add to play", onClick = onAddToPlaylist)
            SelectionBottomBarItem(icon = Icons.Default.Delete, label = "Delete", onClick = onDelete)
            /*SelectionBottomBarItem(icon = Icons.Default.MoreVert, label = "More", onClick = onMore)*/
        }
    }
}
