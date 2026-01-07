package com.sukoon.music.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Folder
import com.sukoon.music.domain.model.FolderSortMode
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.*
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.FolderViewModel
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import kotlinx.coroutines.launch

/**
 * Folders Screen - Displays local music folders with enhanced management.
 *
 * Features:
 * - Toggle between active directories and hidden folders.
 * - Advanced sorting (Name, Track Count, Duration, etc.).
 * - Context actions: Play, Add to Queue, Hide/Unhide, SAF Deletion.
 * - Global MiniPlayer and Ad integration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    onNavigateToFolder: (Long) -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onBackClick: () -> Unit,
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
    val folderSortMode by viewModel.sortMode.collectAsStateWithLifecycle()

    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()

    var folderToDelete by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    // SAF delete permission launcher
    val deletePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.clearDeleteResult()
        } else {
            viewModel.clearDeleteResult()
        }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Folders") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FoldersContent(
                folders = folders,
                hiddenFolders = hiddenFolders,
                folderViewMode = folderViewMode,
                sortMode = folderSortMode,
                playbackState = playbackState,
                onFolderClick = onNavigateToFolder,
                folderViewModel = viewModel,
                menuHandler = rememberSongMenuHandler(
                    playbackRepository = viewModel.playbackRepository
                ),
                onNavigateToNowPlaying = onNavigateToNowPlaying
            )

        }
    }

    // Add to playlist dialog
    if (selectedFolderForPlaylist != null) {
        AddToPlaylistDialog(
            playlists = playlists,
            onPlaylistSelected = { playlistId ->
                viewModel.addFolderToPlaylist(selectedFolderForPlaylist!!, playlistId)
            },
            onDismiss = { viewModel.dismissPlaylistDialog() }
        )
    }

    // Delete confirmation dialog
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
}

@Composable
private fun FolderList(
    folders: List<Folder>,
    isHiddenView: Boolean,
    onFolderClick: (Long) -> Unit,
    onPlay: (String) -> Unit,
    onPlayNext: (String) -> Unit,
    onAddToQueue: (String) -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onHide: (String) -> Unit,
    onUnhide: (String) -> Unit,
    onDelete: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(folders, key = { it.id }) { folder ->
            FolderRow(
                folder = folder,
                isHidden = isHiddenView,
                onFolderClick = { onFolderClick(folder.id) },
                onPlay = { onPlay(folder.path) },
                onPlayNext = { onPlayNext(folder.path) },
                onAddToQueue = { onAddToQueue(folder.path) },
                onAddToPlaylist = { onAddToPlaylist(folder.id) },
                onHide = { onHide(folder.path) },
                onUnhide = { onUnhide(folder.path) },
                onDelete = { onDelete(folder.id) }
            )
        }
    }
}

@Composable
private fun EmptyFoldersState(
    isHiddenView: Boolean = false
) {
    Box(
        modifier = Modifier.fillMaxSize(),
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
                text = if (isHiddenView) "No hidden folders" else "No folders found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isHiddenView)
                    "Folders you hide will appear here"
                else
                    "Scan your media library to see your music folders",
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
        title = { Text("Delete folder?") },
        text = {
            Column {
                Text("This will permanently delete ${folder.songCount} songs from your device:")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = folder.path,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This action cannot be undone.",
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

fun FolderSortMode.toDisplayName(): String = when (this) {
    FolderSortMode.NAME_ASC -> "Name (A-Z)"
    FolderSortMode.NAME_DESC -> "Name (Z-A)"
    FolderSortMode.TRACK_COUNT -> "Most Songs"
    FolderSortMode.RECENTLY_MODIFIED -> "Recently Added"
    FolderSortMode.DURATION -> "Total Duration"
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
    onFolderClick: (Long) -> Unit,
    folderViewModel: com.sukoon.music.ui.viewmodel.FolderViewModel,
    menuHandler: SongMenuHandler,
    onNavigateToNowPlaying: () -> Unit
) {
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
        // Back button when navigating in folders
        if (folderViewMode == FolderViewMode.DIRECTORIES && currentPath != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { folderViewModel.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate up"
                    )
                }
                Text(
                    text = currentPath ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider()
        }

        // Folder list or empty state
        Box(modifier = Modifier.weight(1f)) {
            if (folderViewMode == FolderViewMode.DIRECTORIES && browsingContent.isNotEmpty()) {
                // Hierarchical browsing mode
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(browsingContent.size, key = { index ->
                        when (val item = browsingContent[index]) {
                            is com.sukoon.music.ui.viewmodel.FolderBrowserItem.SubFolder -> "folder_${item.path}"
                            is com.sukoon.music.ui.viewmodel.FolderBrowserItem.SongItem -> "song_${item.song.id}"
                        }
                    }) { index ->
                        when (val item = browsingContent[index]) {
                            is com.sukoon.music.ui.viewmodel.FolderBrowserItem.SubFolder -> {
                                FolderBrowserSubFolderRow(
                                    subFolder = item,
                                    onClick = { folderViewModel.navigateToFolder(item.path) },
                                    onPlay = { folderViewModel.playFolder(item.path) }
                                )
                            }
                            is com.sukoon.music.ui.viewmodel.FolderBrowserItem.SongItem -> {
                                FolderBrowserSongRow(
                                    song = item.song,
                                    isPlaying = item.song.id == playbackState.currentSong?.id,
                                    menuHandler = menuHandler,
                                    onClick = {
                                        val contextSongs = browsingContent.filterIsInstance<com.sukoon.music.ui.viewmodel.FolderBrowserItem.SongItem>()
                                            .map { it.song }
                                        folderViewModel.playSong(item.song, contextSongs)
                                        onNavigateToNowPlaying()
                                    },
                                    onLikeClick = { folderViewModel.toggleLike(item.song.id, item.song.isLiked) }
                                )
                            }
                        }
                    }
                }
            } else if (displayFolders.isEmpty()) {
                EmptyFoldersState(
                    isHiddenView = folderViewMode == FolderViewMode.HIDDEN
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(displayFolders, key = { it.id }) { folder ->
                        FolderRow(
                            folder = folder,
                            isHidden = folderViewMode == FolderViewMode.HIDDEN,
                            onFolderClick = { onFolderClick(folder.id) },
                            onPlay = { folderViewModel.playFolder(folder.path) },
                            onPlayNext = { folderViewModel.playNext(folder.path) },
                            onAddToQueue = { folderViewModel.addToQueue(folder.path) },
                            onAddToPlaylist = { folderViewModel.showAddToPlaylistDialog(folder.id) },
                            onHide = { folderViewModel.excludeFolder(folder.path) },
                            onUnhide = { folderViewModel.unhideFolder(folder.path) },
                            onDelete = {
                                showDeleteConfirmation = folder
                                folderToDeleteId = folder.id
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteConfirmation?.let { folder ->
        DeleteFolderConfirmationDialog(
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

@Composable
private fun FolderBrowserSubFolderRow(
    subFolder: com.sukoon.music.ui.viewmodel.FolderBrowserItem.SubFolder,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (subFolder.albumArtUri != null) {
                SubcomposeAsyncImage(
                    model = subFolder.albumArtUri,
                    contentDescription = "Folder cover",
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
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${subFolder.songCount} songs",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Enter folder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeleteFolderConfirmationDialog(
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
        title = { Text("Delete folder?") },
        text = {
            Column {
                Text("This will permanently delete ${folder.songCount} songs from your device:")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = folder.path,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This action cannot be undone.",
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
private fun FolderBrowserSongRow(
    song: Song,
    isPlaying: Boolean,
    menuHandler: SongMenuHandler,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = song.albumArtUri,
                contentDescription = "Album art for ${song.title}",
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
                        imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${song.artist} • ${song.album}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onLikeClick) {
            Icon(
                imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (song.isLiked) "Unlike" else "Like",
                tint = if (song.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
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
