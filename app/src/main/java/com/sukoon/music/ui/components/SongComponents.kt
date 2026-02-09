package com.sukoon.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.theme.*

@Composable
internal fun AnimatedEqualizer(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "eq$index")
            val height by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300 + index * 100, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((12.dp * height))
                    .background(tint, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
internal fun SongItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    // Animation state for like button
    var prevLikedState by remember(song.id) { mutableStateOf(song.isLiked) }
    var animTrigger by remember(song.id) { mutableStateOf(0) }

    LaunchedEffect(song.id, song.isLiked) {
        if (prevLikedState != song.isLiked) {
            animTrigger++
            prevLikedState = song.isLiked
        }
    }

    val likeScale by animateFloatAsState(
        targetValue = if (animTrigger % 2 == 0) 1f else 1.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "like_scale",
        finishedListener = { if (animTrigger % 2 == 1) animTrigger++ }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = SpacingLarge, vertical = SpacingMedium),
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
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                error = {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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

        IconButton(onClick = onLikeClick) {
            Icon(
                imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (song.isLiked) "Unlike" else "Like",
                tint = if (song.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.scale(likeScale)
            )
        }
    }
}

@Composable
internal fun SongItemWithMenu(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
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
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                error = {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isPlaying) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AnimatedEqualizer(tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SongMenuBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onGoToAlbum: () -> Unit,
    onSetRingtone: () -> Unit,
    onChangeCover: () -> Unit,
    onEditTags: () -> Unit,
    onEditAudio: () -> Unit,
    onUpdateLyrics: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Song header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = {
                            PlaceholderAlbumArt.Placeholder(
                                seed = PlaceholderAlbumArt.generateSeed(
                                    albumName = song.album,
                                    artistName = song.artist,
                                    songId = song.id
                                )
                            )
                        }
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                IconButton(onClick = { /* TODO: Info */ }) {
                    Icon(Icons.Default.Info, contentDescription = "Info")
                }
                IconButton(onClick = { /* TODO: Share */ }) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
            }

            // Pill buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PillButton(
                    text = "Set as ringtone",
                    icon = Icons.Default.Notifications,
                    onClick = { onSetRingtone(); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
                PillButton(
                    text = "Change cover",
                    icon = Icons.Default.Image,
                    onClick = { onChangeCover(); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
                PillButton(
                    text = "Edit tags",
                    icon = Icons.Default.Edit,
                    onClick = { onEditTags(); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Menu items
            MenuOption("Play next", Icons.Default.SkipNext) { onPlayNext(); onDismiss() }
            MenuOption("Add to queue", Icons.Default.PlaylistAdd) { onAddToQueue(); onDismiss() }
            MenuOption("Add to playlist", Icons.Default.PlaylistAdd) { onAddToPlaylist(); onDismiss() }
            MenuOption("Go to album", Icons.Default.Album) { onGoToAlbum(); onDismiss() }
            MenuOption("Edit audio", Icons.Default.MusicNote) { onEditAudio(); onDismiss() }
            MenuOption("Update lyrics", Icons.Default.Edit) { onUpdateLyrics(); onDismiss() }
            MenuOption("Delete from device", Icons.Default.Delete, isDestructive = true) { onDelete(); onDismiss() }
        }
    }
}

@Composable
internal fun SongItemSelectable(
    song: Song,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = {
                    PlaceholderAlbumArt.Placeholder(
                        seed = PlaceholderAlbumArt.generateSeed(
                            albumName = song.album,
                            artistName = song.artist,
                            songId = song.id
                        )
                    )
                }
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(song.artist, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SongSortDialog(
    currentSortMode: String,
    currentOrder: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentSortMode) }
    var selectedOrder by remember { mutableStateOf(currentOrder) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text("Sort by", style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))

            listOf("Song name", "Artist name", "Album name", "Folder name",
                "Time added", "Play count", "Year", "Duration", "Size").forEach { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { selectedMode = mode }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(mode, style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedMode == mode) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface)
                    if (selectedMode == mode) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            listOf("A to Z", "Z to A").forEach { order ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { selectedOrder = order }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("From $order", style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedOrder == order) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface)
                    if (selectedOrder == order) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onDismiss, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurface)
                }
                Button(onClick = { onConfirm(selectedMode, selectedOrder) }, modifier = Modifier.weight(1f)) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
internal fun LastAddedSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onHeaderClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SpacingMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onHeaderClick)
                .padding(horizontal = SpacingLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Last added",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "See all",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(SpacingMedium))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = SpacingLarge),
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            items(songs.size.coerceAtMost(10)) { index ->
                LastAddedCard(
                    song = songs[index],
                    onClick = { onSongClick(songs[index]) }
                )
            }
        }

        Spacer(modifier = Modifier.height(SpacingLarge))
        Divider(
            modifier = Modifier.padding(horizontal = SpacingLarge),
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )
    }
}

@Composable
internal fun LastAddedCard(
    song: Song,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(LastAddedCardWidth)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Album art
                if (song.albumArtUri != null) {
                    SubcomposeAsyncImage(
                        model = song.albumArtUri,
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            PlaceholderAlbumArt.Placeholder(
                                seed = PlaceholderAlbumArt.generateSeed(
                                    albumName = song.album,
                                    artistName = song.artist,
                                    songId = song.id
                                ),
                                icon = Icons.Default.MusicNote
                            )
                        },
                        error = {
                            PlaceholderAlbumArt.Placeholder(
                                seed = PlaceholderAlbumArt.generateSeed(
                                    albumName = song.album,
                                    artistName = song.artist,
                                    songId = song.id
                                ),
                                icon = Icons.Default.MusicNote
                            )
                        }
                    )
                } else {
                    PlaceholderAlbumArt.Placeholder(
                        seed = PlaceholderAlbumArt.generateSeed(
                            albumName = song.album,
                            artistName = song.artist,
                            songId = song.id
                        ),
                        icon = Icons.Default.MusicNote
                    )
                }

                // Light gradient + play button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // TEXT BELOW THE CARD
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.album,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    song: Song,
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
        title = {
            Text("Delete from device?")
        },
        text = {
            Text(
                "\"${song.title}\" will be permanently deleted from your device. This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium
            )
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
