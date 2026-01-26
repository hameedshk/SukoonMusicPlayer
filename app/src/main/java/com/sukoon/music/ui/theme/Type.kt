package com.sukoon.music.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sukoon.music.ui.theme.*

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Semantic typography variants for common use cases.
 * These consolidate repeated .copy() overrides into named, reusable styles.
 */

// Song titles in NowPlayingScreen and similar contexts
val Typography.songTitleLarge: TextStyle
    get() = bodyLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

// Stat labels and secondary information
val Typography.statLabel: TextStyle
    get() = labelMedium.copy(fontWeight = FontWeight.Medium)

// Compact labels for restricted spaces (alphabet scrollbars, tight UI)
// Note: Minimum 11sp for accessibility compliance
val Typography.compactLabel: TextStyle
    get() = labelSmall

// ============================================================================
// Dialog & Modal Typography
// ============================================================================

// Dialog title text (headlines in alert dialogs, modal titles)
// Usage: GradientAlertDialog title parameter
val Typography.dialogTitle: TextStyle
    get() = headlineSmall  // 24sp, Bold

// Dialog body text (main content inside dialogs)
// Usage: Text content, descriptions, explanations in modal dialogs
val Typography.dialogBody: TextStyle
    get() = bodyMedium

// Dialog captions and helper text (small text below dialog content)
// Usage: Hints, notes, or additional context in dialogs
val Typography.dialogCaption: TextStyle
    get() = bodySmall

// ============================================================================
// Card & Grid Item Typography
// ============================================================================

// Card title (used in album cards, artist cards, playlist cards)
// Usage: SmartPlaylistCard, PlaylistCard, AlbumCard, ArtistCard titles
val Typography.cardTitle: TextStyle
    get() = titleMedium.copy(fontWeight = FontWeight.Bold)

// Card subtitle (artist name under song title, album under artist)
// Usage: Secondary info on cards
val Typography.cardSubtitle: TextStyle
    get() = bodySmall

// Compact card title (for smaller/grid card layouts)
// Usage: Album grid titles in artist detail, compact list cards
val Typography.compactCardTitle: TextStyle
    get() = titleSmall.copy(fontWeight = FontWeight.Bold)

// ============================================================================
// List Item Typography
// ============================================================================

// List item primary text (song/album/artist names in lists)
// Usage: HomeScreen song list, SearchScreen results, PlaylistDetailScreen songs
val Typography.listItemTitle: TextStyle
    get() = bodyLarge.copy(fontWeight = FontWeight.Medium)

// List item secondary text (artist name under song, duration, metadata)
// Usage: Secondary info in song/album list items
val Typography.listItemSubtitle: TextStyle
    get() = bodySmall

// ============================================================================
// Section & Header Typography
// ============================================================================

// Section header (category dividers: "Recently Played", "Playlists", etc.)
// Usage: HomeScreen sections, SettingsScreen groups, SearchScreen categories
val Typography.sectionHeader: TextStyle
    get() = titleSmall.copy(fontWeight = FontWeight.Bold)

// Screen/page header (main title at top of screen)
// Usage: HomeScreen header, AlbumsScreen title, SearchScreen results header
val Typography.screenHeader: TextStyle
    get() = headlineSmall  // 24sp, Bold

// ============================================================================
// Empty State Typography
// ============================================================================

// Empty state title (when list/search has no results)
// Usage: SearchScreen empty state, empty playlist message
val Typography.emptyStateTitle: TextStyle
    get() = headlineSmall

// Empty state description (helper text for empty states)
// Usage: "No songs found", "Create your first playlist"
val Typography.emptyStateDescription: TextStyle
    get() = bodyMedium

// ============================================================================
// Button & Action Typography
// ============================================================================

// Button text (action buttons, chips, toggles)
// Usage: Play/pause buttons, action chips, navigation buttons
val Typography.buttonText: TextStyle
    get() = labelLarge  // 14sp, Medium weight

// ============================================================================
// Data & Metadata Typography
// ============================================================================

// Duration and timestamp text (song length, date added, play count)
// Usage: Next to song durations, timestamps
val Typography.duration: TextStyle
    get() = bodySmall

// Tag/badge text (genre tags, quality indicators)
// Usage: Genre labels, quality badges on songs
val Typography.tag: TextStyle
    get() = labelSmall

// Alphabet scrollbar labels (A-Z index labels)
// Usage: Alphabet scrollbar in song lists (HomeScreen, SongsScreen)
// Note: Smaller than labelSmall for tight alphabet bar spacing
val Typography.alphabetLabel: TextStyle
    get() = labelSmall.copy(fontSize = 10.sp)
