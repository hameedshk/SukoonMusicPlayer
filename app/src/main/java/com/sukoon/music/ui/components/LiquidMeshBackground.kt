package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.sukoon.music.ui.util.AlbumPalette
import com.sukoon.music.ui.theme.*

@Composable
fun LiquidMeshBackground(
    palette: AlbumPalette,
    songId: Long?,
    modifier: Modifier = Modifier
) {
    // Fixed predefined gradients per design spec (not extracted from album art)
    // Dark theme: #0F1C1E → #0B0F11
    // Light theme: #EAF6F6 → #FFFFFF
    //
    // Theme detection: Use MaterialTheme to determine current theme (respects user preference)
    // Check onBackground color: light text (#B3B3B3) = dark theme, dark text (#1C1B1F) = light theme
    val isDarkTheme = MaterialTheme.colorScheme.onBackground.red > 0.5f

    val gradientBrush = remember(isDarkTheme) {
        Brush.verticalGradient(
            colors = if (isDarkTheme) {
                listOf(
                    Color(0xFF0F1C1E),  // Dark: top
                    Color(0xFF0B0F11)   // Dark: bottom
                )
            } else {
                listOf(
                    Color(0xFFEAF6F6),  // Light: top
                    Color(0xFFFFFFFF)   // Light: bottom
                )
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush)
    )
}
