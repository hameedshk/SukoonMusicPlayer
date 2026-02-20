package com.sukoon.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.automirrored.filled.Sort
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.SearchHistory
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.model.SortMode
import com.sukoon.music.ui.components.*
import com.sukoon.music.ui.components.ModernSearchBar
import com.sukoon.music.ui.components.HighlightedText
import com.sukoon.music.ui.components.PlaceholderAlbumArt
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.SearchViewModel
import com.sukoon.music.ui.theme.*

/**
 * Enhanced Search Screen - Search and filter local music library with history and sorting.
 */
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onNavigateToNowPlaying: () -> Unit = {},
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToArtist: (Long) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val showLikedOnly by viewModel.showLikedOnly.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    var songToDelete by rememberSaveable { mutableStateOf<Song?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Delete result launcher
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_song_deleted_successfully), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_delete_cancelled), Toast.LENGTH_SHORT).show()
        }
        songToDelete = null
        isDeleting = false
    }

    val menuHandler = rememberSongMenuHandler(
        playbackRepository = viewModel.playbackRepository,
        onNavigateToAlbum = onNavigateToAlbum,
        onNavigateToArtist = onNavigateToArtist,
        onShowDeleteConfirmation = { song ->
            if (!isDeleting) songToDelete = song
        },
        onToggleLike = { songId, isLiked ->
            viewModel.toggleLike(songId, isLiked)
        }
    )

    Scaffold(
        topBar = {
            // Minimal top bar that just holds the search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                ModernSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onClearClick = { viewModel.updateSearchQuery("") },
                    onBackClick = onBackClick,
                    onSearchAction = { viewModel.saveSearchToHistory() }
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search History (shown when query is blank)
            if (searchQuery.isBlank() && searchHistory.isNotEmpty()) {
                SearchHistorySection(
                    history = searchHistory,
                    onHistoryClick = { query ->
                        viewModel.applySearchFromHistory(query)
                    },
                    onDeleteClick = { query ->
                        viewModel.deleteSearchHistory(query)
                    },
                    onClearAllClick = {
                        viewModel.clearAllHistory()
                    }
                )
            }

            // Filter & Sort Controls (shown when there's a query)
            if (searchQuery.isNotBlank()) {
                FilterAndSortSection(
                    showLikedOnly = showLikedOnly,
                    sortMode = sortMode,
                    onToggleLikedFilter = { viewModel.toggleLikedFilter() },
                    onSortClick = { showSortMenu = true }
                )

                // Sort Mode Menu
                SortModeMenu(
                    expanded = showSortMenu,
                    currentMode = sortMode,
                    onDismiss = { showSortMenu = false },
                    onModeSelect = { mode ->
                        viewModel.updateSortMode(mode)
                        showSortMenu = false
                    }
                )
            }

            // Search Results or Empty States
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = Triple(searchQuery.isBlank(), searchHistory.isEmpty(), searchResults.isEmpty()),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "SearchContent"
                ) { (isQueryBlank, isHistoryEmpty, isResultsEmpty) ->
                    when {
                        isQueryBlank && isHistoryEmpty -> {
                            InitialSearchState()
                        }
                        isQueryBlank -> {
                            InitialSearchStateWithHistory()
                        }
                        isResultsEmpty -> {
                            NoResultsState(query = searchQuery)
                        }
                        else -> {
                            SearchResultsContent(
                                songs = searchResults,
                                searchQuery = searchQuery,
                                currentSongId = playbackState.currentSong?.id,
                                isPlayingGlobally = playbackState.isPlaying,
                                menuHandler = menuHandler,
                                onSongClick = { song ->
                                    if (playbackState.currentSong?.id != song.id) {
                                        viewModel.playSong(song)
                                    } else {
                                        onNavigateToNowPlaying()
                                    }
                                },
                                onLikeClick = { song -> viewModel.toggleLike(song.id, song.isLiked) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    songToDelete?.let { song ->
        DeleteConfirmationDialog(
            song = song,
            onConfirm = {
                if (!isDeleting) {
                    isDeleting = true

                    when (val result = menuHandler.performDelete(song)) {
                        is DeleteHelper.DeleteResult.RequiresPermission -> {
                            deleteLauncher.launch(
                                IntentSenderRequest.Builder(result.intentSender).build()
                            )
                            songToDelete = null
                            isDeleting = false
                        }
                        is DeleteHelper.DeleteResult.Success -> {
                            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_song_deleted_successfully), Toast.LENGTH_SHORT).show()
                            songToDelete = null
                            isDeleting = false
                        }
                        is DeleteHelper.DeleteResult.Error -> {
                            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_error_with_message, result.message), Toast.LENGTH_SHORT).show()
                            songToDelete = null
                            isDeleting = false
                        }
                    }
                }
            },
            onDismiss = { songToDelete = null }
        )
    }
}




@Composable
private fun SearchHistorySection(
    history: List<SearchHistory>,
    onHistoryClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onClearAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Header with "Clear all" button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.search_recent_searches),
                style = MaterialTheme.typography.sectionHeader,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onClearAllClick) {
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_clear_all))
            }
        }

        // History chips (horizontal scrollable)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = history,
                key = { it.query }
            ) { searchHistory ->
                SearchHistoryChip(
                    query = searchHistory.query,
                    onClick = { onHistoryClick(searchHistory.query) },
                    onDeleteClick = { onDeleteClick(searchHistory.query) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun SearchHistoryChip(
    query: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(query, maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_remove),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        },
        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun FilterAndSortSection(
    showLikedOnly: Boolean,
    sortMode: SortMode,
    onToggleLikedFilter: () -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Liked Only Filter Chip
        FilterChip(
            selected = showLikedOnly,
            onClick = onToggleLikedFilter,
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (showLikedOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_liked_only))
                }
            },
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = if (!showLikedOnly) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary,
                labelColor = if (!showLikedOnly) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // Sort Mode Chip
        FilterChip(
            selected = true,
            onClick = onSortClick,
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(getSortModeLabel(sortMode))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun SortModeMenu(
    expanded: Boolean,
    currentMode: SortMode,
    onDismiss: () -> Unit,
    onModeSelect: (SortMode) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        SortMode.values().forEach { mode ->
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(getSortModeLabel(mode))
                        if (mode == currentMode) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                onClick = { onModeSelect(mode) }
            )
        }
    }
}

@Composable
private fun getSortModeLabel(mode: SortMode): String {
    return when (mode) {
        SortMode.RELEVANCE -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.search_sort_relevance)
        SortMode.TITLE -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.search_sort_title_asc)
        SortMode.ARTIST -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.search_sort_artist_asc)
        SortMode.DATE_ADDED -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.search_sort_date_added)
    }
}

