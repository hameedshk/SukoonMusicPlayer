package com.sukoon.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.sukoon.music.ui.animation.MotionDirective
import com.sukoon.music.ui.animation.MotionPlayState
import com.sukoon.music.ui.animation.rememberPlaybackMotionClock
import com.sukoon.music.ui.theme.*
import kotlin.math.sin

private const val EQUALIZER_PLAYBACK_MOTION_SPEED = 3.0f

@Composable
internal fun AnimatedEqualizer(
    modifier: Modifier = Modifier,
    motion: MotionDirective,
    phase: Float,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val barPhase = phase * (1.6f + index * 0.25f) + index * 1.2f
            val runningHeight = 0.3f + (0.7f * ((sin(barPhase) + 1f) / 2f))
            val displayHeight = when (motion.state) {
                MotionPlayState.RUNNING,
                MotionPlayState.HOLD -> runningHeight
                MotionPlayState.REST -> 0.4f
            }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((12.dp * displayHeight))
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
                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_album_art_for_song, song.title),
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
                text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_artist_album_pair, song.artist, song.album),
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
                contentDescription = if (song.isLiked) {
                    androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_unlike)
                } else {
                    androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_like)
                },
                tint = if (song.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.scale(likeScale)
            )
        }
    }
}

@Composable
internal fun SongItemWithMenu(
    song: Song,
    isCurrentlyPlaying: Boolean,
    isPlayingGlobally: Boolean,
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
                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_album_art_for_song, song.title),
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
                        fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isCurrentlyPlaying) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AnimatedEqualizer(isAnimating = isPlayingGlobally, tint = MaterialTheme.colorScheme.primary)
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
                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.now_playing_more_options),
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
                    Icon(
                        Icons.Default.Info,
                        contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_info)
                    )
                }
                IconButton(onClick = { /* TODO: Share */ }) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_share)
                    )
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
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_set_as_ringtone),
                    icon = Icons.Default.Notifications,
                    onClick = { onSetRingtone(); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
                PillButton(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_change_cover),
                    icon = Icons.Default.Image,
                    onClick = { onChangeCover(); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
                PillButton(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_edit_tags),
                    icon = Icons.Default.Edit,
                    onClick = { onEditTags(); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Menu items
            MenuOption(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play_next), Icons.Default.SkipNext) { onPlayNext(); onDismiss() }
            MenuOption(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_queue), Icons.Default.PlaylistAdd) { onAddToQueue(); onDismiss() }
            MenuOption(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_playlist), Icons.Default.PlaylistAdd) { onAddToPlaylist(); onDismiss() }
            MenuOption(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_go_to_album), Icons.Default.Album) { onGoToAlbum(); onDismiss() }
            MenuOption(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_edit_audio), Icons.Default.MusicNote) { onEditAudio(); onDismiss() }
            MenuOption(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_update_lyrics), Icons.Default.Edit) { onUpdateLyrics(); onDismiss() }
            MenuOption(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_delete_from_device), Icons.Default.Delete, isDestructive = true) { onDelete(); onDismiss() }
        }
    }
}

@Composable
internal fun AnimatedEqualizer(
    modifier: Modifier = Modifier,
    isAnimating: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val fallbackMotion = remember(isAnimating) {
        MotionDirective(
            state = if (isAnimating) MotionPlayState.RUNNING else MotionPlayState.HOLD,
            songId = null,
            intensity = if (isAnimating) 1f else 0f
        )
    }
    val fallbackPhase by rememberPlaybackMotionClock(
        motion = fallbackMotion,
        speed = EQUALIZER_PLAYBACK_MOTION_SPEED
    )
    AnimatedEqualizer(
        modifier = modifier,
        motion = fallbackMotion,
        phase = fallbackPhase,
        tint = tint
    )
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
            Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_sort_by), style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))

            listOf("Song name", "Artist name", "Album name", "Folder name",
                "Time added", "Play count", "Year", "Duration", "Size").forEach { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { selectedMode = mode }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (mode) {
                            "Song name" -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_mode_song_name)
                            "Artist name" -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_mode_artist_name)
                            "Album name" -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_mode_album_name)
                            "Folder name" -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_mode_folder_name)
                            "Time added" -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_mode_time_added)
                            "Play count" -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_mode_play_count)
                            "Year" -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_mode_year)
                            "Duration" -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_mode_duration)
                            "Size" -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_mode_size)
                            else -> mode
                        },
                        style = MaterialTheme.typography.bodyLarge,
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
                    Text(
                        text = when (order) {
                            "A to Z" -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_order_from_a_to_z)
                            "Z to A" -> androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_sort_order_from_z_to_a)
                            else -> order
                        },
                        style = MaterialTheme.typography.bodyLarge,
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
                    Text(
                        androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_cancel).uppercase(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Button(onClick = { onConfirm(selectedMode, selectedOrder) }, modifier = Modifier.weight(1f)) {
                    Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_ok))
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
            .padding(top = SpacingMedium)
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
                text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_last_added),
                style = MaterialTheme.typography.homeSectionHeader,
                color = MaterialTheme.colorScheme.onBackground
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.library_screens_b_see_all),
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
                    Surface(
                        modifier = Modifier.padding(8.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                }
            }
        }

        // TEXT BELOW THE CARD
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.cardTitle,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.album,
            style = MaterialTheme.typography.cardSubtitle,
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
            Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_delete_from_device_title))
        },
        text = {
            Text(
                androidx.compose.ui.res.stringResource(
                    com.sukoon.music.R.string.library_screens_b_delete_song_message,
                    song.title
                ),
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
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_cancel))
            }
        }
    )
}

@Composable
fun StandardSongListItem(
    song: Song,
    isCurrentSong: Boolean,
    isPlaybackActive: Boolean,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onCheckboxClick: (Boolean) -> Unit = {},
    onLongPress: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrentSong) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick) // enhanced clickable with long press could be added here if needed
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection Checkbox (Visible only in selection mode)
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onCheckboxClick
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Album Art
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

        Spacer(modifier = Modifier.width(16.dp))

        // Song Info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isCurrentSong) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AnimatedEqualizer(
                        isAnimating = isPlaybackActive,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_artist_album_pair, song.artist, song.album),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Action Buttons (Hidden in selection mode)
        if (!isSelectionMode) {
            // Like Button
            AnimatedFavoriteIcon(
                isLiked = song.isLiked,
                songId = song.id,
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { onLikeClick() }
            )

            // Menu Button
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.now_playing_more_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}



