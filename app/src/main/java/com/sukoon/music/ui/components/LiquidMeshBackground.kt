package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.sukoon.music.ui.util.AlbumPalette

@Composable
fun LiquidMeshBackground(
    palette: AlbumPalette,
    songId: Long?,
    modifier: Modifier = Modifier
) {
    // Extract gradient colors from palette
    val primaryColor = palette.vibrant
    val secondaryColor = palette.vibrantDark

    // Create gradient using palette colors
    val gradientBrush = remember(primaryColor, secondaryColor) {
        Brush.linearGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.95f),
                secondaryColor.copy(alpha = 0.95f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush)
    )
}
