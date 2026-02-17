package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Folder
import com.sukoon.music.ui.theme.*
import com.sukoon.music.ui.util.rememberAlbumPalette
import androidx.compose.material.icons.filled.MusicNote

/**
 * Reusable list item for folders.
 *
 * Features:
 * - Leading: Folder icon or album art (48dp rounded)
 * - Middle: Column with folder name + subtitle (path • song count)
 * - Trailing: Three-dot menu button
 * - Context Menu with various actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderRow(
    folder: Folder,
    isHidden: Boolean,
    onFolderClick: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onHide: () -> Unit,
    onUnhide: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onFolderClick,
                indication = ripple(bounded = true),
                interactionSource = remember { MutableInteractionSource() }
            ),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent stripe with extracted vibrant color
            val palette = rememberAlbumPalette(folder.albumArtUris.firstOrNull())
            val accentColor = palette.vibrant.copy(alpha = 0.6f)
            val backgroundTint = palette.mutedLight.copy(alpha = 0.12f)

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(80.dp)
                    .background(accentColor, RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Leading: Album art collage with shadow and dynamic background tint
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp), clip = false)
                    .background(backgroundTint)
            ) {
                FolderAlbumArtCollage(
                    albumArtUris = folder.albumArtUris,
                    size = 80.dp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Middle: Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 4.dp)
                )

                // Metadata badges with vibrant accent colors
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(20.dp)
                ) {
                    val iconAccent = palette.vibrant
                    val badgeBg = palette.vibrant.copy(alpha = 0.15f)

                    // Song count badge
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = iconAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${folder.songCount}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                ),
                                color = iconAccent
                            )
                        }
                    }

                    // Duration badge
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = iconAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = folder.formattedDuration(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                ),
                                color = iconAccent
                            )
                        }
                    }
                }
            }

            // Trailing: Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // Header (non-clickable info)
                    Text(
                        text = folder.path,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider()

                    DropdownMenuItem(
                        text = { Text("Play") },
                        onClick = { onPlay(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Play next") },
                        onClick = { onPlayNext(); showMenu = false },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to queue") },
                        onClick = { onAddToQueue(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.AddToQueue, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to playlist") },
                        onClick = { onAddToPlaylist(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) }
                    )
                    if (isHidden) {
                        DropdownMenuItem(
                            text = { Text("Unhide") },
                            onClick = { onUnhide(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Visibility, null) }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Hide") },
                            onClick = { onHide(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.VisibilityOff, null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete from device") },
                        onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}
