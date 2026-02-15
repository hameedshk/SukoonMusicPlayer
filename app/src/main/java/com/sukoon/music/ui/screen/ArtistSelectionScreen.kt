package com.sukoon.music.ui.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.ui.components.AddToPlaylistDialog
import com.sukoon.music.ui.theme.*
import com.sukoon.music.ui.viewmodel.ArtistsViewModel
import com.sukoon.music.ui.viewmodel.PlaylistViewModel

/**
 * Full-screen artist selection screen
 * Similar to AlbumSelectionScreen but for artists
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArtistSelectionScreen(
    onBackClick: () -> Unit,
    viewModel: ArtistsViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val selectedArtistIds by viewModel.selectedArtistIds.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoreOptionsSheet by remember { mutableStateOf(false) }
    var artistSongsForPlaylist by remember { mutableStateOf<List<com.sukoon.music.domain.model.Song>>(emptyList()) }
    var triggerLoadSongs by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Handle loading songs from selected artists for playlist
    LaunchedEffect(triggerLoadSongs) {
        if (triggerLoadSongs && selectedArtistIds.isNotEmpty()) {
            val allSongs = selectedArtistIds.flatMap { artistId ->
                viewModel.getSongsForArtist(artistId)
            }
            artistSongsForPlaylist = allSongs
            showAddToPlaylistDialog = true
            triggerLoadSongs = false
        }
    }

    // Delete launcher for Android 11+ permission flow
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "Artists deleted successfully", Toast.LENGTH_SHORT).show()
            onBackClick()
        } else {
            Toast.makeText(context, "Delete cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredArtists = remember(artists, searchQuery) {
        if (searchQuery.isBlank()) {
            artists
        } else {
            artists.filter { artist ->
                artist.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val allSelected = filteredArtists.isNotEmpty() && filteredArtists.all { it.id in selectedArtistIds }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${selectedArtistIds.size} selected",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (selectedArtistIds.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.playSelectedArtists()
                                    onBackClick()
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Play",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Add to playlist button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    triggerLoadSongs = true
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Add to playlist",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add to playlist",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Delete button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDeleteDialog = true }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Delete",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // More button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showMoreOptionsSheet = true }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "More",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            // Search bar
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "Search artists",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyLarge,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                singleLine = true
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Select all row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (allSelected) {
                                filteredArtists.forEach { viewModel.toggleArtistSelection(it.id) }
                            } else {
                                filteredArtists.forEach { artist ->
                                    if (artist.id !in selectedArtistIds) {
                                        viewModel.toggleArtistSelection(artist.id)
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select all",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { checked ->
                            if (checked) {
                                filteredArtists.forEach { artist ->
                                    if (artist.id !in selectedArtistIds) {
                                        viewModel.toggleArtistSelection(artist.id)
                                    }
                                }
                            } else {
                                filteredArtists.forEach { viewModel.toggleArtistSelection(it.id) }
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            // Artist list
            items(filteredArtists, key = { it.id }) { artist ->
                ArtistSelectionItem(
                    artist = artist,
                    isSelected = artist.id in selectedArtistIds,
                    onToggle = { viewModel.toggleArtistSelection(artist.id) }
                )
            }
        }
    }

    // Add to playlist dialog
    if (showAddToPlaylistDialog && artistSongsForPlaylist.isNotEmpty()) {
        AddToPlaylistDialog(
            playlists = playlists,
            onPlaylistSelected = { playlistId ->
                artistSongsForPlaylist.forEach { song ->
                    playlistViewModel.addSongToPlaylist(playlistId, song.id)
                }
                showAddToPlaylistDialog = false
                Toast.makeText(context, "${artistSongsForPlaylist.size} songs added to playlist", Toast.LENGTH_SHORT).show()
                viewModel.clearSelection()
                onBackClick()
            },
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog && selectedArtistIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Delete ${selectedArtistIds.size} artist(s)?")
            },
            text = {
                Text("All songs by these artists will be permanently deleted from your device. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedArtistsWithResult { deleteResult ->
                            when (deleteResult) {
                                is DeleteHelper.DeleteResult.RequiresPermission -> {
                                    deleteLauncher.launch(
                                        IntentSenderRequest.Builder(deleteResult.intentSender).build()
                                    )
                                    showDeleteDialog = false
                                }
                                is DeleteHelper.DeleteResult.Success -> {
                                    Toast.makeText(context, "Artists deleted successfully", Toast.LENGTH_SHORT).show()
                                    showDeleteDialog = false
                                    onBackClick()
                                }
                                is DeleteHelper.DeleteResult.Error -> {
                                    Toast.makeText(context, "Error: ${deleteResult.message}", Toast.LENGTH_SHORT).show()
                                    showDeleteDialog = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // More options bottom sheet
    if (showMoreOptionsSheet && selectedArtistIds.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showMoreOptionsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "More options (${selectedArtistIds.size} artists)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Play next
                MoreOptionItem(
                    icon = Icons.Default.PlayArrow,
                    text = "Play next",
                    onClick = {
                        viewModel.playSelectedArtistsNext()
                        showMoreOptionsSheet = false
                        Toast.makeText(context, "${selectedArtistIds.size} artists added to play next", Toast.LENGTH_SHORT).show()
                        viewModel.clearSelection()
                        onBackClick()
                    }
                )

                // Add to queue
                MoreOptionItem(
                    icon = Icons.Default.Queue,
                    text = "Add to queue",
                    onClick = {
                        viewModel.addSelectedArtistsToQueue()
                        showMoreOptionsSheet = false
                        Toast.makeText(context, "${selectedArtistIds.size} artists added to queue", Toast.LENGTH_SHORT).show()
                        viewModel.clearSelection()
                        onBackClick()
                    }
                )

                // Select all
                MoreOptionItem(
                    icon = Icons.Default.SelectAll,
                    text = "Select all",
                    onClick = {
                        artists.forEach { artist ->
                            if (artist.id !in selectedArtistIds) {
                                viewModel.toggleArtistSelection(artist.id)
                            }
                        }
                        showMoreOptionsSheet = false
                    }
                )

                // Deselect all
                MoreOptionItem(
                    icon = Icons.Default.Deselect,
                    text = "Deselect all",
                    onClick = {
                        viewModel.clearSelection()
                        showMoreOptionsSheet = false
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ArtistSelectionItem(
    artist: Artist,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Artist art
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (artist.artworkUri != null) {
                SubcomposeAsyncImage(
                    model = artist.artworkUri,
                    contentDescription = "Artist art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Artist info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${artist.albumCount} album${if (artist.albumCount > 1) "s" else ""} • ${artist.songCount} song${if (artist.songCount > 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Checkbox
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun MoreOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