@Composable
private fun SearchResultsContent(
    songs: List<Song>,
    searchQuery: String,
    currentSongId: Long?,
    isPlayingGlobally: Boolean,
    menuHandler: SongMenuHandler,
    onSongClick: (Song) -> Unit,
    onLikeClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Results header
        item {
            Text(
                text = androidx.compose.ui.res.pluralStringResource(
                    com.sukoon.music.R.plurals.common_result_count,
                    songs.size,
                    songs.size
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
        }

        // Song List
        items(
            items = songs,
            key = { it.id }
        ) { song ->
            val isCurrentSong = song.id == currentSongId
            SearchResultItem(
                song = song,
                searchQuery = searchQuery,
                isCurrentSong = isCurrentSong,
                isPlaybackActive = isCurrentSong && isPlayingGlobally,
                menuHandler = menuHandler,
                onClick = { onSongClick(song) },
                onLikeClick = { onLikeClick(song) }
            )
        }
    }
}

@Composable
private fun SearchResultItem(
    song: Song,
    searchQuery: String,
    isCurrentSong: Boolean,
    isPlaybackActive: Boolean,
    menuHandler: SongMenuHandler,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            Card(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                if (song.albumArtUri != null) {
                    SubcomposeAsyncImage(
                        model = song.albumArtUri,
                        contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_album_art),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = {
                             PlaceholderAlbumArt.Placeholder(
                                seed = PlaceholderAlbumArt.generateSeed(song.album, song.artist, song.id)
                            )
                        }
                    )
                } else {
                     PlaceholderAlbumArt.Placeholder(
                        seed = PlaceholderAlbumArt.generateSeed(song.album, song.artist, song.id)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Song Info with Highlighting
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        HighlightedText(
                            text = song.title,
                            query = searchQuery,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (isCurrentSong) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AnimatedEqualizer(
                            isAnimating = isPlaybackActive,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                     HighlightedText(
                        text = song.artist,
                        query = searchQuery,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.search_result_separator),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HighlightedText(
                        text = song.album,
                        query = searchQuery,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Like Button
            IconButton(onClick = onLikeClick) {
                AnimatedFavoriteIcon(
                    isLiked = song.isLiked,
                    songId = song.id,
                    tint = if (song.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 24.dp
                )
            }
            
             // More options menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.now_playing_more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                 if (showMenu) {
                    SongContextMenu(
                        song = song,
                        menuHandler = menuHandler,
                        onDismiss = { showMenu = false }
                    )
                }
            }
        }
    }
}



@Composable
private fun InitialSearchState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.search_initial_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.search_initial_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InitialSearchStateWithHistory(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Move towards top since history is below search bar
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.surfaceContainerHigh
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.search_history_state_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun NoResultsState(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            modifier = Modifier.size(120.dp)
        ) {
             Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.search_no_results_title),
            style = MaterialTheme.typography.headlineSmall,
             fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.search_no_results_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.search_query_with_quotes, query),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SearchScreenPreview() {
    SukoonMusicPlayerTheme(theme = AppTheme.DARK) {
        SearchScreen(onBackClick = {})
    }
}





