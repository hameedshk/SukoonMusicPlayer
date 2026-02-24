package com.sukoon.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Playlist
import com.sukoon.music.domain.model.SmartPlaylist
import com.sukoon.music.domain.model.SmartPlaylistType
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.getSmartPlaylistIcon
import com.sukoon.music.ui.components.SortOption
import com.sukoon.music.R
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.components.PlaceholderAlbumArt
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.PlaylistSortMode
import com.sukoon.music.ui.viewmodel.PlaylistSortOrder
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import com.sukoon.music.ui.theme.*

/**
 * Playlists Screen - Shows smart playlists and user playlists.
 *
 * Features:
 * - Quick actions row: create, import, restore
 * - User playlists as the primary section with search + sorting
 * - Smart playlists as a secondary browsing section
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    onNavigateToPlaylist: (Long) -> Unit,
    onNavigateToSmartPlaylist: (SmartPlaylistType) -> Unit,
    onNavigateToRestore: () -> Unit,
    onBackClick: () -> Unit,
    additionalBottomInset: Dp = 0.dp,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val visiblePlaylists by viewModel.visiblePlaylists.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val smartPlaylists by viewModel.smartPlaylists.collectAsStateWithLifecycle()
    val availableSongs by viewModel.availableSongs.collectAsStateWithLifecycle()
    val deletedPlaylistsCount by viewModel.deletedPlaylistsCount.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<String?>(null) }
    var importSuccess by remember { mutableStateOf<Boolean?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var createDialogInitialName by remember { mutableStateOf("") }
    var newPlaylistId by remember { mutableStateOf<Long?>(null) }
    var showAddSongsDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val playlistSubtitle = if (searchQuery.isBlank()) {
        pluralStringResource(
            id = R.plurals.playlists_screen_playlist_count,
            count = visiblePlaylists.size,
            visiblePlaylists.size
        )
    } else {
        stringResource(
            R.string.playlists_screen_visible_of_total_results,
            visiblePlaylists.size,
            playlists.size
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 0.dp,
            bottom = MiniPlayerHeight + SpacingSmall + additionalBottomInset,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SmartPlaylistsSection(
                smartPlaylists = smartPlaylists,
                onSmartPlaylistClick = onNavigateToSmartPlaylist
            )
        }

        item {
            PlaylistActionsSection(
                onCreateClick = {
                    createDialogInitialName = ""
                    showCreateDialog = true
                },
                onRestoreClick = onNavigateToRestore,
                onImportClick = {
                    showImportDialog = true
                    importResult = null
                    importSuccess = null
                },
                deletedPlaylistsCount = deletedPlaylistsCount
            )
        }

        if (playlists.isNotEmpty()) {
    item {
        PlaylistSortHeader(
            playlistCount = playlists.size,
            subtitle = playlistSubtitle,
            onSortClick = { showSortDialog = true }
        )
    }
            item {
                PlaylistSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) }
                )
            }

            if (visiblePlaylists.isNotEmpty()) {
                items(
                    items = visiblePlaylists,
                    key = { it.id }
                ) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        onClick = { onNavigateToPlaylist(playlist.id) },
                        onPlay = { viewModel.playPlaylist(playlist.id) },
                        onPlayNext = { viewModel.playNextPlaylist(playlist.id) },
                        onRename = { playlistToRename = playlist },
                        onDelete = { playlistToDelete = playlist }
                    )
                }
            } else {
                item {
                    EmptyFilteredPlaylistsState(
                        query = searchQuery,
                        onCreateFromQuery = { query ->
                            createDialogInitialName = query
                            showCreateDialog = true
                        }
                    )
                }
            }
        } else {
            item {
                EmptyPlaylistsState(
                    onCreateClick = {
                        createDialogInitialName = ""
                        showCreateDialog = true
                    }
                )
            }
        }
    }

    if (showSortDialog) {
        PlaylistSortDialog(
            currentMode = sortMode,
            currentOrder = sortOrder,
            onDismiss = { showSortDialog = false },
            onSortModeChange = {
                viewModel.setSortMode(it)
                showSortDialog = false
            },
            onOrderChange = {
                viewModel.setSortOrder(it)
                showSortDialog = false
            }
        )
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            initialName = createDialogInitialName,
            onDismiss = {
                showCreateDialog = false
                createDialogInitialName = ""
            },
            onConfirm = { name, description ->
                viewModel.createPlaylist(name, description) { playlistId ->
                    newPlaylistId = playlistId
                    viewModel.loadPlaylist(playlistId)
                    showAddSongsDialog = true
                }
                showCreateDialog = false
                createDialogInitialName = ""
            }
        )
    }

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

    if (showImportDialog) {
        ImportPlaylistDialog(
            onDismiss = {
                showImportDialog = false
                importResult = null
                importSuccess = null
            },
            onImport = { json ->
                scope.launch {
                    val count = viewModel.importPlaylists(json)
                    importResult = if (count > 0) {
                        context.resources.getQuantityString(
                            R.plurals.playlists_screen_import_success,
                            count,
                            count
                        )
                    } else {
                        context.getString(R.string.playlists_screen_import_failed)
                    }
                    importSuccess = count > 0
                }
            },
            importResult = importResult,
            importSuccess = importSuccess
        )
    }

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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.playlists_screen_smart_playlists_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.playlists_screen_smart_playlists_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val rows = smartPlaylists.chunked(2)
        rows.forEach { rowPlaylists ->
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                if (rowPlaylists.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
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
        SmartPlaylistType.MY_FAVOURITE -> Color(0xFF5E7FA3)
        SmartPlaylistType.LAST_ADDED -> Color(0xFF5A8A74)
        SmartPlaylistType.RECENTLY_PLAYED -> Color(0xFF6D7FA1)
        SmartPlaylistType.MOST_PLAYED -> Color(0xFFB06D6A)
        SmartPlaylistType.NEVER_PLAYED -> Color(0xFF6E8894)
        SmartPlaylistType.DISCOVER -> Color(0xFF9AA75E)
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
    val accentColor = getSmartPlaylistColor(smartPlaylist.type)
    val smartPlaylistSongCountText = pluralStringResource(
        id = R.plurals.playlists_screen_song_count,
        count = smartPlaylist.songCount,
        smartPlaylist.songCount
    )
    val smartPlaylistContentDescription = stringResource(
        R.string.playlists_screen_playlist_content_description,
        smartPlaylist.title,
        smartPlaylistSongCountText
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(14.dp)
            )
            .semantics {
                contentDescription = smartPlaylistContentDescription
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(accentColor.copy(alpha = 0.7f))
            )
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getSmartPlaylistIcon(smartPlaylist.type),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = accentColor
                    )
                }
                Text(
                    text = smartPlaylist.title,
                    style = MaterialTheme.typography.cardTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accentColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = smartPlaylistSongCountText,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Quick actions row for playlist tab.
 */
