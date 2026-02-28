package com.sukoon.music.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.ui.navigation.Routes
import com.sukoon.music.data.premium.PremiumManager
import kotlinx.coroutines.flow.flowOf
import com.sukoon.music.domain.model.Folder
import com.sukoon.music.domain.model.FolderSortMode
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.*
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.theme.SpacingLarge
import com.sukoon.music.ui.viewmodel.FolderViewModel
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import com.sukoon.music.ui.theme.*
/**
 * Folders Screen - Displays local music folders with enhanced management.
 *
 * Features:
 * - Toggle between active directories and hidden folders.
 * - Advanced sorting (Name, Track Count, Duration, etc.).
 * - Context actions: Play, Add to Queue, Hide/Unhide, SAF Deletion.
 * - Global MiniPlayer and Ad integration.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FoldersScreen(
    onNavigateToFolder: (Long) -> Unit,
    onNavigateToFolderByPath: (String) -> Unit = {},
    onNavigateToFolderSelection: () -> Unit = {},
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToArtist: (Long) -> Unit = {},
    onBackClick: () -> Unit,
    navController: NavController? = null,
    premiumManager: PremiumManager? = null,
    viewModel: FolderViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val hiddenFolders by viewModel.hiddenFolders.collectAsStateWithLifecycle()
    val folderViewMode by viewModel.folderViewMode.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val selectedFolderForPlaylist by viewModel.selectedFolderForPlaylist.collectAsStateWithLifecycle()
    val deleteResult by viewModel.deleteResult.collectAsStateWithLifecycle()
    val selectedFolderIds by viewModel.selectedFolderIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()

    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()
    val isPremium by (premiumManager?.isPremiumUser ?: flowOf(false)).collectAsStateWithLifecycle(false)

    var folderToDelete by remember { mutableStateOf<Long?>(null) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var songForInfo by remember { mutableStateOf<Song?>(null) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // SAF delete permission launcher (for folders)
    val deletePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.clearDeleteResult()
        } else {
            viewModel.clearDeleteResult()
        }
    }

    // Song delete launcher
    val songDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_song_deleted_successfully), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_delete_cancelled), Toast.LENGTH_SHORT).show()
        }
        songToDelete = null
    }

    // Handle delete result
    LaunchedEffect(deleteResult) {
        when (val result = deleteResult) {
            is DeleteHelper.DeleteResult.RequiresPermission -> {
                deletePermissionLauncher.launch(
                    IntentSenderRequest.Builder(result.intentSender).build()
                )
            }
            is DeleteHelper.DeleteResult.Success -> {
                viewModel.clearDeleteResult()
            }
            is DeleteHelper.DeleteResult.Error -> {
                viewModel.clearDeleteResult()
            }
            null -> {}
        }
    }

    val shareHandler = rememberShareHandler()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                com.sukoon.music.R.string.label_selected_count,
                                selectedFolderIds.size
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleSelectionMode(false) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(com.sukoon.music.R.string.common_exit_selection)
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isSelectionMode && selectedFolderIds.isNotEmpty()) {
                MultiSelectActionBottomBar(
                    onPlay = { viewModel.playSelectedFolders() },
                    onAddToPlaylist = { viewModel.showAddSelectedToPlaylistDialog() },
                    onDelete = { viewModel.deleteSelectedFolders() },
                    onPlayNext = { viewModel.playSelectedFoldersNext() },
                    onAddToQueue = { viewModel.addSelectedFoldersToQueue() }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isSelectionMode) Modifier.padding(paddingValues) else Modifier
                )
        ) {
            FoldersContent(
                folders = folders,
                hiddenFolders = hiddenFolders,
                folderViewMode = folderViewMode,
                sortMode = sortMode,
                playbackState = playbackState,
                isSelectionMode = isSelectionMode,
                selectedFolderIds = selectedFolderIds,
                onFolderClick = onNavigateToFolder,
                onNavigateToFolderByPath = onNavigateToFolderByPath,
                onNavigateToFolderSelection = onNavigateToFolderSelection,
                folderViewModel = viewModel,
                menuHandler = rememberSongMenuHandler(
                    playbackRepository = viewModel.playbackRepository,
                    onNavigateToAlbum = onNavigateToAlbum,
                    onNavigateToArtist = onNavigateToArtist,
                    onShowPlaylistSelector = { song ->
                        songToAddToPlaylist = song
                        showAddToPlaylistDialog = true
                    },
                    onShowSongInfo = { song ->
                        songForInfo = song
                    },
                    onShowDeleteConfirmation = { song -> songToDelete = song },
                    onShowEditAudio = { song ->
                        if (isPremium) {
                            navController?.navigate("audio_editor/${song.id}")
                        } else {
                            navController?.navigate(Routes.Settings.createRoute(openPremiumDialog = true))
                        }
                    },
                    onToggleLike = { songId, isLiked ->
                        viewModel.toggleLike(songId, isLiked)
                    },
                    onShare = shareHandler
                ),
                onNavigateToNowPlaying = onNavigateToNowPlaying
            )
        }
    }

    // Song Info Dialog
    songForInfo?.let { song ->
        SongInfoDialog(
            song = song,
            onDismiss = { songForInfo = null }
        )
    }

    // Add to Playlist Dialog (for songs)
    if (showAddToPlaylistDialog && songToAddToPlaylist != null) {
        AddToPlaylistDialog(
            playlists = playlists,
            onPlaylistSelected = { playlistId ->
                playlistViewModel.addSongToPlaylist(playlistId, songToAddToPlaylist!!.id)
                showAddToPlaylistDialog = false
                songToAddToPlaylist = null
            },
            onDismiss = {
                showAddToPlaylistDialog = false
                songToAddToPlaylist = null
            }
        )
    }

    // Add to playlist dialog (for folders)
    if (selectedFolderForPlaylist != null) {
        AddToPlaylistDialog(
            playlists = playlists,
            onPlaylistSelected = { playlistId ->
                viewModel.addFolderToPlaylist(selectedFolderForPlaylist!!, playlistId)
            },
            onDismiss = { viewModel.dismissPlaylistDialog() }
        )
    }

    // Delete confirmation dialog (for folders)
    folderToDelete?.let { folderId ->
        val allAvailableFolders = if (folderViewMode == FolderViewMode.DIRECTORIES) folders else hiddenFolders
        val folder = allAvailableFolders.find { it.id == folderId }
        if (folder != null) {
            DeleteConfirmationDialog(
                folder = folder,
                onConfirm = {
                    viewModel.deleteFolder(folderId)
                    folderToDelete = null
                },
                onDismiss = {
                    folderToDelete = null
                }
            )
        }
    }

    // Delete confirmation dialog (for songs)
    songToDelete?.let { song ->
        com.sukoon.music.ui.components.DeleteConfirmationDialog(
            song = song,
            onConfirm = {
                when (val result = DeleteHelper.deleteSongs(context, listOf(song))) {
                    is DeleteHelper.DeleteResult.RequiresPermission -> {
                        songDeleteLauncher.launch(
                            IntentSenderRequest.Builder(result.intentSender).build()
                        )
                    }
                    is DeleteHelper.DeleteResult.Success -> {
                        Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_song_deleted_successfully), Toast.LENGTH_SHORT).show()
                        songToDelete = null
                    }
                    is DeleteHelper.DeleteResult.Error -> {
                        Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_error_with_message, result.message), Toast.LENGTH_SHORT).show()
                        songToDelete = null
                    }
                }
            },
            onDismiss = { songToDelete = null }
        )
    }
}

@Composable
private fun EmptyFoldersState(
    isHiddenView: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = ContentTopPadding,
                bottom = ContentBottomPadding + 16.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isHiddenView) Icons.Default.VisibilityOff else Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isHiddenView) {
                    androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.folders_empty_hidden_title)
                } else {
                    androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.folders_empty_visible_title)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isHiddenView)
                    androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.folders_empty_hidden_message)
                else
                    androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.folders_empty_visible_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    folder: Folder,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_delete_folder_title)) },
        text = {
            Column {
                Text(
                    androidx.compose.ui.res.pluralStringResource(
                        com.sukoon.music.R.plurals.folders_delete_confirmation_message,
                        folder.songCount,
                        folder.songCount
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = folder.path,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.folders_delete_confirmation_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_cancel))
            }
        }
    )
}

@Composable
fun FolderSortMode.toDisplayName(): String = when (this) {
    FolderSortMode.NAME_ASC -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.folders_sort_name_asc)
    FolderSortMode.NAME_DESC -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.folders_sort_name_desc)
    FolderSortMode.TRACK_COUNT -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.folders_sort_track_count)
    FolderSortMode.RECENTLY_MODIFIED -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.folders_sort_recently_modified)
    FolderSortMode.DURATION -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.folders_sort_duration)
}

@Preview(showBackground = true)
@Composable
private fun FoldersScreenPreview() {
    SukoonMusicPlayerTheme {
        EmptyFoldersState()
    }
}

/**
 * Folders Content - Displays music folders with management options inline within HomeScreen.
 */
