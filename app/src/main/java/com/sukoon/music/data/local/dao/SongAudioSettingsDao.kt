package com.sukoon.music.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sukoon.music.data.local.entity.SongAudioSettingsEntity

/**
 * Data Access Object for per-song audio settings.
 */
@Dao
interface SongAudioSettingsDao {
    /**
     * Retrieve audio settings for a specific song.
     *
     * @param songId Song ID
     * @return Audio settings if exists, null otherwise
     */
    @Query("SELECT * FROM song_audio_settings WHERE songId = :songId")
    suspend fun getSettings(songId: Long): SongAudioSettingsEntity?

    /**
     * Save or update audio settings for a song (upsert operation).
     *
     * @param settings Audio settings entity to save
     */
    @Upsert
    suspend fun upsert(settings: SongAudioSettingsEntity)

    /**
     * Delete audio settings for a song.
     *
     * @param songId Song ID
     */
    @Query("DELETE FROM song_audio_settings WHERE songId = :songId")
    suspend fun delete(songId: Long)

    /**
     * Delete all audio settings (used when clearing data).
     */
    @Query("DELETE FROM song_audio_settings")
    suspend fun deleteAll()
}
