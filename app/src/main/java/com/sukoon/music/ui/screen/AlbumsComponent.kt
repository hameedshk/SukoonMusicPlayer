package com.sukoon.music.ui.screen.albums

import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Album
import com.sukoon.music.ui.components.PlaceholderAlbumArt
import com.sukoon.music.ui.components.SelectionBottomBarItem
import com.sukoon.music.ui.components.SortOption
import com.sukoon.music.ui.viewmodel.AlbumSortMode
import com.sukoon.music.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AlbumRow(
    album: Album,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val placeholderSeed = PlaceholderAlbumArt.generateSeed(
        albumName = album.title,
        artistName = album.artist,
        albumId = album.id
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = SpacingLarge, vertical = SpacingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(MiniPlayerAlbumArtSize)
                .clip(RoundedCornerShape(CompactButtonCornerRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (album.albumArtUri != null) {
                SubcomposeAsyncImage(
                    model = album.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        PlaceholderAlbumArt.Placeholder(seed = placeholderSeed)
                    },
                    error = {
                        PlaceholderAlbumArt.Placeholder(seed = placeholderSeed)
                    }
                )
            } else {
                PlaceholderAlbumArt.Placeholder(seed = placeholderSeed)
            }
        }

        Spacer(modifier = Modifier.width(SpacingLarge))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.compactCardTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(SpacingXSmall))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.cardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${album.songCount}",
                    style = MaterialTheme.typography.genreMetadata,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = SpacingSmall)
                )
            }
        }

        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        } else {
            IconButton(onClick = onMoreClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal  fun AlbumSortDialog(
    currentSortMode: AlbumSortMode,
    isAscending: Boolean,
    onDismiss: () -> Unit,
    onSortModeChange: (AlbumSortMode) -> Unit,
    onOrderChange: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK", color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
        title = { Text("Sort by") },
        text = {
            Column {
                SortOption(
                    text = "Album name",
                    isSelected = currentSortMode == AlbumSortMode.ALBUM_NAME,
                    onClick = { onSortModeChange(AlbumSortMode.ALBUM_NAME) }
                )
                SortOption(
                    text = "Artist name",
                    isSelected = currentSortMode == AlbumSortMode.ARTIST_NAME,
                    onClick = { onSortModeChange(AlbumSortMode.ARTIST_NAME) }
                )
                SortOption(
                    text = "Number of songs",
                    isSelected = currentSortMode == AlbumSortMode.SONG_COUNT,
                    onClick = { onSortModeChange(AlbumSortMode.SONG_COUNT) }
                )
                SortOption(
                    text = "Year",
                    isSelected = currentSortMode == AlbumSortMode.YEAR,
                    onClick = { onSortModeChange(AlbumSortMode.YEAR) }
                )
                SortOption(
                    text = "Random",
                    isSelected = currentSortMode == AlbumSortMode.RANDOM,
                    onClick = { onSortModeChange(AlbumSortMode.RANDOM) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SortOption(
                    text = "From A to Z",
                    isSelected = isAscending,
                    onClick = { onOrderChange(true) }
                )
                SortOption(
                    text = "From Z to A",
                    isSelected = !isAscending,
                    onClick = { onOrderChange(false) }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlbumSelectionTopBar(
    selectedCount: Int,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@Composable
internal fun AlbumSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search albums", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        }
    }
}

@Composable
private  fun SelectAllRow(
    isAllSelected: Boolean,
    onToggleSelectAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelectAll)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Select all",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (isAllSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AlbumSelectionBottomBar(
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            SelectionBottomBarItem(icon = Icons.Default.PlayArrow, label = "Play", onClick = onPlay)
            SelectionBottomBarItem(icon = Icons.Default.PlaylistAdd, label = "Add to play", onClick = onAddToPlaylist)
            SelectionBottomBarItem(icon = Icons.Default.Delete, label = "Delete", onClick = onDelete)
            SelectionBottomBarItem(icon = Icons.Default.MoreVert, label = "More", onClick = onMore)
        }
    }
}

@Composable
private fun AlbumDefaultCover(albumId: Long, albumName: String) {
    com.sukoon.music.ui.components.PlaceholderAlbumArt.Placeholder(
        seed = com.sukoon.music.ui.components.PlaceholderAlbumArt.generateSeed(
            albumName = albumName,
            albumId = albumId
        ),
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
        icon = Icons.Default.MusicNote,
        iconSize = 64,
        iconOpacity = 0.35f
    )
}
