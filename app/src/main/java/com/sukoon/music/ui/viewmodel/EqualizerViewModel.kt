package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.EqualizerPreset
import com.sukoon.music.domain.model.EqualizerSettings
import com.sukoon.music.domain.repository.AudioEffectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Equalizer screen.
 *
 * Responsibilities:
 * - Expose current EQ settings
 * - Manage preset selection
 * - Handle band level adjustments
 * - Save/delete custom presets
 * - Toggle effects on/off
 */
@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val audioEffectRepository: AudioEffectRepository
) : ViewModel() {

    // --- Current Settings ---

    /**
     * Current equalizer settings (reactive).
     * Updates automatically when user changes EQ.
     */
    val equalizerSettings: StateFlow<EqualizerSettings> =
        audioEffectRepository.equalizerSettings
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = EqualizerSettings.DEFAULT
            )

    /**
     * All available presets (built-in + custom).
     */
    val allPresets: StateFlow<List<EqualizerPreset>> =
        audioEffectRepository.getAllPresets()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = EqualizerPreset.BUILT_IN_PRESETS
            )

    // --- UI State ---

    private val _showSavePresetDialog = MutableStateFlow(false)
    val showSavePresetDialog: StateFlow<Boolean> = _showSavePresetDialog.asStateFlow()

    // --- User Actions ---

    /**
     * Toggle equalizer on/off.
     */
    fun toggleEnabled() {
        viewModelScope.launch {
            val currentSettings = equalizerSettings.value
            audioEffectRepository.setEffectsEnabled(!currentSettings.isEnabled)
        }
    }

    /**
     * Apply a preset.
     * Enables EQ and updates all settings to match preset.
     *
     * @param preset The preset to apply
     */
    fun applyPreset(preset: EqualizerPreset) {
        viewModelScope.launch {
            audioEffectRepository.applyPreset(preset)
        }
    }

    /**
     * Update a specific EQ band level.
     *
     * @param bandIndex Band index (0-4)
     * @param level New level in millibels (-1500 to +1500)
     */
    fun updateBandLevel(bandIndex: Int, level: Int) {
        viewModelScope.launch {
            val currentSettings = equalizerSettings.value
            val newBandLevels = currentSettings.bandLevels.toMutableList().apply {
                if (bandIndex in 0..4) {
                    this[bandIndex] = level.coerceIn(-1500, 1500)
                }
            }

            val newSettings = currentSettings.copy(
                bandLevels = newBandLevels,
                currentPresetId = -999 // Custom (not matching any preset)
            )
            audioEffectRepository.updateEqualizerSettings(newSettings)
        }
    }

    /**
     * Update bass boost strength.
     *
     * @param strength Bass boost strength (0-1000)
     */
    fun updateBassBoost(strength: Int) {
        viewModelScope.launch {
            val currentSettings = equalizerSettings.value
            val newSettings = currentSettings.copy(
                bassBoost = strength.coerceIn(0, 1000),
                currentPresetId = -999 // Custom
            )
            audioEffectRepository.updateEqualizerSettings(newSettings)
        }
    }

    /**
     * Update virtualizer strength.
     *
     * @param strength Virtualizer strength (0-1000)
     */
    fun updateVirtualizer(strength: Int) {
        viewModelScope.launch {
            val currentSettings = equalizerSettings.value
            val newSettings = currentSettings.copy(
                virtualizerStrength = strength.coerceIn(0, 1000),
                currentPresetId = -999 // Custom
            )
            audioEffectRepository.updateEqualizerSettings(newSettings)
        }
    }

    /**
     * Show save preset dialog.
     */
    fun showSavePresetDialog() {
        _showSavePresetDialog.value = true
    }

    /**
     * Hide save preset dialog.
     */
    fun hideSavePresetDialog() {
        _showSavePresetDialog.value = false
    }

    /**
     * Save current settings as a new custom preset.
     *
     * @param name Display name for the preset
     */
    fun saveAsPreset(name: String) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isNotBlank()) {
                audioEffectRepository.saveCurrentAsPreset(trimmedName)
                hideSavePresetDialog()
            }
        }
    }

    /**
     * Delete a custom preset.
     * Cannot delete built-in presets (id < 0).
     *
     * @param presetId ID of the preset to delete
     */
    fun deletePreset(presetId: Long) {
        if (presetId < 0) return // Cannot delete built-in presets
        viewModelScope.launch {
            audioEffectRepository.deletePreset(presetId)
        }
    }

    /**
     * Reset EQ to flat (all bands at 0, effects disabled).
     */
    fun resetToFlat() {
        viewModelScope.launch {
            audioEffectRepository.applyPreset(EqualizerPreset.FLAT)
        }
    }
}
