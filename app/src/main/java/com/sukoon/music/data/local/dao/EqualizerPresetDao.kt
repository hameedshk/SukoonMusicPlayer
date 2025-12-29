package com.sukoon.music.data.local.dao

import androidx.room.*
import com.sukoon.music.data.local.entity.EqualizerPresetEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing custom equalizer presets.
 * Built-in presets are not stored in DB.
 */
@Dao
interface EqualizerPresetDao {

    /**
     * Get all custom equalizer presets.
     * Returns Flow for reactive UI updates.
     */
    @Query("SELECT * FROM equalizer_presets ORDER BY name ASC")
    fun getAllPresets(): Flow<List<EqualizerPresetEntity>>

    /**
     * Get a specific preset by ID.
     */
    @Query("SELECT * FROM equalizer_presets WHERE id = :presetId")
    suspend fun getPresetById(presetId: Long): EqualizerPresetEntity?

    /**
     * Insert a new custom preset.
     * Returns the ID of the inserted preset.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: EqualizerPresetEntity): Long

    /**
     * Update an existing preset.
     */
    @Update
    suspend fun updatePreset(preset: EqualizerPresetEntity)

    /**
     * Delete a custom preset.
     */
    @Delete
    suspend fun deletePreset(preset: EqualizerPresetEntity)

    /**
     * Delete a preset by ID.
     */
    @Query("DELETE FROM equalizer_presets WHERE id = :presetId")
    suspend fun deletePresetById(presetId: Long)

    /**
     * Delete all custom presets.
     */
    @Query("DELETE FROM equalizer_presets")
    suspend fun deleteAllPresets()
}
