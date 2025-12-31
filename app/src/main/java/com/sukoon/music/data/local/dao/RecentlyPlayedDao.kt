package com.sukoon.music.data.local.dao

import androidx.room.*
import com.sukoon.music.data.local.entity.RecentlyPlayedEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing recently played songs.
 */
@Dao
interface RecentlyPlayedDao {

    /**
     * Get the most recently played songs, sorted by last played timestamp.
     * Limited to 30 songs for recently played section.
     *
     * @return Flow of recently played song IDs
     */
    @Query("""
        SELECT * FROM recently_played
        ORDER BY lastPlayedAt DESC
        LIMIT 30
    """)
    fun getRecentlyPlayed(): Flow<List<RecentlyPlayedEntity>>

    /**
     * Log a song play. If the song already exists, updates the timestamp and increments play count.
     * If it's new, inserts with playCount = 1.
     *
     * @param songId The song ID to log
     */
    @Query("""
        INSERT OR REPLACE INTO recently_played (songId, lastPlayedAt, playCount)
        VALUES (
            :songId,
            :timestamp,
            COALESCE((SELECT playCount FROM recently_played WHERE songId = :songId), 0) + 1
        )
    """)
    suspend fun logPlay(songId: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * Clear all recently played history.
     * Used when user wants to reset their listening history.
     */
    @Query("DELETE FROM recently_played")
    suspend fun clearAll()

    /**
     * Remove a specific song from recently played.
     *
     * @param songId The song ID to remove
     */
    @Query("DELETE FROM recently_played WHERE songId = :songId")
    suspend fun removeSong(songId: Long)

    /**
     * Get the most played songs, sorted by play count (highest first).
     * Used for "Most Played" smart playlist.
     */
    @Query("""
        SELECT * FROM recently_played
        ORDER BY playCount DESC
        LIMIT 100
    """)
    fun getMostPlayed(): Flow<List<RecentlyPlayedEntity>>

    /**
     * Get count of recently played songs.
     */
    @Query("SELECT COUNT(*) FROM recently_played")
    fun getRecentlyPlayedCount(): Flow<Int>

    /**
     * Get count of most played songs (songs with play count > 0).
     */
    @Query("SELECT COUNT(*) FROM recently_played WHERE playCount > 0")
    fun getMostPlayedCount(): Flow<Int>
}