@Composable
private fun FoldersContent(
    folders: List<Folder>,
    hiddenFolders: List<Folder>,
    folderViewMode: FolderViewMode,
    sortMode: FolderSortMode,
    playbackState: PlaybackState,
    isSelectionMode: Boolean,
    selectedFolderIds: Set<Long>,
    onFolderClick: (Long) -> Unit,
    onNavigateToFolderByPath: (String) -> Unit = {},
    onNavigateToFolderSelection: () -> Unit = {},
    folderViewModel: com.sukoon.music.ui.viewmodel.FolderViewModel,
    menuHandler: SongMenuHandler,
    onNavigateToNowPlaying: () -> Unit
) {
    var showSortDialog by remember { mutableStateOf(false) }
    var selectedFolderForMenu by remember { mutableStateOf<Folder?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<Folder?>(null) }
    var folderToDeleteId by remember { mutableStateOf<Long?>(null) }

    val browsingContent by folderViewModel.browsingContent.collectAsStateWithLifecycle()
    val currentPath by folderViewModel.currentPath.collectAsStateWithLifecycle()

    val displayFolders = if (folderViewMode == FolderViewMode.DIRECTORIES) {
        folders
    } else {
        hiddenFolders
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (showSortDialog) {
            AlertDialog(
                onDismissRequest = { showSortDialog = false },
                title = { Text(stringResource(com.sukoon.music.R.string.common_sort_by)) },
                text = {
                    Column {
                        FolderSortMode.values().forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        folderViewModel.setSortMode(mode)
                                        showSortDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = mode.toDisplayName())
                                if (mode == sortMode) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSortDialog = false }) {
                        Text(stringResource(com.sukoon.music.R.string.common_close))
                    }
                }
            )
        }

        if (!isSelectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingLarge, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${displayFolders.size} ${stringResource(com.sukoon.music.R.string.home_tab_folders)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    IconButton(onClick = { showSortDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(com.sukoon.music.R.string.library_common_sort)
                        )
                    }
                    IconButton(
                        onClick = { onNavigateToFolderSelection() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(com.sukoon.music.R.string.common_select)
                        )
                    }
                }
            }
        }

