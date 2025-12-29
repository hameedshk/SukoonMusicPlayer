package com.sukoon.music.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.ArtistDetailViewModel

/**
 * Artist Detail Screen - Shows artist info with Songs and Albums tabs.
 *
 * Features:
 * - Artist header with circular artwork, name, and stats
 * - Play and Shuffle buttons for all artist songs
 * - Tabs for Songs and Albums
 * - Songs tab: List of all songs by artist
 * - Albums tab: Grid of all albums by artist
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistId: Long,
    onBackClick: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit = {},
    viewModel: ArtistDetailViewModel = hiltViewModel()
) {
    val artist by viewModel.artist.collectAsStateWithLifecycle()
    val artistSongs by viewModel.artistSongs.collectAsStateWithLifecycle()
    val artistAlbums by viewModel.artistAlbums.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }

    // Load artist when screen opens
    LaunchedEffect(artistId) {
        viewModel.loadArtist(artistId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artist") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (artist == null) {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Artist Header
                ArtistHeader(
                    artist = artist!!,
                    onPlayClick = { viewModel.playArtist(artistSongs) },
                    onShuffleClick = { viewModel.shuffleArtist(artistSongs) }
                )

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Songs (${artistSongs.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Albums (${artistAlbums.size})") }
                    )
                }

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // Songs Tab
                        if (artistSongs.isEmpty()) {
                            EmptyStateMessage("No songs found")
                        } else {
                            SongsList(
                                songs = artistSongs,
                                currentSongId = playbackState.currentSong?.id,
                                isPlaying = playbackState.isPlaying,
                                onSongClick = { song ->
                                    viewModel.playSong(song, artistSongs)
                                },
                                onLikeClick = { song ->
                                    viewModel.toggleLike(song.id, song.isLiked)
                                }
                            )
                        }
                    }
                    1 -> {
                        // Albums Tab
                        if (artistAlbums.isEmpty()) {
                            EmptyStateMessage("No albums found")
                        } else {
                            AlbumsGrid(
                                albums = artistAlbums,
                                onAlbumClick = onNavigateToAlbum,
                                onPlayAlbum = { albumId ->
                                    viewModel.playAlbum(albumId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistHeader(
    artist: Artist,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circular Artwork
        if (artist.artworkUri != null) {
            SubcomposeAsyncImage(
                model = artist.artworkUri,
                contentDescription = "Artist artwork",
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                },
                error = {
                    DefaultArtistIcon()
                }
            )
        } else {
            DefaultArtistIcon()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Artist Name
        Text(
            text = artist.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Stats
        Text(
            text = "${artist.albumCount} albums • ${artist.songCount} songs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Play Button
            Button(
                onClick = onPlayClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1DB954)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play")
            }

            // Shuffle Button
            OutlinedButton(
                onClick = onShuffleClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
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
    }
}

@Composable
private fun DefaultArtistIcon() {
    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun SongsList(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onLikeClick: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(
            items = songs,
            key = { song -> song.id }
        ) { song ->
            SongItem(
                song = song,
                isCurrentSong = song.id == currentSongId,
                isPlaying = isPlaying && song.id == currentSongId,
                onClick = { onSongClick(song) },
                onLikeClick = { onLikeClick(song) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongItem(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrentSong) Color(0xFF1DB954) else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = song.album,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            if (isCurrentSong && isPlaying) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Playing",
                    tint = Color(0xFF1DB954)
                )
            } else {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = song.durationFormatted(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onLikeClick) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (song.isLiked) "Unlike" else "Like",
                    tint = if (song.isLiked) Color(0xFF1DB954) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun AlbumsGrid(
    albums: List<Album>,
    onAlbumClick: (Long) -> Unit,
    onPlayAlbum: (Long) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = albums,
            key = { album -> album.id }
        ) { album ->
            AlbumCard(
                album = album,
                onClick = { onAlbumClick(album.id) },
                onPlayClick = { onPlayAlbum(album.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Album Art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (album.albumArtUri != null) {
                    SubcomposeAsyncImage(
                        model = album.albumArtUri,
                        contentDescription = "Album cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        },
                        error = {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            // Album Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${album.songCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}
