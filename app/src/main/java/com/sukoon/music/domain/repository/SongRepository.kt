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
 *
 * Following the architecture pattern established by PlaybackRepository:
 * - Domain layer defines the contract
 * - Data layer provides implementation
 * - StateFlow for reactive state observation
 */
interface SongRepository {

    /**
     * Reactive state of the MediaStore scanning operation.
     * Observe this to track scanning progress in UI.
     */
    val scanState: StateFlow<ScanState>

    /**
     * Observe all songs from the local database.
     * Returns a Flow that updates automatically when data changes.
     */
    fun getAllSongs(): Flow<List<Song>>

    /**
     * Observe all liked songs from the local database.
     * Returns a Flow that updates automatically when data changes.
     */
    fun getLikedSongs(): Flow<List<Song>>

    /**
     * Get a specific song by its ID.
     * @param songId The unique ID of the song
     * @return Song if found, null otherwise
     */
    suspend fun getSongById(songId: Long): Song?

    /**
     * Scan MediaStore for local audio files and save to database.
     * This operation:
     * - Checks READ_MEDIA_AUDIO permission (Android 13+) or READ_EXTERNAL_STORAGE (Android 12-)
     * - Queries MediaStore.Audio.Media for all music files
     * - Extracts metadata (title, artist, album, duration, album art)
     * - Saves to Room database using REPLACE strategy
     * - Updates scanState throughout the process
     *
     * CRITICAL REQUIREMENT:
     * - Must ONLY be called from foreground context
     * - Never invoke from background service or worker
     * - Returns immediately if permissions are denied
     *
     * @return True if scan completed successfully, false if permission denied or error occurred
     */
    suspend fun scanLocalMusic(): Boolean

    /**
     * Update the like status of a song.
     * @param songId The unique ID of the song
     * @param isLiked True to like, false to unlike
     */
    suspend fun updateLikeStatus(songId: Long, isLiked: Boolean)

    /**
     * Delete all songs from the database.
     * Note: This does NOT delete physical audio files, only database metadata.
     */
    suspend fun clearDatabase()

    /**
     * Get recently played songs for the 2x3 grid on Home screen.
     * Returns up to 6 most recently played songs.
     */
    fun getRecentlyPlayed(): Flow<List<Song>>

    /**
     * Log a song play to the recently played history.
     * Should NOT be called when private session is active.
     *
     * @param songId The unique ID of the song
     */
    suspend fun logSongPlay(songId: Long)

    /**
     * Clear all recently played history.
     */
    suspend fun clearRecentlyPlayed()

    /**
     * Get all albums by grouping songs.
     * Albums are created by grouping songs with the same album name.
     * Returns a Flow that updates when songs change.
     */
    fun getAllAlbums(): Flow<List<Album>>

    /**
     * Get a specific album by its ID.
     * @param albumId The unique ID of the album
     * @return Flow emitting the album if found, null otherwise
     */
    fun getAlbumById(albumId: Long): Flow<Album?>

    /**
     * Get all songs in a specific album.
     * @param albumId The unique ID of the album
     * @return Flow emitting list of songs in the album
     */
    fun getSongsByAlbumId(albumId: Long): Flow<List<Song>>

    /**
     * Get all artists by grouping songs.
     * Artists are created by grouping songs with the same artist name.
     * Returns a Flow that updates when songs change.
     */
    fun getAllArtists(): Flow<List<Artist>>

    /**
     * Get a specific artist by ID.
     * @param artistId The unique ID of the artist
     * @return Flow emitting the artist if found, null otherwise
     */
    fun getArtistById(artistId: Long): Flow<Artist?>

    /**
     * Get all songs by a specific artist.
     * @param artistId The unique ID of the artist
     * @return Flow emitting list of songs by this artist
     */
    fun getSongsByArtistId(artistId: Long): Flow<List<Song>>

    /**
     * Get all albums by a specific artist.
     * @param artistId The unique ID of the artist
     * @return Flow emitting list of albums by this artist
     */
    fun getAlbumsByArtistId(artistId: Long): Flow<List<Album>>

    // ============================================
    // FOLDER METHODS
    // ============================================

    /**
     * Get all folders by grouping songs.
     * Folders are created by grouping songs with the same folder path.
     * Returns a Flow that updates when songs change.
     */
    fun getAllFolders(): Flow<List<com.sukoon.music.domain.model.Folder>>

    /**
     * Get a specific folder by its ID.
     * @param folderId The unique ID of the folder
     * @return Flow emitting the folder if found, null otherwise
     */
    fun getFolderById(folderId: Long): Flow<com.sukoon.music.domain.model.Folder?>

    /**
     * Get all songs in a specific folder.
     * @param folderId The unique ID of the folder
     * @return Flow emitting list of songs in the folder
     */
    fun getSongsByFolderId(folderId: Long): Flow<List<Song>>

    // ============================================
    // SMART PLAYLIST METHODS
    // ============================================

    /**
     * Get songs for "Last Added" smart playlist.
     * Returns recently added songs sorted by date added (newest first).
     */
    fun getLastAddedSongs(): Flow<List<Song>>

    /**
     * Get songs for "Most Played" smart playlist.
     * Returns songs sorted by play count (highest first).
     */
    fun getMostPlayedSongs(): Flow<List<Song>>

    /**
     * Get count of liked songs.
     * Used for displaying count in smart playlist card.
     */
    fun getLikedSongsCount(): Flow<Int>

    /**
     * Get count of recently played songs.
     * Used for displaying count in smart playlist card.
     */
    fun getRecentlyPlayedCount(): Flow<Int>

    /**
     * Get count of most played songs.
     * Used for displaying count in smart playlist card.
     */
    fun getMostPlayedCount(): Flow<Int>
}
