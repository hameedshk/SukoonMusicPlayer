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

    /**
     * Get albums to rediscover - played before but not in the last 30 days.
     * Returns album names sorted by oldest last played (ASC) for true rediscovery.
     */
    @Query("""
        SELECT albumName FROM recently_played_albums
        WHERE lastPlayedAt < :thirtyDaysAgo
        ORDER BY lastPlayedAt ASC
        LIMIT 20
    """)
    fun getRediscoverAlbumNames(thirtyDaysAgo: Long = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000): Flow<List<String>>

    /**
     * Get all album names that have been played at least once.
     * Used to identify never-played albums for fallback.
     */
    @Query("SELECT albumName FROM recently_played_albums")
    fun getAllPlayedAlbumNames(): Flow<List<String>>
}