// Global folder navigation header (Samsung-style)
        if (folderViewMode == FolderViewMode.DIRECTORIES) {
            currentPath?.let { path ->

                var folderMenuExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(horizontal = SpacingLarge, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ?? Global UP button
                    IconButton(onClick = { folderViewModel.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.folders_cd_go_to_parent_folder),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // ?? Navigation Title (Current folder name)
                    Text(
                        text = path.substringAfterLast('/').ifEmpty { path },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // ? Overflow menu
                    Box {
                        IconButton(onClick = { folderMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.folders_cd_folder_options),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = folderMenuExpanded,
                            onDismissRequest = { folderMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_play_folder)) },
                                onClick = {
                                    folderMenuExpanded = false
                                    folderViewModel.playFolder(path)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_shuffle)) },
                                onClick = {
                                    folderMenuExpanded = false
                                    folderViewModel.playFolder(path)
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()
            }
        }

        // Folder list or empty state
        Box(modifier = Modifier.weight(1f)) {
            if (folderViewMode == FolderViewMode.DIRECTORIES) {
                if (browsingContent.isNotEmpty()) {
                    // Hierarchical browsing mode
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = 0.dp,
                            bottom = MiniPlayerHeight + SpacingSmall
                        )
                    ) {
                        items(browsingContent.size, key = { index ->
                            when (val item = browsingContent[index]) {
                                is com.sukoon.music.ui.viewmodel.FolderBrowserItem.SubFolder -> "folder_${item.path}"
                                is com.sukoon.music.ui.viewmodel.FolderBrowserItem.SongItem -> "song_${item.song.id}"
                            }
                        }) { index ->
                            when (val item = browsingContent[index]) {
                                is com.sukoon.music.ui.viewmodel.FolderBrowserItem.SubFolder -> {
                                    val itemPathHash = item.path.hashCode().toLong()
                                    FolderBrowserSubFolderRow(
                                        subFolder = item,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = selectedFolderIds.contains(itemPathHash),
                                        onClick = {
                                            onNavigateToFolderByPath(item.path)
                                        },
                                        onLongClick = {
                                            if (!isSelectionMode) {
                                                folderViewModel.toggleSelectionMode(true)
                                            }
                                            folderViewModel.toggleSelection(itemPathHash)
                                        },
                                        onPlay = { folderViewModel.playFolder(item.path) }
                                    )
                                }
                                is com.sukoon.music.ui.viewmodel.FolderBrowserItem.SongItem -> {
                                    FolderBrowserSongRow(
                                        song = item.song,
                                        isCurrentlyPlaying = item.song.id == playbackState.currentSong?.id,
                                        isPlayingGlobally = playbackState.isPlaying,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = selectedFolderIds.contains(item.song.id),
                                        menuHandler = menuHandler,
                                        onClick = {
                                            if (isSelectionMode) {
                                                folderViewModel.toggleSelection(item.song.id)
                                            } else {
                                                val contextSongs = browsingContent.filterIsInstance<com.sukoon.music.ui.viewmodel.FolderBrowserItem.SongItem>()
                                                    .map { it.song }
                                                folderViewModel.playSong(item.song, contextSongs)
                                                onNavigateToNowPlaying()
                                            }
                                        },
                                        onLongClick = {
                                            if (!isSelectionMode) {
                                                folderViewModel.toggleSelectionMode(true)
                                            }
                                            folderViewModel.toggleSelection(item.song.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    EmptyFoldersState(isHiddenView = false)
                }
            } else if (displayFolders.isEmpty()) {
                EmptyFoldersState(
                    isHiddenView = folderViewMode == FolderViewMode.HIDDEN
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = 0.dp,
                        bottom = MiniPlayerHeight + SpacingSmall
                    )
                ) {
                    items(displayFolders, key = { it.id }) { folder ->
                        FolderRow(
                            folder = folder,
                            isHidden = folderViewMode == FolderViewMode.HIDDEN,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedFolderIds.contains(folder.id),
                            onClick = {
                                onFolderClick(folder.id)
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    folderViewModel.toggleSelectionMode(true)
                                }
                                folderViewModel.toggleFolderSelection(folder.id)
                            },
                            onMoreClick = { selectedFolderForMenu = folder }
                        )
                    }
                }
            }
        }
    }

    selectedFolderForMenu?.let { folder ->
        FolderContextMenuBottomSheet(
            folder = folder,
            isHidden = folderViewMode == FolderViewMode.HIDDEN,
            onDismiss = { selectedFolderForMenu = null },
            onPlay = {
                folderViewModel.playFolder(folder.path)
                selectedFolderForMenu = null
            },
            onPlayNext = {
                folderViewModel.playNext(folder.path)
                selectedFolderForMenu = null
            },
            onAddToQueue = {
                folderViewModel.addToQueue(folder.path)
                selectedFolderForMenu = null
            },
            onAddToPlaylist = {
                folderViewModel.showAddToPlaylistDialog(folder.id)
                selectedFolderForMenu = null
            },
            onHide = {
                if (folderViewMode == FolderViewMode.HIDDEN) {
                    folderViewModel.unhideFolder(folder.path)
                } else {
                    folderViewModel.excludeFolder(folder.path)
                }
                selectedFolderForMenu = null
            },
            onDelete = {
                showDeleteConfirmation = folder
                folderToDeleteId = folder.id
                selectedFolderForMenu = null
            }
        )
    }

    // Delete confirmation dialog
    showDeleteConfirmation?.let { folder: Folder ->
        DeleteConfirmationDialog(
            folder = folder,
            onConfirm = {
                folderToDeleteId?.let { folderViewModel.deleteFolder(it) }
                showDeleteConfirmation = null
                folderToDeleteId = null
            },
            onDismiss = {
                showDeleteConfirmation = null
                folderToDeleteId = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderBrowserSubFolderRow(
    subFolder: com.sukoon.music.ui.viewmodel.FolderBrowserItem.SubFolder,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPlay: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (subFolder.albumArtUri != null) {
                SubcomposeAsyncImage(
                    model = subFolder.albumArtUri,
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.folders_cd_folder_cover),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    error = {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = subFolder.name,
                style = MaterialTheme.typography.listItemTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = androidx.compose.ui.res.pluralStringResource(
                    com.sukoon.music.R.plurals.folders_subfolder_song_count,
                    subFolder.songCount,
                    subFolder.songCount
                ),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (!isSelectionMode) {
            Box {
                IconButton(
                    onClick = { menuExpanded = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.folders_cd_folder_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_open)) },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        },
                        leadingIcon = { Icon(Icons.Default.FolderOpen, null) }
                    )

                    DropdownMenuItem(
                        text = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_play_folder)) },
                        onClick = {
                            menuExpanded = false
                            onPlay()
                        },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderContextMenuBottomSheet(
    folder: Folder,
    isHidden: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onHide: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Text(
            text = folder.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        Text(
            text = folder.path,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(com.sukoon.music.R.string.common_play)) },
            leadingContent = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onPlay)
        )
        ListItem(
            headlineContent = { Text(stringResource(com.sukoon.music.R.string.common_play_next)) },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onPlayNext)
        )
        ListItem(
            headlineContent = { Text(stringResource(com.sukoon.music.R.string.common_add_to_queue)) },
            leadingContent = { Icon(Icons.Default.MusicNote, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onAddToQueue)
        )
        ListItem(
            headlineContent = { Text(stringResource(com.sukoon.music.R.string.common_add_to_playlist)) },
            leadingContent = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onAddToPlaylist)
        )
        ListItem(
            headlineContent = {
                Text(
                    if (isHidden) {
                        stringResource(com.sukoon.music.R.string.label_unhide)
                    } else {
                        stringResource(com.sukoon.music.R.string.label_hide)
                    }
                )
            },
            leadingContent = {
                Icon(
                    imageVector = if (isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null
                )
            },
            modifier = Modifier.clickable(onClick = onHide)
        )
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(com.sukoon.music.R.string.common_delete),
                    color = MaterialTheme.colorScheme.error
                )
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            modifier = Modifier.clickable(onClick = onDelete)
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderBrowserSongRow(
    song: Song,
    isCurrentlyPlaying: Boolean,
    isPlayingGlobally: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    menuHandler: SongMenuHandler,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = song.albumArtUri,
                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_album_art_for_song, song.title),
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
                        imageVector = if (isCurrentlyPlaying) Icons.Default.PlayArrow else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isCurrentlyPlaying) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AnimatedEqualizer(isAnimating = isPlayingGlobally, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_artist_album_pair, song.artist, song.album),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (!isSelectionMode) {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.now_playing_more_options),
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




