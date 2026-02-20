package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAdView
import com.sukoon.music.ui.theme.*

/**
 * Native Ad Card that mimics the song row UI.
 *
 * Features:
 * - Matches SongItem layout for native look
 * - Shows "Ad" badge in top-left corner
 * - Placeholder thumbnail + headline + description
 * - Click opens the ad without pausing music
 * - Non-intrusive styling
 *
 * @param onAdClick Callback when ad is clicked (opens advertiser)
 * @param modifier Optional modifier
 */
@Composable
fun NativeAdCard(
    onAdClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onAdClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ad thumbnail placeholder
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Campaign,
                contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.ad_badge_label),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Ad content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Ad headline
            Text(
                text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.native_ad_sponsored_label),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Ad description
            Text(
                text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.native_ad_featured_promotion),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Ad badge
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 2.dp
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.ad_badge_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

/**
 * Helper function to inject native ad cards into a list at regular intervals.
 *
 * Usage:
 * ```
 * items(injectNativeAds(songs, interval = 15)) { item ->
 *     when (item) {
 *         is ListItem.SongItem -> SongItem(item.song, ...)
 *         is ListItem.AdItem -> NativeAdCard(...)
 *     }
 * }
 * ```
 *
 * @param items Original list of items
 * @param interval How many items between ads (default: 15)
 * @return List with ad items injected
 */
fun <T> injectNativeAds(
    items: List<T>,
    interval: Int = 15
): List<ListItem<T>> {
    if (items.isEmpty()) return emptyList()

    val result = mutableListOf<ListItem<T>>()
    items.forEachIndexed { index, item ->
        result.add(ListItem.SongItem(item))
        // Inject ad after every 'interval' items
        if ((index + 1) % interval == 0 && index + 1 < items.size) {
            result.add(ListItem.AdItem())
        }
    }
    return result
}

/**
 * Sealed class to represent list items (songs + ads).
 */
sealed class ListItem<T> {
    data class SongItem<T>(val item: T) : ListItem<T>()
    data class AdItem<T>(val dummy: Unit = Unit) : ListItem<T>()
}
