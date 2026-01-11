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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukoon.music.data.mediastore.DeleteHelper
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

    var showSongContextSheet by remember { mutableStateOf<Song?>(null) }
    var songPendingDeletion by remember { mutableStateOf<Song?>(null) }
    var showError by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Delete result launcher
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "Song deleted successfully", Toast.LENGTH_SHORT).show()
            viewModel.loadGenre(genreId)
        }
        songPendingDeletion = null
    }

    // Create menu handler for song context menu
    val menuHandler = rememberSongMenuHandler(
        playbackRepository = viewModel.playbackRepository,
        onShowSongInfo = { song -> /* TODO */ },
        onShowDeleteConfirmation = { song -> songPendingDeletion = song },
        onToggleLike = { songId, isLiked -> viewModel.toggleLike(songId, isLiked) }
    )

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

            // Song Context Menu
            showSongContextSheet?.let { song ->
                SongContextMenu(
                    song = song,
                    menuHandler = menuHandler,
                    onDismiss = { showSongContextSheet = null }
                )
            }

            // Delete confirmation dialog
            songPendingDeletion?.let { song ->
                DeleteConfirmationDialog(
                    song = song,
                    onConfirm = {
                        when (val result = menuHandler.performDelete(song)) {
                            is DeleteHelper.DeleteResult.RequiresPermission -> {
                                deleteLauncher.launch(
                                    IntentSenderRequest.Builder(result.intentSender).build()
                                )
                            }
                            is DeleteHelper.DeleteResult.Success -> {
                                Toast.makeText(context, "Song deleted successfully", Toast.LENGTH_SHORT).show()
                                viewModel.loadGenre(genreId)
                                songPendingDeletion = null
                            }
                            is DeleteHelper.DeleteResult.Error -> {
                                Toast.makeText(context, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                                songPendingDeletion = null
                            }
                        }
                    },
                    onDismiss = { songPendingDeletion = null }
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
