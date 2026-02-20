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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Genre
import com.sukoon.music.ui.components.AddToPlaylistDialog
import com.sukoon.music.ui.components.PlaceholderAlbumArt
import com.sukoon.music.ui.theme.*
import com.sukoon.music.ui.viewmodel.GenresViewModel
import com.sukoon.music.ui.viewmodel.PlaylistViewModel

/**
 * Full-screen genre selection screen
 * Similar to AlbumSelectionScreen but for genres
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GenreSelectionScreen(
    onBackClick: () -> Unit,
    viewModel: GenresViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val selectedGenreIds by viewModel.selectedGenreIds.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoreOptionsSheet by remember { mutableStateOf(false) }
    var genreSongsForPlaylist by remember { mutableStateOf<List<com.sukoon.music.domain.model.Song>>(emptyList()) }
    var triggerLoadSongs by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Handle loading songs from selected genres for playlist
    LaunchedEffect(triggerLoadSongs) {
        if (triggerLoadSongs && selectedGenreIds.isNotEmpty()) {
            val allSongs = selectedGenreIds.flatMap { genreId ->
                viewModel.getSongsForGenre(genreId).toList()
            }
            genreSongsForPlaylist = allSongs
            showAddToPlaylistDialog = true
            triggerLoadSongs = false
        }
    }

    // Delete launcher for Android 11+ permission flow
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_genre_deleted_successfully), Toast.LENGTH_SHORT).show()
            onBackClick()
        } else {
            Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_delete_cancelled), Toast.LENGTH_SHORT).show()
        }
    }

    val filteredGenres = remember(genres, searchQuery) {
        if (searchQuery.isBlank()) {
            genres
        } else {
            genres.filter { genre ->
                genre.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val allSelected = filteredGenres.isNotEmpty() && filteredGenres.all { it.id in selectedGenreIds }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_selected_count, selectedGenreIds.size),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (selectedGenreIds.isNotEmpty()) {
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
                                    viewModel.playSelectedGenres()
                                    onBackClick()
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play),
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
                                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_playlist),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_playlist),
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
                                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_delete),
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
                                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_more),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_more),
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
                                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_search),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_search_genres),
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
                                        contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_clear_search),
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
                                filteredGenres.forEach { viewModel.toggleGenreSelection(it.id) }
                            } else {
                                filteredGenres.forEach { genre ->
                                    if (genre.id !in selectedGenreIds) {
                                        viewModel.toggleGenreSelection(genre.id)
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_select_all),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { checked ->
                            if (checked) {
                                filteredGenres.forEach { genre ->
                                    if (genre.id !in selectedGenreIds) {
                                        viewModel.toggleGenreSelection(genre.id)
                                    }
                                }
                            } else {
                                filteredGenres.forEach { viewModel.toggleGenreSelection(it.id) }
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

            // Genre list
            items(filteredGenres, key = { it.id }) { genre ->
                GenreSelectionItem(
                    genre = genre,
                    isSelected = genre.id in selectedGenreIds,
                    onToggle = { viewModel.toggleGenreSelection(genre.id) }
                )
            }
        }
    }

    // Add to playlist dialog
    if (showAddToPlaylistDialog && genreSongsForPlaylist.isNotEmpty()) {
        AddToPlaylistDialog(
            playlists = playlists,
            onPlaylistSelected = { playlistId ->
                genreSongsForPlaylist.forEach { song ->
                    playlistViewModel.addSongToPlaylist(playlistId, song.id)
                }
                showAddToPlaylistDialog = false
                Toast.makeText(context, context.resources.getQuantityString(com.sukoon.music.R.plurals.songs_added_to_playlist, genreSongsForPlaylist.size, genreSongsForPlaylist.size), Toast.LENGTH_SHORT).show()
                viewModel.clearSelection()
                onBackClick()
            },
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog && selectedGenreIds.isNotEmpty()) {
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
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_delete_selected_genres_title, selectedGenreIds.size))
            },
            text = {
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_delete_genre_songs_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedGenresWithResult { deleteResult ->
                            when (deleteResult) {
                                is DeleteHelper.DeleteResult.RequiresPermission -> {
                                    deleteLauncher.launch(
                                        IntentSenderRequest.Builder(deleteResult.intentSender).build()
                                    )
                                    showDeleteDialog = false
                                }
                                is DeleteHelper.DeleteResult.Success -> {
                                    Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_genre_deleted_successfully), Toast.LENGTH_SHORT).show()
                                    showDeleteDialog = false
                                    onBackClick()
                                }
                                is DeleteHelper.DeleteResult.Error -> {
                                    Toast.makeText(context, context.getString(com.sukoon.music.R.string.toast_error_with_message, deleteResult.message), Toast.LENGTH_SHORT).show()
                                    showDeleteDialog = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_cancel))
                }
            }
        )
    }

    // More options bottom sheet
    if (showMoreOptionsSheet && selectedGenreIds.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showMoreOptionsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        com.sukoon.music.R.string.library_screens_b_more_options_genres_title,
                        selectedGenreIds.size
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Play next
                MoreOptionItem(
                    icon = Icons.Default.PlayArrow,
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play_next),
                    onClick = {
                        viewModel.playSelectedNext()
                        showMoreOptionsSheet = false
                        Toast.makeText(context, context.resources.getQuantityString(com.sukoon.music.R.plurals.genres_added_to_play_next, selectedGenreIds.size, selectedGenreIds.size), Toast.LENGTH_SHORT).show()
                        viewModel.clearSelection()
                        onBackClick()
                    }
                )

                // Add to queue
                MoreOptionItem(
                    icon = Icons.Default.Queue,
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_queue),
                    onClick = {
                        viewModel.addSelectedToQueue()
                        showMoreOptionsSheet = false
                        Toast.makeText(context, context.resources.getQuantityString(com.sukoon.music.R.plurals.genres_added_to_queue, selectedGenreIds.size, selectedGenreIds.size), Toast.LENGTH_SHORT).show()
                        viewModel.clearSelection()
                        onBackClick()
                    }
                )

                // Select all
                MoreOptionItem(
                    icon = Icons.Default.SelectAll,
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_select_all),
                    onClick = {
                        genres.forEach { genre ->
                            if (genre.id !in selectedGenreIds) {
                                viewModel.toggleGenreSelection(genre.id)
                            }
                        }
                        showMoreOptionsSheet = false
                    }
                )

                // Deselect all
                MoreOptionItem(
                    icon = Icons.Default.Deselect,
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_deselect_all),
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
private fun GenreSelectionItem(
    genre: Genre,
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
        // Genre artwork
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (genre.artworkUri != null) {
                SubcomposeAsyncImage(
                    model = genre.artworkUri,
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_genre_artwork),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    },
                    error = {
                        PlaceholderAlbumArt.Placeholder(
                            seed = PlaceholderAlbumArt.generateSeed(
                                albumName = genre.name,
                                albumId = genre.id
                            )
                        )
                    }
                )
            } else {
                PlaceholderAlbumArt.Placeholder(
                    seed = PlaceholderAlbumArt.generateSeed(
                        albumName = genre.name,
                        albumId = genre.id
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Genre info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = genre.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = androidx.compose.ui.res.pluralStringResource(
                    com.sukoon.music.R.plurals.library_screens_b_song_count,
                    genre.songCount,
                    genre.songCount
                ),
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




