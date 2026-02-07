package com.sukoon.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import android.util.Log
import coil.compose.SubcomposeAsyncImage
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.sukoon.music.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukoon.music.domain.model.SmartPlaylistType
import com.sukoon.music.ui.theme.*

@Composable
internal fun PillButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun MenuOption(
    text: String,
    icon: ImageVector,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun SelectionActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Persistent global indicator strip for private session.
 * Displayed in the top bar on all screens when session is active.
 * Compact version showing lock icon + status + countdown.
 */
@Composable
internal fun PrivateSessionIndicatorStrip(
    sessionState: com.sukoon.music.domain.model.PlaybackSessionState,
    modifier: Modifier = Modifier
) {
    if (!sessionState.isActive) return

    val remainingMs by remember(sessionState.startedAtMs) {
        derivedStateOf { sessionState.getTimeRemainingMs() }
    }
    val remainingMinutes = (remainingMs / 1000 / 60).toInt()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Private Session Active",
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Private Session",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "$remainingMinutes min",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
internal fun RedesignedTopBar(
    onPremiumClick: () -> Unit,
    onGlobalSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoClick: () -> Unit = {},
    sessionState: com.sukoon.music.domain.model.PlaybackSessionState = com.sukoon.music.domain.model.PlaybackSessionState()
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Main header container - uses gradient background for seamless integration
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
                // Logo - fixed size (40dp) with press feedback and click pulse animation
                val logoInteractionSource = remember { MutableInteractionSource() }
                var isLogoPressed by remember { mutableStateOf(false) }
                LaunchedEffect(logoInteractionSource) {
                    logoInteractionSource.interactions.collect { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> isLogoPressed = true
                            is PressInteraction.Release -> isLogoPressed = false
                            is PressInteraction.Cancel -> isLogoPressed = false
                        }
                    }
                }

                // Click pulse trigger (animates briefly when clicked)
                var logoAnimTrigger by remember { mutableStateOf(0) }

                val pressScale by animateFloatAsState(
                    targetValue = if (isLogoPressed) 0.92f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessHigh
                    ),
                    label = "logo_press_scale"
                )

                val clickPulseScale by animateFloatAsState(
                    targetValue = if (logoAnimTrigger % 2 == 1) 1.18f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "logo_click_pulse",
                    finishedListener = { if (logoAnimTrigger % 2 == 1) logoAnimTrigger++ }
                )

                val logoScale = pressScale * clickPulseScale

                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Sukoon Music Logo",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .scale(logoScale)
                        .clickable(
                            interactionSource = logoInteractionSource,
                            indication = null,
                            onClick = {
                                logoAnimTrigger++
                                onLogoClick()
                            }
                        )
                )

                // App name - full "Sukoon Music"
                Text(
                    text = "Sukoon Music",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.wrapContentWidth()
                )

                // Spacer to push icons to the right
                Spacer(modifier = Modifier.weight(1f))

                // Search icon button
                IconButton(
                    onClick = onGlobalSearchClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Settings icon button
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

        // Persistent global indicator strip - shown when private session is active
        PrivateSessionIndicatorStrip(sessionState = sessionState)
    }
}

