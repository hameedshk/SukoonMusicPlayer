package com.sukoon.music.domain.repository

import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.domain.model.Genre
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
     * Reactive flag indicating if a scan is currently in progress.
     * Used to prevent concurrent scans and provide UI feedback.
     */
    val isScanning: StateFlow<Boolean>

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
     * Delete a specific song and notify MediaStore.
     */
    suspend fun deleteSong(songId: Long)

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

    /**
     * Get recently played artists.
     */
    fun getRecentlyPlayedArtists(): Flow<List<Artist>>

    /**
     * Log an artist play to the recently played history.
     */
    suspend fun logArtistPlay(artistName: String)

    /**
     * Get all genres by grouping songs.
     */
    fun getAllGenres(): Flow<List<Genre>>

    /**
     * Get a specific genre by ID.
     */
    fun getGenreById(genreId: Long): Flow<Genre?>

    /**
     * Get a specific genre by name.
     */
    fun getGenreByName(genreName: String): Flow<Genre?>

    /**
     * Get all songs in a specific genre.
     */
    fun getSongsByGenreId(genreId: Long): Flow<List<Song>>

    /**
     * Update genre name for all songs with the old genre.
     */
    suspend fun updateGenreTags(oldGenre: String, newGenre: String)

    /**
     * Set custom artwork for a genre.
     */
    suspend fun setGenreCover(genreId: Long, artworkUri: String)

    /**
     * Get custom artwork for a genre.
     */
    suspend fun getGenreCover(genreId: Long): String?

    /**
     * Remove custom artwork for a genre.
     */
    suspend fun removeGenreCover(genreId: Long)

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

    /**
     * Get subfolders and songs in a specific folder by path.
     * Returns a flow of FolderItem list (mixed folders and songs).
     */
    fun getSubfoldersAndSongsByPath(folderPath: String): Flow<List<com.sukoon.music.domain.model.FolderItem>>

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

    /**
     * Get albums to rediscover - albums played before but not in the last 30 days.
     */
    fun getRediscoverAlbums(): Flow<List<Album>>
}
