package com.sukoon.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukoon.music.domain.manager.SleepTimerState
import com.sukoon.music.domain.model.ScanState

/**
 * Quick actions row with 4 action buttons.
 *
 * Layout:
 * - Phone portrait: Row of 4 buttons, equal width
 * - Phone landscape (<360dp): 2x2 grid
 * - Landscape: 48dp height, 20dp icons
 *
 * Actions:
 * 1. Shuffle All: Shuffles and plays entire library
 * 2. Sleep Timer: Opens dialog to set auto-stop timer
 * 3. Scan Music: Triggers MediaStore scan for new files
 * 4. Liked Songs: Navigates to liked songs screen
 *
 * @param onShuffleAll Callback when Shuffle All is tapped
 * @param onSleepTimer Callback when Sleep Timer is tapped
 * @param onScanMusic Callback when Scan Music is tapped
 * @param onLikedSongs Callback when Liked Songs is tapped
 * @param shuffleEnabled Whether shuffle button should be enabled
 * @param sleepTimerState Current sleep timer state
 * @param scanState Current scan state
 * @param likedSongsCount Number of liked songs
 * @param modifier Modifier for layout
 */
@Composable
fun QuickActionsRow(
    onShuffleAll: () -> Unit,
    onSleepTimer: () -> Unit,
    onScanMusic: () -> Unit,
    onLikedSongs: () -> Unit,
    shuffleEnabled: Boolean,
    sleepTimerState: SleepTimerState,
    scanState: ScanState,
    likedSongsCount: Int,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isSmallScreen = screenWidthDp < 360

    // Use 2x2 grid for small screens, row for larger screens
    if (isSmallScreen) {
        QuickActionsGrid(
            onShuffleAll = onShuffleAll,
            onSleepTimer = onSleepTimer,
            onScanMusic = onScanMusic,
            onLikedSongs = onLikedSongs,
            shuffleEnabled = shuffleEnabled,
            sleepTimerState = sleepTimerState,
            scanState = scanState,
            likedSongsCount = likedSongsCount,
            modifier = modifier
        )
    } else {
        QuickActionsRowLayout(
            onShuffleAll = onShuffleAll,
            onSleepTimer = onSleepTimer,
            onScanMusic = onScanMusic,
            onLikedSongs = onLikedSongs,
            shuffleEnabled = shuffleEnabled,
            sleepTimerState = sleepTimerState,
            scanState = scanState,
            likedSongsCount = likedSongsCount,
            modifier = modifier
        )
    }
}

/**
 * Row layout for normal-sized screens.
 */
@Composable
private fun QuickActionsRowLayout(
    onShuffleAll: () -> Unit,
    onSleepTimer: () -> Unit,
    onScanMusic: () -> Unit,
    onLikedSongs: () -> Unit,
    shuffleEnabled: Boolean,
    sleepTimerState: SleepTimerState,
    scanState: ScanState,
    likedSongsCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionButton(
            icon = Icons.Default.Shuffle,
            label = "Shuffle All",
            onClick = onShuffleAll,
            enabled = shuffleEnabled,
            isActive = false,
            isLoading = false,
            badgeCount = null,
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            icon = Icons.Default.Timer,
            label = "Sleep Timer",
            onClick = onSleepTimer,
            enabled = true,
            isActive = sleepTimerState is SleepTimerState.Active,
            isLoading = false,
            badgeCount = (sleepTimerState as? SleepTimerState.Active)?.remainingMinutes,
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            icon = Icons.Default.Refresh,
            label = "Scan Music",
            onClick = onScanMusic,
            enabled = scanState !is ScanState.Scanning,
            isActive = false,
            isLoading = scanState is ScanState.Scanning,
            badgeCount = null,
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            icon = Icons.Default.Favorite,
            label = "Liked Songs",
            onClick = onLikedSongs,
            enabled = likedSongsCount > 0,
            isActive = false,
            isLoading = false,
            badgeCount = formatBadgeCount(likedSongsCount),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Grid layout (2x2) for small screens.
 */
@Composable
private fun QuickActionsGrid(
    onShuffleAll: () -> Unit,
    onSleepTimer: () -> Unit,
    onScanMusic: () -> Unit,
    onLikedSongs: () -> Unit,
    shuffleEnabled: Boolean,
    sleepTimerState: SleepTimerState,
    scanState: ScanState,
    likedSongsCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Shuffle,
                label = "Shuffle",
                onClick = onShuffleAll,
                enabled = shuffleEnabled,
                isActive = false,
                isLoading = false,
                badgeCount = null,
                modifier = Modifier.weight(1f)
            )

            QuickActionButton(
                icon = Icons.Default.Timer,
                label = "Timer",
                onClick = onSleepTimer,
                enabled = true,
                isActive = sleepTimerState is SleepTimerState.Active,
                isLoading = false,
                badgeCount = (sleepTimerState as? SleepTimerState.Active)?.remainingMinutes,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Refresh,
                label = "Scan",
                onClick = onScanMusic,
                enabled = scanState !is ScanState.Scanning,
                isActive = false,
                isLoading = scanState is ScanState.Scanning,
                badgeCount = null,
                modifier = Modifier.weight(1f)
            )

            QuickActionButton(
                icon = Icons.Default.Favorite,
                label = "Liked",
                onClick = onLikedSongs,
                enabled = likedSongsCount > 0,
                isActive = false,
                isLoading = false,
                badgeCount = formatBadgeCount(likedSongsCount),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual quick action button.
 */
@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    isActive: Boolean,
    isLoading: Boolean,
    badgeCount: Int?,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            isActive -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.secondaryContainer
        },
        label = "button_color"
    )

    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        containerColor = containerColor
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    // Loading spinner
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else if (badgeCount != null) {
                    // Icon with badge
                    BadgedBox(
                        badge = {
                            Badge {
                                Text(
                                    text = badgeCount.toString(),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    // Plain icon
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = label,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Format badge count with compression for large numbers.
 * Examples: 42 → "42", 99 → "99", 100 → "99+", 10000 → "10k+"
 */
private fun formatBadgeCount(count: Int): Int {
    return when {
        count > 10000 -> 10000 // Show "10k+"
        count > 100 -> 99 // Show "99+"
        else -> count
    }
}
