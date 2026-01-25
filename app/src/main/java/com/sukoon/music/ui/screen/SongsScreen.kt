package com.sukoon.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.*
import com.sukoon.music.ui.theme.SpacingLarge
import com.sukoon.music.ui.theme.SpacingMedium
import com.sukoon.music.ui.viewmodel.HomeViewModel
import com.sukoon.music.util.DevLogger
import com.sukoon.music.ui.theme.*

/**
 * Songs Screen - Displays all songs in alphabetical order with search functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    onBackClick: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToArtist: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    playlistViewModel: com.sukoon.music.ui.viewmodel.PlaylistViewModel = hiltViewModel()
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()
    val selectedSongIds by viewModel.selectedSongIds.collectAsStateWithLifecycle()
    val isSongSelectionMode by viewModel.isSongSelectionMode.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showInfoForSong by remember { mutableStateOf<Song?>(null) }
    var sortOrder by remember { mutableStateOf(false) } // false = A-Z, true = Z-A
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    // Share handler
    val shareHandler = rememberShareHandler()

    // Create menu handler for song context menu
    val menuHandler = rememberSongMenuHandler(
        playbackRepository = viewModel.playbackRepository,
        onNavigateToAlbum = onNavigateToAlbum,
        onNavigateToArtist = onNavigateToArtist,
        onShowPlaylistSelector = { song ->
            songToAddToPlaylist = song
            showAddToPlaylistDialog = true
        },
        onShowSongInfo = { song -> showInfoForSong = song },
        onToggleLike = { songId, isLiked -> viewModel.toggleLike(songId, isLiked) },
        onShare = shareHandler
    )

    // Sort songs alphabetically and filter by search query
    val filteredSongs = remember(songs, searchQuery, sortOrder) {
        val filtered = songs
            .filter { song ->
                searchQuery.isEmpty() ||
                song.title.contains(searchQuery, ignoreCase = true) ||
                song.artist.contains(searchQuery, ignoreCase = true) ||
                song.album.contains(searchQuery, ignoreCase = true)
            }

        if (sortOrder) {
            filtered.sortedByDescending { it.title.lowercase() }
        } else {
            filtered.sortedBy { it.title.lowercase() }
        }
    }

    // Get available letters from filtered songs
    val availableLetters = remember(filteredSongs) {
        filteredSongs
            .mapNotNull { it.title.firstOrNull()?.uppercaseChar() }
            .distinct()
            .sorted()
    }

    val lazyListState = rememberLazyListState()

    if (filteredSongs.isEmpty()) {
        // Empty state
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
                    text = if (searchQuery.isEmpty()) "No songs found" else "No results for \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        // Songs List with Alphabetical Index
        Scaffold(
            topBar = {
                if (isSongSelectionMode) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "${selectedSongIds.size} selected",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                viewModel.toggleSongSelectionMode(false)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Exit selection"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            },
            bottomBar = {
                if (isSongSelectionMode && selectedSongIds.isNotEmpty()) {
                    MultiSelectActionBottomBar(
                        onPlay = { viewModel.playSelectedSongs() },
                        onAddToPlaylist = { songToAddToPlaylist = null },
                        onDelete = { },
                        onPlayNext = { viewModel.playSelectedSongsNext() },
                        onAddToQueue = { viewModel.addSelectedSongsToQueue() }
                    )
                }
            }
        ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding() + ContentTopPadding,
                    bottom = paddingValues.calculateBottomPadding() + ContentBottomPadding + 8.dp,
                    end = 24.dp,
                    start = 0.dp
                ),
                state = lazyListState
            ) {
                // Header: Back button, Title (non-scrolling)
                stickyHeader(key = "header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        // Top App Bar
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Songs",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = onBackClick) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        // Search Bar
                        if (!isSongSelectionMode) {
                            SearchBar(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                onClearQuery = { searchQuery = "" }
                            )
                        }

                        // Song Count Header with Sort and Selection buttons
                        if (!isSongSelectionMode) {
                            SongSortHeader(
                                songCount = filteredSongs.size,
                                onSortClick = { sortOrder = !sortOrder },
                                onSelectionClick = { viewModel.toggleSongSelectionMode(true) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                            )
                        }
                    }
                }

                // Play All and Shuffle buttons
                if (filteredSongs.isNotEmpty()) {
                    item {
                        SongsPlayControlButtons(
                            onPlayAll = { viewModel.playAll() },
                            onShuffleAll = { viewModel.shuffleAll() },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Song items
                items(
                    items = filteredSongs,
                    key = { it.id }
                ) { song ->
                    val isPlaying = playbackState.currentSong?.id == song.id && playbackState.isPlaying
                    val index = songs.indexOf(song)
                    val isSelected = song.id in selectedSongIds

                    SongListItem(
                        song = song,
                        isPlaying = isPlaying,
                        isSelected = isSelected,
                        isSelectionMode = isSongSelectionMode,
                        menuHandler = menuHandler,
                        onClick = {
                            if (isSongSelectionMode) {
                                viewModel.toggleSongSelection(song.id)
                            } else {
                                viewModel.playQueue(songs, index)
                            }
                        },
                        onCheckboxClick = {
                            viewModel.toggleSongSelection(song.id)
                        },
                        onLongPress = {
                            viewModel.toggleSongSelectionMode(true)
                            viewModel.toggleSongSelection(song.id)
                        },
                        onLikeClick = {
                            viewModel.toggleLike(song.id, song.isLiked)
                        }
                    )
                }
            }

            // Alphabetical Index on the Right (overlaid, doesn't scroll)
            if (availableLetters.isNotEmpty()) {
                AlphabeticalIndex(
                    letters = availableLetters,
                    songs = filteredSongs,
                    lazyListState = lazyListState,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
        }
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
}

/**
 * Search bar component for filtering songs.
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingLarge, vertical = SpacingMedium),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingLarge, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Search songs, artists, albums...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                singleLine = true
            )

            if (query.isNotEmpty()) {
                IconButton(onClick = onClearQuery) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search"
                    )
                }
            }
        }
    }
}

/**
 * Individual song item in the list.
 */
