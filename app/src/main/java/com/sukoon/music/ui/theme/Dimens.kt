package com.sukoon.music.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.sukoon.music.ui.theme.*

/**
 * Design system dimensions and shapes for SukoonMusic redesign.
 */

// Spacing
val SpacingSmall = 8.dp
val SpacingMedium = 12.dp
val SpacingLarge = 16.dp
val SpacingXLarge = 24.dp
val SpacingXXLarge = 32.dp

// Card dimensions
val CardCornerRadius = 12.dp

// Tiered elevation system for Material 3
val CardElevationLow = 2.dp      // Stats cards, informational content
val CardElevationMedium = 4.dp   // Featured content, album cards, important UI

// Section spacing for HomeTab
val SectionSpacing = 12.dp       // Vertical gap between major sections

@Deprecated(
    message = "Use CardElevationLow or CardElevationMedium for tiered elevation",
    replaceWith = ReplaceWith("CardElevationMedium")
)
val CardElevation = 4.dp

// Action button dimensions
val ActionButtonHeight = 56.dp
val ActionButtonCornerRadius = 12.dp

// Tab pill dimensions
val TabPillHeight = 44.dp
val TabPillCornerRadius = 22.dp // Fully rounded (half of height)

// Widget banner
val WidgetBannerHeight = 56.dp
val WidgetBannerCornerRadius = 8.dp

// Mini player
val MiniPlayerHeight = 64.dp
val MiniPlayerCornerRadius = 12.dp
val MiniPlayerAlbumArtSize = 56.dp

// Last added card
val LastAddedCardWidth = 140.dp
val LastAddedCardHeight = 140.dp

// Shapes
val CardShape = RoundedCornerShape(CardCornerRadius)
val PillShape = RoundedCornerShape(TabPillCornerRadius)
val ActionButtonShape = RoundedCornerShape(ActionButtonCornerRadius)
val MiniPlayerShape = RoundedCornerShape(MiniPlayerCornerRadius)
