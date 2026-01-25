package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukoon.music.domain.repository.ListeningStatsSnapshot
import com.sukoon.music.ui.theme.SpacingMedium
import com.sukoon.music.ui.theme.*
import com.sukoon.music.ui.theme.CardElevationLow

/**
 * Listening Stats Card for HomeScreen.
 * Displays aggregated listening stats in a compact, non-intrusive card.
 *
 * Display Logic:
 * - Shows all 3 stat lines: total listening time, peak time of day, and top artist
 * - Hide if total listening time < 30 minutes
 * - Never shows numbers that feel "judgey" (no streaks, no counts)
 * - Warm, reflective tone
 *
 * @param stats The aggregated listening statistics (null = don't show)
 */
@Composable
fun ListeningStatsCard(stats: ListeningStatsSnapshot?) {
    if (stats == null) {
        return  // Don't render if no data
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingMedium, vertical = 8.dp),
        enableBlur = false,
        elevation = CardElevationLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with gradient accent
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Your Week",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Subtle divider
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            )

            // All three stat lines with better styling
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stat 1: Total Listening Time (emphasized)
                EnhancedStatLine(
                    icon = Icons.Default.MusicNote,
                    label = "Total Listening",
                    value = formatListeningTime(stats.totalListeningTimeMinutes),
                    isHighlighted = true
                )

                // Stat 2: Peak Time of Day
                EnhancedStatLine(
                    icon = Icons.Default.History,
                    label = "Mostly",
                    value = "${stats.peakTimeOfDay} 🎧",
                    isHighlighted = false
                )

                // Stat 3: Top Artist (only if available)
                if (!stats.topArtist.isNullOrBlank()) {
                    EnhancedStatLine(
                        icon = Icons.Default.Favorite,
                        label = "Top Artist",
                        value = stats.topArtist,
                        isHighlighted = false
                    )
                }
            }
        }
    }
}

/**
 * Enhanced stat line with separate label and value.
 * Displays icon + label + value with optional highlighting.
 */
@Composable
private fun EnhancedStatLine(
    icon: ImageVector,
    label: String,
    value: String,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Icon with background
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = if (isHighlighted)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isHighlighted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Label and Value
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = if (isHighlighted) 15.sp else 13.sp,
                    fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isHighlighted)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Format listening time in minutes to a human-readable string.
 * Examples: "32 minutes", "2.5 hours", "1.2 hours"
 */
private fun formatListeningTime(minutes: Long): String {
    return when {
        minutes < 60 -> "$minutes minutes listened"
        else -> {
            val hours = minutes / 60.0
            when {
                hours < 10 -> "%.1f hours listened".format(hours)
                else -> "${hours.toInt()} hours listened"
            }
        }
    }
}

/**
 * Preview for Listening Stats Card.
 */
@Composable
fun ListeningStatsCardPreview() {
    val sampleStats = ListeningStatsSnapshot(
        totalListeningTimeMinutes = 320,
        topArtist = "Arijit Singh",
        peakTimeOfDay = "night"
    )

    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ListeningStatsCard(stats = sampleStats)
        }
    }
}