@Composable
internal fun TabPills(
    tabs: List<String>,
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    /* val tabs = listOf("Hi Hameed", "Songs","Playlist", "Folders", "Albums", "Artists", "Genres")*/

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(
            count = tabs.size,
            key = { index -> tabs[index] }
        ) { index ->
            val tab = tabs[index]
            val isSelected = tab == selectedTab

            Surface(
                modifier = Modifier
                    .height(TabPillHeight)
                    .clickable { onTabSelected(tab) },
                shape = PillShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun WidgetBanner(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingLarge, vertical = SpacingMedium)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SpacingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Add widgets to your home screen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Go",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun ActionButtonGrid(
    onShuffleAllClick: () -> Unit,
    onPlayAllClick: () -> Unit,
    onScanClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingLarge, vertical = SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(SpacingMedium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            ActionButton(
                text = "Shuffle",
                icon = Icons.Default.Shuffle,
                onClick = onShuffleAllClick,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = "Play",
                icon = Icons.Default.PlayArrow,
                onClick = onPlayAllClick,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            ActionButton(
                text = "Scan music",
                icon = Icons.Default.Refresh,
                onClick = onScanClick,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = "Settings",
                icon = Icons.Default.Settings,
                onClick = onSettingsClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation for press effect
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "button_press_scale"
    )

    // Track press state via interaction source
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPressed = true
                is PressInteraction.Release -> isPressed = false
                is PressInteraction.Cancel -> isPressed = false
            }
        }
    }

    Surface(
        modifier = modifier
            .height(56.dp)
            .scale(scale),
        onClick = onClick,
        shape = ActionButtonShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(SpacingLarge),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(SpacingMedium))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
internal fun AlphabetScroller(
    highlightChar: Char?,
    onCharClick: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#".toList()
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.1f), CircleShape),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        alphabet.forEach { char ->
            val isHighlighted = char == highlightChar
            Text(
                text = char.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                ),
                modifier = Modifier
                    .padding(vertical = 1.dp, horizontal = 4.dp)
                    .clickable { onCharClick(char) },
                color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun SortOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
internal fun SelectionBottomBarItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Reusable multi-select action bottom bar for any tab (Albums, Artists, Playlists, etc.)
 * Shows Play, Add to Playlist, Delete, and More (with Play Next & Add to Queue submenu).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectActionBottomBar(
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionBottomBarItem(
                icon = Icons.Default.PlayArrow,
                label = "Play",
                onClick = onPlay
            )
            SelectionBottomBarItem(
                icon = Icons.Default.PlaylistAdd,
                label = "Add to playlist",
                onClick = onAddToPlaylist
            )
            SelectionBottomBarItem(
                icon = Icons.Default.Delete,
                label = "Delete",
                onClick = onDelete
            )
            Box {
                SelectionBottomBarItem(
                    icon = Icons.Default.MoreVert,
                    label = "More",
                    onClick = { showMoreMenu = true }
                )
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    modifier = Modifier.width(180.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Play next") },
                        leadingIcon = { Icon(Icons.Default.SkipNext, null) },
                        onClick = {
                            onPlayNext()
                            showMoreMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to queue") },
                        leadingIcon = { Icon(Icons.Default.QueueMusic, null) },
                        onClick = {
                            onAddToQueue()
                            showMoreMenu = false
                        }
                    )
                }
            }
        }
    }
}

internal fun getSmartPlaylistIcon(type: SmartPlaylistType): ImageVector {
    return when (type) {
        SmartPlaylistType.MY_FAVOURITE -> Icons.Default.Favorite
        SmartPlaylistType.LAST_ADDED -> Icons.Default.Add
        SmartPlaylistType.RECENTLY_PLAYED -> Icons.Default.History
        SmartPlaylistType.MOST_PLAYED -> Icons.Default.PlayArrow
    }
}

/**
 * Reusable animated favorite icon with bounce effect.
 * Automatically animates when isLiked state changes.
 */
@Composable
fun AnimatedFavoriteIcon(
    isLiked: Boolean,
    songId: Long,
    modifier: Modifier = Modifier,
    tint: Color = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    // Animation state for like button
    var prevLikedState by remember(songId) { mutableStateOf(isLiked) }
    var animTrigger by remember(songId) { mutableStateOf(0) }

    LaunchedEffect(songId, isLiked) {
        if (prevLikedState != isLiked) {
            animTrigger++
            prevLikedState = isLiked
        }
    }

    val likeScale by animateFloatAsState(
        targetValue = if (animTrigger % 2 == 0) 1f else 1.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "like_scale",
        finishedListener = { if (animTrigger % 2 == 1) animTrigger++ }
    )

    Icon(
        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        contentDescription = if (isLiked) "Unlike" else "Like",
        tint = tint,
        modifier = modifier
            .size(size)
            .scale(likeScale)
    )
}

