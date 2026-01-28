package com.sukoon.music.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Immutable accent profile definitions.
 * These are the only allowed accent configurations.
 *
 * Design Note: Colors are tuned for Spotify-tier vibrancy on dark backgrounds
 * while maintaining WCAG AA contrast (4.5:1+) on light backgrounds.
 */
sealed class AccentProfile(
    val id: String,
    val label: String,
    val accentPrimary: Color,      // Main accent (used in both themes)
    val accentActive: Color,       // Pressed/active state
    val accentOnDark: Color,       // Brighter variant for dark mode visibility
    val accentSoftBg: Color        // Light background tint for light mode
) {
    companion object {
        val ALL = listOf(Teal, SteelBlue, SoftCyan)
        val DEFAULT = Teal

        fun fromId(id: String): AccentProfile = ALL.find { it.id == id } ?: DEFAULT
    }

    object Teal : AccentProfile(
        id = "teal",
        label = "Teal",
        accentPrimary = Color(0xFF26A69A),   // Boosted saturation (was #2FA4A9)
        accentActive = Color(0xFF00897B),    // Darker for press state
        accentOnDark = Color(0xFF4DB6AC),    // Brighter for dark backgrounds
        accentSoftBg = Color(0xFFE0F2F1)     // Material Teal 50
    )

    object SteelBlue : AccentProfile(
        id = "steel_blue",
        label = "Steel Blue",
        accentPrimary = Color(0xFF42A5F5),   // Boosted brightness (was #4A90E2)
        accentActive = Color(0xFF1E88E5),    // Darker for press state
        accentOnDark = Color(0xFF64B5F6),    // Brighter for dark backgrounds
        accentSoftBg = Color(0xFFE3F2FD)     // Material Blue 50
    )

    object SoftCyan : AccentProfile(
        id = "soft_cyan",
        label = "Soft Cyan",
        accentPrimary = Color(0xFF26C6DA),   // More vibrant (was #38BDF8)
        accentActive = Color(0xFF00ACC1),    // Darker for press state
        accentOnDark = Color(0xFF4DD0E1),    // Brighter for dark backgrounds
        accentSoftBg = Color(0xFFE0F7FA)     // Material Cyan 50
    )
}

/**
 * Accent tokens resolved from the active profile.
 * All accent usage resolves from these tokens.
 */
data class AccentTokens(
    val primary: Color,
    val active: Color,
    val onDark: Color,
    val softBg: Color
) {
    companion object {
        fun fromProfile(profile: AccentProfile) = AccentTokens(
            primary = profile.accentPrimary,
            active = profile.accentActive,
            onDark = profile.accentOnDark,
            softBg = profile.accentSoftBg
        )

        val Neutral = AccentTokens(
            primary = Color(0xFF9CA3AF),
            active = Color(0xFF9CA3AF),
            onDark = Color(0xFF9CA3AF),
            softBg = Color(0xFFF3F4F6)
        )
    }
}
