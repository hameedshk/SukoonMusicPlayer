package com.sukoon.music.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukoon.music.ui.viewmodel.ExcludedFoldersViewModel
import com.sukoon.music.ui.viewmodel.ExcludedFolderItem
import com.sukoon.music.ui.theme.*

/**
 * Excluded Folders Screen - Manage folders hidden from the music library.
 *
 * Features:
 * - List of all currently excluded folder paths.
 * - Ability to remove a folder from the exclusion list via a delete button.
 * - Confirmation dialog before removing an exclusion.
 * - Empty state when no folders are excluded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcludedFoldersScreen(
    onBackClick: () -> Unit,
    viewModel: ExcludedFoldersViewModel = hiltViewModel()
) {
    val excludedFolders by viewModel.excludedFolders.collectAsStateWithLifecycle()
    var folderToRemove by remember { mutableStateOf<ExcludedFolderItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Excluded Folders") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (excludedFolders.isEmpty()) {
                EmptyExcludedState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        Text(
                            text = "Songs in these folders are hidden from your library.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    items(excludedFolders, key = { it.path }) { item ->
                        ExcludedFolderListItem(
                            item = item,
                            onRemoveClick = { folderToRemove = item }
                        )
                    }
                }
            }
        }

        // Confirmation Dialog
        folderToRemove?.let { item ->
            AlertDialog(
                onDismissRequest = { folderToRemove = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text("Restore Folder?") },
                text = {
                    Text("Do you want to show songs from \"${item.name}\" in your library again?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.removeExclusion(item.path)
                            folderToRemove = null
                        }
                    ) {
                        Text("Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { folderToRemove = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ExcludedFolderListItem(
    item: ExcludedFolderItem,
    onRemoveClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = item.name,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = item.path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            IconButton(onClick = onRemoveClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove exclusion",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun EmptyExcludedState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(
                top = ContentTopPadding,
                bottom = ContentBottomPadding + 16.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No excluded folders",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Folders you hide from your library will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