@Composable
private fun PlaylistActionsSection(
    onCreateClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onImportClick: () -> Unit,
    deletedPlaylistsCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.playlists_screen_quick_actions_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Add,
                    text = stringResource(R.string.common_create),
                    onClick = onCreateClick,
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
                QuickActionButton(
                    icon = Icons.Default.Upload,
                    text = stringResource(R.string.common_import),
                    onClick = onImportClick,
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.tertiary
                )
                QuickActionButton(
                    icon = Icons.Default.Restore,
                    text = stringResource(R.string.playlists_screen_restore_action),
                    onClick = onRestoreClick,
                    modifier = Modifier.weight(1f),
                    badgeCount = deletedPlaylistsCount,
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

/**
 * Compact quick action button used in the top row.
 */
@Composable
private fun QuickActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(84.dp)
            .semantics { contentDescription = text },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(
            width = 1.dp,
            color = contentColor.copy(alpha = 0.22f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 1.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(18.dp),
                        tint = contentColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = contentColor
                )
            }
            if (badgeCount > 0) {
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(badgeCount.toString())
                }
            }
        }
    }
}

@Composable
private fun PlaylistSortHeader(
    playlistCount: Int,
    subtitle: String,
    onSortClick: () -> Unit
) {
    val accentTokens = accent()
    val sortInteractionSource = remember { MutableInteractionSource() }
    val isSortPressed by sortInteractionSource.collectIsPressedAsState()
    val pressedContainerColor = accentTokens.softBg.copy(alpha = 0.30f)
    val actionIconColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pluralStringResource(
                    id = R.plurals.playlists_screen_playlist_count,
                    count = playlistCount,
                    playlistCount
                ),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    contentDescription = stringResource(R.string.library_common_sort),
                    tint = if (isSortPressed) accentTokens.active else actionIconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaylistSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.playlists_screen_search_content_description),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.playlists_screen_search_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.playlists_screen_clear_search_content_description)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val playlistSongCountText = pluralStringResource(
        id = R.plurals.playlists_screen_song_count,
        count = playlist.songCount,
        playlist.songCount
    )
    val playlistContentDescription = stringResource(
        R.string.playlists_screen_playlist_content_description,
        playlist.name,
        playlistSongCountText
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = playlistContentDescription },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (playlist.coverImageUri != null) {
                    SubcomposeAsyncImage(
                        model = playlist.coverImageUri,
                        contentDescription = stringResource(R.string.playlists_screen_playlist_cover_content_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        },
                        error = {
                            DefaultPlaylistCover(playlist.id)
                        }
                    )
                } else {
                    DefaultPlaylistCover(playlist.id)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playlistSongCountText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.playlists_screen_play_playlist_content_description),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.playlists_screen_options_content_description)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.common_play_next)) },
                        onClick = {
                            onPlayNext()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.common_rename)) },
                        onClick = {
                            onRename()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.common_delete)) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistSortDialog(
    currentMode: PlaylistSortMode,
    currentOrder: PlaylistSortOrder,
    onDismiss: () -> Unit,
    onSortModeChange: (PlaylistSortMode) -> Unit,
    onOrderChange: (PlaylistSortOrder) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.common_sort_by)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PlaylistSortMode.values().forEach { mode ->
                    SortOption(
                        text = stringResource(
                            when (mode) {
                                PlaylistSortMode.RECENT -> R.string.playlists_screen_sort_recent
                                PlaylistSortMode.NAME -> R.string.playlists_screen_sort_name
                                PlaylistSortMode.SONG_COUNT -> R.string.playlists_screen_sort_songs
                            }
                        ),
                        isSelected = currentMode == mode,
                        onClick = { onSortModeChange(mode) }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentOrder == PlaylistSortOrder.ASCENDING,
                        onClick = { onOrderChange(PlaylistSortOrder.ASCENDING) },
                        label = {
                            Text(
                                stringResource(R.string.library_common_sort_order_a_to_z),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    FilterChip(
                        selected = currentOrder == PlaylistSortOrder.DESCENDING,
                        onClick = { onOrderChange(PlaylistSortOrder.DESCENDING) },
                        label = {
                            Text(
                                stringResource(R.string.library_common_sort_order_z_to_a),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_ok))
            }
        }
    )
}

@Composable
private fun EmptyFilteredPlaylistsState(
    query: String,
    onCreateFromQuery: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.playlists_screen_no_matches_title, query),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.playlists_screen_no_matches_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (query.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onCreateFromQuery(query.trim()) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.playlists_screen_create_query, query))
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
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        icon = Icons.Default.MusicNote,
        iconSize = 56,
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
            .padding(vertical = 56.dp, horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp
        ) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.playlists_screen_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.playlists_screen_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.label_create_playlist))
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlists_screen_create_playlist_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.common_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.common_description_optional)) },
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
                Text(stringResource(R.string.common_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
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
        title = { Text(stringResource(R.string.playlists_screen_rename_playlist_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlists_screen_playlist_name_label)) },
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
                Text(stringResource(R.string.common_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
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
        title = { Text(stringResource(R.string.dialog_delete_playlist_title)) },
        text = {
            Text(stringResource(R.string.playlists_screen_delete_playlist_message, playlistName))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.common_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
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
    importResult: String?,
    importSuccess: Boolean?
) {
    var json by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_import_playlist_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.playlists_screen_import_paste_json_hint),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = json,
                    onValueChange = { json = it },
                    label = { Text(stringResource(R.string.dialog_json_data_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10,
                    placeholder = {
                        Text(stringResource(R.string.playlists_screen_import_json_placeholder))
                    }
                )
                if (importResult != null) {
                    Text(
                        text = importResult,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (importSuccess == true)
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
                Text(stringResource(R.string.common_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

/**
 * Mini Player for playlist screens - Shows current song with playback controls
 */
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

    BasicAlertDialog(
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
                        text = stringResource(R.string.playlists_screen_add_songs_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selectedSongs.isNotEmpty()) {
                        Text(
                            text = pluralStringResource(
                                id = R.plurals.playlists_screen_selected_count,
                                count = selectedSongs.size,
                                selectedSongs.size
                            ),
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
                    placeholder = { Text(stringResource(R.string.dialog_search_songs_placeholder)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.playlists_screen_search_content_description)
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
                                stringResource(R.string.playlists_screen_no_songs_available)
                            } else {
                                stringResource(R.string.playlists_screen_no_songs_found)
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
                                                    contentDescription = stringResource(R.string.playlists_screen_album_art_content_description),
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
                        Text(stringResource(R.string.common_skip))
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
                                stringResource(R.string.playlists_screen_add_button)
                            } else {
                                stringResource(R.string.playlists_screen_add_button_with_count, selectedSongs.size)
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


