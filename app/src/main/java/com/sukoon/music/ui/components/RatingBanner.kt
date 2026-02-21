package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sukoon.music.ui.theme.CardElevationLow
import com.sukoon.music.ui.theme.CardShape
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.theme.cardTitle
import androidx.compose.ui.tooling.preview.Preview

/**
 * Rating Banner Component
 *
 * A dismissible banner that prompts users to rate the app on Google Play with a green gradient background.
 * Only shown to engaged users (10+ launches, 3+ days since install) who haven't already dismissed or rated.
 *
 * Features:
 * - Green gradient background
 * - Star icon + title + subtitle text
 * - Dismissible via X button
 * - Clickable to open in-app review dialog
 * - Smart visibility based on engagement metrics
 */
@Composable
fun RatingBanner(
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevationLow)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4CAF50), // Green
                            Color(0xFF2E7D32)  // Darker Green
                        )
                    )
                )
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.rating_banner_title),
                        style = MaterialTheme.typography.cardTitle,
                        color = Color.White
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.rating_banner_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_dismiss),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RatingBannerPreview() {
    SukoonMusicPlayerTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            RatingBanner(
                onDismiss = {},
                onClick = {}
            )
        }
    }
}
