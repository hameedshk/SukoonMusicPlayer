package com.sukoon.music.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Folder
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.FolderViewModel

/**
 * Folders Screen - Shows all folders in a grid view.
 *
 * Features:
 * - 2-column grid layout (Spotify-style)
 * - Folder cards with cover art, name, and song count
 * - Context menu for play/shuffle options
 * - Empty state when no folders exist
 * - Tap to navigate to folder detail screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    onNavigateToFolder: (Long) -> Unit,
    onBackClick: () -> Unit,
    viewModel: FolderViewModel = hiltViewModel()
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (folders.isEmpty()) {
                EmptyFoldersState()
            } else {
                FoldersGrid(
                    folders = folders,
                    onFolderClick = onNavigateToFolder,
                    onPlayFolder = { folderId ->
                        viewModel.playFolder(folderId)
                    },
                    onShuffleFolder = { folderId ->
                        viewModel.shuffleFolder(folderId)
                    }
                )
            }
        }
    }
}

@Composable
private fun FoldersGrid(
    folders: List<Folder>,
    onFolderClick: (Long) -> Unit,
    onPlayFolder: (Long) -> Unit,
    onShuffleFolder: (Long) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(folders, key = { it.id }) { folder ->
            FolderCard(
                folder = folder,
                onClick = { onFolderClick(folder.id) },
                onPlay = { onPlayFolder(folder.id) },
                onShuffle = { onShuffleFolder(folder.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderCard(
    folder: Folder,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // Folder Art / Icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                if (folder.albumArtUri != null) {
                    SubcomposeAsyncImage(
                        model = folder.albumArtUri,
                        contentDescription = "Folder art for ${folder.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(strokeWidth = 2.dp)
                            }
                        },
                        error = {
                            FolderPlaceholderIcon()
                        }
                    )
                } else {
                    FolderPlaceholderIcon()
                }

                // Context Menu Button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
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
                                onPlay()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Shuffle") },
                            onClick = {
                                onShuffle()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Shuffle, contentDescription = null)
                            }
                        )
                    }
                }
            }

            // Folder Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${folder.songCount} ${if (folder.songCount == 1) "song" else "songs"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FolderPlaceholderIcon() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = "Folder icon",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun EmptyFoldersState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "No folders",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No folders found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scan your media library to see your music folders",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FoldersScreenPreview() {
    SukoonMusicPlayerTheme {
        EmptyFoldersState()
    }
}
