package com.sukoon.music.data.local.dao

import androidx.room.*
import com.sukoon.music.data.local.entity.RecentlyPlayedArtistEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing recently played artists.
 */
@Dao
interface RecentlyPlayedArtistDao {

    /**
     * Get the most recently played artists.
     * Limited to 8 items as per requirements.
     */
    @Query("SELECT * FROM recently_played_artists ORDER BY lastPlayedAt DESC LIMIT 8")
    fun getRecentlyPlayedArtists(): Flow<List<RecentlyPlayedArtistEntity>>

    /**
     * Log an artist play.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logArtistPlay(entity: RecentlyPlayedArtistEntity)

    /**
     * Clear all recently played artist history.
     */
    @Query("DELETE FROM recently_played_artists")
    suspend fun clearAll()
}
