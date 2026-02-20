package com.sukoon.music.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukoon.music.domain.model.DeletedPlaylist
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.sukoon.music.ui.theme.*

/**
 * Restore Playlist Screen - Shows deleted playlists and allows restoration.
 *
 * Features:
 * - List of deleted playlists with deletion date
 * - Restore individual playlists
 * - Permanently delete playlists
 * - Clear all trash
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestorePlaylistScreen(
    onBackClick: () -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val deletedPlaylists by viewModel.deletedPlaylists.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showClearTrashDialog by remember { mutableStateOf(false) }
    var restoreResult by remember { mutableStateOf<String?>(null) }
    var isRestoreResultSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_restore_playlist_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_back)
                        )
                    }
                },
                actions = {
                    if (deletedPlaylists.isNotEmpty()) {
                        IconButton(onClick = { showClearTrashDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.restore_playlist_cd_clear_all_trash)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (deletedPlaylists.isEmpty()) {
            EmptyTrashState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header info
                item {
                    Text(
                        text = androidx.compose.ui.res.pluralStringResource(
                            com.sukoon.music.R.plurals.restore_playlist_deleted_count,
                            deletedPlaylists.size,
                            deletedPlaylists.size
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Result message
                if (restoreResult != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRestoreResultSuccess)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = restoreResult!!,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                                color = if (isRestoreResultSuccess)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Deleted playlists list
                items(
                    items = deletedPlaylists,
                    key = { it.id }
                ) { deletedPlaylist ->
                    DeletedPlaylistCard(
                        deletedPlaylist = deletedPlaylist,
                        onRestore = {
                            scope.launch {
                                val success = viewModel.restorePlaylist(deletedPlaylist.id)
                                isRestoreResultSuccess = success
                                restoreResult = if (success) {
                                    context.getString(
                                        com.sukoon.music.R.string.restore_playlist_result_success,
                                        deletedPlaylist.name
                                    )
                                } else {
                                    context.getString(
                                        com.sukoon.music.R.string.restore_playlist_result_failed,
                                        deletedPlaylist.name
                                    )
                                }
                            }
                        },
                        onPermanentlyDelete = {
                            viewModel.permanentlyDeletePlaylist(deletedPlaylist.id)
                            isRestoreResultSuccess = false
                            restoreResult = context.getString(
                                com.sukoon.music.R.string.restore_playlist_result_permanently_deleted,
                                deletedPlaylist.name
                            )
                        }
                    )
                }
            }
        }

        // Clear Trash Confirmation Dialog
        if (showClearTrashDialog) {
            AlertDialog(
                onDismissRequest = { showClearTrashDialog = false },
                title = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_clear_all_trash_title)) },
                text = {
                    Text(
                        androidx.compose.ui.res.pluralStringResource(
                            com.sukoon.music.R.plurals.restore_playlist_clear_trash_confirmation,
                            deletedPlaylists.size,
                            deletedPlaylists.size
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearTrash()
                            showClearTrashDialog = false
                            isRestoreResultSuccess = false
                            restoreResult = context.getString(com.sukoon.music.R.string.restore_playlist_result_trash_cleared)
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.restore_playlist_clear_all_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearTrashDialog = false }) {
                        Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun DeletedPlaylistCard(
    deletedPlaylist: DeletedPlaylist,
    onRestore: () -> Unit,
    onPermanentlyDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Playlist name
            Text(
                text = deletedPlaylist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description (if available)
            if (deletedPlaylist.description != null) {
                Text(
                    text = deletedPlaylist.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Deletion date
            Text(
                text = androidx.compose.ui.res.stringResource(
                    com.sukoon.music.R.string.restore_playlist_deleted_on_date,
                    formatDate(deletedPlaylist.deletedAt)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_delete_forever))
                }

                Button(onClick = onRestore) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_restore))
                }
            }
        }
    }

    // Permanent Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_delete_forever_title)) },
            text = {
                Text(
                    androidx.compose.ui.res.stringResource(
                        com.sukoon.music.R.string.restore_playlist_permanent_delete_confirmation,
                        deletedPlaylist.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPermanentlyDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_delete_forever))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun EmptyTrashState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.restore_playlist_empty_trash_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.restore_playlist_empty_trash_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}


