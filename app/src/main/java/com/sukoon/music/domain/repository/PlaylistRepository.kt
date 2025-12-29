package com.sukoon.music.domain.repository

import com.sukoon.music.domain.model.Playlist
import com.sukoon.music.domain.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Playlist operations.
 *
 * Provides a clean API for the domain layer to interact with playlist data,
 * abstracting away the implementation details (Room database).
 *
 * All Flow-based methods are reactive and emit new values when data changes.
 * All suspend functions are for single operations.
 */
interface PlaylistRepository {

    // ============================================
    // PLAYLIST CRUD
    // ============================================

    /**
     * Get all playlists.
     * Returns a Flow that emits whenever playlists are added/removed/updated.
     */
    fun getAllPlaylists(): Flow<List<Playlist>>

    /**
     * Get a single playlist with its metadata.
     * Returns a Flow that emits when the playlist or its song count changes.
     */
    fun getPlaylistWithSongs(playlistId: Long): Flow<Playlist?>

    /**
     * Get a playlist by ID (single snapshot).
     */
    suspend fun getPlaylistById(playlistId: Long): Playlist?

    /**
     * Create a new playlist.
     * Returns the ID of the newly created playlist.
     */
    suspend fun createPlaylist(name: String, description: String? = null): Long

    /**
     * Update an existing playlist's metadata (name, description, cover image).
     */
    suspend fun updatePlaylist(playlist: Playlist)

    /**
     * Delete a playlist.
     * All songs in the playlist are automatically removed (CASCADE).
     */
    suspend fun deletePlaylist(playlistId: Long)

    // ============================================
    // PLAYLIST-SONG MANAGEMENT
    // ============================================

    /**
     * Get all songs in a playlist.
     * Returns a Flow that emits when songs are added/removed/reordered.
     */
    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>>

    /**
     * Add a song to a playlist.
     * If the song is already in the playlist, this operation is ignored.
     */
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)

    /**
     * Remove a song from a playlist.
     */
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    /**
     * Check if a song is in a playlist.
     */
    suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Boolean

    /**
     * Update the position of a song within a playlist.
     * Used for manual drag-and-drop reordering.
     */
    suspend fun updateSongPosition(playlistId: Long, songId: Long, newPosition: Int)

    // ============================================
    // IMPORT/EXPORT OPERATIONS
    // ============================================

    /**
     * Export a playlist to JSON format.
     * Returns JSON string containing playlist metadata and songs.
     */
    suspend fun exportPlaylist(playlistId: Long): String?

    /**
     * Export all playlists to JSON format.
     * Returns JSON string containing all playlists and their songs.
     */
    suspend fun exportAllPlaylists(): String

    /**
     * Import playlists from JSON string.
     * Creates new playlists with the imported data.
     * Matches songs by title, artist, and album.
     *
     * @param json JSON string containing playlist data
     * @return Number of playlists successfully imported
     */
    suspend fun importPlaylists(json: String): Int

    // ============================================
    // TRASH/RESTORE OPERATIONS
    // ============================================

    /**
     * Get all deleted playlists from trash.
     * Returns a Flow that emits whenever deleted playlists change.
     */
    fun getDeletedPlaylists(): Flow<List<com.sukoon.music.domain.model.DeletedPlaylist>>

    /**
     * Get count of deleted playlists in trash.
     */
    fun getDeletedPlaylistsCount(): Flow<Int>

    /**
     * Restore a deleted playlist from trash.
     * Recreates the playlist and matches songs from local library.
     *
     * @param deletedPlaylistId ID of the deleted playlist in trash
     * @return True if restored successfully, false otherwise
     */
    suspend fun restorePlaylist(deletedPlaylistId: Long): Boolean

    /**
     * Permanently delete a playlist from trash.
     * This cannot be undone.
     *
     * @param deletedPlaylistId ID of the deleted playlist in trash
     */
    suspend fun permanentlyDeletePlaylist(deletedPlaylistId: Long)

    /**
     * Clear all deleted playlists from trash.
     */
    suspend fun clearTrash()

    /**
     * Auto-cleanup: Delete playlists older than 30 days from trash.
     */
    suspend fun cleanupOldDeletedPlaylists()
}
