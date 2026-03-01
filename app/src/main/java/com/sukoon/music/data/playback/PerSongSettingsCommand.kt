package com.sukoon.music.data.playback

import android.os.Bundle
import com.sukoon.music.domain.model.SongAudioSettings

object PerSongSettingsCommand {
    const val ACTION_APPLY = "com.sukoon.music.action.APPLY_PER_SONG_SETTINGS"

    private const val KEY_SONG_ID = "song_id"
    private const val KEY_IS_ENABLED = "is_enabled"
    private const val KEY_TRIM_START_MS = "trim_start_ms"
    private const val KEY_TRIM_END_MS = "trim_end_ms"
    private const val KEY_EQ_ENABLED = "eq_enabled"
    private const val KEY_BAND_60 = "band_60"
    private const val KEY_BAND_230 = "band_230"
    private const val KEY_BAND_910 = "band_910"
    private const val KEY_BAND_3600 = "band_3600"
    private const val KEY_BAND_14000 = "band_14000"
    private const val KEY_BASS_BOOST = "bass_boost"
    private const val KEY_VIRTUALIZER = "virtualizer_strength"
    private const val KEY_REVERB_PRESET = "reverb_preset"
    private const val KEY_PITCH = "pitch"
    private const val KEY_SPEED = "speed"
    private const val KEY_UPDATED_AT = "updated_at"

    fun toBundle(settings: SongAudioSettings): Bundle {
        return Bundle().apply {
            putLong(KEY_SONG_ID, settings.songId)
            putBoolean(KEY_IS_ENABLED, settings.isEnabled)
            putLong(KEY_TRIM_START_MS, settings.trimStartMs)
            putLong(KEY_TRIM_END_MS, settings.trimEndMs)
            putBoolean(KEY_EQ_ENABLED, settings.eqEnabled)
            putInt(KEY_BAND_60, settings.band60Hz)
            putInt(KEY_BAND_230, settings.band230Hz)
            putInt(KEY_BAND_910, settings.band910Hz)
            putInt(KEY_BAND_3600, settings.band3600Hz)
            putInt(KEY_BAND_14000, settings.band14000Hz)
            putInt(KEY_BASS_BOOST, settings.bassBoost)
            putInt(KEY_VIRTUALIZER, settings.virtualizerStrength)
            putShort(KEY_REVERB_PRESET, settings.reverbPreset)
            putFloat(KEY_PITCH, settings.pitch)
            putFloat(KEY_SPEED, settings.speed)
            putLong(KEY_UPDATED_AT, settings.updatedAt)
        }
    }

    fun fromBundle(bundle: Bundle): SongAudioSettings? {
        if (!bundle.containsKey(KEY_SONG_ID)) return null
        return SongAudioSettings(
            songId = bundle.getLong(KEY_SONG_ID),
            isEnabled = bundle.getBoolean(KEY_IS_ENABLED, true),
            trimStartMs = bundle.getLong(KEY_TRIM_START_MS, 0L),
            trimEndMs = bundle.getLong(KEY_TRIM_END_MS, -1L),
            eqEnabled = bundle.getBoolean(KEY_EQ_ENABLED, false),
            band60Hz = bundle.getInt(KEY_BAND_60, 0),
            band230Hz = bundle.getInt(KEY_BAND_230, 0),
            band910Hz = bundle.getInt(KEY_BAND_910, 0),
            band3600Hz = bundle.getInt(KEY_BAND_3600, 0),
            band14000Hz = bundle.getInt(KEY_BAND_14000, 0),
            bassBoost = bundle.getInt(KEY_BASS_BOOST, 0),
            virtualizerStrength = bundle.getInt(KEY_VIRTUALIZER, 0),
            reverbPreset = bundle.getShort(KEY_REVERB_PRESET, 0),
            pitch = bundle.getFloat(KEY_PITCH, 1.0f),
            speed = bundle.getFloat(KEY_SPEED, 1.0f),
            updatedAt = bundle.getLong(KEY_UPDATED_AT, 0L)
        )
    }
}
