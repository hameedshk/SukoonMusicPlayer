package com.sukoon.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sukoon.music.domain.model.Genre
import com.sukoon.music.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreContextMenu(
    genre: Genre,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEditTags: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.size(64.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    GenreIcon(
                        genreName = genre.name,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = genre.name,
                        style = MaterialTheme.typography.cardTitle
                    )
                    Text(
                        text = androidx.compose.ui.res.pluralStringResource(
                            com.sukoon.music.R.plurals.common_song_count,
                            genre.songCount,
                            genre.songCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play)) },
                leadingContent = { Icon(Icons.Default.PlayArrow, null) },
                modifier = Modifier.clickable {
                    onPlay()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_play_next)) },
                leadingContent = { Icon(Icons.Default.SkipNext, null) },
                modifier = Modifier.clickable {
                    onPlayNext()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_queue)) },
                leadingContent = { Icon(Icons.Default.Queue, null) },
                modifier = Modifier.clickable {
                    onAddToQueue()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_add_to_playlist)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                modifier = Modifier.clickable {
                    onAddToPlaylist()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_edit_tags)) },
                leadingContent = { Icon(Icons.Default.Edit, null) },
                modifier = Modifier.clickable {
                    onEditTags()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_change_cover)) },
                leadingContent = { Icon(Icons.Default.Image, null) },
                modifier = Modifier.clickable {
                    onChangeCover()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = {
                    Text(
                        androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_delete_from_device),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable {
                    onDelete()
                    onDismiss()
                }
            )
        }
    }
}
