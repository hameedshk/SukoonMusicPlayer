package com.sukoon.music.domain.model

/**
 * Domain model for per-song audio effects and playback settings.
 * Maps to SongAudioSettingsEntity for database persistence.
 */
data class SongAudioSettings(
    val songId: Long,
    val isEnabled: Boolean = true,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = -1L,
    val eqEnabled: Boolean = false,
    val band60Hz: Int = 0,
    val band230Hz: Int = 0,
    val band910Hz: Int = 0,
    val band3600Hz: Int = 0,
    val band14000Hz: Int = 0,
    val bassBoost: Int = 0,
    val virtualizerStrength: Int = 0,
    val reverbPreset: Short = 0,
    val pitch: Float = 1.0f,
    val speed: Float = 1.0f,
    val updatedAt: Long = 0L
)
