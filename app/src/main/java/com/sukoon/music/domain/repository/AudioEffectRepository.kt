package com.sukoon.music.domain.repository

import com.sukoon.music.domain.model.EqualizerPreset
import com.sukoon.music.domain.model.EqualizerSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for audio effects and equalizer management.
 * Provides access to EQ settings (DataStore) and custom presets (Room DB).
 *
 * Following the architecture pattern established by other repositories.
 */
interface AudioEffectRepository {

    // --- Settings (DataStore) ---

    /**
     * Observe current equalizer settings.
     * Updates reactively when user changes EQ.
     */
    val equalizerSettings: Flow<EqualizerSettings>

    /**
     * Update equalizer settings.
     * Settings are persisted to DataStore and applied to AudioEffectManager.
     *
     * @param settings New equalizer settings
     */
    suspend fun updateEqualizerSettings(settings: EqualizerSettings)

    /**
     * Enable or disable all audio effects.
     *
     * @param enabled True to enable, false to disable
     */
    suspend fun setEffectsEnabled(enabled: Boolean)

    /**
     * Apply a preset to equalizer.
     * Updates settings to match the preset's band levels and effects.
     *
     * @param preset The preset to apply
     */
    suspend fun applyPreset(preset: EqualizerPreset)

    // --- Custom Presets (Room DB) ---

    /**
     * Get all custom presets.
     * Built-in presets are not stored in DB (use EqualizerPreset.BUILT_IN_PRESETS).
     *
     * @return Flow of custom presets
     */
    fun getCustomPresets(): Flow<List<EqualizerPreset>>

    /**
     * Get all presets (built-in + custom).
     * Combines hardcoded built-in presets with custom presets from DB.
     *
     * @return Flow of all presets
     */
    fun getAllPresets(): Flow<List<EqualizerPreset>>

    /**
     * Save current settings as a new custom preset.
     *
     * @param name Display name for the preset
     * @return ID of the newly created preset
     */
    suspend fun saveCurrentAsPreset(name: String): Long

    /**
     * Delete a custom preset.
     * Cannot delete built-in presets (id < 0).
     *
     * @param presetId ID of the preset to delete
     */
    suspend fun deletePreset(presetId: Long)

    /**
     * Get a specific preset by ID.
     * Searches both built-in and custom presets.
     *
     * @param presetId Preset ID (negative = built-in, positive = custom)
     * @return Preset if found, null otherwise
     */
    suspend fun getPresetById(presetId: Long): EqualizerPreset?
}
