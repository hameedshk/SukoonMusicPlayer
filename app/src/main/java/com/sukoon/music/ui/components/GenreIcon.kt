package com.sukoon.music.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sukoon.music.ui.theme.*

/**
 * A circular icon component for music genres with consistent color mapping.
 */
@Composable
fun GenreIcon(
    genreName: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = getGenreColor(genreName)

    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (backgroundColor == MaterialTheme.colorScheme.surfaceVariant)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else Color.White,
                modifier = Modifier.fillMaxSize(0.6f)
            )
        }
    }
}

@Composable
private fun getGenreColor(genreName: String): Color {
    if (genreName.equals("Unknown Genre", ignoreCase = true)) {
        return MaterialTheme.colorScheme.surfaceVariant
    }

    // Predefined colors for variety
    val colors = listOf(
        Color(0xFFEF5350), // Red
        Color(0xFFEC407A), // Pink
        Color(0xFF42A5F5), // Blue
        Color(0xFFAB47BC), // Purple
        Color(0xFF26C6DA), // Cyan
        Color(0xFFFFA726), // Orange
        Color(0xFF8D6E63), // Brown
        Color(0xFF66BB6A), // Green
        Color(0xFF26A69A), // Teal
        Color(0xFF5C6BC0)  // Indigo
    )

    // Ensure positive index for color selection
    val index = kotlin.math.abs(genreName.hashCode() % colors.size)
    return colors[index]
}
