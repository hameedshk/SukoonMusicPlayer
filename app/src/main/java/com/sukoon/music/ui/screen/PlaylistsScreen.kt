package com.sukoon.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Playlist
import com.sukoon.music.ui.components.getSmartPlaylistIcon
import com.sukoon.music.domain.model.SmartPlaylist
import com.sukoon.music.domain.model.SmartPlaylistType
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.NativeAdCard
import com.sukoon.music.ui.components.PlaceholderAlbumArt
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import com.sukoon.music.ui.theme.*

private enum class PlaylistSortMode(val label: String) {
    RECENT("Recently added"),
    NAME("Name A-Z"),
    SONG_COUNT("Song count")
}

/**
 * Playlists Screen - Shows smart playlists and user playlists.
 *
 * Features:
 * - Primary Grid: 2x2 grid of 4 smart playlists (My favourite, Last added, Recently played, Most played)
 * - Secondary Action List: Create, Restore, Import playlist actions
 * - User Playlists: 2-column grid of user-created playlists
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    onNavigateToPlaylist: (Long) -> Unit,
    onNavigateToSmartPlaylist: (SmartPlaylistType) -> Unit,
    onNavigateToRestore: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val smartPlaylists by viewModel.smartPlaylists.collectAsStateWithLifecycle()
    val availableSongs by viewModel.availableSongs.collectAsStateWithLifecycle()
    val deletedPlaylistsCount by viewModel.deletedPlaylistsCount.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<String?>(null) }
    var playlistToDelete by remember { mutableStateOf<com.sukoon.music.domain.model.Playlist?>(null) }
    var playlistToRename by remember { mutableStateOf<com.sukoon.music.domain.model.Playlist?>(null) }
    var newPlaylistId by remember { mutableStateOf<Long?>(null) }
    var showAddSongsDialog by remember { mutableStateOf(false) }
    var playlistSearchQuery by remember { mutableStateOf("") }
    var playlistSortMode by remember { mutableStateOf(PlaylistSortMode.RECENT) }

    val scope = rememberCoroutineScope()
    val visiblePlaylists = remember(playlists, playlistSearchQuery, playlistSortMode) {
        val filtered = playlists.filter { playlist ->
            if (playlistSearchQuery.isBlank()) {
                true
            } else {
                playlist.name.contains(playlistSearchQuery, ignoreCase = true) ||
                    (playlist.description?.contains(playlistSearchQuery, ignoreCase = true) == true)
            }
        }
        when (playlistSortMode) {
            PlaylistSortMode.RECENT -> filtered.sortedByDescending { it.createdAt }
            PlaylistSortMode.NAME -> filtered.sortedBy { it.name.lowercase() }
            PlaylistSortMode.SONG_COUNT -> filtered.sortedByDescending { it.songCount }
        }
    }

    LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = MiniPlayerHeight + SpacingSmall,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Primary Grid Section - 4 Smart Playlists
            item {
                SmartPlaylistsSection(
                    smartPlaylists = smartPlaylists,
                    onSmartPlaylistClick = onNavigateToSmartPlaylist
                )
            }

            // Secondary Action List Section
            item {
                PlaylistActionsSection(
                    onCreateClick = { showCreateDialog = true },
                    onRestoreClick = onNavigateToRestore,
                    onImportClick = {
                        showImportDialog = true
                        importResult = null
                    },
                    deletedPlaylistsCount = deletedPlaylistsCount
                )
            }

            // User Playlists Grid
            if (playlists.isNotEmpty()) {
                item {
                    PlaylistListControls(
                        query = playlistSearchQuery,
                        onQueryChange = { playlistSearchQuery = it },
                        sortMode = playlistSortMode,
                        onSortModeChange = { playlistSortMode = it }
                    )
                }
                item {
                    val heading = if (playlistSearchQuery.isBlank()) {
                        "My playlists (${visiblePlaylists.size})"
                    } else {
                        "My playlists (${visiblePlaylists.size} of ${playlists.size})"
                    }
                    Text(
                        text = heading,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (visiblePlaylists.isNotEmpty()) {
                    items(
                        items = visiblePlaylists.chunked(2),
                        key = { row -> row.first().id }
                    ) { rowPlaylists ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowPlaylists.forEach { playlist ->
                                Box(modifier = Modifier.weight(1f)) {
                                    PlaylistCard(
                                        playlist = playlist,
                                        onClick = { onNavigateToPlaylist(playlist.id) },
                                        onDeleteClick = { playlistToDelete = playlist },
                                        onPlayClick = { viewModel.playPlaylist(playlist.id) },
                                        onPlayNextClick = { viewModel.playNextPlaylist(playlist.id) },
                                        onRenameClick = { playlistToRename = playlist }
                                    )
                                }
                            }
                            // Add empty space if row has only one playlist
                            if (rowPlaylists.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    item {
                        EmptyFilteredPlaylistsState(query = playlistSearchQuery)
                    }
                }
            } else {
                item {
                    EmptyPlaylistsState(
                        onCreateClick = { showCreateDialog = true }
                    )
                }
            }
        }

    // Create Playlist Dialog
        if (showCreateDialog) {
            CreatePlaylistDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { name, description ->
                    viewModel.createPlaylist(name, description) { playlistId ->
                        // Store the new playlist ID and show song selection dialog
                        newPlaylistId = playlistId
                        viewModel.loadPlaylist(playlistId)
                        showAddSongsDialog = true
                    }
                    showCreateDialog = false
                }
            )
        }

        // Add Songs Dialog for newly created playlist
        if (showAddSongsDialog && newPlaylistId != null) {
            AddSongsToNewPlaylistDialog(
                availableSongs = availableSongs,
                onDismiss = {
                    showAddSongsDialog = false
                    newPlaylistId?.let { onNavigateToPlaylist(it) }
                    newPlaylistId = null
                },
                onAddSongs = { selectedSongs ->
                    selectedSongs.forEach { song ->
                        viewModel.addSongToPlaylist(newPlaylistId!!, song.id)
                    }
                    showAddSongsDialog = false
                    newPlaylistId?.let { onNavigateToPlaylist(it) }
                    newPlaylistId = null
                }
            )
        }

        // Import Playlist Dialog
        if (showImportDialog) {
            ImportPlaylistDialog(
                onDismiss = {
                    showImportDialog = false
                    importResult = null
                },
                onImport = { json ->
                    scope.launch {
                        val count = viewModel.importPlaylists(json)
                        importResult = if (count > 0) {
                            "Successfully imported $count playlist${if (count > 1) "s" else ""}"
                        } else {
                            "Failed to import playlists. Check JSON format."
                        }
                    }
                },
                importResult = importResult
            )
        }

        // Delete Confirmation Dialog
        playlistToDelete?.let { playlist ->
            DeletePlaylistConfirmationDialog(
                playlistName = playlist.name,
                onDismiss = { playlistToDelete = null },
                onConfirm = {
                    viewModel.deletePlaylist(playlist.id)
                    playlistToDelete = null
                }
            )
        }

        // Rename Dialog
        playlistToRename?.let { playlist ->
            RenamePlaylistDialog(
                currentName = playlist.name,
                onDismiss = { playlistToRename = null },
                onConfirm = { newName ->
                    viewModel.updatePlaylist(playlist.copy(name = newName))
                    playlistToRename = null
                }
            )
        }
}

/**
 * Smart Playlists Section - 2x2 grid of 4 smart playlists
 */
