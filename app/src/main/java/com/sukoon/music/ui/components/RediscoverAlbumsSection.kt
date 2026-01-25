package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.theme.LastAddedCardWidth
import com.sukoon.music.ui.theme.SpacingLarge
import com.sukoon.music.ui.theme.SpacingMedium
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.theme.*
import com.sukoon.music.ui.theme.CardElevationMedium
import com.sukoon.music.ui.theme.CardShape

/**
 * Rediscover Albums Section - Shows albums played before but not in the last 30 days.
 * This is a global component that can be used across different screens.
 */
@Composable
fun RediscoverAlbumsSection(
    albums: List<Album>,
    onAlbumClick: (Long) -> Unit,
    onHeaderClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (albums.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SpacingMedium)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onHeaderClick)
                .padding(horizontal = SpacingLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "You Loved This Before",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "See all",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        /*Text(
            text = "Albums waiting to be rediscovered",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = SpacingLarge)
        )*/

        Spacer(modifier = Modifier.height(SpacingMedium))

        // Horizontal scrolling album list
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = SpacingLarge),
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            items(
                items = albums.take(10),
                key = { it.id }
            ) { album ->
                RediscoverAlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album.id) }
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

/**
 * Album card for Rediscover section with play button overlay.
 */
@Composable
fun RediscoverAlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(LastAddedCardWidth)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clickable(onClick = onClick),
            shape = CardShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = CardElevationMedium
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Album art
                if (album.albumArtUri != null) {
                    SubcomposeAsyncImage(
                        model = album.albumArtUri,
                        contentDescription = album.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Album,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                // Gradient overlay + play button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f)
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

                // Rediscover badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Album info below the card
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun RediscoverAlbumsSectionPreview() {
    SukoonMusicPlayerTheme(theme = AppTheme.DARK) {
        RediscoverAlbumsSection(
            albums = listOf(
                Album(id = 1, title = "Abbey Road", artist = "The Beatles", songCount = 17, totalDuration = 2874000, albumArtUri = null, songIds = emptyList()),
                Album(id = 2, title = "Dark Side of the Moon", artist = "Pink Floyd", songCount = 10, totalDuration = 2580000, albumArtUri = null, songIds = emptyList()),
                Album(id = 3, title = "Thriller", artist = "Michael Jackson", songCount = 9, totalDuration = 2520000, albumArtUri = null, songIds = emptyList())
            ),
            onAlbumClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RediscoverAlbumCardPreview() {
    SukoonMusicPlayerTheme {
        RediscoverAlbumCard(
            album = Album(id = 1, title = "Abbey Road", artist = "The Beatles", songCount = 17, totalDuration = 2874000, albumArtUri = null, songIds = emptyList()),
            onClick = {}
        )
    }
}
