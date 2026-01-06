package com.sukoon.music.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukoon.music.domain.model.Genre
import com.sukoon.music.ui.components.*
import com.sukoon.music.ui.viewmodel.GenresViewModel
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import kotlinx.coroutines.launch

/**
 * Genres Content - Displays genres in a list.
 * Genres Screen - Redesigned with Alphabet Scroll Bar, Selection Mode, and Search.
 *
 */
@Composable
private fun GenresContent(
    genres: List<Genre>,
    isSelectionMode: Boolean,
    selectedGenreIds: Set<Long>,
    onGenreClick: (Long) -> Unit,
    onGenreLongClick: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onSelectAllToggle: () -> Unit,
    onPlayGenre: (Long) -> Unit,
    onPlayNextGenre: (Long) -> Unit,
    onAddToQueue: (Long) -> Unit,
    onAddToPlaylist: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (genres.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No genres found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Genre Header with Selection Button
                if (!isSelectionMode) {
                    item {
                        GenreHeader(
                            genreCount = genres.size,
                            onSelectionClick = onSelectAllToggle
                        )
                    }
                    items(
                        items = genres,
                        key = { it.id }
                    ) { genre ->
                        GenreRow(
                            genre = genre,
                            isSelected = selectedGenreIds.contains(genre.id),
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) onToggleSelection(genre.id)
                                else onGenreClick(genre.id)
                            },
                            onSelectionToggle = onSelectAllToggle,
                            onPlayClick = { onPlayGenre(genre.id) },
                            onPlayNextClick = { onPlayNextGenre(genre.id) },
                            onAddToQueueClick = { onAddToQueue(genre.id) },
                            onAddToPlaylistClick = { onAddToPlaylist(genre.id) },
                            onDeleteClick = { /* Handled via selection mode */ }
                        )
                    }
                }
            }
        }
    }
}
    /**
     * Genres Screen - Redesigned with Alphabet Scroll Bar, Selection Mode, and Search.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GenresScreen(
        onNavigateToGenre: (Long) -> Unit,
        onBackClick: () -> Unit,
        viewModel: GenresViewModel = hiltViewModel(),
        playlistViewModel: PlaylistViewModel = hiltViewModel()
    ) {
        val genres by viewModel.genres.collectAsStateWithLifecycle()
        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
        val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
        val selectedGenreIds by viewModel.selectedGenreIds.collectAsStateWithLifecycle()
        val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

        val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()

        var showSearchBar by remember { mutableStateOf(false) }
        var genreForMore by remember { mutableStateOf<Genre?>(null) }
        var showAddToPlaylistDialog by remember { mutableStateOf<List<Long>?>(null) }
        var genresToDelete by remember { mutableStateOf<List<Genre>?>(null) }
        var genreToEdit by remember { mutableStateOf<Genre?>(null) }

        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val focusRequester = remember { FocusRequester() }

        // Pre-calculated letter-to-index map for fast scrolling
        val letterToIndexMap = remember(genres) {
            val map = mutableMapOf<String, Int>()
            genres.forEachIndexed { index, genre ->
                val firstChar = genre.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                val key = if (firstChar.firstOrNull()?.isLetter() == true) firstChar else "#"
                if (!map.containsKey(key)) {
                    map[key] = index
                }
            }
            map
        }

        // Active letter for Alphabet Scroll Bar highlight
        val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
        val activeLetter = remember(firstVisibleIndex, genres) {
            if (genres.isNotEmpty() && firstVisibleIndex < genres.size) {
                val firstChar = genres[firstVisibleIndex].name.firstOrNull()
                if (firstChar?.isLetter() == true) firstChar.uppercaseChar().toString() else "#"
            } else null
        }

        // Handle back press
        BackHandler(enabled = isSelectionMode || showSearchBar) {
            if (showSearchBar) {
                showSearchBar = false
                viewModel.setSearchQuery("")
            } else if (isSelectionMode) {
                viewModel.toggleSelectionMode(false)
            }
        }

        Scaffold(
            topBar = {
                Column {
                    if (showSearchBar) {
                        GenresSearchTopBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onCloseClick = {
                                showSearchBar = false
                                viewModel.setSearchQuery("")
                            },
                            focusRequester = focusRequester
                        )
                    } else {
                        TopAppBar(
                            title = { Text(if (isSelectionMode) "${selectedGenreIds.size} Selected" else "Genres") },
                            navigationIcon = {
                                IconButton(
                                    onClick = if (isSelectionMode) {
                                        { viewModel.toggleSelectionMode(false) }
                                    } else onBackClick) {
                                    Icon(
                                        imageVector = if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = if (isSelectionMode) "Clear selection" else "Back"
                                    )
                                }
                            },
                            actions = {
                                if (!isSelectionMode) {
                                    IconButton(onClick = { showSearchBar = true }) {
                                        Icon(Icons.Default.Search, contentDescription = "Search")
                                    }
                                    IconButton(onClick = { viewModel.toggleSelectionMode(true) }) {
                                        Icon(
                                            Icons.Default.Checklist,
                                            contentDescription = "Selection Mode"
                                        )
                                    }
                                } else {
                                    IconButton(onClick = { viewModel.selectAllGenres() }) {
                                        Icon(
                                            Icons.Default.SelectAll,
                                            contentDescription = "Select All"
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            },
            bottomBar = {
                Column {
                    // Bulk action bar in selection mode
                    if (isSelectionMode && selectedGenreIds.isNotEmpty()) {
                        GenreSelectionBottomBar(
                            selectedCount = selectedGenreIds.size,
                            onPlay = { viewModel.playSelectedGenres() },
                            onAddToPlaylist = {
                                showAddToPlaylistDialog = selectedGenreIds.toList()
                            },
                            onDelete = {
                                genresToDelete = genres.filter { it.id in selectedGenreIds }
                            },
                            onPlayNext = { viewModel.playSelectedNext() },
                            onAddToQueue = { viewModel.addSelectedToQueue() }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (genres.isEmpty()) {
                    EmptyGenresState(
                        isSearching = searchQuery.isNotEmpty(),
                        onScanClick = { viewModel.scanMediaLibrary() }
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (isSelectionMode) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedGenreIds.size == genres.size && genres.isNotEmpty()) {
                                            viewModel.clearSelection()
                                        } else {
                                            viewModel.selectAllGenres()
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedGenreIds.size == genres.size && genres.isNotEmpty(),
                                    onCheckedChange = {
                                        if (it) viewModel.selectAllGenres() else viewModel.clearSelection()
                                    }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Select all",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(
                                    items = genres,
                                    key = { it.id }
                                ) { genre ->
                                    GenreRow(
                                        genre = genre,
                                        isSelected = selectedGenreIds.contains(genre.id),
                                        isSelectionMode = isSelectionMode,
                                        onClick = {
                                            if (isSelectionMode) viewModel.toggleGenreSelection(
                                                genre.id
                                            )
                                            else onNavigateToGenre(genre.id)
                                        },
                                        onLongClick = {
                                            if (!isSelectionMode) {
                                                viewModel.toggleSelectionMode(true)
                                                viewModel.toggleGenreSelection(genre.id)
                                            }
                                        },
                                        onSelectionToggle = { viewModel.toggleGenreSelection(genre.id) },
                                        onPlayClick = { viewModel.playGenre(genre.id) },
                                        onPlayNextClick = { viewModel.playGenreNext(genre.id) },
                                        onAddToQueueClick = { viewModel.addGenreToQueue(genre.id) },
                                        onAddToPlaylistClick = {
                                            showAddToPlaylistDialog = listOf(genre.id)
                                        },
                                        onDeleteClick = { genresToDelete = listOf(genre) },
                                        onMoreClick = { genreForMore = it }
                                    )
                                }
                            }

                            // Alphabet Scroll Bar Overlay
                            if (!showSearchBar) {
                                AlphabetScrollBar(
                                    onLetterClick = { letter ->
                                        letterToIndexMap[letter]?.let { index ->
                                            scope.launch {
                                                listState.animateScrollToItem(index)
                                            }
                                        }
                                    },
                                    activeLetter = activeLetter,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Genre Context Menu
                if (genreForMore != null) {
                    GenreContextMenu(
                        genre = genreForMore!!,
                        onDismiss = { genreForMore = null },
                        onPlay = { viewModel.playGenre(genreForMore!!.id) },
                        onPlayNext = { viewModel.playGenreNext(genreForMore!!.id) },
                        onAddToQueue = { viewModel.addGenreToQueue(genreForMore!!.id) },
                        onAddToPlaylist = { showAddToPlaylistDialog = listOf(genreForMore!!.id) },
                        onEditTags = { genreToEdit = genreForMore },
                        onChangeCover = {
                            // TODO: Implement cover picker with gallery integration
                            genreForMore = null
                        },
                        onDelete = { genresToDelete = listOf(genreForMore!!) }
                    )
                }

                // Add to Playlist Dialog
                if (showAddToPlaylistDialog != null) {
                    AddToPlaylistDialog(
                        playlists = playlists,
                        onPlaylistSelected = { playlistId ->
                            if (isSelectionMode) {
                                viewModel.addSelectedToPlaylist(playlistId)
                            } else {
                                showAddToPlaylistDialog?.forEach { genreId ->
                                    if (!selectedGenreIds.contains(genreId)) {
                                        viewModel.toggleGenreSelection(genreId)
                                        viewModel.addSelectedToPlaylist(playlistId)
                                    } else {
                                        viewModel.addSelectedToPlaylist(playlistId)
                                    }
                                }
                            }
                            showAddToPlaylistDialog = null
                        },
                        onDismiss = { showAddToPlaylistDialog = null }
                    )
                }

                // Delete Confirmation
                if (genresToDelete != null) {
                    AlertDialog(
                        onDismissRequest = { genresToDelete = null },
                        title = {
                            val count = genresToDelete!!.size
                            Text("Delete ${if (count == 1) "Genre" else "$count Genres"}?")
                        },
                        text = { Text("Are you sure you want to delete the selected genre(s) and all their songs? This action cannot be undone.") },
                        confirmButton = {
                            TextButton(onClick = {
                                if (isSelectionMode) {
                                    viewModel.deleteSelectedGenres()
                                } else {
                                    // Single genre deletion logic
                                }
                                genresToDelete = null
                            }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { genresToDelete = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // Edit Tags Dialog
                if (genreToEdit != null) {
                    EditGenreTagsDialog(
                        currentGenreName = genreToEdit!!.name,
                        onDismiss = { genreToEdit = null },
                        onSave = { newName ->
                            viewModel.updateGenreName(genreToEdit!!.name, newName)
                            genreToEdit = null
                        }
                    )
                }
            }
        }

        // Auto-focus search field
        LaunchedEffect(showSearchBar) {
            if (showSearchBar) {
                focusRequester.requestFocus()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun GenresSearchTopBar(
        query: String,
        onQueryChange: (String) -> Unit,
        onCloseClick: () -> Unit,
        focusRequester: FocusRequester
    ) {
        TopAppBar(
            title = {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("Search genres...") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            },
            navigationIcon = {
                IconButton(onClick = onCloseClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Search")
                }
            },
            actions = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            }
        )
    }

    @Composable
    private fun EmptyGenresState(
        isSearching: Boolean,
        onScanClick: () -> Unit = {}
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No genres found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isSearching) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scan your media library to discover music by genre",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onScanClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan music")
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun GenreSelectionTopBar(
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
    fun GenreHeader(
        genreCount: Int,
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
                text = "$genreCount ${if (genreCount == 1) "genre" else "genres"}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onSelectionClick) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Select")
            }
        }
    }

