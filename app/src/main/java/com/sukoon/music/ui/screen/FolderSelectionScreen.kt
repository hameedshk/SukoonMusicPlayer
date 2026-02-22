package com.sukoon.music.ui.screen

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Folder
import com.sukoon.music.ui.theme.*
import com.sukoon.music.ui.viewmodel.FolderViewModel

/**
 * Folder Selection Screen - Multi-select interface for folders
 * Allows users to select multiple folders and perform bulk actions
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FolderSelectionScreen(
    onBackClick: () -> Unit,
    viewModel: FolderViewModel = hiltViewModel()
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val selectedFolderIds by viewModel.selectedFolderIds.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoreOptionsSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Delete launcher for Android 11+ permission flow
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "Folder deleted successfully", Toast.LENGTH_SHORT).show()
            viewModel.clearSelection()
        } else {
            Toast.makeText(context, "Delete cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredFolders = folders

    val allSelected = filteredFolders.isNotEmpty() && filteredFolders.all { it.id in selectedFolderIds }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(com.sukoon.music.R.string.label_selected_count, selectedFolderIds.size)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearSelection()
                        onBackClick()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(com.sukoon.music.R.string.common_back)
                        )
                    }
                },
                actions = {
                    if (filteredFolders.isNotEmpty()) {
                        IconButton(onClick = {
                            if (allSelected) {
                                viewModel.clearSelection()
                            } else {
                                filteredFolders.forEach { folder ->
                                    if (folder.id !in selectedFolderIds) {
                                        viewModel.toggleFolderSelection(folder.id)
                                    }
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (allSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = stringResource(com.sukoon.music.R.string.common_select_all)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectedFolderIds.isNotEmpty()) {
                com.sukoon.music.ui.components.MultiSelectActionBottomBar(
                    onPlay = { viewModel.playSelectedFolders() },
                    onAddToPlaylist = { viewModel.showAddSelectedToPlaylistDialog() },
                    onDelete = { showDeleteDialog = true },
                    onPlayNext = { viewModel.playSelectedFoldersNext() },
                    onAddToQueue = { viewModel.addSelectedFoldersToQueue() }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (filteredFolders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(com.sukoon.music.R.string.folders_empty_visible_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredFolders, key = { it.id }) { folder ->
                        FolderSelectionRow(
                            folder = folder,
                            isSelected = folder.id in selectedFolderIds,
                            onClick = {
                                viewModel.toggleFolderSelection(folder.id)
                            },
                            onLongClick = {
                                viewModel.toggleFolderSelection(folder.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(com.sukoon.music.R.string.dialog_delete_folder_title)) },
            text = {
                Text(
                    "Delete ${selectedFolderIds.size} selected folder(s)? This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteSelectedFolders()
                        viewModel.clearSelection()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(com.sukoon.music.R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(com.sukoon.music.R.string.common_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderSelectionRow(
    folder: Folder,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

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
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = folder.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = androidx.compose.ui.res.pluralStringResource(
                        com.sukoon.music.R.plurals.common_song_count,
                        folder.songCount,
                        folder.songCount
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
