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
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
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

    // Theme-aware colors from Material 3 colorScheme
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
        MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
    )

    // Ensure positive index for color selection
    val index = kotlin.math.abs(genreName.hashCode() % colors.size)
    return colors[index]
}
