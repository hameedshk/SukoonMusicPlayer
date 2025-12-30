package com.sukoon.music.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Folder
import com.sukoon.music.domain.model.FolderSortMode
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

    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()

    var showDeleteConfirmation by remember { mutableStateOf<Folder?>(null) }
    var folderToDeleteId by remember { mutableStateOf<Long?>(null) }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val displayFolders = if (folderViewMode == FolderViewMode.DIRECTORIES) {
                folders
            } else {
                hiddenFolders
            }

            // Category pill row (count + sort)
            CategoryPillRow(
                itemCount = displayFolders.size,
                itemLabel = if (displayFolders.size == 1) "folder" else "folders",
                sortOptions = FolderSortMode.entries,
                currentSortMode = sortMode,
                onSortModeChanged = { viewModel.setSortMode(it) },
                sortModeToDisplayName = { it.toDisplayName() }
            )

            // Context header (Directories / Hidden)
            FolderContextHeader(
                selectedMode = folderViewMode,
                onModeChanged = { viewModel.setFolderViewMode(it) }
            )

            // Folder list or empty state
            Box(modifier = Modifier.weight(1f)) {
                if (displayFolders.isEmpty()) {
                    EmptyFoldersState(
                        isHiddenView = folderViewMode == FolderViewMode.HIDDEN
                    )
                } else {
                    FolderList(
                        folders = displayFolders,
                        isHiddenView = folderViewMode == FolderViewMode.HIDDEN,
                        onFolderClick = onNavigateToFolder,
                        onPlay = { viewModel.playFolder(it) },
                        onPlayNext = { viewModel.playNext(it) },
                        onAddToQueue = { viewModel.addToQueue(it) },
                        onAddToPlaylist = { viewModel.showAddToPlaylistDialog(it) },
                        onHide = { viewModel.excludeFolder(it) },
                        onUnhide = { viewModel.unhideFolder(it) },
                        onDelete = { folderId ->
                            val folder = displayFolders.find { it.id == folderId }
                            showDeleteConfirmation = folder
                            folderToDeleteId = folderId
                        }
                    )
                }
            }

            // Mini player (conditional)
            if (playbackState.currentSong != null) {
                MiniPlayer(
                    playbackState = playbackState,
                    onPlayPauseClick = { 
                        scope.launch {
                            viewModel.playbackRepository.playPause()
                        }
                    },
                    onNextClick = { 
                        scope.launch {
                            viewModel.playbackRepository.seekToNext()
                        }
                    },
                    onClick = onNavigateToNowPlaying
                )
            }

            // Ad banner
            BannerAdView(
                adMobManager = viewModel.adMobManager,
                modifier = Modifier.fillMaxWidth()
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
    showDeleteConfirmation?.let { folder ->
        DeleteConfirmationDialog(
            folder = folder,
            onConfirm = {
                folderToDeleteId?.let { viewModel.deleteFolder(it) }
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
private fun FolderList(
    folders: List<Folder>,
    isHiddenView: Boolean,
    onFolderClick: (Long) -> Unit,
    onPlay: (String) -> Unit,
    onPlayNext: (String) -> Unit,
    onAddToQueue: (String) -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onHide: (String) -> Unit,
    onUnhide: (Long) -> Unit,
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
                onUnhide = { onUnhide(folder.id) },
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

private fun FolderSortMode.toDisplayName(): String = when (this) {
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
