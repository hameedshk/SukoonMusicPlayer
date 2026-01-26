package com.sukoon.music.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Immutable accent profile definitions.
 * These are the only allowed accent configurations.
 */
sealed class AccentProfile(
    val id: String,
    val label: String,
    val accentPrimary: Color,
    val accentActive: Color,
    val accentOnDark: Color,
    val accentSoftBg: Color
) {
    companion object {
        val ALL = listOf(Teal, SteelBlue, SoftCyan)
        val DEFAULT = Teal

        fun fromId(id: String): AccentProfile = ALL.find { it.id == id } ?: DEFAULT
    }

    object Teal : AccentProfile(
        id = "teal",
        label = "Teal",
        accentPrimary = Color(0xFF2FA4A9),
        accentActive = Color(0xFF238A8E),
        accentOnDark = Color(0xFF4FC3C7),
        accentSoftBg = Color(0xFFE6F5F5)
    )

    object SteelBlue : AccentProfile(
        id = "steel_blue",
        label = "Steel Blue",
        accentPrimary = Color(0xFF4A90E2),
        accentActive = Color(0xFF3A74B8),
        accentOnDark = Color(0xFF6BA8F0),
        accentSoftBg = Color(0xFFEAF2FD)
    )

    object SoftCyan : AccentProfile(
        id = "soft_cyan",
        label = "Soft Cyan",
        accentPrimary = Color(0xFF38BDF8),
        accentActive = Color(0xFF2594C6),
        accentOnDark = Color(0xFF5CCEFF),
        accentSoftBg = Color(0xFFEAF7FD)
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
