package com.sukoon.music.ui.screen.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Shared theme colors for settings components.
 * Ensures consistency across pie chart, legend, and other visualizations.
 */
object StorageChartTheme {
    /**
     * Music segment color (green).
     */
    @Composable
    fun musicColor(): Color = Color(0xFF4CAF50)

    /**
     * Cache segment color (amber).
     */
    @Composable
    fun cacheColor(): Color = Color(0xFFFFC107)

    /**
     * Database segment color (blue).
     */
    @Composable
    fun databaseColor(): Color = Color(0xFF2196F3)

    /**
     * Empty state color (gray).
     */
    @Composable
    fun emptyColor(): Color = MaterialTheme.colorScheme.surfaceVariant
}