@Composable
private fun SongListItem(
    song: Song,
    isPlaying: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    menuHandler: SongMenuHandler,
    onClick: () -> Unit,
    onCheckboxClick: () -> Unit,
    onLongPress: () -> Unit,
    onLikeClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() }
                )
            },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingLarge, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            // Album Art
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                SubcomposeAsyncImage(
                    model = song.albumArtUri,
                    contentDescription = "Album art for ${song.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }

            // Song details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration
            Text(
                text = song.durationFormatted(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )

            // Like button or Checkbox
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Deselect" else "Select",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onCheckboxClick() }
                        .padding(end = 8.dp)
                )
            } else {
                IconButton(onClick = onLikeClick) {
                    Icon(
                        imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (song.isLiked) "Unlike" else "Like",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // More options menu (only when not in selection mode)
            if (!isSelectionMode) {
                IconButton(onClick = {
                    showMenu = true
                    DevLogger.click("SongMenu", "More options for: ${song.title}", handled = true)
                }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showMenu) {
        SongContextMenu(
            song = song,
            menuHandler = menuHandler,
            onDismiss = { showMenu = false }
        )
    }
}

/**
 * Play All and Shuffle buttons for Songs Screen.
 */
@Composable
private fun SongsPlayControlButtons(
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onPlayAll,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.PlayArrow, "Play All", Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Play All")
        }

        OutlinedButton(
            onClick = onShuffleAll,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Shuffle, "Shuffle", Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Shuffle")
        }
    }
}

/**
 * Sort header for Songs Screen showing song count and action buttons.
 */
@Composable
private fun SongSortHeader(
    songCount: Int,
    onSortClick: () -> Unit,
    onSelectionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$songCount ${if (songCount == 1) "song" else "songs"}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row {
            IconButton(onClick = onSortClick) {
                Icon(Icons.Default.Sort, contentDescription = "Sort")
            }
            IconButton(onClick = onSelectionClick) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Select")
            }
        }
    }
}

/**
 * Alphabetical index for quick navigation through song list.
 * Displays A-Z letters on the right side for fast scrolling.
 */
@Composable
private fun AlphabeticalIndex(
    letters: List<Char>,
    songs: List<Song>,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedLetter by remember { mutableStateOf<Char?>(null) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 16.dp, horizontal = 2.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach { letter ->
            val isSelected = selectedLetter == letter

            Text(
                text = letter.toString(),
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        selectedLetter = letter
                        coroutineScope.launch {
                            val index = songs.indexOfFirst {
                                it.title.firstOrNull()?.uppercaseChar() == letter
                            }
                            if (index >= 0) {
                                lazyListState.scrollToItem(index)
                            }
                        }
                    }
                    .padding(2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
