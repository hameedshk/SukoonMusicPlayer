package com.sukoon.music.ui.components

import android.media.MediaMetadataRetriever
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sukoon.music.domain.model.Song
import androidx.core.net.toUri
import android.net.Uri
import com.sukoon.music.ui.theme.*

enum class AudioQuality {
    LOW,
    NORMAL,
    HIGH,
    VERY_HIGH
}

data class TechnicalInfo(
    // Primary (always safe to show)
    val qualityLabel: AudioQuality,
    val codec: String,
    val bitrateKbps: Int?,

    // Advanced (optional, hidden unless available)
    val samplingRateHz: Int? = null,
    val channels: Int? = null
)

private fun mapBitrateToQuality(bitrateKbps: Int?): AudioQuality {
    return when {
        bitrateKbps == null -> AudioQuality.NORMAL
        bitrateKbps < 96 -> AudioQuality.LOW
        bitrateKbps < 160 -> AudioQuality.NORMAL
        bitrateKbps < 256 -> AudioQuality.HIGH
        else -> AudioQuality.VERY_HIGH
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    // 1. Properly fetch technical metadata using remember
    val context = androidx.compose.ui.platform.LocalContext.current
    val technicalInfo = remember(song.path) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, song.uri.toUri())

            val bitrate = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull()
                ?.div(1000)

            val samplingRate = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                ?.toIntOrNull()

            val codec = getFormatFromPath(song.path)

            TechnicalInfo(
                qualityLabel = mapBitrateToQuality(bitrate),
                codec = codec,
                bitrateKbps = bitrate,
                samplingRateHz = samplingRate,
                channels = 2 // safe assumption on mobile
            )
        } catch (e: Exception) {
            TechnicalInfo(
                qualityLabel = AudioQuality.NORMAL,
                codec = getFormatFromPath(song.path),
                bitrateKbps = null
            )
        } finally {
            retriever.release()
        }
    }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_info_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    item {
                        InfoField(
                            Icons.Default.Title,
                            androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_info_label_title),
                            song.title
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    item {
                        val format = getFormatFromPath(song.path)
                        InfoField(
                            Icons.Default.AudioFile,
                            androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_info_label_format),
                            format.ifEmpty {
                                androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_info_unknown)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    item {
                        InfoField(
                            Icons.Default.Storage,
                            androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_info_label_size),
                            formatFileSize(song.size)
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    item {
                        InfoField(
                            Icons.Default.Timer,
                            androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_info_label_duration),
                            song.durationFormatted()
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    // --- TECHNICAL DETAILS (Using technicalInfo) ---
                    item {technicalInfo.bitrateKbps?.let {
                        InfoField(
                            Icons.Default.SettingsSuggest,
                            androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_info_label_bitrate),
                            androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_info_value_bitrate_kbps, it)
                        )
                    }}
                    
                    item {
                        technicalInfo.samplingRateHz?.let {
                            Spacer(modifier = Modifier.height(16.dp))
                            InfoField(
                                Icons.Default.Speed,
                                androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_info_label_sampling_rate),
                                androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_info_value_sampling_rate_hz, it)
                            )
                        }
                    }

                    item {
                        technicalInfo.channels?.let {
                            Spacer(modifier = Modifier.height(16.dp))
                            InfoField(
                                Icons.Default.Speaker,
                                androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_info_label_channels),
                                androidx.compose.ui.res.stringResource(
                                    if (it == 1) com.sukoon.music.R.string.song_info_channel_mono
                                    else com.sukoon.music.R.string.song_info_channel_stereo
                                )
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    // --- REMAINING METADATA ---
                    item {
                        InfoField(
                            Icons.Default.Person,
                            androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_info_label_artist),
                            song.artist.ifEmpty {
                                androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_info_unknown)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    item {
                        val fileName = song.path.substringAfterLast("/")
                        InfoField(
                            Icons.Default.Description,
                            androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_info_label_file_name),
                            fileName
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    item {
                        InfoField(
                            Icons.Default.Folder,
                            androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_info_label_location),
                            song.path
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_ok))
                }
            }
        }
    }
}

@Composable
private fun InfoField(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) {
        return androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.song_info_unknown)
    }
    val mb = bytes / (1024.0 * 1024.0)
    return androidx.compose.ui.res.stringResource(
        com.sukoon.music.R.string.song_info_file_size_mb,
        mb
    )
}

private fun getFormatFromPath(path: String): String {
    val extension = path.substringAfterLast(".", "")
    return extension.uppercase()
}

