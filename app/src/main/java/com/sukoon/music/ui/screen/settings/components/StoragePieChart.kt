package com.sukoon.music.ui.screen.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sukoon.music.R
import com.sukoon.music.domain.repository.StorageStats
import kotlin.math.cos
import kotlin.math.sin

/**
 * Storage usage pie chart with legend.
 *
 * Features:
 * - Visual breakdown of Music/Cache/Database storage
 * - Zero-data empty state
 * - TalkBack semantics for accessibility
 * - Formatted size labels (GB/MB/KB)
 */
@Composable
fun StoragePieChart(
    storageStats: StorageStats,
    modifier: Modifier = Modifier
) {
    val total = storageStats.audioLibrarySizeBytes + storageStats.cacheSizeBytes + storageStats.databaseSizeBytes
    val musicColor = Color(0xFF4CAF50)
    val cacheColor = Color(0xFFFFC107)
    val databaseColor = Color(0xFF2196F3)

    if (total == 0L) {
        // Empty state
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.settings_storage_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Pie Chart
            val chartDescription = buildString {
                append(stringResource(R.string.settings_storage_chart_label))
                append(": ")
                append(storageStats.audioLibrarySizeBytes.formatBytes())
                append(", ")
                append(storageStats.cacheSizeBytes.formatBytes())
                append(", ")
                append(storageStats.databaseSizeBytes.formatBytes())
            }
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.CenterHorizontally)
                    .semantics {
                        contentDescription = chartDescription
                    }
            ) {
                drawStoragePieChart(
                    musicBytes = storageStats.audioLibrarySizeBytes,
                    cacheBytes = storageStats.cacheSizeBytes,
                    databaseBytes = storageStats.databaseSizeBytes,
                    total = total,
                    musicColor = musicColor,
                    cacheColor = cacheColor,
                    databaseColor = databaseColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legend
            StorageLegend(
                musicBytes = storageStats.audioLibrarySizeBytes,
                cacheBytes = storageStats.cacheSizeBytes,
                databaseBytes = storageStats.databaseSizeBytes,
                musicColor = musicColor,
                cacheColor = cacheColor,
                databaseColor = databaseColor
            )
        }
    }
}

@Composable
private fun StorageLegend(
    musicBytes: Long,
    cacheBytes: Long,
    databaseBytes: Long,
    musicColor: Color,
    cacheColor: Color,
    databaseColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StorageLegendItem(
            color = musicColor,
            label = stringResource(R.string.settings_storage_music),
            value = musicBytes
        )
        StorageLegendItem(
            color = cacheColor,
            label = stringResource(R.string.settings_storage_cache),
            value = cacheBytes
        )
        StorageLegendItem(
            color = databaseColor,
            label = stringResource(R.string.settings_storage_database),
            value = databaseBytes
        )
    }
}

@Composable
private fun StorageLegendItem(
    color: Color,
    label: String,
    value: Long
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(16.dp)
        ) {
            drawCircle(color = color)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.formatBytes(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun DrawScope.drawStoragePieChart(
    musicBytes: Long,
    cacheBytes: Long,
    databaseBytes: Long,
    total: Long,
    musicColor: Color,
    cacheColor: Color,
    databaseColor: Color
) {
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)

    var startAngle = -90f

    // Music segment
    if (musicBytes > 0) {
        val angle = (musicBytes / total) * 360f
        drawArc(
            color = musicColor,
            startAngle = startAngle,
            sweepAngle = angle,
            useCenter = true,
            topLeft = center - Offset(radius, radius),
            size = Size(radius * 2, radius * 2)
        )
        startAngle += angle
    }

    // Cache segment
    if (cacheBytes > 0) {
        val angle = (cacheBytes / total) * 360f
        drawArc(
            color = cacheColor,
            startAngle = startAngle,
            sweepAngle = angle,
            useCenter = true,
            topLeft = center - Offset(radius, radius),
            size = Size(radius * 2, radius * 2)
        )
        startAngle += angle
    }

    // Database segment
    if (databaseBytes > 0) {
        val angle = (databaseBytes / total) * 360f
        drawArc(
            color = databaseColor,
            startAngle = startAngle,
            sweepAngle = angle,
            useCenter = true,
            topLeft = center - Offset(radius, radius),
            size = Size(radius * 2, radius * 2)
        )
    }
}

/**
 * Format bytes to human-readable string (GB/MB/KB).
 */
fun Long.formatBytes(): String {
    return when {
        this >= 1_000_000_000L -> String.format("%.2f GB", this / 1_000_000_000.0)
        this >= 1_000_000L -> String.format("%.2f MB", this / 1_000_000.0)
        this >= 1_000L -> String.format("%.2f KB", this / 1_000.0)
        else -> "$this B"
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun StoragePieChartPreview() {
    StoragePieChart(
        storageStats = StorageStats(
            databaseSizeBytes = 12_000_000L,
            cacheSizeBytes = 450_000_000L,
            audioLibrarySizeBytes = 2_300_000_000L
        )
    )
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun StoragePieChartEmptyPreview() {
    StoragePieChart(
        storageStats = StorageStats(
            databaseSizeBytes = 0L,
            cacheSizeBytes = 0L,
            audioLibrarySizeBytes = 0L
        )
    )
}
