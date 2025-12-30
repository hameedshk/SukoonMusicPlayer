package com.sukoon.music.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Internal color constants used only for theme definitions.
 * DO NOT use these directly in UI code.
 * Always access colors via MaterialTheme.colorScheme.* instead.
 */

// Legacy Material colors (deprecated - will be removed)
@Deprecated("Use MaterialTheme.colorScheme instead", ReplaceWith("MaterialTheme.colorScheme"))
internal val Purple80 = Color(0xFFD0BCFF)
@Deprecated("Use MaterialTheme.colorScheme instead", ReplaceWith("MaterialTheme.colorScheme"))
internal val PurpleGrey80 = Color(0xFFCCC2DC)
@Deprecated("Use MaterialTheme.colorScheme instead", ReplaceWith("MaterialTheme.colorScheme"))
internal val Pink80 = Color(0xFFEFB8C8)
@Deprecated("Use MaterialTheme.colorScheme instead", ReplaceWith("MaterialTheme.colorScheme"))
internal val Purple40 = Color(0xFF6650a4)
@Deprecated("Use MaterialTheme.colorScheme instead", ReplaceWith("MaterialTheme.colorScheme"))
internal val PurpleGrey40 = Color(0xFF625b71)
@Deprecated("Use MaterialTheme.colorScheme instead", ReplaceWith("MaterialTheme.colorScheme"))
internal val Pink40 = Color(0xFF7D5260)

// Brand color constants (internal - used only in Theme.kt)
@Deprecated("Use MaterialTheme.colorScheme.primary instead", ReplaceWith("MaterialTheme.colorScheme.primary"))
internal val SukoonOrange = Color(0xFFDD4411)
@Deprecated("Use MaterialTheme.colorScheme.primary instead", ReplaceWith("MaterialTheme.colorScheme.primary"))
internal val SukoonOrangeLight = Color(0xFFFF5722)
@Deprecated("Use MaterialTheme.colorScheme.primaryContainer instead", ReplaceWith("MaterialTheme.colorScheme.primaryContainer"))
internal val SukoonOrangeDark = Color(0xFFBB3311)

// Surface color constants (internal - used only in Theme.kt)
@Deprecated("Use MaterialTheme.colorScheme.surface instead", ReplaceWith("MaterialTheme.colorScheme.surface"))
internal val DarkCard = Color(0xFF1E1E1E)
@Deprecated("Use MaterialTheme.colorScheme.surfaceContainer instead", ReplaceWith("MaterialTheme.colorScheme.surfaceContainer"))
internal val DarkCardElevated = Color(0xFF2A2A2A)
@Deprecated("Use MaterialTheme.colorScheme.background instead", ReplaceWith("MaterialTheme.colorScheme.background"))
internal val DarkSurface = Color(0xFF121212)
@Deprecated("Use MaterialTheme.colorScheme.surfaceVariant instead", ReplaceWith("MaterialTheme.colorScheme.surfaceVariant"))
internal val DarkSurfaceVariant = Color(0xFF1C1C1C)

// Text color constants (internal - used only in Theme.kt)
@Deprecated("Use MaterialTheme.colorScheme.onSurface instead", ReplaceWith("MaterialTheme.colorScheme.onSurface"))
internal val TextPrimary = Color(0xFFFFFFFF)
@Deprecated("Use MaterialTheme.colorScheme.onSurfaceVariant instead", ReplaceWith("MaterialTheme.colorScheme.onSurfaceVariant"))
internal val TextSecondary = Color(0xFFB3B3B3)
@Deprecated("Use MaterialTheme.colorScheme.outline instead", ReplaceWith("MaterialTheme.colorScheme.outline"))
internal val TextTertiary = Color(0xFF808080)