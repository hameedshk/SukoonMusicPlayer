package com.sukoon.music.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sukoon.music.data.local.entity.DeletedPlaylistEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing deleted playlists (trash system).
 */
@Dao
interface DeletedPlaylistDao {

    /**
     * Get all deleted playlists, sorted by deletion date (newest first).
     */
    @Query("SELECT * FROM deleted_playlists ORDER BY deletedAt DESC")
    fun getAllDeletedPlaylists(): Flow<List<DeletedPlaylistEntity>>

    /**
     * Get a specific deleted playlist by its trash ID.
     */
    @Query("SELECT * FROM deleted_playlists WHERE id = :id")
    suspend fun getDeletedPlaylistById(id: Long): DeletedPlaylistEntity?

    /**
     * Insert a deleted playlist into the trash.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedPlaylist(deletedPlaylist: DeletedPlaylistEntity): Long

    /**
     * Permanently delete a playlist from trash.
     */
    @Query("DELETE FROM deleted_playlists WHERE id = :id")
    suspend fun permanentlyDelete(id: Long)

    /**
     * Clear all deleted playlists from trash.
     */
    @Query("DELETE FROM deleted_playlists")
    suspend fun clearTrash()

    /**
     * Delete playlists older than the specified timestamp (for auto-cleanup).
     * Used to automatically remove playlists after 30 days.
     */
    @Query("DELETE FROM deleted_playlists WHERE deletedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    /**
     * Get count of deleted playlists.
     */
    @Query("SELECT COUNT(*) FROM deleted_playlists")
    fun getDeletedPlaylistsCount(): Flow<Int>
}
