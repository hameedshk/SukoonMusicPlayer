package com.sukoon.music.domain.repository

import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.domain.model.ScanState
import com.sukoon.music.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for song data operations.
 * Provides access to local song library and MediaStore scanning.
 */
interface SongRepository {

    /**
     * Reactive state of the MediaStore scanning operation.
     */
    val scanState: StateFlow<ScanState>

    /**
     * Observe all songs from the local database.
     */
    fun getAllSongs(): Flow<List<Song>>

    /**
     * Observe all liked songs from the local database.
     */
    fun getLikedSongs(): Flow<List<Song>>

    /**
     * Get a specific song by its ID.
     */
    suspend fun getSongById(songId: Long): Song?

    /**
     * Scan MediaStore for local audio files and save to database.
     */
    suspend fun scanLocalMusic(): Boolean

    /**
     * Update the like status of a song.
     */
    suspend fun updateLikeStatus(songId: Long, isLiked: Boolean)

    /**
     * Delete all songs from the database.
     */
    suspend fun clearDatabase()

    /**
     * Get recently played songs.
     */
    fun getRecentlyPlayed(): Flow<List<Song>>

    /**
     * Log a song play to the recently played history.
     */
    suspend fun logSongPlay(songId: Long)

    /**
     * Clear all recently played history.
     */
    suspend fun clearRecentlyPlayed()

    /**
     * Get all albums by grouping songs.
     */
    fun getAllAlbums(): Flow<List<Album>>

    /**
     * Get a specific album by its ID.
     */
    fun getAlbumById(albumId: Long): Flow<Album?>

    /**
     * Get all songs in a specific album.
     */
    fun getSongsByAlbumId(albumId: Long): Flow<List<Song>>

    /**
     * Get all artists by grouping songs.
     */
    fun getAllArtists(): Flow<List<Artist>>

    /**
     * Get a specific artist by ID.
     */
    fun getArtistById(artistId: Long): Flow<Artist?>

    /**
     * Get all songs by a specific artist.
     */
    fun getSongsByArtistId(artistId: Long): Flow<List<Song>>

    /**
     * Get all albums by a specific artist.
     */
    fun getAlbumsByArtistId(artistId: Long): Flow<List<Album>>

    // ============================================
    // FOLDER METHODS
    // ============================================

    /**
     * Get all folders by grouping songs.
     */
    fun getAllFolders(): Flow<List<com.sukoon.music.domain.model.Folder>>

    /**
     * Get folders that are currently excluded from the library.
     */
    fun getExcludedFolders(): Flow<List<com.sukoon.music.domain.model.Folder>>

    /**
     * Get a specific folder by its ID.
     */
    fun getFolderById(folderId: Long): Flow<com.sukoon.music.domain.model.Folder?>

    /**
     * Get all songs in a specific folder.
     */
    fun getSongsByFolderId(folderId: Long): Flow<List<Song>>

    // ============================================
    // PLAYLIST METHODS
    // ============================================

    /**
     * Add a song to a playlist.
     */
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)

    // ============================================
    // SMART PLAYLIST METHODS
    // ============================================

    fun getLastAddedSongs(): Flow<List<Song>>
    fun getMostPlayedSongs(): Flow<List<Song>>
    fun getLikedSongsCount(): Flow<Int>
    fun getRecentlyPlayedCount(): Flow<Int>
    fun getMostPlayedCount(): Flow<Int>
}
