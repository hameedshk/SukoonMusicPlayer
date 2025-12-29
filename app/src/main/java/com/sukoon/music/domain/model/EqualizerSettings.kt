package com.sukoon.music.domain.model

/**
 * Current equalizer settings applied to playback.
 * Stored in DataStore for persistence across app sessions.
 *
 * @property isEnabled Master enable/disable switch for all audio effects
 * @property currentPresetId ID of currently active preset (-1 = Flat, -2 = Rock, etc., positive = custom)
 * @property bandLevels Current 5-band equalizer levels in millibels
 * @property bassBoost Current bass boost strength (0-1000)
 * @property virtualizerStrength Current virtualizer strength (0-1000)
 */
data class EqualizerSettings(
    val isEnabled: Boolean = false,
    val currentPresetId: Long = -1, // -1 = Flat (default)
    val bandLevels: List<Int> = listOf(0, 0, 0, 0, 0),
    val bassBoost: Int = 0,
    val virtualizerStrength: Int = 0
) {
    companion object {
        /**
         * Default settings (Flat preset, effects disabled).
         */
        val DEFAULT = EqualizerSettings(
            isEnabled = false,
            currentPresetId = -1,
            bandLevels = listOf(0, 0, 0, 0, 0),
            bassBoost = 0,
            virtualizerStrength = 0
        )
    }
}
