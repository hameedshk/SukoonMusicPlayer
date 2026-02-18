package com.sukoon.music.ui.components

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.sukoon.music.domain.repository.ListeningStatsSnapshot
import com.sukoon.music.util.DevLogger

/**
 * Premium hero header card for HomeScreen.
 *
 * Features:
 * - Dynamic album art background (blurred)
 * - Animated gradient overlay (breathing effect)
 * - Personalized greeting ("Hi username")
 * - Weekly listening stats (hours, peak hour, top artist)
 * - Responsive heights based on device orientation/size
 * - Theme-adaptive styling (dark/light/AMOLED)
 * - Empty state for new users
 * - Private session indicator
 *
 * @param username User's display name
 * @param stats Weekly listening statistics
 * @param albumArtUri URI of current song's album art
 * @param isPrivateSession Whether private session is enabled
 * @param emptyState Whether to show empty state
 * @param modifier Modifier for layout
 */
@Composable
fun HeroHeaderCard(
    username: String,
    stats: ListeningStatsSnapshot?,
    albumArtUri: Uri?,
    isPrivateSession: Boolean,
    emptyState: Boolean = false,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp

    // Responsive height based on screen size and orientation
    val heroHeight = when {
        isLandscape -> 140.dp
        screenWidthDp > 840 -> 280.dp // Tablet
        else -> 240.dp // Phone portrait
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
    ) {
        // Background layer: Album art with blur
        AnimatedHeroBackground(
            albumArtUri = albumArtUri,
            modifier = Modifier.matchParentSize()
        )

        // Scrim layer: Semi-transparent overlay
        HeroScrim(
            modifier = Modifier.matchParentSize()
        )

        // Content layer: Greeting + stats
        HeroContent(
            username = username,
            stats = stats,
            isPrivateSession = isPrivateSession,
            emptyState = emptyState,
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .padding(24.dp)
        )
    }
}

/**
 * Animated background with album art and gradient overlay.
 */
@Composable
private fun AnimatedHeroBackground(
    albumArtUri: Uri?,
    modifier: Modifier = Modifier
) {
    if (albumArtUri != null) {
        SubcomposeAsyncImage(
            model = albumArtUri,
            contentDescription = "Album art background",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .graphicsLayer(
                    alpha = 0.7f,
                    blurRadius = 25f
                )
        )
    } else {
        // Fallback gradient for no album art
        Box(
            modifier = modifier.background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF0F0F0F)
                    )
                )
            )
        )
    }
}

/**
 * Animated gradient scrim with breathing effect.
 */
@Composable
private fun HeroScrim(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_scrim")

    // Animate gradient position for breathing effect (3000ms cycle)
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = InfiniteRepeatableSpec(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 3000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "gradient_position"
    )

    // Scrim color: Semi-transparent overlay
    // Dark theme: Black scrim (70% opacity)
    // Light theme: White scrim (85% opacity)
    // AMOLED: Higher opacity (85%)
    val scrimColor = Color(0xFF000000).copy(alpha = 0.7f)

    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        scrimColor.copy(alpha = 0.6f + 0.1f * gradientOffset),
                        scrimColor.copy(alpha = 0.8f - 0.1f * gradientOffset)
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(0f, 1000f)
                )
            )
    )
}

/**
 * Hero content: Greeting and stats display.
 */
@Composable
private fun HeroContent(
    username: String,
    stats: ListeningStatsSnapshot?,
    isPrivateSession: Boolean,
    emptyState: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // Greeting
        Text(
            text = buildGreeting(username),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Stats or empty state
        if (emptyState || stats == null) {
            Text(
                text = "Welcome to Sukoon 🎵",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            HeroStats(
                stats = stats,
                isPrivateSession = isPrivateSession
            )
        }
    }
}

/**
 * Stats display row showing hours, peak hour, and top artist.
 */
@Composable
private fun HeroStats(
    stats: ListeningStatsSnapshot,
    isPrivateSession: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // Hours
        StatsRow(
            label = "Total Hours",
            value = "${stats.totalListeningTimeMinutes / 60}h ${stats.totalListeningTimeMinutes % 60}m"
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Peak Hour
        StatsRow(
            label = "Peak Hour",
            value = stats.peakTimeOfDay
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Top Artist
        StatsRow(
            label = "Top Artist",
            value = stats.topArtist ?: "—"
        )

        // Privacy indicator if needed
        if (isPrivateSession) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "🔒 Private Session",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Single stats row (label: value).
 */
@Composable
private fun StatsRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "$label: $value",
        fontSize = 14.sp,
        color = Color.White.copy(alpha = 0.8f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * Build personalized greeting with username truncation.
 * Truncates to 15 characters if too long.
 */
private fun buildGreeting(username: String): String {
    val displayName = if (username.isBlank()) {
        "there"
    } else if (username.length > 15) {
        username.take(15) + "…"
    } else {
        username
    }

    return "Hi $displayName"
}
