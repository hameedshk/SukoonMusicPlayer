package com.sukoon.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores per-song audio effects and playback settings.
 * Non-destructive: Original audio files are never modified.
 * Trim is applied via ExoPlayer ClippingConfiguration during playback.
 *
 * @param songId Song ID (foreign key from SongEntity)
 * @param isEnabled Master toggle: true if per-song settings are active
 * @param trimStartMs Trim start position in milliseconds (0 = no trim)
 * @param trimEndMs Trim end position in milliseconds (-1 = no end clip)
 * @param eqEnabled Enable/disable per-song EQ
 * @param band60Hz EQ band level at 60 Hz (millibels, -1500 to +1500)
 * @param band230Hz EQ band level at 230 Hz
 * @param band910Hz EQ band level at 910 Hz
 * @param band3600Hz EQ band level at 3.6 kHz
 * @param band14000Hz EQ band level at 14 kHz
 * @param bassBoost Bass boost strength (0-1000)
 * @param virtualizerStrength Virtualizer (spatial audio) strength (0-1000)
 * @param reverbPreset Reverb preset ID: 0=None, 1=Room, 2=Hall, 3=Plate, 4=Church (Android PresetReverb)
 * @param pitch Pitch factor (0.5-2.0, 1.0 = normal)
 * @param speed Speed factor (0.5-2.0, 1.0 = normal)
 * @param updatedAt Timestamp of last update
 */
@Entity(tableName = "song_audio_settings")
data class SongAudioSettingsEntity(
    @PrimaryKey val songId: Long,
    val isEnabled: Boolean = true,
    // Trim (non-destructive, applied via ClippingConfiguration)
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = -1L,
    // EQ (millibels, typically -1500 to +1500)
    val eqEnabled: Boolean = false,
    val band60Hz: Int = 0,
    val band230Hz: Int = 0,
    val band910Hz: Int = 0,
    val band3600Hz: Int = 0,
    val band14000Hz: Int = 0,
    // Effects
    val bassBoost: Int = 0,
    val virtualizerStrength: Int = 0,
    val reverbPreset: Short = 0,
    // Playback
    val pitch: Float = 1.0f,
    val speed: Float = 1.0f,
    val updatedAt: Long = 0L
)
