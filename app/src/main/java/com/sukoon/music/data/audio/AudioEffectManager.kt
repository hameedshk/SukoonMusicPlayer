package com.sukoon.music.data.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.sukoon.music.domain.model.EqualizerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages audio effects (Equalizer, Bass Boost, Virtualizer) for ExoPlayer.
 *
 * CRITICAL REQUIREMENTS (from CLAUDE.md):
 * - AudioEffects live in MediaSessionService (same lifecycle as ExoPlayer)
 * - UI never directly accesses AudioEffect objects
 * - Effects attached to ExoPlayer's audio session ID
 * - Effects released when service stops
 *
 * Android AudioEffect API:
 * - Equalizer: 5-band EQ with adjustable frequency bands
 * - BassBoost: Low-frequency enhancement (0-1000 strength)
 * - Virtualizer: Spatial audio effect (0-1000 strength)
 *
 * @param audioSessionId ExoPlayer's audio session ID (from player.audioSessionId)
 */
class AudioEffectManager(private val audioSessionId: Int) {

    private val TAG = "AudioEffectManager"

    // Audio effect objects (nullable because they may not be supported on all devices)
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    // Current settings state
    private val _currentSettings = MutableStateFlow(EqualizerSettings.DEFAULT)
    val currentSettings: StateFlow<EqualizerSettings> = _currentSettings.asStateFlow()

    // Device capabilities
    private val _isEqualizerSupported = MutableStateFlow(false)
    val isEqualizerSupported: StateFlow<Boolean> = _isEqualizerSupported.asStateFlow()

    /**
     * Initialize audio effects.
     * Must be called when ExoPlayer is ready and has valid audio session ID.
     *
     * @return True if effects initialized successfully, false otherwise
     */
    fun initialize(): Boolean {
        return try {
            Log.d(TAG, "Initializing audio effects with session ID: $audioSessionId")

            // Create Equalizer (5-band)
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = false // Start disabled, enabled by user
                _isEqualizerSupported.value = true
                Log.d(TAG, "Equalizer initialized: ${numberOfBands} bands")
            }

            // Create Bass Boost
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = false
                Log.d(TAG, "Bass Boost initialized")
            }

            // Create Virtualizer
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = false
                Log.d(TAG, "Virtualizer initialized")
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize audio effects", e)
            _isEqualizerSupported.value = false
            false
        }
    }

    /**
     * Apply equalizer settings.
     * Updates all effects (EQ bands, bass boost, virtualizer) based on settings.
     *
     * @param settings The equalizer settings to apply
     */
    fun applySettings(settings: EqualizerSettings) {
        try {
            Log.d(TAG, "Applying settings: enabled=${settings.isEnabled}, preset=${settings.currentPresetId}")

            _currentSettings.value = settings

            // Enable/disable all effects
            equalizer?.enabled = settings.isEnabled
            bassBoost?.enabled = settings.isEnabled
            virtualizer?.enabled = settings.isEnabled

            if (settings.isEnabled) {
                // Apply EQ band levels
                applyEqualizerBands(settings.bandLevels)

                // Apply bass boost
                bassBoost?.setStrength(settings.bassBoost.toShort())

                // Apply virtualizer
                virtualizer?.setStrength(settings.virtualizerStrength.toShort())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply settings", e)
        }
    }

    /**
     * Apply equalizer band levels.
     *
     * @param bandLevels List of 5 band levels in millibels (range: -1500 to +1500)
     */
    private fun applyEqualizerBands(bandLevels: List<Int>) {
        equalizer?.let { eq ->
            if (bandLevels.size != 5) {
                Log.w(TAG, "Invalid band levels size: ${bandLevels.size}, expected 5")
                return
            }

            // Android Equalizer uses millibels (mB)
            // Range: getBandLevelRange() typically returns [-1500, +1500]
            bandLevels.forEachIndexed { index, level ->
                try {
                    eq.setBandLevel(index.toShort(), level.toShort())
                    Log.d(TAG, "Set band $index to $level mB")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to set band $index", e)
                }
            }
        }
    }

    /**
     * Get the frequency range for each EQ band.
     * Used for UI display (e.g., "60Hz", "230Hz", etc.)
     *
     * @return List of 5 center frequencies in Hz
     */
    fun getBandFrequencies(): List<Int> {
        return equalizer?.let { eq ->
            (0 until 5).map { band ->
                eq.getCenterFreq(band.toShort()) / 1000 // Convert mHz to Hz
            }
        } ?: listOf(60, 230, 910, 3600, 14000) // Default frequencies
    }

    /**
     * Get the EQ band level range.
     * Typically [-1500, +1500] millibels.
     *
     * @return Pair of (min, max) in millibels
     */
    fun getBandLevelRange(): Pair<Short, Short> {
        return equalizer?.let { eq ->
            val range = eq.bandLevelRange
            Pair(range[0], range[1])
        } ?: Pair(-1500, 1500) // Default range
    }

    /**
     * Enable or disable all audio effects.
     *
     * @param enabled True to enable, false to disable
     */
    fun setEnabled(enabled: Boolean) {
        val newSettings = _currentSettings.value.copy(isEnabled = enabled)
        applySettings(newSettings)
    }

    /**
     * Release all audio effects.
     * MUST be called when ExoPlayer is released (in MediaSessionService.onDestroy).
     */
    fun release() {
        try {
            Log.d(TAG, "Releasing audio effects")
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            equalizer = null
            bassBoost = null
            virtualizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release audio effects", e)
        }
    }
}
