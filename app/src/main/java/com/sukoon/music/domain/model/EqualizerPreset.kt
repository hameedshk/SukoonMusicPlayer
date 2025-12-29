package com.sukoon.music.domain.model

/**
 * Represents an equalizer preset.
 * Can be a built-in preset (Rock, Pop, etc.) or a custom user-created preset.
 *
 * @property id Unique identifier for the preset
 * @property name Display name (e.g., "Rock", "Pop", "My Custom EQ")
 * @property bandLevels Array of 5 band levels in millibels (mB)
 * @property bassBoost Bass boost strength (0-1000, where 1000 = max)
 * @property virtualizerStrength Virtualizer strength (0-1000, where 1000 = max)
 * @property isBuiltIn True for built-in presets (cannot be deleted), false for custom
 */
data class EqualizerPreset(
    val id: Long = 0,
    val name: String,
    val bandLevels: List<Int>, // 5 bands: [60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz]
    val bassBoost: Int = 0,
    val virtualizerStrength: Int = 0,
    val isBuiltIn: Boolean = false
) {
    companion object {
        /**
         * Built-in preset definitions.
         * Band levels are in millibels (mB), range: -1500 to +1500
         * 0 = flat/neutral, positive = boost, negative = cut
         */

        val FLAT = EqualizerPreset(
            id = -1,
            name = "Flat",
            bandLevels = listOf(0, 0, 0, 0, 0),
            bassBoost = 0,
            virtualizerStrength = 0,
            isBuiltIn = true
        )

        val ROCK = EqualizerPreset(
            id = -2,
            name = "Rock",
            bandLevels = listOf(500, 300, -300, 0, 500),
            bassBoost = 300,
            virtualizerStrength = 200,
            isBuiltIn = true
        )

        val POP = EqualizerPreset(
            id = -3,
            name = "Pop",
            bandLevels = listOf(-100, 300, 500, 300, -100),
            bassBoost = 200,
            virtualizerStrength = 300,
            isBuiltIn = true
        )

        val JAZZ = EqualizerPreset(
            id = -4,
            name = "Jazz",
            bandLevels = listOf(400, 200, -200, 200, 400),
            bassBoost = 100,
            virtualizerStrength = 500,
            isBuiltIn = true
        )

        val CLASSICAL = EqualizerPreset(
            id = -5,
            name = "Classical",
            bandLevels = listOf(500, 300, -200, 300, 500),
            bassBoost = 0,
            virtualizerStrength = 600,
            isBuiltIn = true
        )

        val BASS_BOOST = EqualizerPreset(
            id = -6,
            name = "Bass Boost",
            bandLevels = listOf(800, 500, 0, 0, 0),
            bassBoost = 800,
            virtualizerStrength = 100,
            isBuiltIn = true
        )

        val TREBLE_BOOST = EqualizerPreset(
            id = -7,
            name = "Treble Boost",
            bandLevels = listOf(0, 0, 0, 500, 800),
            bassBoost = 0,
            virtualizerStrength = 200,
            isBuiltIn = true
        )

        val VOCAL = EqualizerPreset(
            id = -8,
            name = "Vocal",
            bandLevels = listOf(-200, 200, 400, 400, 0),
            bassBoost = 0,
            virtualizerStrength = 400,
            isBuiltIn = true
        )

        /**
         * All built-in presets.
         */
        val BUILT_IN_PRESETS = listOf(
            FLAT, ROCK, POP, JAZZ, CLASSICAL, BASS_BOOST, TREBLE_BOOST, VOCAL
        )
    }
}