/**
 * Persistent visual indicator for active private session.
 * Displayed as a chip at the top of HomeScreen when session is active.
 * Shows remaining time before auto-expiry.
 */
@Composable
fun PrivateSessionIndicator(
    sessionState: com.sukoon.music.domain.model.PlaybackSessionState,
    modifier: Modifier = Modifier
) {
    if (!sessionState.isActive) return

    val remainingMs by remember(sessionState.startedAtMs) {
        derivedStateOf { sessionState.getTimeRemainingMs() }
    }
    val remainingMinutes = (remainingMs / 1000 / 60).toInt()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Private Session Active",
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Private Session Active",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "No listening history recorded • Expires in $remainingMinutes min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Continue Listening Card - Primary action on Home screen
 * Full-width card with album art, track info, and play button
 * Refined spacing (16dp margins), typography hierarchy, and micro-interactions
 */
/**
 * Expert UX Tip: Clean filenames into readable titles.
 * Removes leading numbers, bracketed website tags, and extensions.
 */
private fun String.cleanMetadata(): String {
    return this
        // 1. Remove leading numbers followed by dots/spaces (e.g., "05. ")
        .replace(Regex("""^[\d\s\.\-_]+"""), "")
        // 2. Remove anything inside brackets or parentheses (e.g., "[www.site.com]")
        .replace(Regex("""\s*[\[\(].*?[\]\)]"""), "")
        // 3. Remove common file extensions
        .replace(Regex("""(?i)\.(mp3|wav|flac|m4a|aac)$"""), "")
        .trim()
        .ifEmpty { this } // Fallback to original if regex clears everything
}

@Composable
fun ContinueListeningCard(
    song: com.sukoon.music.domain.model.Song?,
    onPlayClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (song == null) return

    val cardInteractionSource = remember { MutableInteractionSource() }
    val playButtonInteractionSource = remember { MutableInteractionSource() }
    var isCardPressed by remember { mutableStateOf(false) }
    var isPlayButtonPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(cardInteractionSource) {
        cardInteractionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isCardPressed = true
                is PressInteraction.Release -> isCardPressed = false
                is PressInteraction.Cancel -> isCardPressed = false
            }
        }
    }

    LaunchedEffect(playButtonInteractionSource) {
        playButtonInteractionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPlayButtonPressed = true
                is PressInteraction.Release -> isPlayButtonPressed = false
                is PressInteraction.Cancel -> isPlayButtonPressed = false
            }
        }
    }

    // Wrap card + text in a Column so text appears below the card
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenSafeAreaMargin)
    ) {
        // Card with album art and play button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(ContinueListeningCardHeight)
                .scale(if (isCardPressed) 0.98f else 1f)
                .clickable(
                    interactionSource = cardInteractionSource,
                    indication = null,
                    enabled = true,
                    onClick = onClick
                ),
            shape = RoundedCornerShape(ContinueListeningCornerRadius),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 8.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Album artwork (full background)
                SubcomposeAsyncImage(
                    model = song.albumArtUri,
                    contentDescription = "Album art for ${song.title} by ${song.artist}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(ContinueListeningCornerRadius)),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                )

                // Play button positioned at bottom-right
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    IconButton(
                        onClick = {
                            Log.d("ContinueListeningCard", "Play clicked: ${song.title}")
                            onPlayClick()
                            coroutineScope.launch {
                                delay(100)
                                onClick()
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .zIndex(1f)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                            .scale(if (isPlayButtonPressed) 0.95f else 1f),
                        interactionSource = playButtonInteractionSource
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play ${song.title} by ${song.artist}",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // Song info BELOW the card
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = song.title.cleanMetadata(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Recently Played Horizontal Scroll
 * Secondary section with album art items
 */
@Composable
fun RecentlyPlayedScrollSection(
    songs: List<com.sukoon.music.domain.model.Song>,
    onItemClick: (com.sukoon.music.domain.model.Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Section header - consistent with other sections
        Text(
            text = "Recently played",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = RecentlyPlayedHorizontalPadding)
        )

        Spacer(modifier = Modifier.height(SpacingMedium))

        // Horizontal scroll list
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = RecentlyPlayedHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(RecentlyPlayedItemSpacing)
        ) {
            items(songs.size.coerceAtMost(10)) { index ->
                val song = songs[index]

                Column(
                    modifier = Modifier.width(RecentlyPlayedItemSize)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(CardCornerRadius))
                            .clickable(onClick = { onItemClick(song) })
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            SubcomposeAsyncImage(
                                model = song.albumArtUri,
                                contentDescription = "Play ${song.title} by ${song.artist}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    PlaceholderAlbumArt.Placeholder(
                                        seed = PlaceholderAlbumArt.generateSeed(
                                            albumName = song.album,
                                            artistName = song.artist,
                                            songId = song.id
                                        ),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(CardCornerRadius)),
                                        icon = Icons.Default.MusicNote,
                                        iconSize = 48
                                    )
                                },
                                error = {
                                    PlaceholderAlbumArt.Placeholder(
                                        seed = PlaceholderAlbumArt.generateSeed(
                                            albumName = song.album,
                                            artistName = song.artist,
                                            songId = song.id
                                        ),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(CardCornerRadius)),
                                        icon = Icons.Default.MusicNote,
                                        iconSize = 48
                                    )
                                }
                            )

                            // Gradient overlay + play button (always visible like other sections)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                IconButton(
                                    onClick = { onItemClick(song) },
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Text labels below card
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist.ifBlank { "Unknown Artist" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Library Navigation Cards - Tertiary section
 * Songs, Playlists, Albums, Folders
 */
@Composable
fun LibraryNavigationCards(
    onSongsClick: () -> Unit,
    onPlaylistsClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onFoldersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenSafeAreaMargin)
    ) {
        // Section header - consistent with other sections
        Text(
            text = "Your library",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(SpacingMedium))

        // 2x2 grid layout
        Column(
            verticalArrangement = Arrangement.spacedBy(LibraryCardSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LibraryCardSpacing)
            ) {
                LibraryCard(
                    title = "Songs",
                    icon = Icons.Default.MusicNote,
                    onClick = onSongsClick,
                    modifier = Modifier.weight(1f)
                )
                LibraryCard(
                    title = "Playlists",
                    icon = Icons.AutoMirrored.Filled.List,
                    onClick = onPlaylistsClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LibraryCardSpacing)
            ) {
                LibraryCard(
                    title = "Albums",
                    icon = Icons.Default.Album,
                    onClick = onAlbumsClick,
                    modifier = Modifier.weight(1f)
                )
                LibraryCard(
                    title = "Folders",
                    icon = Icons.Default.Folder,
                    onClick = onFoldersClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Individual library navigation card
 */
@Composable
private fun LibraryCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPressed = true
                is PressInteraction.Release -> isPressed = false
                is PressInteraction.Cancel -> isPressed = false
            }
        }
    }

    Card(
        modifier = modifier
            .height(LibraryCardHeight)  // 100dp meets Material 3 48dp minimum touch target
            .scale(if (isPressed) 0.97f else 1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(LibraryCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevationLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Navigate to $title",
                modifier = Modifier.size(24.dp),
                tint = accent().primary
            )

            Text(
                text = title,
                style = MaterialTheme.typography.cardTitle,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RedesignedTopBarPreview() {
    SukoonMusicPlayerTheme {
        RedesignedTopBar(
            onPremiumClick = {},
            onGlobalSearchClick = {},
            onSettingsClick = {},
            sessionState = com.sukoon.music.domain.model.PlaybackSessionState()
        )
    }
}
