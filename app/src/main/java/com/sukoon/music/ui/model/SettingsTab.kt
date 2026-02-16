package com.sukoon.music.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.sukoon.music.R

/**
 * Settings screen tabs with explicit positions to prevent reordering bugs.
 *
 * IMPORTANT: Use [position] field for tab selection, NOT ordinal.
 * Ordinal can break if enum entries are reordered.
 */
enum class SettingsTab(
    val position: Int,
    val titleRes: Int,
    val icon: ImageVector
) {
    GENERAL(
        position = 0,
        titleRes = R.string.settings_tab_general,
        icon = Icons.Default.Settings
    ),
    PREMIUM(
        position = 1,
        titleRes = R.string.settings_tab_premium,
        icon = Icons.Default.Star
    ),
    AUDIO(
        position = 2,
        titleRes = R.string.settings_tab_audio,
        icon = Icons.Default.Equalizer
    ),
    LIBRARY(
        position = 3,
        titleRes = R.string.settings_tab_library,
        icon = Icons.Default.Folder
    ),
    STORAGE(
        position = 4,
        titleRes = R.string.settings_tab_storage,
        icon = Icons.Default.Storage
    ),
    ABOUT(
        position = 5,
        titleRes = R.string.settings_tab_about,
        icon = Icons.Default.Info
    );

    companion object {
        /**
         * Find tab by position (safe alternative to ordinal).
         */
        fun byPosition(position: Int): SettingsTab? =
            entries.find { it.position == position }

        /**
         * Get first tab (General).
         */
        fun default(): SettingsTab = GENERAL
    }
}
