package com.sukoon.music.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.SmartPlaylist
import com.sukoon.music.domain.model.SmartPlaylistType
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.SmartPlaylistViewModel

/**
 * Smart Playlist Detail Screen - Shows songs in a smart playlist.
 *
 * Features:
 * - Displays songs based on smart playlist type (Liked, Last Added, Recently Played, Most Played)
 * - Play All and Shuffle buttons
 * - Individual song playback
 * - Empty state when no songs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPlaylistDetailScreen(
    smartPlaylistType: String,
    onBackClick: () -> Unit,
    viewModel: SmartPlaylistViewModel = hiltViewModel()
) {
    // Parse smart playlist type
    val playlistType = try {
        SmartPlaylistType.valueOf(smartPlaylistType)
    } catch (e: IllegalArgumentException) {
        SmartPlaylistType.MY_FAVOURITE // Fallback
    }

    // Load smart playlist data
    LaunchedEffect(playlistType) {
        viewModel.loadSmartPlaylist(playlistType)
    }

    val songs by viewModel.currentSmartPlaylistSongs.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    val title = SmartPlaylist.getDisplayName(playlistType)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
        SmartPlaylistDetailContent(
            playlistType = playlistType,
            songs = songs,
            onPlayAll = { viewModel.playSmartPlaylist(playlistType) },
            onShuffle = { viewModel.shuffleSmartPlaylist(playlistType) },
            onSongClick = { song -> viewModel.playSong(song) },
            onLikeClick = { song -> viewModel.toggleLike(song.id, song.isLiked) },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun SmartPlaylistDetailContent(
    playlistType: SmartPlaylistType,
    songs: List<Song>,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onSongClick: (Song) -> Unit,
    onLikeClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Playlist Header
        item {
            SmartPlaylistHeader(
                playlistType = playlistType,
                songCount = songs.size,
                onPlayAll = onPlayAll,
                onShuffle = onShuffle
            )
        }

        // Song List
        if (songs.isEmpty()) {
            item {
                EmptySmartPlaylistState(playlistType)
            }
        } else {
            itemsIndexed(
                items = songs,
                key = { _, song -> song.id }
            ) { index, song ->
                SmartPlaylistSongItem(
                    song = song,
                    index = index + 1,
                    onClick = { onSongClick(song) },
                    onLikeClick = { onLikeClick(song) }
                )
            }
        }
    }
}

@Composable
private fun SmartPlaylistHeader(
    playlistType: SmartPlaylistType,
    songCount: Int,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon
        Card(
            modifier = Modifier.size(200.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getSmartPlaylistIcon(playlistType),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Playlist Name
        Text(
            text = SmartPlaylist.getDisplayName(playlistType),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$songCount songs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            // Play All Button
            Button(
                onClick = onPlayAll,
                enabled = songCount > 0,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play All")
            }

            // Shuffle Button
            OutlinedButton(
                onClick = onShuffle,
                enabled = songCount > 0,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Shuffle")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
    }
}

@Composable
private fun SmartPlaylistSongItem(
    song: Song,
    index: Int,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track Number
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Album Art
            Card(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (song.albumArtUri != null) {
                        SubcomposeAsyncImage(
                            model = song.albumArtUri,
                            contentDescription = "Album art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            loading = {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            error = {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            // Like button
            IconButton(
                onClick = onLikeClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (song.isLiked) "Unlike" else "Like",
                    tint = if (song.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptySmartPlaylistState(playlistType: SmartPlaylistType) {
    val (message, description) = when (playlistType) {
        SmartPlaylistType.MY_FAVOURITE -> "No liked songs yet" to "Like songs to see them here"
        SmartPlaylistType.LAST_ADDED -> "No songs added" to "Scan your music library to get started"
        SmartPlaylistType.RECENTLY_PLAYED -> "No recently played songs" to "Start playing music to see your history"
        SmartPlaylistType.MOST_PLAYED -> "No play history yet" to "Songs you play often will appear here"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = getSmartPlaylistIcon(playlistType),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

private fun getSmartPlaylistIcon(type: SmartPlaylistType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        SmartPlaylistType.MY_FAVOURITE -> Icons.Default.Favorite
        SmartPlaylistType.LAST_ADDED -> Icons.Default.Add
        SmartPlaylistType.RECENTLY_PLAYED -> Icons.Default.History
        SmartPlaylistType.MOST_PLAYED -> Icons.Default.PlayArrow
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SmartPlaylistDetailScreenPreview() {
    SukoonMusicPlayerTheme(darkTheme = true) {
        SmartPlaylistDetailScreen(
            smartPlaylistType = "MY_FAVOURITE",
            onBackClick = {}
        )
    }
}
