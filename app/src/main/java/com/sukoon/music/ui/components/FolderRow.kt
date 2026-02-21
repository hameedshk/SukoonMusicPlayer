package com.sukoon.music.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sukoon.music.domain.model.Folder
import com.sukoon.music.ui.theme.CompactButtonCornerRadius
import com.sukoon.music.ui.theme.SpacingLarge
import com.sukoon.music.ui.theme.SpacingMedium
import com.sukoon.music.ui.theme.SpacingSmall
import com.sukoon.music.ui.theme.SpacingTiny
import com.sukoon.music.ui.theme.SpacingXSmall

private val FolderRowArtSize = 56.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderRow(
    folder: Folder,
    isHidden: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowShape = RoundedCornerShape(12.dp)
    val rowSelected = isSelectionMode && isSelected
    val rowContainerColor = if (rowSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        Color.Transparent
    }
    val songCountLabel = androidx.compose.ui.res.pluralStringResource(
        com.sukoon.music.R.plurals.common_song_count,
        folder.songCount,
        folder.songCount
    )
    val metaLine = "$songCountLabel \u2022 ${folder.formattedDuration()}"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingMedium, vertical = SpacingTiny)
            .clip(rowShape)
            .background(rowContainerColor)
            .border(
                width = if (rowSelected) 1.dp else 0.dp,
                color = if (rowSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = rowShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = SpacingLarge, vertical = SpacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(FolderRowArtSize)
                .clip(RoundedCornerShape(CompactButtonCornerRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            FolderAlbumArtCollage(
                albumArtUris = folder.albumArtUris,
                size = FolderRowArtSize
            )
        }

        Spacer(modifier = Modifier.width(SpacingMedium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(SpacingXSmall))
            Text(
                text = metaLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isHidden) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_hidden_folders),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = androidx.compose.ui.res.stringResource(
                        if (isSelected) {
                            com.sukoon.music.R.string.library_screens_b_checked
                        } else {
                            com.sukoon.music.R.string.library_screens_b_unchecked
                        }
                    ),
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            IconButton(onClick = onMoreClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_more),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
