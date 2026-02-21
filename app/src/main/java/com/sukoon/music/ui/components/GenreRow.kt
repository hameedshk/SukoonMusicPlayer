package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.model.Genre
import com.sukoon.music.ui.theme.SpacingLarge
import com.sukoon.music.ui.theme.SpacingMedium
import com.sukoon.music.ui.theme.SpacingSmall
import com.sukoon.music.ui.theme.SpacingTiny
import com.sukoon.music.ui.theme.SpacingXSmall
import com.sukoon.music.ui.theme.*

@Composable
fun GenreRow(
    genre: Genre,
    onClick: () -> Unit,
    onPlayClick: () -> Unit = {},
    onPlayNextClick: () -> Unit = {},
    onAddToQueueClick: () -> Unit = {},
    onAddToPlaylistClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onMoreClick: (Genre) -> Unit = {},
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onSelectionToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
    key: Any? = null
) {
    val rowShape = RoundedCornerShape(12.dp)
    val rowSelected = isSelectionMode && isSelected
    val rowContainerColor = if (rowSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        Color.Transparent
    }
    val rowBorderWidth = if (rowSelected) 1.dp else 0.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingMedium, vertical = SpacingTiny)
            .clip(rowShape)
            .background(rowContainerColor)
            .border(
                width = rowBorderWidth,
                color = if (rowSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = rowShape
            )
            .clickable(onClick = if (isSelectionMode) onSelectionToggle else onClick)
            .padding(horizontal = SpacingLarge, vertical = SpacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (genre.artworkUri != null) {
                SubcomposeAsyncImage(
                    model = genre.artworkUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        PlaceholderAlbumArt.Placeholder(
                            seed = PlaceholderAlbumArt.generateSeed(
                                albumName = genre.name,
                                albumId = genre.id
                            )
                        )
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

        Spacer(modifier = Modifier.width(SpacingMedium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = genre.name,
                style = MaterialTheme.typography.compactCardTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.size(SpacingXSmall))
            Text(
                text = androidx.compose.ui.res.pluralStringResource(
                    com.sukoon.music.R.plurals.common_song_count,
                    genre.songCount,
                    genre.songCount
                ),
                style = MaterialTheme.typography.cardSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSelectionToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = androidx.compose.ui.res.stringResource(
                        if (isSelected) com.sukoon.music.R.string.library_screens_b_checked
                        else com.sukoon.music.R.string.library_screens_b_unchecked
                    ),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            IconButton(onClick = { onMoreClick(genre) }, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_more),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
