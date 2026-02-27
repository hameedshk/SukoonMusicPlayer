package com.sukoon.music.data.repository

import com.sukoon.music.data.local.dao.SongAudioSettingsDao
import com.sukoon.music.data.local.entity.SongAudioSettingsEntity
import com.sukoon.music.domain.model.SongAudioSettings
import com.sukoon.music.domain.repository.AudioEditorRepository

/**
 * Implementation of AudioEditorRepository.
 * Handles persistence of per-song audio settings to Room database.
 */
class AudioEditorRepositoryImpl(
    private val songAudioSettingsDao: SongAudioSettingsDao
) : AudioEditorRepository {

    override suspend fun getSettings(songId: Long): SongAudioSettings? {
        return songAudioSettingsDao.getSettings(songId)?.toDomain()
    }

    override suspend fun saveSettings(settings: SongAudioSettings) {
        songAudioSettingsDao.upsert(settings.toEntity())
    }

    override suspend fun deleteSettings(songId: Long) {
        songAudioSettingsDao.delete(songId)
    }

    override suspend fun deleteAll() {
        songAudioSettingsDao.deleteAll()
    }

    private fun SongAudioSettingsEntity.toDomain(): SongAudioSettings {
        return SongAudioSettings(
            songId = songId,
            isEnabled = isEnabled,
            trimStartMs = trimStartMs,
            trimEndMs = trimEndMs,
            eqEnabled = eqEnabled,
            band60Hz = band60Hz,
            band230Hz = band230Hz,
            band910Hz = band910Hz,
            band3600Hz = band3600Hz,
            band14000Hz = band14000Hz,
            bassBoost = bassBoost,
            virtualizerStrength = virtualizerStrength,
            reverbPreset = reverbPreset,
            pitch = pitch,
            speed = speed,
            updatedAt = updatedAt
        )
    }

    private fun SongAudioSettings.toEntity(): SongAudioSettingsEntity {
        return SongAudioSettingsEntity(
            songId = songId,
            isEnabled = isEnabled,
            trimStartMs = trimStartMs,
            trimEndMs = trimEndMs,
            eqEnabled = eqEnabled,
            band60Hz = band60Hz,
            band230Hz = band230Hz,
            band910Hz = band910Hz,
            band3600Hz = band3600Hz,
            band14000Hz = band14000Hz,
            bassBoost = bassBoost,
            virtualizerStrength = virtualizerStrength,
            reverbPreset = reverbPreset,
            pitch = pitch,
            speed = speed,
            updatedAt = updatedAt
        )
    }
}
