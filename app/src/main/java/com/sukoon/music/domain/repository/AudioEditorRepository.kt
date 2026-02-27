package com.sukoon.music.domain.repository

import com.sukoon.music.domain.model.SongAudioSettings

/**
 * Repository interface for per-song audio settings.
 * Provides access to per-song EQ, effects, pitch, speed, and trim settings.
 */
interface AudioEditorRepository {
    /**
     * Retrieve audio settings for a specific song.
     *
     * @param songId Song ID
     * @return Audio settings if exists, null otherwise
     */
    suspend fun getSettings(songId: Long): SongAudioSettings?

    /**
     * Save or update audio settings for a song.
     *
     * @param settings Audio settings to save
     */
    suspend fun saveSettings(settings: SongAudioSettings)

    /**
     * Delete audio settings for a song.
     *
     * @param songId Song ID
     */
    suspend fun deleteSettings(songId: Long)

    /**
     * Delete all audio settings.
     */
    suspend fun deleteAll()
}