@Composable
private fun SmartPlaylistsSection(
    smartPlaylists: List<SmartPlaylist>,
    onSmartPlaylistClick: (SmartPlaylistType) -> Unit
) {
    Column {
        Text(
            text = "Smart Playlists",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        // 2x2 Grid
        val rows = smartPlaylists.chunked(2)
        rows.forEach { rowPlaylists ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowPlaylists.forEach { smartPlaylist ->
                    Box(modifier = Modifier.weight(1f)) {
                        SmartPlaylistCard(
                            smartPlaylist = smartPlaylist,
                            onClick = { onSmartPlaylistClick(smartPlaylist.type) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Get themed background color for smart playlist cards
 */
@Composable
private fun getSmartPlaylistColor(type: SmartPlaylistType): Color {
    return when (type) {
        SmartPlaylistType.MY_FAVOURITE -> Color(0xFF4A6FA5).copy(alpha = 0.2f)  // Blue
        SmartPlaylistType.LAST_ADDED -> Color(0xFF4A9B6A).copy(alpha = 0.2f)    // Green
        SmartPlaylistType.RECENTLY_PLAYED -> Color(0xFF7E57C2).copy(alpha = 0.2f) // Purple
        SmartPlaylistType.MOST_PLAYED -> Color(0xFFE57373).copy(alpha = 0.2f)   // Red
        SmartPlaylistType.NEVER_PLAYED -> Color(0xFF90A4AE).copy(alpha = 0.2f)  // Gray-Blue
        SmartPlaylistType.DISCOVER -> Color(0xFFD4E157).copy(alpha = 0.2f)      // Lime
    }
}

/**
 * Smart Playlist Card - Large rounded rectangular card with themed colors
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartPlaylistCard(
    smartPlaylist: SmartPlaylist,
    onClick: () -> Unit
) {
    val backgroundColor = getSmartPlaylistColor(smartPlaylist.type)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
        ) {
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = smartPlaylist.title,
                        style = MaterialTheme.typography.cardTitle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${smartPlaylist.songCount} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Icon in bottom right with better visibility
            Icon(
                imageVector = getSmartPlaylistIcon(smartPlaylist.type),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
            )
        }
    }
}

/**
 * Playlist Actions Section - Create, Restore, Import
 */
@Composable
private fun PlaylistActionsSection(
    onCreateClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onImportClick: () -> Unit,
    deletedPlaylistsCount: Int
) {
    Column {
        Text(
            text = "Playlist actions",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        // Action items
        PlaylistActionItem(
            icon = Icons.Default.Add,
            text = "Create playlist",
            onClick = onCreateClick
        )

        PlaylistActionItem(
            icon = Icons.Default.Restore,
            text = "Restore from trash",
            onClick = onRestoreClick,
            trailingContent = if (deletedPlaylistsCount > 0) {
                {
                    Badge {
                        Text(deletedPlaylistsCount.toString())
                    }
                }
            } else {
                null
            }
        )

        PlaylistActionItem(
            icon = Icons.Default.Upload,
            text = "Import playlist",
            onClick = onImportClick
        )
    }
}

/**
 * Individual action item in the list
 */
@Composable
private fun PlaylistActionItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    trailingContent: (@Composable (() -> Unit))? = null
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            trailingContent?.invoke()
        }
    }
}

@Composable
private fun PlaylistListControls(
    query: String,
    onQueryChange: (String) -> Unit,
    sortMode: PlaylistSortMode,
    onSortModeChange: (PlaylistSortMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search playlists...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search playlists"
                )
            }
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PlaylistSortMode.values().toList()) { mode ->
                FilterChip(
                    selected = sortMode == mode,
                    onClick = { onSortModeChange(mode) },
                    label = { Text(mode.label) }
                )
            }
        }
    }
}

@Composable
private fun EmptyFilteredPlaylistsState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No playlists match \"$query\"",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try another search or change sorting.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlaylistsGrid(
    playlists: List<Playlist>,
    adMobManager: com.sukoon.music.data.ads.AdMobManager,
    onPlaylistClick: (Long) -> Unit,
    onDeletePlaylist: (Long) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Intersperse native ads every 6 playlists
        val adInterval = 6
        var itemIndex = 0

        playlists.forEachIndexed { index, playlist ->
            // Add native ad before this playlist if we've shown enough playlists
            if (index > 0 && index % adInterval == 0) {
                item(key = "ad_$itemIndex") {
                    // Native ad takes full width (spans 2 columns)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        NativeAdCard(
                            adMobManager = adMobManager,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                itemIndex++
            }

            // Regular playlist card
            item(key = playlist.id) {
                PlaylistCard(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist.id) },
                    onDeleteClick = { onDeletePlaylist(playlist.id) },
                    onPlayClick = {},
                    onPlayNextClick = {},
                    onRenameClick = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPlayClick: () -> Unit,
    onPlayNextClick: () -> Unit,
    onRenameClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Cover Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (playlist.coverImageUri != null) {
                    SubcomposeAsyncImage(
                        model = playlist.coverImageUri,
                        contentDescription = "Playlist cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        error = {
                            DefaultPlaylistCover(playlist.id)
                        }
                    )
                } else {
                    DefaultPlaylistCover(playlist.id)
                }

                // Options menu button with semi-transparent background
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Play") },
                            onClick = {
                                onPlayClick()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Play next") },
                            onClick = {
                                onPlayNextClick()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                onRenameClick()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                onDeleteClick()
                                showMenu = false
                            }
                        )
                    }
                }
            }

            // Playlist Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.cardTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${playlist.songCount} songs",
                    style = MaterialTheme.typography.cardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DefaultPlaylistCover(playlistId: Long) {
    PlaceholderAlbumArt.Placeholder(
        seed = playlistId.toString(),
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
        icon = Icons.Default.MusicNote,
        iconSize = 64,
        iconOpacity = 0.35f
    )
}

@Composable
private fun EmptyPlaylistsState(
    onCreateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Decorative folder illustration with theme accent color
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .offset(x = 12.dp, y = (-8).dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No playlists yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap the button below to create one",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create playlist")
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Playlist") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, description.ifBlank { null })
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for renaming a playlist.
 */
@Composable
fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && name != currentName) {
                        onConfirm(name)
                    }
                },
                enabled = name.isNotBlank() && name != currentName
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Delete Playlist Confirmation Dialog
 */
