package com.sukoon.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.theme.*

/**
 * Hybrid Bottom Sheet context menu for a Song.
 * Combines S's scannability with offline-first utility.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongContextMenu(
    song: Song,
    menuHandler: SongMenuHandler,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberLazyListState()

    LaunchedEffect(Unit) {
        scrollState.scrollToItem(0)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            item {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    SubcomposeAsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    }
                }
            }
            item {
                // Utility Row
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                UtilityIconButton(
                    icon = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.now_playing_like),
                    isLiked = song.isLiked,
                    songId = song.id,
                    onClick = {
                        menuHandler.handleToggleLike(song, song.isLiked)
                    }
                )
                UtilityIconButton(
                    icon = Icons.Default.Share,
                    label = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_share),
                    onClick = {
                        menuHandler.handleShare(song)
                        onDismiss()
                    }
                )
                UtilityIconButton(
                    icon = Icons.Default.Info,
                    label = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_info),
                    onClick = {
                        menuHandler.handleShowSongInfo(song)
                        onDismiss()
                    }
                )
                UtilityIconButton(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    label = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_context_playlist_label),
                    onClick = {
                        menuHandler.handleAddToPlaylist(song)
                        onDismiss()
                    }
                    )
                }
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item {
                // Group A: Playback
                OptionItem(
                icon = Icons.Default.SkipNext,
                text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play_next),
                    onClick = {
                        menuHandler.handlePlayNext(song)
                        onDismiss()
                    }
                )
                OptionItem(
                    icon = Icons.Default.QueueMusic,
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_queue),
                    onClick = {
                        menuHandler.handleAddToQueue(song)
                        onDismiss()
                    }
                )
                OptionItem(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_playlist),
                    onClick = {
                        menuHandler.handleAddToPlaylist(song)
                        onDismiss()
                    }
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item {
                // Group B: Navigation
                OptionItem(
                    icon = Icons.Default.Album,
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_context_go_to_album),
                    onClick = {
                        menuHandler.handleGoToAlbum(song)
                        onDismiss()
                    }
                )
                OptionItem(
                    icon = Icons.Default.Person,
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_context_go_to_artist),
                    onClick = {
                        menuHandler.handleGoToArtist(song)
                        onDismiss()
                    }
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item {
                // Group C: System
                OptionItem(
                    icon = Icons.Default.Lyrics,
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_context_update_lyrics),
                    onClick = {
                        menuHandler.handleUpdateLyrics(song)
                        onDismiss()
                    }
                )
                OptionItem(
                    icon = Icons.Default.Edit,
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_context_edit_audio),
                    trailing = {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_context_premium_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    },
                    onClick = {
                        android.util.Log.d("SongContextMenu", "Edit Audio clicked for song: ${song.title}")
                        menuHandler.handleEditAudio(song)
                        onDismiss()
                    }
                )
                OptionItem(
                    icon = Icons.Default.Delete,
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_delete_from_device),
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        menuHandler.handleDeleteFromDevice(song)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun UtilityIconButton(
    icon: ImageVector,
    label: String,
    isLiked: Boolean? = null,
    songId: Long? = null,
    onClick: () -> Unit
) {
    // Animation state for like button (only if isLiked is provided)
    var prevLikedState by remember(songId) { mutableStateOf(isLiked ?: false) }
    var animTrigger by remember(songId) { mutableStateOf(0) }

    LaunchedEffect(songId, isLiked) {
        if (isLiked != null && prevLikedState != isLiked) {
            animTrigger++
            prevLikedState = isLiked
        }
    }

    val likeScale by animateFloatAsState(
        targetValue = if (isLiked != null && animTrigger % 2 == 1) 1.4f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "like_scale",
        finishedListener = { if (animTrigger % 2 == 1) animTrigger++ }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = if (isLiked != null) Modifier.scale(likeScale) else Modifier
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OptionItem(
    icon: ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = tint,
                modifier = Modifier.weight(1f)
            )
            if (trailing != null) {
                trailing()
            }
        }
    }
}

