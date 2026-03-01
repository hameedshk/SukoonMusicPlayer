package com.sukoon.music.data.local.dao

import androidx.room.*
import com.sukoon.music.data.local.entity.LyricsEntity

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics WHERE trackId = :trackId")
    suspend fun getLyricsByTrackId(trackId: Long): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: LyricsEntity)

    @Query("UPDATE lyrics SET syncOffset = :syncOffset WHERE trackId = :trackId")
    suspend fun updateSyncOffset(trackId: Long, syncOffset: Long)

    @Query("DELETE FROM lyrics WHERE trackId = :trackId")
    suspend fun deleteLyricsByTrackId(trackId: Long)

    @Delete
    suspend fun deleteLyrics(lyrics: LyricsEntity)
}
