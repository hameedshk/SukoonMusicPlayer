package com.sukoon.music.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.ui.components.AddToPlaylistDialog
import com.sukoon.music.ui.components.BannerAdView
import com.sukoon.music.ui.components.RecentlyPlayedArtistsRow
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.ArtistSortMode
import com.sukoon.music.ui.viewmodel.ArtistsViewModel
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Artists Screen - Shows all artists in a list view.
 *
 * Features:
 * - List layout with artist rows
 * - Artist rows with artwork, name, song count, and album count
 * - Context menu for various actions
 * - Empty state when no artists exist
 * - Recently played artists section
 * - Selection mode with multi-select
 * - Tap to navigate to artist detail screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(
    onNavigateToArtist: (Long) -> Unit,
    onBackClick: () -> Unit,
    onShowAddToPlaylistDialog: (List<Long>) -> Unit = {},
    onShowEditTagsDialog: (Artist) -> Unit = {},
    onShowChangeCoverDialog: (Artist) -> Unit = {},
    onShowDeleteConfirmDialog: (Artist) -> Unit = {},
    viewModel: ArtistsViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val recentlyPlayedArtists by viewModel.recentlyPlayedArtists.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()

    var showMenuForArtist by remember { mutableStateOf<Artist?>(null) }
    var artistToDelete by remember { mutableStateOf<Artist?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var artistToAddToPlaylist by remember { mutableStateOf<Artist?>(null) }
    var showSortDialog by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedArtistIds by remember { mutableStateOf(setOf<Long>()) }

    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        if (artists.isEmpty()) {
            EmptyArtistsState()
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                if (isSelectionMode) {
                    // Selection mode header
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                isSelectionMode = false
                                selectedArtistIds = emptySet()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Exit selection"
                                )
                            }
                            Text(
                                text = "${selectedArtistIds.size} selected",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Search bar in selection mode
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search artists") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    // Select all
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedArtistIds = if (selectedArtistIds.size == artists.size) {
                                    emptySet()
                                } else {
                                    artists.map { it.id }.toSet()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select all",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = selectedArtistIds.size == artists.size,
                            onCheckedChange = null
                        )
                    }
                } else {
                    // Normal mode header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${artists.size} ${if (artists.size == 1) "artist" else "artists"}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showSortDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Sort"
                            )
                        }
                        IconButton(onClick = {
                            isSelectionMode = true
                            selectedArtistIds = emptySet()
                        }) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Select"
                            )
                        }
                    }

                    // Recently played section
                    if (recentlyPlayedArtists.isNotEmpty()) {
                        Column {
                            RecentlyPlayedArtistsRow(
                                artists = recentlyPlayedArtists,
                                onArtistClick = { id ->
                                    onNavigateToArtist(id)
                                    viewModel.logArtistInteraction(id)
                                },
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }

                // Artists list
                Box(modifier = Modifier.weight(1f)) {
                    if (isSelectionMode) {
                        ArtistsListSelectable(
                            artists = artists,
                            selectedArtistIds = selectedArtistIds,
                            onArtistClick = { artist ->
                                selectedArtistIds = if (selectedArtistIds.contains(artist.id)) {
                                    selectedArtistIds - artist.id
                                } else {
                                    selectedArtistIds + artist.id
                                }
                            }
                        )
                    } else {
                        ArtistsListWithMenu(
                            artists = artists,
                            onArtistClick = { id ->
                                onNavigateToArtist(id)
                                viewModel.logArtistInteraction(id)
                            },
                            onMenuClick = { artist -> showMenuForArtist = artist },
                            adMobManager = playlistViewModel.adMobManager
                        )
                    }

                    // Alphabet scroller
                    if (!isSelectionMode) {
                        AlphabetScroller(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            onLetterClick = { /* TODO: Scroll to letter */ }
                        )
                    }
                }
            }

            // Selection mode bottom action bar
            if (isSelectionMode && selectedArtistIds.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SelectionActionButton(icon = Icons.Default.PlayArrow, label = "Play", onClick = { /* TODO */ })
                        SelectionActionButton(icon = Icons.Default.PlaylistAdd, label = "Playlist", onClick = { /* TODO */ })
                        SelectionActionButton(icon = Icons.Default.Add, label = "Add", onClick = { /* TODO */ })
                        SelectionActionButton(icon = Icons.Default.Delete, label = "Delete", onClick = { /* TODO */ })
                        SelectionActionButton(icon = Icons.Default.MoreVert, label = "More", onClick = { /* TODO */ })
                    }
                }
            }
        }

        // Artist menu bottom sheet
        if (showMenuForArtist != null) {
            ArtistMenuBottomSheet(
                artist = showMenuForArtist!!,
                recentlyPlayedArtists = recentlyPlayedArtists.take(3),
                onDismiss = { showMenuForArtist = null },
                onPlay = {
                    viewModel.playArtist(showMenuForArtist!!.id)
                    showMenuForArtist = null
                },
                onPlayNext = {
                    viewModel.playArtistNext(showMenuForArtist!!.id)
                    showMenuForArtist = null
                },
                onAddToQueue = {
                    viewModel.addArtistToQueue(showMenuForArtist!!.id)
                    showMenuForArtist = null
                },
                onAddToPlaylist = {
                    artistToAddToPlaylist = showMenuForArtist
                    showAddToPlaylistDialog = true
                    showMenuForArtist = null
                },
                onEditTags = {
                    onShowEditTagsDialog(showMenuForArtist!!)
                    showMenuForArtist = null
                },
                onChangeCover = {
                    onShowChangeCoverDialog(showMenuForArtist!!)
                    showMenuForArtist = null
                },
                onDelete = {
                    artistToDelete = showMenuForArtist
                    showMenuForArtist = null
                }
            )
        }

        // Add to playlist dialog
        if (showAddToPlaylistDialog && artistToAddToPlaylist != null) {
            AddToPlaylistDialog(
                playlists = playlists,
                onPlaylistSelected = { playlistId ->
                    val artistId = artistToAddToPlaylist!!.id
                    scope.launch {
                        viewModel.songRepository.getSongsByArtistId(artistId).first().let { songs ->
                            songs.forEach { song ->
                                playlistViewModel.addSongToPlaylist(playlistId, song.id)
                            }
                        }
                        showAddToPlaylistDialog = false
                        artistToAddToPlaylist = null
                    }
                },
                onDismiss = {
                    showAddToPlaylistDialog = false
                    artistToAddToPlaylist = null
                }
            )
        }

        // Delete confirmation dialog
        if (artistToDelete != null) {
            AlertDialog(
                onDismissRequest = { artistToDelete = null },
                title = { Text("Delete ${artistToDelete?.songCount} songs?") },
                text = {
                    Text("This will permanently delete all songs by ${artistToDelete?.name} from your device. This action cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            artistToDelete?.let { viewModel.deleteArtist(it.id) }
                            artistToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { artistToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Sort dialog
        if (showSortDialog) {
            ArtistSortDialog(
                currentSortMode = sortMode,
                onDismiss = { showSortDialog = false },
                onConfirm = { mode ->
                    viewModel.setSortMode(mode)
                    showSortDialog = false
                }
            )
        }
    }
}

@Composable
private fun ArtistsListWithMenu(
    artists: List<Artist>,
    onArtistClick: (Long) -> Unit,
    onMenuClick: (Artist) -> Unit,
    adMobManager: com.sukoon.music.data.ads.AdMobManager
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(
            items = artists,
            key = { artist -> artist.id }
        ) { artist ->
            ArtistItemWithMenu(
                artist = artist,
                onClick = { onArtistClick(artist.id) },
                onMenuClick = { onMenuClick(artist) }
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                BannerAdView(adMobManager = adMobManager)
            }
        }
    }
}

@Composable
private fun ArtistItemWithMenu(
    artist: Artist,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (artist.artworkUri  != null) {
                SubcomposeAsyncImage(
                    model = artist.artworkUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Artist info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${artist.albumCount} ${if (artist.albumCount == 1) "album" else "albums"} · ${artist.songCount} ${if (artist.songCount == 1) "song" else "songs"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Menu button
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ArtistsListSelectable(
    artists: List<Artist>,
    selectedArtistIds: Set<Long>,
    onArtistClick: (Artist) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(
            items = artists,
            key = { artist -> artist.id }
        ) { artist ->
            ArtistItemSelectable(
                artist = artist,
                isSelected = selectedArtistIds.contains(artist.id),
                onClick = { onArtistClick(artist) }
            )
        }
    }
}

@Composable
private fun ArtistItemSelectable(
    artist: Artist,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (artist.artworkUri != null) {
                SubcomposeAsyncImage(
                    model = artist.artworkUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Artist info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${artist.albumCount} ${if (artist.albumCount == 1) "album" else "albums"} · ${artist.songCount} ${if (artist.songCount == 1) "song" else "songs"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Checkbox
        RadioButton(
            selected = isSelected,
            onClick = null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistMenuBottomSheet(
    artist: Artist,
    recentlyPlayedArtists: List<Artist>,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEditTags: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Recently played section (if available)
            if (recentlyPlayedArtists.isNotEmpty()) {
                Text(
                    text = "Recently played",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    recentlyPlayedArtists.forEach { recentArtist ->
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (recentArtist.artworkUri != null) {
                                SubcomposeAsyncImage(
                                    model = recentArtist.artworkUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Artist header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (artist.artworkUri  != null) {
                        SubcomposeAsyncImage(
                            model = artist.artworkUri ,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${artist.albumCount} ${if (artist.albumCount == 1) "album" else "albums"} · ${artist.songCount} ${if (artist.songCount == 1) "song" else "songs"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { /* TODO: Info */ }) {
                    Icon(Icons.Default.Info, contentDescription = "Info")
                }
                IconButton(onClick = { /* TODO: Share */ }) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Menu items
            MenuBottomSheetItem(icon = Icons.Default.PlayArrow, text = "Play", onClick = onPlay)
            MenuBottomSheetItem(icon = Icons.Default.PlayArrow, text = "Play next", onClick = onPlayNext)
            MenuBottomSheetItem(icon = Icons.Default.Add, text = "Add to queue", onClick = onAddToQueue)
            MenuBottomSheetItem(icon = Icons.Default.PlaylistAdd, text = "Add to playlist", onClick = onAddToPlaylist)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            MenuBottomSheetItem(icon = Icons.Default.Edit, text = "Edit tags", onClick = onEditTags)
            MenuBottomSheetItem(icon = Icons.Default.Image, text = "Change cover", onClick = onChangeCover)
            MenuBottomSheetItem(
                icon = Icons.Default.Delete,
                text = "Delete from device",
                onClick = onDelete,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun MenuBottomSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistSortDialog(
    currentSortMode: ArtistSortMode,
    onDismiss: () -> Unit,
    onConfirm: (ArtistSortMode) -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentSortMode) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Sort by",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ArtistSortMode.values().forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedMode = mode
                            onConfirm(mode)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (mode) {
                            ArtistSortMode.ARTIST_NAME -> "Artist name"
                            ArtistSortMode.ALBUM_COUNT -> "Album count"
                            ArtistSortMode.SONG_COUNT -> "Song count"
                            ArtistSortMode.RANDOM -> "Random"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedMode == mode) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlphabetScroller(
    modifier: Modifier = Modifier,
    onLetterClick: (Char) -> Unit
) {
    val letters = ('A'..'Z').toList() + '#'

    Column(
        modifier = modifier
            .padding(end = 4.dp)
            .width(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach { letter ->
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                modifier = Modifier
                    .clickable { onLetterClick(letter) }
                    .padding(vertical = 1.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectionActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun EmptyArtistsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No artists found",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Scan your library to discover artists from your music collection",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ArtistsScreenPreview() {
    SukoonMusicPlayerTheme(darkTheme = true) {
        ArtistsScreen(
            onNavigateToArtist = {},
            onBackClick = {}
        )
    }
}
