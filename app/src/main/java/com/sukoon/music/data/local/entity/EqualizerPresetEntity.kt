package com.sukoon.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing custom equalizer presets.
 * Built-in presets are hardcoded in EqualizerPreset companion object.
 *
 * @property id Auto-generated unique ID
 * @property name User-defined preset name
 * @property band60Hz Level for 60Hz band in millibels
 * @property band230Hz Level for 230Hz band in millibels
 * @property band910Hz Level for 910Hz band in millibels
 * @property band3600Hz Level for 3.6kHz band in millibels
 * @property band14000Hz Level for 14kHz band in millibels
 * @property bassBoost Bass boost strength (0-1000)
 * @property virtualizerStrength Virtualizer strength (0-1000)
 * @property createdAt Timestamp when preset was created
 */
@Entity(tableName = "equalizer_presets")
data class EqualizerPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    // 5-band equalizer levels (in millibels)
    val band60Hz: Int,
    val band230Hz: Int,
    val band910Hz: Int,
    val band3600Hz: Int,
    val band14000Hz: Int,

    // Effects
    val bassBoost: Int,
    val virtualizerStrength: Int,

    val createdAt: Long = System.currentTimeMillis()
)
