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
val SpacingXSmall = 4.dp    // Tight padding (chips, cards)
val SpacingMicro = 2.dp     // Borders, thin dividers
val SpacingTiny = 1.dp      // Hairline separators

// Card dimensions
val CardCornerRadius = 12.dp

// Tiered elevation system for Material 3
val CardElevationLow = 2.dp      // Stats cards, informational content
val CardElevationMedium = 4.dp   // Featured content, album cards, important UI

// Section spacing for HomeTab (refined to 24dp for generous, calm aesthetic)
val SectionSpacing = 24.dp       // Vertical gap between major sections

// Screen layout refinements
val ScreenSafeAreaMargin = 16.dp     // Margin from edges (safe area)
val ScreenSafeAreaBottom = 4.dp      // Bottom safe area for gesture zones

// Continue Listening card
val ContinueListeningCardHeight = 300.dp
val ContinueListeningCornerRadius = 16.dp

// Recently Played grid
val RecentlyPlayedItemSize = 140.dp
val RecentlyPlayedItemSpacing = 12.dp
val RecentlyPlayedHorizontalPadding = 16.dp

// Library navigation cards
val LibraryCardWidth = 160.dp
val LibraryCardHeight = 100.dp
val LibraryCardSpacing = 12.dp
val LibraryCardCornerRadius = 12.dp

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

// Accessibility & Safe Areas
val MinimumTouchTargetSize = 48.dp      // Material 3 recommended minimum touch target
val MinimumTouchSpacing = 8.dp          // Space between touch targets

// Safe area insets (system bars handled by WindowInsets, but for reference/fallback)
val SafeAreaTopDefault = 24.dp          // Status bar typical height (varies by device)
val SafeAreaBottomGestureBar = 32.dp    // Gesture bar/navigation bar typical height
val SafeAreaSideNotch = 16.dp           // Typical notch width on sides

// Content padding within safe areas
val ContentTopPadding = 8.dp            // Additional padding below status bar/notch
val ContentBottomPadding = 16.dp        // Additional padding above gesture bar

// Accessibility focus indicators
val FocusIndicatorThickness = 3.dp
val FocusIndicatorPadding = 2.dp

// Shapes
val CardShape = RoundedCornerShape(CardCornerRadius)
val PillShape = RoundedCornerShape(TabPillCornerRadius)
val ActionButtonShape = RoundedCornerShape(ActionButtonCornerRadius)
val MiniPlayerShape = RoundedCornerShape(MiniPlayerCornerRadius)

// Compact button/chip shape (8dp radius for smaller UI elements)
val CompactButtonCornerRadius = 8.dp
val CompactButtonShape = RoundedCornerShape(CompactButtonCornerRadius)
