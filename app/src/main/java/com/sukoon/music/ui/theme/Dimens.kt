package com.sukoon.music.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.sukoon.music.ui.theme.*

/**
 * Design system dimensions and shapes for SukoonMusic redesign.
 */

// Spacing (φ-based Golden Ratio: 4 → 6.5 → 11 → 17.6 → 29 → 47)
val SpacingXSmall = 4.dp      // Base rhythm
val SpacingSmall = 6.5.dp     // 4 × φ (MinimumTouchSpacing for Settings rows)
val SpacingMedium = 11.dp     // 6.5 × φ (UNIFIED ROW PADDING - Audit fix #3)
val SpacingLarge = 17.6.dp    // 11 × φ
val SpacingXLarge = 29.dp     // 17.6 × φ
val SpacingXXLarge = 47.dp    // 29 × φ
val SpacingMicro = 2.dp       // Borders, thin dividers
val SpacingTiny = 1.dp        // Hairline separators

// Card dimensions
val CardCornerRadius = 12.dp

// Tiered elevation system for Material 3
val CardElevationLow = 2.dp      // Stats cards, informational content
val CardElevationMedium = 4.dp   // Featured content, album cards, important UI

// Section spacing for HomeTab (φ-scaled: 24dp → 29dp for harmonic verticality)
val SectionSpacing = 29.dp       // Vertical gap between major sections (17.6 × φ)

// Screen layout refinements (φ-aligned)
val ScreenSafeAreaMargin = 17.6.dp   // Margin from edges (safe area) = SpacingLarge
val ScreenSafeAreaBottom = 4.dp      // Bottom safe area for gesture zones

// Continue Listening card
val ContinueListeningCardHeight = 300.dp
val ContinueListeningCornerRadius = 16.dp

// Recently Played grid (φ-aligned)
val RecentlyPlayedItemSize = 140.dp
val RecentlyPlayedItemSpacing = 11.dp       // SpacingMedium
val RecentlyPlayedHorizontalPadding = 17.6.dp // SpacingLarge

// Library navigation cards (φ-aligned)
val LibraryCardWidth = 160.dp
val LibraryCardHeight = 100.dp
val LibraryCardSpacing = 11.dp              // SpacingMedium
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
val TabPillHeight = 48.dp
val TabPillCornerRadius = 22.dp // Fully rounded (half of height)
val TabPillMaxWidth = 120.dp // Prevents orphaned text on narrow screens (~32% on 360dp device)

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
val MinimumTouchSpacing = 6.5.dp        // SpacingSmall: space between touch targets

// Safe area insets (system bars handled by WindowInsets, but for reference/fallback)
val SafeAreaTopDefault = 24.dp          // Status bar typical height (varies by device)
val SafeAreaBottomGestureBar = 32.dp    // Gesture bar/navigation bar typical height
val SafeAreaSideNotch = 16.dp           // Typical notch width on sides

// Content padding within safe areas (φ-aligned)
val ContentTopPadding = 6.5.dp          // SpacingSmall: padding below status bar/notch
val ContentBottomPadding = 17.6.dp      // SpacingLarge: padding above gesture bar

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
