package com.sukoon.music.data.repository

import com.sukoon.music.data.local.dao.EqualizerPresetDao
import com.sukoon.music.data.local.entity.EqualizerPresetEntity
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.domain.model.EqualizerPreset
import com.sukoon.music.domain.model.EqualizerSettings
import com.sukoon.music.domain.repository.AudioEffectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AudioEffectRepository.
 *
 * Data Sources:
 * - EQ Settings: DataStore via PreferencesManager (reactive, persisted)
 * - Custom Presets: Room DB via EqualizerPresetDao
 * - Built-in Presets: Hardcoded in EqualizerPreset.BUILT_IN_PRESETS
 */
@Singleton
class AudioEffectRepositoryImpl @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val equalizerPresetDao: EqualizerPresetDao
) : AudioEffectRepository {

    // --- Settings (DataStore) ---

    override val equalizerSettings: Flow<EqualizerSettings> =
        preferencesManager.equalizerSettingsFlow

    override suspend fun updateEqualizerSettings(settings: EqualizerSettings) {
        preferencesManager.updateEqualizerSettings(settings)
    }

    override suspend fun setEffectsEnabled(enabled: Boolean) {
        preferencesManager.setEqualizerEnabled(enabled)
    }

    override suspend fun applyPreset(preset: EqualizerPreset) {
        val newSettings = EqualizerSettings(
            isEnabled = true, // Applying preset enables EQ
            currentPresetId = preset.id,
            bandLevels = preset.bandLevels,
            bassBoost = preset.bassBoost,
            virtualizerStrength = preset.virtualizerStrength
        )
        updateEqualizerSettings(newSettings)
    }

    // --- Custom Presets (Room DB) ---

    override fun getCustomPresets(): Flow<List<EqualizerPreset>> {
        return equalizerPresetDao.getAllPresets().map { entities ->
            entities.map { it.toEqualizerPreset() }
        }
    }

    override fun getAllPresets(): Flow<List<EqualizerPreset>> {
        // Combine built-in presets with custom presets from DB
        return getCustomPresets().map { customPresets ->
            EqualizerPreset.BUILT_IN_PRESETS + customPresets
        }
    }

    override suspend fun saveCurrentAsPreset(name: String): Long {
        return withContext(Dispatchers.IO) {
            // Get current settings from Flow (first emission)
            val settings = equalizerSettings.first()

            val entity = EqualizerPresetEntity(
                name = name,
                band60Hz = settings.bandLevels.getOrElse(0) { 0 },
                band230Hz = settings.bandLevels.getOrElse(1) { 0 },
                band910Hz = settings.bandLevels.getOrElse(2) { 0 },
                band3600Hz = settings.bandLevels.getOrElse(3) { 0 },
                band14000Hz = settings.bandLevels.getOrElse(4) { 0 },
                bassBoost = settings.bassBoost,
                virtualizerStrength = settings.virtualizerStrength
            )

            equalizerPresetDao.insertPreset(entity)
        }
    }

    override suspend fun deletePreset(presetId: Long) {
        if (presetId < 0) {
            // Cannot delete built-in presets
            return
        }
        withContext(Dispatchers.IO) {
            equalizerPresetDao.deletePresetById(presetId)
        }
    }

    override suspend fun getPresetById(presetId: Long): EqualizerPreset? {
        return if (presetId < 0) {
            // Built-in preset
            EqualizerPreset.BUILT_IN_PRESETS.find { it.id == presetId }
        } else {
            // Custom preset from DB
            withContext(Dispatchers.IO) {
                equalizerPresetDao.getPresetById(presetId)?.toEqualizerPreset()
            }
        }
    }

    // --- Mappers ---

    /**
     * Convert EqualizerPresetEntity (data layer) to EqualizerPreset (domain layer).
     */
    private fun EqualizerPresetEntity.toEqualizerPreset(): EqualizerPreset {
        return EqualizerPreset(
            id = id,
            name = name,
            bandLevels = listOf(band60Hz, band230Hz, band910Hz, band3600Hz, band14000Hz),
            bassBoost = bassBoost,
            virtualizerStrength = virtualizerStrength,
            isBuiltIn = false
        )
    }
}
