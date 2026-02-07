package com.sukoon.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Folder
import com.sukoon.music.domain.model.FolderItem
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.AnimatedEqualizer
import com.sukoon.music.ui.components.SongContextMenu
import com.sukoon.music.ui.components.SongMenuHandler
import com.sukoon.music.ui.components.rememberSongMenuHandler
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.FolderDetailViewModel
import com.sukoon.music.ui.theme.*
import com.sukoon.music.ui.util.rememberAlbumPalette

/**
 * Folder Detail Screen - Shows songs in a specific folder.
 *
 * Features:
 * - Folder header with icon/cover art and metadata
 * - Play All and Shuffle buttons
 * - List of songs with like functionality
 * - Empty state when no songs (shouldn't happen, but defensive)
 * - Song playback on tap
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderId: Long,
    folderPath: String? = null,
    onBackClick: () -> Unit,
    onNavigateToParent: (String?) -> Unit = { onBackClick() },
    onNavigateToSubfolder: (String) -> Unit = {},
    onNavigateToNowPlaying: () -> Unit = {},
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToArtist: (Long) -> Unit = {},
    viewModel: FolderDetailViewModel = hiltViewModel()
) {
    // Load folder data (hierarchical or flat mode)
    LaunchedEffect(folderId, folderPath) {
        if (folderPath != null) {
            viewModel.loadFolderByPath(folderPath)
        } else {
            viewModel.loadFolder(folderId)
        }
    }

    val folder by viewModel.folder.collectAsStateWithLifecycle()
    val songs by viewModel.folderSongs.collectAsStateWithLifecycle()
    val folderItems by viewModel.folderItems.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val parentFolderPath by viewModel.parentFolderPath.collectAsStateWithLifecycle()

    // Create menu handler for song context menu
    val menuHandler = rememberSongMenuHandler(
        playbackRepository = viewModel.playbackRepository,
        onNavigateToAlbum = onNavigateToAlbum,
        onNavigateToArtist = onNavigateToArtist
    )

    // Handle case where folder is deleted or excluded while viewing
    LaunchedEffect(folder) {
        if (folder == null && folderId > 0L && folderPath == null) {
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folderPath?.substringAfterLast("/") ?: folder?.name ?: "Folder") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (folderPath != null) {
                            onNavigateToParent(parentFolderPath)
                        } else {
                            onBackClick()
                        }
                    }) {
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
        if (folderPath != null) {
            // Hierarchical mode: show folders + songs
            HierarchicalFolderContent(
                folderItems = folderItems,
                currentSongId = playbackState.currentSong?.id,
                menuHandler = menuHandler,
                onFolderClick = onNavigateToSubfolder,
                onSongClick = { song ->
                    if (playbackState.currentSong?.id != song.id) {
                        val allSongs = folderItems.filterIsInstance<FolderItem.SongType>()
                            .map { it.song }
                        viewModel.playSong(song, allSongs)
                    } else {
                        onNavigateToNowPlaying()
                    }
                },
                onToggleLike = { songId, isLiked ->
                    viewModel.toggleLike(songId, isLiked)
                },
                modifier = Modifier.padding(paddingValues)
            )
        } else if (folder != null) {
            // Flat mode: show only songs (backward compatibility)
            FolderDetailContent(
                folder = folder!!,
                songs = songs,
                currentSongId = playbackState.currentSong?.id,
                menuHandler = menuHandler,
                onPlayAll = { viewModel.playFolder(songs) },
                onShuffle = { viewModel.shuffleFolder(songs) },
                onSongClick = { song ->
                    if (playbackState.currentSong?.id != song.id) {
                        viewModel.playSong(song, songs)
                    } else {
                        onNavigateToNowPlaying()
                    }
                },
                onToggleLike = { songId, isLiked ->
                    viewModel.toggleLike(songId, isLiked)
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun HierarchicalFolderContent(
    folderItems: List<FolderItem>,
    currentSongId: Long?,
    menuHandler: SongMenuHandler,
    onFolderClick: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    onToggleLike: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        if (folderItems.isEmpty()) {
            item {
                EmptyFolderState()
            }
        } else {
            items(
                items = folderItems,
                key = { item ->
                    when (item) {
                        is FolderItem.FolderType -> "folder_${item.folder.id}"
                        is FolderItem.SongType -> "song_${item.song.id}"
                    }
                }
            ) { item ->
                when (item) {
                    is FolderItem.FolderType -> {
                        FolderItemRow(
                            folder = item.folder,
                            onClick = { onFolderClick(item.folder.path) }
                        )
                    }
                    is FolderItem.SongType -> {
                        FolderSongItem(
                            song = item.song,
                            index = 0, // Not showing index in hierarchical mode
                            isCurrentlyPlaying = item.song.id == currentSongId,
                            menuHandler = menuHandler,
                            onClick = { onSongClick(item.song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderItemRow(
    folder: Folder,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folder Icon
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Folder Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.listItemTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${folder.songCount} songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Navigate arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open folder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FolderDetailContent(
    folder: Folder,
    songs: List<Song>,
    currentSongId: Long?,
    menuHandler: SongMenuHandler,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onSongClick: (Song) -> Unit,
    onToggleLike: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Folder Header
        item {
            FolderHeader(
                folder = folder,
                onPlayAll = onPlayAll,
                onShuffle = onShuffle
            )
        }

        // Song List
        if (songs.isEmpty()) {
            item {
                EmptyFolderState()
            }
        } else {
            itemsIndexed(
                items = songs,
                key = { _, song -> song.id }
            ) { index, song ->
                FolderSongItem(
                    song = song,
                    index = index + 1,
                    isCurrentlyPlaying = song.id == currentSongId,
                    menuHandler = menuHandler,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun FolderHeader(
    folder: Folder,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    // Extract palette from folder's album art
    val palette = rememberAlbumPalette(folder.albumArtUri)
    val vibrantColor = palette.vibrant
    val backgroundTint = palette.mutedLight.copy(alpha = 0.12f)
    val accentColor = vibrantColor.copy(alpha = 0.6f)

    // Dynamic gradient background (dark overlay for contrast)
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            vibrantColor.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.surface
        ),
        startY = 0f,
        endY = 500f
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradientBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Left accent stripe
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(240.dp)
                        .background(accentColor, RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Folder Cover with shadow
                Card(
                    modifier = Modifier
                        .size(240.dp)
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), clip = false),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (folder.albumArtUri != null) {
                            SubcomposeAsyncImage(
                                model = folder.albumArtUri,
                                contentDescription = "Folder cover",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp)
                                    )
                                },
                                error = {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Folder Name - Large and Bold
            Text(
                text = folder.name,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Folder Path (tertiary)
            Text(
                text = folder.path,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Badges (matching FolderRow style)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(24.dp)
            ) {
                val badgeBg = vibrantColor.copy(alpha = 0.15f)

                // Song count badge
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = vibrantColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${folder.songCount} songs",
                            style = MaterialTheme.typography.labelSmall,
                            color = vibrantColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Duration badge
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = vibrantColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = folder.formattedDuration(),
                            style = MaterialTheme.typography.labelSmall,
                            color = vibrantColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons with vibrant accent
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Play All Button - Vibrant color
                Button(
                    onClick = onPlayAll,
                    enabled = folder.songCount > 0,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = vibrantColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play All", fontWeight = FontWeight.SemiBold)
                }

                // Shuffle Button - Outlined with vibrant stroke
                OutlinedButton(
                    onClick = onShuffle,
                    enabled = folder.songCount > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shuffle", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun FolderSongItem(
    song: Song,
    index: Int,
    isCurrentlyPlaying: Boolean,
    menuHandler: SongMenuHandler,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = if (isCurrentlyPlaying) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track Number
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrentlyPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.width(32.dp)
            )

            // Song Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrentlyPlaying) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCurrentlyPlaying) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AnimatedEqualizer(tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
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
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // More options menu
            IconButton(onClick = { showMenu = true }) {
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

@Composable
private fun EmptyFolderState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "No songs",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No songs in this folder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyFolderStatePreview() {
    SukoonMusicPlayerTheme {
        EmptyFolderState()
    }
}
