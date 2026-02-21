package com.sukoon.music.ui.screen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Genre
import com.sukoon.music.ui.components.AddToPlaylistDialog
import com.sukoon.music.ui.components.AlphabetScrollBar
import com.sukoon.music.ui.components.EditGenreTagsDialog
import com.sukoon.music.ui.components.GenreContextMenu
import com.sukoon.music.ui.components.GenreRow
import com.sukoon.music.ui.components.MultiSelectActionBottomBar
import com.sukoon.music.ui.theme.ContentBottomPadding
import com.sukoon.music.ui.theme.ContentTopPadding
import com.sukoon.music.ui.theme.MiniPlayerHeight
import com.sukoon.music.ui.theme.SpacingSmall
import com.sukoon.music.ui.theme.accent
import com.sukoon.music.ui.viewmodel.GenreSortMode
import com.sukoon.music.ui.viewmodel.GenresViewModel
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GenresScreen(
    onNavigateToGenre: (Long) -> Unit,
    onBackClick: () -> Unit,
    onNavigateToGenreSelection: () -> Unit = {},
    viewModel: GenresViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedGenreIds by viewModel.selectedGenreIds.collectAsStateWithLifecycle()
    val deleteResult by viewModel.deleteResult.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val isAscending by viewModel.isAscending.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()

    var showSortDialog by remember { mutableStateOf(false) }
    var genreForMore by remember { mutableStateOf<Genre?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf<List<Long>?>(null) }
    var genresToDelete by remember { mutableStateOf<List<Genre>?>(null) }
    var genreToEdit by remember { mutableStateOf<Genre?>(null) }
    var genresPendingDeletion by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(
                context,
                context.getString(com.sukoon.music.R.string.toast_genre_deleted_successfully),
                Toast.LENGTH_SHORT
            ).show()
            genresPendingDeletion = false
            genresToDelete = null
        } else {
            Toast.makeText(
                context,
                context.getString(com.sukoon.music.R.string.toast_delete_cancelled),
                Toast.LENGTH_SHORT
            ).show()
        }
        genresPendingDeletion = false
    }

    LaunchedEffect(deleteResult) {
        when (val result = deleteResult) {
            is DeleteHelper.DeleteResult.RequiresPermission -> {
                deleteLauncher.launch(IntentSenderRequest.Builder(result.intentSender).build())
            }

            is DeleteHelper.DeleteResult.Success -> {
                if (genresPendingDeletion) {
                    Toast.makeText(
                        context,
                        context.getString(com.sukoon.music.R.string.toast_genre_deleted_successfully),
                        Toast.LENGTH_SHORT
                    ).show()
                    genresPendingDeletion = false
                    genresToDelete = null
                    viewModel.clearDeleteResult()
                }
            }

            is DeleteHelper.DeleteResult.Error -> {
                Toast.makeText(
                    context,
                    context.getString(com.sukoon.music.R.string.toast_error_with_message, result.message),
                    Toast.LENGTH_SHORT
                ).show()
                genresPendingDeletion = false
                viewModel.clearDeleteResult()
            }

            null -> Unit
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

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

    val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val activeLetter = remember(firstVisibleIndex, genres) {
        if (genres.isNotEmpty() && firstVisibleIndex < genres.size) {
            val firstChar = genres[firstVisibleIndex].name.firstOrNull()
            if (firstChar?.isLetter() == true) firstChar.uppercaseChar().toString() else "#"
        } else {
            null
        }
    }

    BackHandler(enabled = isSelectionMode) {
        viewModel.toggleSelectionMode(false)
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
                                selectedGenreIds.size
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleSelectionMode(false) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_exit_selection)
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.home_tab_genres))
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
                .then(if (isSelectionMode) Modifier.padding(paddingValues) else Modifier)
        ) {
            if (genres.isEmpty()) {
                EmptyGenresState(onScanClick = { viewModel.scanMediaLibrary() })
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = MiniPlayerHeight + SpacingSmall)
                    ) {
                        if (!isSelectionMode) {
                            stickyHeader(key = "header") {
                                GenreSortHeader(
                                    genreCount = genres.size,
                                    onSortClick = { showSortDialog = true },
                                    onSelectionClick = onNavigateToGenreSelection,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background)
                                )
                            }
                        }

                        items(
                            items = genres,
                            key = { it.id },
                            contentType = { "genre_row" }
                        ) { genre ->
                            GenreRow(
                                genre = genre,
                                isSelected = selectedGenreIds.contains(genre.id),
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) viewModel.toggleGenreSelection(genre.id)
                                    else onNavigateToGenre(genre.id)
                                },
                                onSelectionToggle = { viewModel.toggleGenreSelection(genre.id) },
                                onPlayClick = { viewModel.playGenre(genre.id) },
                                onPlayNextClick = { viewModel.playGenreNext(genre.id) },
                                onAddToQueueClick = { viewModel.addGenreToQueue(genre.id) },
                                onAddToPlaylistClick = { showAddToPlaylistDialog = listOf(genre.id) },
                                onDeleteClick = {
                                    genresToDelete = listOf(genre)
                                    genresPendingDeletion = true
                                },
                                onMoreClick = { genreForMore = it }
                            )
                        }
                    }

                    AlphabetScrollBar(
                        onLetterClick = { letter ->
                            letterToIndexMap[letter]?.let { index ->
                                scope.launch { listState.animateScrollToItem(index) }
                            }
                        },
                        activeLetter = activeLetter,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                    )
                }
            }

            genreForMore?.let { currentGenre ->
                GenreContextMenu(
                    genre = currentGenre,
                    onDismiss = { genreForMore = null },
                    onPlay = { viewModel.playGenre(currentGenre.id) },
                    onPlayNext = { viewModel.playGenreNext(currentGenre.id) },
                    onAddToQueue = { viewModel.addGenreToQueue(currentGenre.id) },
                    onAddToPlaylist = { showAddToPlaylistDialog = listOf(currentGenre.id) },
                    onEditTags = { genreToEdit = currentGenre },
                    onChangeCover = { genreForMore = null },
                    onDelete = {
                        genresToDelete = listOf(currentGenre)
                        genresPendingDeletion = true
                        genreForMore = null
                    }
                )
            }

            if (showAddToPlaylistDialog != null) {
                AddToPlaylistDialog(
                    playlists = playlists,
                    onPlaylistSelected = { playlistId ->
                        if (isSelectionMode) {
                            viewModel.addSelectedToPlaylist(playlistId)
                        } else {
                            val targetIds = showAddToPlaylistDialog.orEmpty()
                            viewModel.clearSelection()
                            targetIds.forEach { id -> viewModel.toggleGenreSelection(id) }
                            viewModel.addSelectedToPlaylist(playlistId)
                        }
                        showAddToPlaylistDialog = null
                        Toast.makeText(
                            context,
                            context.getString(com.sukoon.music.R.string.toast_songs_added_to_playlist),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onDismiss = { showAddToPlaylistDialog = null }
                )
            }

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
                        Text(
                            if (count == 1) {
                                androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_genres_delete_title_single)
                            } else {
                                androidx.compose.ui.res.stringResource(
                                    com.sukoon.music.R.string.library_genres_delete_title_multiple,
                                    count
                                )
                            }
                        )
                    },
                    text = {
                        Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_delete_genre_songs_message))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                genresToDelete?.let { targetGenres ->
                                    viewModel.deleteGenres(targetGenres.map { it.id })
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_delete))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            genresPendingDeletion = false
                            genresToDelete = null
                        }) {
                            Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_cancel))
                        }
                    }
                )
            }

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

            if (showSortDialog) {
                GenreSortDialog(
                    currentSortMode = sortMode,
                    isAscending = isAscending,
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
    onScanClick: () -> Unit
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
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_genres_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_genres_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onScanClick) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_scan_music))
        }
    }
}

@Composable
private fun GenreSortHeader(
    genreCount: Int,
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
                    com.sukoon.music.R.string.library_genres_header_count_format,
                    genreCount,
                    androidx.compose.ui.res.stringResource(
                        if (genreCount == 1) com.sukoon.music.R.string.library_genres_word_genre_singular
                        else com.sukoon.music.R.string.library_genres_word_genre_plural
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
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_genres_sort_genre_name),
                    selected = currentSortMode == GenreSortMode.NAME,
                    onClick = { onSortModeChange(GenreSortMode.NAME) }
                )
                SortOptionRow(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_genres_sort_number_of_songs),
                    selected = currentSortMode == GenreSortMode.SONG_COUNT,
                    onClick = { onSortModeChange(GenreSortMode.SONG_COUNT) }
                )
                SortOptionRow(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_common_random),
                    selected = currentSortMode == GenreSortMode.RANDOM,
                    onClick = { onSortModeChange(GenreSortMode.RANDOM) }
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