@Composable
fun DeletePlaylistConfirmationDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Playlist?") },
        text = {
            Text("Are you sure you want to delete \"$playlistName\"? This will move it to trash where you can restore it later.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Import Playlist Dialog - Input JSON to import playlists
 */
@Composable
fun ImportPlaylistDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    importResult: String?
) {
    var json by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Playlist") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Paste playlist JSON data below:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = json,
                    onValueChange = { json = it },
                    label = { Text("JSON Data") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10,
                    placeholder = {
                        Text("{\"version\":1,\"playlists\":[...]}")
                    }
                )
                if (importResult != null) {
                    Text(
                        text = importResult,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (importResult.startsWith("Successfully"))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (json.isNotBlank()) {
                        onImport(json)
                    }
                },
                enabled = json.isNotBlank()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Filter chips for filtering playlists
 */
@Composable
private fun PlaylistFilterChips(
    selectedFilter: Boolean?,
    onFilterChange: (Boolean?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterChange(null) },
                label = { Text("All") },
                modifier = if (selectedFilter != null) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .surfaceLevel2Gradient()
                } else {
                    Modifier
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (selectedFilter != null) Color.Transparent else MaterialTheme.colorScheme.primary,
                    labelColor = if (selectedFilter != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                )
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == true,
                onClick = { onFilterChange(true) },
                label = { Text("Smart Playlists") },
                leadingIcon = if (selectedFilter == true) {
                    { Icon(Icons.Default.Star, null, Modifier.size(18.dp)) }
                } else null,
                modifier = if (selectedFilter != true) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .surfaceLevel2Gradient()
                } else {
                    Modifier
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (selectedFilter != true) Color.Transparent else MaterialTheme.colorScheme.primary,
                    labelColor = if (selectedFilter != true) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                )
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == false,
                onClick = { onFilterChange(false) },
                label = { Text("My Playlists") },
                leadingIcon = if (selectedFilter == false) {
                    { Icon(Icons.Default.Folder, null, Modifier.size(18.dp)) }
                } else null,
                modifier = if (selectedFilter != false) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .surfaceLevel2Gradient()
                } else {
                    Modifier
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (selectedFilter != false) Color.Transparent else MaterialTheme.colorScheme.primary,
                    labelColor = if (selectedFilter != false) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

/**
 * Mini Player for playlist screens - Shows current song with playback controls
 */
@Composable
private fun PlaylistMiniPlayer(
    playbackState: com.sukoon.music.domain.model.PlaybackState,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (playbackState.currentSong?.albumArtUri != null) {
                    SubcomposeAsyncImage(
                        model = playbackState.currentSong.albumArtUri,
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        error = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playbackState.currentSong?.title ?: "No song playing",
                    style = MaterialTheme.typography.listItemTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = playbackState.currentSong?.artist ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Playback controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousClick) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                IconButton(onClick = onNextClick) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Dialog for selecting songs to add to a newly created playlist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSongsToNewPlaylistDialog(
    availableSongs: List<com.sukoon.music.domain.model.Song>,
    onDismiss: () -> Unit,
    onAddSongs: (List<com.sukoon.music.domain.model.Song>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSongs by remember { mutableStateOf(setOf<Long>()) }

    val filteredSongs = remember(availableSongs, searchQuery) {
        if (searchQuery.isBlank()) {
            availableSongs
        } else {
            availableSongs.filter { song ->
                song.title.contains(searchQuery, ignoreCase = true) ||
                        song.artist.contains(searchQuery, ignoreCase = true) ||
                        song.album.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Songs to Playlist",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selectedSongs.isNotEmpty()) {
                        Text(
                            text = "${selectedSongs.size} selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End
                        )
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Search songs...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Songs List
                if (filteredSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) {
                                "No songs available"
                            } else {
                                "No songs found"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        itemsIndexed(
                            items = filteredSongs,
                            key = { _: Int, song: com.sukoon.music.domain.model.Song -> song.id }
                        ) { _: Int, song: com.sukoon.music.domain.model.Song ->
                            val isSelected = selectedSongs.contains(song.id)

                            Surface(
                                onClick = {
                                    selectedSongs = if (isSelected) {
                                        selectedSongs - song.id
                                    } else {
                                        selectedSongs + song.id
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                } else {
                                    Color.Transparent
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Checkbox
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Album Art
                                    Card(
                                        modifier = Modifier.size(48.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                alpha = 0.5f
                                                            )
                                                        )
                                                    },
                                                    error = {
                                                        Icon(
                                                            imageVector = Icons.Default.MusicNote,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(24.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                alpha = 0.5f
                                                            )
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
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = song.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Skip")
                    }

                    Button(
                        onClick = {
                            val songsToAdd = availableSongs.filter { it.id in selectedSongs }
                            onAddSongs(songsToAdd)
                        },
                        enabled = selectedSongs.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (selectedSongs.isEmpty()) {
                                "Add"
                            } else {
                                "Add (${selectedSongs.size})"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun PlaylistsScreenPreview() {
    SukoonMusicPlayerTheme(theme = AppTheme.DARK) {
        PlaylistsScreen(
            onNavigateToPlaylist = {},
            onNavigateToSmartPlaylist = {},
            onNavigateToRestore = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaylistCardPreview() {
    SukoonMusicPlayerTheme {
        PlaylistCard(
            playlist = Playlist(
                id = 1,
                name = "My Favorite Songs",
                description = "The best tracks",
                songCount = 42
            ),
            onClick = {},
            onDeleteClick = {},
            onPlayClick = {},
            onPlayNextClick = {},
            onRenameClick = {}
        )
    }
}

/**
 * Playlists Content - Displays smart playlists and user playlists inline within HomeScreen.
 */
/* @Composable
private fun PlaylistContent(
    playlists: List<Playlist>,
    smartPlaylists: List<SmartPlaylist>,
    playlistFilter: Boolean?,
    onFilterChange: (Boolean?) -> Unit,

    onPlaylistClick: (Long) -> Unit,
    onSmartPlaylistClick: (SmartPlaylistType) -> Unit,

    onCreatePlaylistClick: () -> Unit,
    onDeletePlaylistClick: (Long) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistFilter by remember { mutableStateOf<Boolean?>(null) }
    var newPlaylistId by remember { mutableStateOf<Long?>(null) }
    var showAddSongsDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 0.dp, bottom = 8.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filter Chips Section
        item {
            PlaylistFilterChips(
                selectedFilter = playlistFilter,
                onFilterChange = { playlistFilter = it }
            )
        }

        // Smart Playlists Section (show only if filter allows)
        if (playlistFilter == null || playlistFilter == true) {
            item {
                SmartPlaylistsSection(
                    smartPlaylists = smartPlaylists,
                    onSmartPlaylistClick = onSmartPlaylistClick
                )
            }
        }
        // Actions and User Playlists (show only if filter allows)
        if (playlistFilter == null || playlistFilter == false) {
            item {
                PlaylistActionsSection(
                    playlistCount = playlists.size,
                    onCreateClick = { showCreateDialog = true }
                )
            }

            // User Playlists Grid
            if (playlists.isNotEmpty()) {
                item {
                    Text(
                        text = "My playlists (${playlists.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(
                    items = playlists.chunked(2),
                    key = { row: List<Playlist> -> row.first().id }
                ) { rowPlaylists: List<Playlist> ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowPlaylists.forEach { playlist ->
                            Box(modifier = Modifier.weight(1f)) {
                                PlaylistCard(
                                    playlist = playlist,
                                    onClick = { onPlaylistClick(playlist.id) },
                                    onDeleteClick = { playlistToDelete = playlist },
                                    onPlayClick = {},
                                    onPlayNextClick = {},
                                    onRenameClick = {}
                                )
                            }
                        }
                        if (rowPlaylists.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No playlists yet. Create your first playlist!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Create Playlist Dialog
    if (showCreateDialog) {
        HomeScreenCreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description ->
                playlistViewModel.createPlaylist(name, description) { playlistId ->
                    newPlaylistId = playlistId
                    playlistViewModel.loadPlaylist(playlistId)
                    showAddSongsDialog = true
                }
                showCreateDialog = false
            }
        )
    }

    // Add Songs Dialog
    if (showAddSongsDialog && newPlaylistId != null) {
        HomeScreenAddSongsToNewPlaylistDialog(
            availableSongs = availableSongs,
            onDismiss = {
                showAddSongsDialog = false
                newPlaylistId?.let { onPlaylistClick(it) }
                newPlaylistId = null
            },
            onAddSongs = { selectedSongs ->
                selectedSongs.forEach { song ->
                    playlistViewModel.addSongToPlaylist(newPlaylistId!!, song.id)
                }
                showAddSongsDialog = false
                newPlaylistId?.let { onPlaylistClick(it) }
                newPlaylistId = null
            }
        )
    }

    // Delete Confirmation
    playlistToDelete?.let { playlist ->
        HomeScreenDeletePlaylistConfirmationDialog(
            playlistName = playlist.name,
            onDismiss = { playlistToDelete = null },
            onConfirm = {
                playlistViewModel.deletePlaylist(playlist.id)
                playlistToDelete = null
            }
        )
    }
} */

@Composable
private fun HomeScreenDeletePlaylistConfirmationDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Playlist") },
        text = { Text("Are you sure you want to delete '$playlistName'?") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
private fun HomeScreenCreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Playlist") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description.ifBlank { null }) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = { name = ""; description = ""; onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenAddSongsToNewPlaylistDialog(
    availableSongs: List<Song>,
    onDismiss: () -> Unit,
    onAddSongs: (List<Song>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSongs by remember { mutableStateOf(setOf<Long>()) }

    val filteredSongs = remember(availableSongs, searchQuery) {
        if (searchQuery.isBlank()) {
            availableSongs
        } else {
            availableSongs.filter { song ->
                song.title.contains(searchQuery, ignoreCase = true) ||
                        song.artist.contains(searchQuery, ignoreCase = true) ||
                        song.album.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Songs to Playlist",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    if (selectedSongs.isNotEmpty()) {
                        Text(
                            text = "${selectedSongs.size} selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Search songs...") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (filteredSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No songs available" else "No songs found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        itemsIndexed(
                            items = filteredSongs,
                            key = { _: Int, song: Song -> song.id }
                        ) { _: Int, song: Song ->
                            val isSelected = selectedSongs.contains(song.id)

                            Surface(
                                onClick = {
                                    selectedSongs = if (isSelected) {
                                        selectedSongs - song.id
                                    } else {
                                        selectedSongs + song.id
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                } else {
                                    Color.Transparent
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (song.albumArtUri != null) {
                                            SubcomposeAsyncImage(
                                                model = song.albumArtUri,
                                                contentDescription = "Album art",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
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

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = song.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Skip")
                    }

                    Button(
                        onClick = {
                            val songsToAdd = availableSongs.filter { it.id in selectedSongs }
                            onAddSongs(songsToAdd)
                        },
                        enabled = selectedSongs.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (selectedSongs.isEmpty()) "Add" else "Add (${selectedSongs.size})"
                        )
                    }
                }
            }
        }
    }
}

