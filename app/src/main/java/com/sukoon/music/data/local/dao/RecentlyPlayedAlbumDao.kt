package com.sukoon.music.data.local.dao

import androidx.room.*
import com.sukoon.music.data.local.entity.RecentlyPlayedAlbumEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing recently played albums.
 */
@Dao
interface RecentlyPlayedAlbumDao {

    /**
     * Get the most recently played albums.
     * Limited to 8 items as per requirements.
     */
    @Query("SELECT * FROM recently_played_albums ORDER BY lastPlayedAt DESC LIMIT 8")
    fun getRecentlyPlayedAlbums(): Flow<List<RecentlyPlayedAlbumEntity>>

    /**
     * Log an album play.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logAlbumPlay(entity: RecentlyPlayedAlbumEntity)

    /**
     * Clear all recently played album history.
     */
    @Query("DELETE FROM recently_played_albums")
    suspend fun clearAll()
}
