package com.sukoon.music.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukoon.music.domain.model.Genre
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.*
import com.sukoon.music.ui.viewmodel.GenreDetailViewModel
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import kotlinx.coroutines.launch

/**
 * Genre Detail Screen - Shows songs in a specific genre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreDetailScreen(
    genreId: Long,
    onBackClick: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    viewModel: GenreDetailViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    // Load genre data
    LaunchedEffect(genreId) {
        viewModel.loadGenre(genreId)
    }

    val genre by viewModel.genre.collectAsStateWithLifecycle()
    val songs by viewModel.genreSongs.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()

    var showSongContextSheet by remember { mutableStateOf<Song?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf<Song?>(null) }
    var showError by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Error state handling - show error if genre not found after timeout
    LaunchedEffect(genreId, genre) {
        if (genre == null) {
            kotlinx.coroutines.delay(3000) // 3 second timeout
            if (genre == null) {
                showError = true
            }
        } else {
            showError = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(genre?.name ?: "Genre") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                showError -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Genre not found",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "This genre could not be loaded. It may have been deleted or does not exist.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(onClick = onBackClick) {
                            Text("Go Back")
                        }
                    }
                }
                genre == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    GenreDetailContent(
                        genre = genre!!,
                        songs = songs,
                        currentSongId = playbackState.currentSong?.id,
                        isPlaying = playbackState.isPlaying,
                        onPlayAll = { viewModel.playGenre(songs) },
                        onShuffle = { viewModel.shuffleGenre(songs) },
                        onSongClick = { song ->
                            if (playbackState.currentSong?.id != song.id) {
                                viewModel.playSong(song, songs)
                            } else {
                                onNavigateToNowPlaying()
                            }
                        },
                        onSongLongClick = { song -> showSongContextSheet = song },
                        onLikeClick = { song -> viewModel.toggleLike(song.id, song.isLiked) }
                    )
                }
            }

            // Song Context Bottom Sheet
            if (showSongContextSheet != null) {
                val song = showSongContextSheet!!
                ModalBottomSheet(
                    onDismissRequest = { showSongContextSheet = null }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                    ) {
                        // Top Row Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ContextTopAction(
                                icon = Icons.Default.NotificationsActive,
                                label = "Ringtone",
                                onClick = { /* TODO */ showSongContextSheet = null }
                            )
                            ContextTopAction(
                                icon = Icons.Default.Image,
                                label = "Cover",
                                onClick = { /* TODO */ showSongContextSheet = null }
                            )
                            ContextTopAction(
                                icon = Icons.Default.Edit,
                                label = "Tags",
                                onClick = { /* TODO */ showSongContextSheet = null }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Full List Options
                        ContextMenuItem(
                            icon = Icons.Default.SkipNext,
                            label = "Play next",
                            onClick = {
                                viewModel.playNext(song)
                                showSongContextSheet = null
                            }
                        )
                        ContextMenuItem(
                            icon = Icons.Default.PlaylistAdd,
                            label = "Add to queue",
                            onClick = {
                                viewModel.addToQueue(song)
                                showSongContextSheet = null
                            }
                        )
                        ContextMenuItem(
                            icon = Icons.Default.QueueMusic,
                            label = "Add to playlist",
                            onClick = {
                                showAddToPlaylistDialog = song
                                showSongContextSheet = null
                            }
                        )
                        ContextMenuItem(
                            icon = Icons.Default.Album,
                            label = "Go to album",
                            onClick = { /* TODO */ showSongContextSheet = null }
                        )
                        ContextMenuItem(
                            icon = Icons.Default.Audiotrack,
                            label = "Edit audio",
                            onClick = { /* TODO */ showSongContextSheet = null }
                        )
                        ContextMenuItem(
                            icon = Icons.Default.Lyrics,
                            label = "Update lyrics",
                            onClick = { /* TODO */ showSongContextSheet = null }
                        )
                        ContextMenuItem(
                            icon = Icons.Default.Delete,
                            label = "Delete from device",
                            isError = true,
                            onClick = {
                                showDeleteConfirmDialog = song
                                showSongContextSheet = null
                            }
                        )
                    }
                }
            }

            // Delete Confirmation Dialog
            if (showDeleteConfirmDialog != null) {
                val song = showDeleteConfirmDialog!!
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = null },
                    title = { Text("Delete '${song.title}'?") },
                    text = {
                        Text("This will permanently delete this song from your device. This action cannot be undone.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteSong(song)
                                showDeleteConfirmDialog = null
                            }
                        ) {
                            Text("DELETE", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = null }) {
                            Text("CANCEL")
                        }
                    }
                )
            }

            // Add to Playlist Dialog
            if (showAddToPlaylistDialog != null) {
                AddToPlaylistDialog(
                    playlists = playlists,
                    onPlaylistSelected = { playlistId ->
                        scope.launch {
                            playlistViewModel.addSongToPlaylist(playlistId, showAddToPlaylistDialog!!.id)
                            showAddToPlaylistDialog = null
                        }
                    },
                    onDismiss = { showAddToPlaylistDialog = null }
                )
            }
        }
    }
}

@Composable
private fun GenreDetailContent(
    genre: Genre,
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onLikeClick: (Song) -> Unit
) {
    val albumCount = remember(songs) { songs.map { it.album }.distinct().size }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GenreIcon(
                    genreName = genre.name,
                    modifier = Modifier.size(120.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = genre.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = genre.formattedSongCount(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (albumCount == 1) "1 album" else "$albumCount albums",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onShuffle,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Shuffle")
                    }
                    
                    Button(
                        onClick = onPlayAll,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play All")
                    }
                }
            }
        }

        // Songs List
        if (songs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No songs in this genre",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            itemsIndexed(
                items = songs,
                key = { _, song -> song.id }
            ) { _, song ->
                val isCurrentSong = song.id == currentSongId
                
                SongRow(
                    song = song,
                    isCurrent = isCurrentSong,
                    isPlaying = isCurrentSong && isPlaying,
                    onClick = { onSongClick(song) },
                    onLongClick = { onSongLongClick(song) },
                    onLikeClick = { onLikeClick(song) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLikeClick: () -> Unit
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${song.album}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        IconButton(onClick = onLikeClick) {
            Icon(
                imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (song.isLiked) "Unlike" else "Like",
                tint = if (song.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onLongClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ContextTopAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ContextMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isError: Boolean = false
) {
    ListItem(
        headlineContent = { 
            Text(
                text = label,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            ) 
        },
        leadingContent = { 
            Icon(
                imageVector = icon, 
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            ) 
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
