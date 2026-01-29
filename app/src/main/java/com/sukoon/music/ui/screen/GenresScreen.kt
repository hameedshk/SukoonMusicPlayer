package com.sukoon.music.ui.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Genre
import com.sukoon.music.ui.components.*
import com.sukoon.music.ui.components.PlaceholderAlbumArt
import com.sukoon.music.ui.viewmodel.GenreSortMode
import com.sukoon.music.ui.viewmodel.GenresViewModel
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import kotlinx.coroutines.launch
import com.sukoon.music.ui.theme.*

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
        val deleteResult by viewModel.deleteResult.collectAsStateWithLifecycle()

        val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()

        var showSortDialog by remember { mutableStateOf(false) }
        var genreForMore by remember { mutableStateOf<Genre?>(null) }
        var showAddToPlaylistDialog by remember { mutableStateOf<List<Long>?>(null) }
        var genresToDelete by remember { mutableStateOf<List<Genre>?>(null) }
        var genreToEdit by remember { mutableStateOf<Genre?>(null) }
        var genresPendingDeletion by remember { mutableStateOf(false) }

        val context = LocalContext.current

        // Delete launcher for handling file deletion permissions
        val deleteLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                Toast.makeText(context, "Genres deleted successfully", Toast.LENGTH_SHORT).show()
                genresPendingDeletion = false
            } else {
                Toast.makeText(context, "Delete cancelled", Toast.LENGTH_SHORT).show()
            }
            genresPendingDeletion = false
        }

        // Handle delete result
        LaunchedEffect(deleteResult) {
            when (val result = deleteResult) {
                is DeleteHelper.DeleteResult.RequiresPermission -> {
                    deleteLauncher.launch(
                        IntentSenderRequest.Builder(result.intentSender).build()
                    )
                }
                is DeleteHelper.DeleteResult.Success -> {
                    if (genresPendingDeletion) {
                        Toast.makeText(context, "Genres deleted successfully", Toast.LENGTH_SHORT).show()
                        genresPendingDeletion = false
                        viewModel.clearDeleteResult()
                    }
                }
                is DeleteHelper.DeleteResult.Error -> {
                    Toast.makeText(context, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                    genresPendingDeletion = false
                    viewModel.clearDeleteResult()
                }
                null -> {}
            }
        }

        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

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
        BackHandler(enabled = isSelectionMode) {
            if (isSelectionMode) {
                viewModel.toggleSelectionMode(false)
            }
        }

        Scaffold(
            topBar = {
                if (isSelectionMode) {
                    TopAppBar(
                        title = { Text("${selectedGenreIds.size} selected") },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.toggleSelectionMode(false) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                if (isSelectionMode && selectedGenreIds.isNotEmpty()) {
                    MultiSelectActionBottomBar(
                        onPlay = { viewModel.playSelectedGenres() },
                        onAddToPlaylist = {
                            showAddToPlaylistDialog = selectedGenreIds.toList()
                        },
                        onDelete = {
                            genresToDelete = genres.filter { it.id in selectedGenreIds }
                            genresPendingDeletion = true
                        },
                        onPlayNext = { viewModel.playSelectedNext() },
                        onAddToQueue = { viewModel.addSelectedToQueue() }
                    )
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
                        isSearching = false,
                        onScanClick = { viewModel.scanMediaLibrary() }
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                // Sort Header / Selection Header (sticky)
                                stickyHeader(key = "header") {
                                    GenreSortHeader(
                                        genreCount = genres.size,
                                        isSelectionMode = isSelectionMode,
                                        selectedCount = selectedGenreIds.size,
                                        onSortClick = { showSortDialog = true },
                                        onSelectionClick = { viewModel.toggleSelectionMode(true) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.background)
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
                                            if (isSelectionMode) viewModel.toggleGenreSelection(
                                                genre.id
                                            )
                                            else onNavigateToGenre(genre.id)
                                        },
                                        onSelectionToggle = { viewModel.toggleGenreSelection(genre.id) },
                                        onPlayClick = { viewModel.playGenre(genre.id) },
                                        onPlayNextClick = { viewModel.playGenreNext(genre.id) },
                                        onAddToQueueClick = { viewModel.addGenreToQueue(genre.id) },
                                        onAddToPlaylistClick = {
                                            showAddToPlaylistDialog = listOf(genre.id)
                                        },
                                        onDeleteClick = {
                                            genresToDelete = listOf(genre)
                                            genresPendingDeletion = true
                                        },
                                        onMoreClick = { genreForMore = it }
                                    )
                                }
                            }

                            // Alphabet Scroll Bar Overlay
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
                        onDelete = {
                            genresToDelete = listOf(genreForMore!!)
                            genresPendingDeletion = true
                            genreForMore = null
                        }
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

                // Delete Confirmation Dialog
                if (genresPendingDeletion && genresToDelete != null) {
                    AlertDialog(
                        onDismissRequest = {
                            genresPendingDeletion = false
                            genresToDelete = null
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        title = {
                            val count = genresToDelete!!.size
                            Text("Delete ${if (count == 1) "Genre" else "$count Genres"}?")
                        },
                        text = {
                            Text("All songs in these genres will be permanently deleted from your device. This cannot be undone.")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    genresToDelete?.let { genres ->
                                        viewModel.deleteGenres(genres.map { it.id })
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                genresPendingDeletion = false
                                genresToDelete = null
                            }) {
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

                // Sort Dialog
                if (showSortDialog) {
                    GenreSortDialog(
                        currentSortMode = viewModel.sortMode.collectAsStateWithLifecycle().value,
                        isAscending = viewModel.isAscending.collectAsStateWithLifecycle().value,
                        onDismiss = { showSortDialog = false },
                        onSortModeChange = { viewModel.setSortMode(it) },
                        onOrderChange = { viewModel.setAscending(it) }
                    )
                }
            }
        }
    }

    @Composable
    private fun EmptyGenresState(
        isSearching: Boolean,
        onScanClick: () -> Unit = {}
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(
                    top = ContentTopPadding,
                    bottom = ContentBottomPadding + 16.dp
                ),
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
                    style = MaterialTheme.typography.emptyStateTitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isSearching) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scan your media library to discover music by genre",
                        style = MaterialTheme.typography.emptyStateDescription,
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
                text = "$genreCount ${if (genreCount == 1) "genre" else "genres"}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onSelectionClick) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Select")
            }
        }
    }

    @Composable
    private fun GenreSortHeader(
        genreCount: Int,
        isSelectionMode: Boolean = false,
        selectedCount: Int = 0,
        onSortClick: () -> Unit,
        onSelectionClick: () -> Unit = {},
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSelectionMode) {
                    "$selectedCount selected"
                } else {
                    "$genreCount ${if (genreCount == 1) "genre" else "genres"}"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isSelectionMode) {
                // No select all/none buttons - selection is done via individual rows
            } else {
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
    }

    @Composable
    private fun GenreSortDialog(
        currentSortMode: GenreSortMode,
        isAscending: Boolean,
        onDismiss: () -> Unit,
        onSortModeChange: (GenreSortMode) -> Unit,
        onOrderChange: (Boolean) -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("CANCEL") }
            },
            title = { Text("Sort by") },
            text = {
                Column {
                    SortOption(
                        text = "Genre name",
                        isSelected = currentSortMode == GenreSortMode.NAME,
                        onClick = { onSortModeChange(GenreSortMode.NAME) }
                    )
                    SortOption(
                        text = "Number of songs",
                        isSelected = currentSortMode == GenreSortMode.SONG_COUNT,
                        onClick = { onSortModeChange(GenreSortMode.SONG_COUNT) }
                    )
                    SortOption(
                        text = "Random",
                        isSelected = currentSortMode == GenreSortMode.RANDOM,
                        onClick = { onSortModeChange(GenreSortMode.RANDOM) }
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

