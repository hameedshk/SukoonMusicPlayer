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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import androidx.compose.ui.tooling.preview.Preview

/**
 * Premium Banner Component
 *
 * A dismissible banner that promotes premium features with a gold gradient background.
 * Only shown when user is not premium and banner hasn't been dismissed.
 *
 * Features:
 * - Gold gradient background
 * - Star icon + title + price info
 * - Dismissible via X button
 * - Clickable to open full premium dialog
 * - Reappears after app restart even if dismissed
 */
@Composable
fun PremiumBanner(
    isPremium: Boolean,
    isDismissed: Boolean,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isPremium || isDismissed) {
        return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFD700), // Gold
                            Color(0xFFFFA500)  // Orange
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
                        text = "Unlock Premium",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Remove ads • Lifetime • \$4.99",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PremiumBannerPreview() {
    SukoonMusicPlayerTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            PremiumBanner(
                isPremium = false,
                isDismissed = false,
                onDismiss = {},
                onClick = {}
            )
        }
    }
}
