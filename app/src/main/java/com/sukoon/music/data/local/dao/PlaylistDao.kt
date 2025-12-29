package com.sukoon.music.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sukoon.music.data.local.entity.PlaylistEntity
import com.sukoon.music.data.local.entity.PlaylistSongCrossRef
import com.sukoon.music.data.local.entity.PlaylistWithSongs
import com.sukoon.music.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Playlist operations.
 *
 * Provides CRUD operations for:
 * - Playlists (create, read, update, delete)
 * - Playlist-Song relationships (add/remove songs from playlists)
 * - Advanced queries (song counts, existence checks, position updates)
 *
 * Uses Flow for reactive queries and suspend functions for single operations.
 */
@Dao
interface PlaylistDao {

    // ============================================
    // PLAYLIST CRUD OPERATIONS
    // ============================================

    /**
     * Get all playlists ordered by creation date (newest first).
     * Returns a reactive Flow that emits whenever playlists are added/removed/updated.
     */
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    /**
     * Get a specific playlist by ID.
     * Returns null if playlist doesn't exist.
     */
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

    /**
     * Insert a new playlist or replace if exists.
     * Returns the row ID of the inserted playlist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    /**
     * Update an existing playlist.
     */
    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    /**
     * Delete a playlist.
     * CASCADE constraint automatically removes all playlist-song associations.
     */
    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    /**
     * Delete a playlist by ID.
     * CASCADE constraint automatically removes all playlist-song associations.
     */
    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)

    // ============================================
    // PLAYLIST WITH SONGS (JOIN QUERIES)
    // ============================================

    /**
     * Get a playlist with all its songs.
     * Uses @Transaction to ensure the playlist and songs are loaded atomically.
     */
    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistWithSongs(playlistId: Long): PlaylistWithSongs?

    /**
     * Get all playlists with their songs.
     * Reactive Flow that updates when playlists or songs change.
     */
    @Transaction
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylistsWithSongs(): Flow<List<PlaylistWithSongs>>

    // ============================================
    // PLAYLIST-SONG MANAGEMENT
    // ============================================

    /**
     * Add a song to a playlist.
     * Uses IGNORE conflict strategy to prevent adding the same song twice.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    /**
     * Remove a song from a playlist.
     */
    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    /**
     * Get the number of songs in a playlist.
     */
    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun getPlaylistSongCount(playlistId: Long): Int

    /**
     * Get all songs in a playlist ordered by position (manual order) and then by added date.
     * Returns a reactive Flow that updates when songs are added/removed/reordered.
     */
    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_song_cross_ref ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC, ps.addedAt DESC
    """)
    fun getSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>>

    /**
     * Check if a song exists in a playlist.
     * Returns true if the song is in the playlist, false otherwise.
     */
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM playlist_song_cross_ref
            WHERE playlistId = :playlistId AND songId = :songId
        )
    """)
    suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Boolean

    // ============================================
    // ADVANCED OPERATIONS
    // ============================================

    /**
     * Update the position of a song within a playlist.
     * Used for drag-and-drop reordering.
     */
    @Query("""
        UPDATE playlist_song_cross_ref
        SET position = :newPosition
        WHERE playlistId = :playlistId AND songId = :songId
    """)
    suspend fun updateSongPosition(playlistId: Long, songId: Long, newPosition: Int)
}
