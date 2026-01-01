package com.sukoon.music.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sukoon.music.domain.model.Song
import java.text.SimpleDateFormat
import java.util.*

/**
 * Song information dialog showing detailed metadata.
 * Displays song properties like title, format, size, duration, and technical details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        sheetMaxWidth = androidx.compose.ui.unit.Dp.Unspecified
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Text(
                    text = "Song information",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                // Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    // Title
                    item {
                        InfoField("Title", song.title)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Format
                    item {
                        val format = getFormatFromPath(song.path)
                        InfoField("Format", format)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Size
                    item {
                        val size = formatFileSize(song.size)
                        InfoField("Size", size)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Duration
                    item {
                        InfoField("Duration", song.durationFormatted())
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Album
                    item {
                        InfoField("Album", song.album.ifEmpty { "<unknown>" })
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Artist
                    item {
                        InfoField("Artist", song.artist.ifEmpty { "<unknown>" })
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Genre
                    item {
                        InfoField("Genre", song.genre ?: "<unknown>")
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Track number
                    item {
                        InfoField("Track number", "<unknown>")
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Album artist
                    item {
                        InfoField("Album artist", "<unknown>")
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Composer
                    item {
                        InfoField("Composer", "<unknown>")
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Year
                    item {
                        val year = if (song.year > 0) song.year.toString() else "<unknown>"
                        InfoField("Year", year)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Added time
                    item {
                        val addedTime = formatTimestamp(song.dateAdded)
                        InfoField("Added time", addedTime)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Modified time
                    item {
                        InfoField("Modified time", formatTimestamp(song.dateAdded))
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Bitrate
                    item {
                        InfoField("Bitrate", "<unknown>")
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Sampling rate
                    item {
                        InfoField("Sampling rate", "<unknown>")
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Channels
                    item {
                        InfoField("Channels", "<unknown>")
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Bits per sample
                    item {
                        InfoField("Bits per sample", "<unknown>")
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // File name
                    item {
                        val fileName = song.path.substringAfterLast("/")
                        InfoField("File name", fileName)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Location
                    item {
                        InfoField("Location", song.path)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // OK Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "OK",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.6f)
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "<unknown>"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> String.format("%.1f GB", gb)
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return "<unknown>"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp * 1000)) // Convert seconds to milliseconds
}

private fun getFormatFromPath(path: String): String {
    val extension = path.substringAfterLast(".", "")
    return when (extension.lowercase()) {
        "mp3" -> "mp3(MPEG)"
        "flac" -> "flac"
        "m4a", "aac" -> "m4a(AAC)"
        "ogg" -> "ogg"
        "wav" -> "wav"
        "opus" -> "opus"
        else -> extension.ifEmpty { "<unknown>" }
    }
}
