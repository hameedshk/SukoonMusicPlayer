package com.sukoon.music.data.local.dao

import androidx.room.*
import com.sukoon.music.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isLiked = 1 ORDER BY dateAdded DESC")
    fun getLikedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: Long): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET isLiked = :isLiked WHERE id = :songId")
    suspend fun updateLikeStatus(songId: Long, isLiked: Boolean)

    @Delete
    suspend fun deleteSong(song: SongEntity)

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()

    @Query("SELECT COUNT(*) FROM songs WHERE isLiked = 1")
    fun getLikedSongsCount(): Flow<Int>
}
