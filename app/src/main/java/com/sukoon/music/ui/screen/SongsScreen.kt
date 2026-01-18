package com.sukoon.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

/**
 * Songs Screen - Displays all songs in alphabetical order with search functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    onBackClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showInfoForSong by remember { mutableStateOf<Song?>(null) }

    // Share handler
    val shareHandler = rememberShareHandler()

    // Create menu handler for song context menu
    val menuHandler = rememberSongMenuHandler(
        playbackRepository = viewModel.playbackRepository,
        onShowSongInfo = { song -> showInfoForSong = song },
        onShare = shareHandler
    )

    // Sort songs alphabetically and filter by search query
    val filteredSongs = remember(songs, searchQuery) {
        songs
            .sortedBy { it.title.lowercase() }
            .filter { song ->
                searchQuery.isEmpty() ||
                song.title.contains(searchQuery, ignoreCase = true) ||
                song.artist.contains(searchQuery, ignoreCase = true) ||
                song.album.contains(searchQuery, ignoreCase = true)
            }
    }

    Scaffold(
        topBar = {
            Column {
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
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClearQuery = { searchQuery = "" }
                )
            }
        }
    ) { paddingValues ->
        if (filteredSongs.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
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
            // Songs List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = filteredSongs,
                    key = { it.id }
                ) { song ->
                    val isPlaying = playbackState.currentSong?.id == song.id && playbackState.isPlaying
                    val index = songs.indexOf(song)

                    SongListItem(
                        song = song,
                        isPlaying = isPlaying,
                        menuHandler = menuHandler,
                        onClick = {
                            viewModel.playQueue(songs, index)
                        },
                        onLikeClick = {
                            viewModel.toggleLike(song.id, song.isLiked)
                        }
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
    menuHandler: SongMenuHandler,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
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

            // Like button
            IconButton(onClick = onLikeClick) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (song.isLiked) "Unlike" else "Like",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // More options menu
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

    if (showMenu) {
        SongContextMenu(
            song = song,
            menuHandler = menuHandler,
            onDismiss = { showMenu = false }
        )
    }
}
