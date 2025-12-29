package com.sukoon.music.data.repository

import com.sukoon.music.data.local.dao.RecentlyPlayedDao
import com.sukoon.music.data.local.dao.SongDao
import com.sukoon.music.data.local.entity.SongEntity
import com.sukoon.music.data.source.MediaStoreScanner
import com.sukoon.music.di.ApplicationScope
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.domain.model.ScanState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SongRepository following the architecture established by PlaybackRepositoryImpl.
 *
 * Responsibilities:
 * - Mediate between data sources (Room, MediaStore) and domain layer
 * - Manage scanning state via StateFlow
 * - Transform entities to domain models
 * - Execute operations on appropriate dispatchers
 */
@Singleton
class SongRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val recentlyPlayedDao: RecentlyPlayedDao,
    private val mediaStoreScanner: MediaStoreScanner,
    @ApplicationScope private val scope: CoroutineScope
) : SongRepository {

    // Scan State Management
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    override val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // Song Queries

    override fun getAllSongs(): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            entities.map { it.toSong() }
        }
    }

    override fun getLikedSongs(): Flow<List<Song>> {
        return songDao.getLikedSongs().map { entities ->
            entities.map { it.toSong() }
        }
    }

    override suspend fun getSongById(songId: Long): Song? {
        return withContext(Dispatchers.IO) {
            songDao.getSongById(songId)?.toSong()
        }
    }

    // MediaStore Scanning

    override suspend fun scanLocalMusic(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Check permissions first
                if (!mediaStoreScanner.hasAudioPermission()) {
                    _scanState.update {
                        ScanState.Error("Storage permission not granted. Please enable in Settings.")
                    }
                    return@withContext false
                }

                // Start scanning
                _scanState.update { ScanState.Scanning(scannedCount = 0) }

                // Scan with progress callback
                val songs = mediaStoreScanner.scanAudioFiles { count, title ->
                    _scanState.update {
                        ScanState.Scanning(
                            scannedCount = count,
                            message = title
                        )
                    }
                }

                // Batch insert into database
                // SongDao uses OnConflictStrategy.REPLACE, so existing songs will be updated
                if (songs.isNotEmpty()) {
                    songDao.insertSongs(songs)
                }

                // Update state to success
                _scanState.update {
                    ScanState.Success(totalSongs = songs.size)
                }

                true

            } catch (e: SecurityException) {
                // Permission error
                _scanState.update {
                    ScanState.Error("Permission denied: ${e.message}")
                }
                false
            } catch (e: Exception) {
                // General error
                _scanState.update {
                    ScanState.Error("Scan failed: ${e.message}")
                }
                false
            }
        }
    }

    // Database Mutations

    override suspend fun updateLikeStatus(songId: Long, isLiked: Boolean) {
        withContext(Dispatchers.IO) {
            songDao.updateLikeStatus(songId, isLiked)
        }
    }

    override suspend fun clearDatabase() {
        withContext(Dispatchers.IO) {
            songDao.deleteAllSongs()
        }
    }

    // Recently Played

    override fun getRecentlyPlayed(): Flow<List<Song>> {
        return recentlyPlayedDao.getRecentlyPlayed().map { recentlyPlayedEntities ->
            // Get the song IDs from recently played
            val songIds = recentlyPlayedEntities.map { it.songId }

            // Fetch full song data for those IDs
            songIds.mapNotNull { songId ->
                songDao.getSongById(songId)?.toSong()
            }
        }
    }

    override suspend fun logSongPlay(songId: Long) {
        withContext(Dispatchers.IO) {
            recentlyPlayedDao.logPlay(songId, System.currentTimeMillis())
        }
    }

    override suspend fun clearRecentlyPlayed() {
        withContext(Dispatchers.IO) {
            recentlyPlayedDao.clearAll()
        }
    }

    // Albums

    override fun getAllAlbums(): Flow<List<Album>> {
        return songDao.getAllSongs().map { entities ->
            // Group songs by album name
            entities
                .groupBy { it.album }
                .map { (albumName, songs) ->
                    createAlbumFromSongs(albumName, songs)
                }
                .sortedBy { it.title.lowercase() }
        }
    }

    override fun getAlbumById(albumId: Long): Flow<Album?> {
        return songDao.getAllSongs().map { entities ->
            // Group songs by album and find the one matching albumId
            entities
                .groupBy { it.album }
                .map { (albumName, songs) ->
                    createAlbumFromSongs(albumName, songs)
                }
                .find { it.id == albumId }
        }
    }

    override fun getSongsByAlbumId(albumId: Long): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            // First, find the album by grouping
            val albums = entities
                .groupBy { it.album }
                .map { (albumName, songs) ->
                    albumName to createAlbumFromSongs(albumName, songs)
                }

            // Find the album with matching ID
            val targetAlbum = albums.find { it.second.id == albumId }?.second

            // Return songs that belong to this album
            if (targetAlbum != null) {
                entities
                    .filter { it.album == targetAlbum.title }
                    .map { it.toSong() }
                    .sortedBy { it.title.lowercase() }
            } else {
                emptyList()
            }
        }
    }

    // Mapper: Entity -> Domain Model

    /**
     * Convert SongEntity (data layer) to Song (domain layer).
     * This follows the separation of concerns principle:
     * - Entities represent database schema
     * - Domain models represent business logic
     */
    private fun SongEntity.toSong(): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            uri = uri,
            albumArtUri = albumArtUri,
            dateAdded = dateAdded,
            isLiked = isLiked
        )
    }

    /**
     * Create an Album from a group of SongEntities.
     * Generates a consistent ID based on album name hash.
     */
    private fun createAlbumFromSongs(albumName: String, songs: List<SongEntity>): Album {
        return Album(
            id = albumName.hashCode().toLong(),
            title = albumName,
            artist = songs.firstOrNull()?.artist ?: "Unknown Artist",
            songCount = songs.size,
            totalDuration = songs.sumOf { it.duration },
            albumArtUri = songs.firstOrNull()?.albumArtUri,
            songIds = songs.map { it.id }
        )
    }

    // Artists

    override fun getAllArtists(): Flow<List<Artist>> {
        return songDao.getAllSongs().map { entities ->
            // Group songs by artist name
            entities
                .groupBy { it.artist }
                .map { (artistName, songs) ->
                    createArtistFromSongs(artistName, songs, entities)
                }
                .sortedBy { it.name.lowercase() }
        }
    }

    override fun getArtistById(artistId: Long): Flow<Artist?> {
        return songDao.getAllSongs().map { entities ->
            // Group songs by artist and find the one matching artistId
            entities
                .groupBy { it.artist }
                .map { (artistName, songs) ->
                    createArtistFromSongs(artistName, songs, entities)
                }
                .find { it.id == artistId }
        }
    }

    override fun getSongsByArtistId(artistId: Long): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            // First, find the artist by grouping
            val artists = entities
                .groupBy { it.artist }
                .map { (artistName, songs) ->
                    artistName to createArtistFromSongs(artistName, songs, entities)
                }

            // Find the artist with matching ID
            val targetArtist = artists.find { it.second.id == artistId }?.second

            // Return songs by this artist
            if (targetArtist != null) {
                entities
                    .filter { it.artist == targetArtist.name }
                    .map { it.toSong() }
                    .sortedBy { it.title.lowercase() }
            } else {
                emptyList()
            }
        }
    }

    override fun getAlbumsByArtistId(artistId: Long): Flow<List<Album>> {
        return songDao.getAllSongs().map { entities ->
            // First, find the artist by grouping
            val artists = entities
                .groupBy { it.artist }
                .map { (artistName, songs) ->
                    artistName to createArtistFromSongs(artistName, songs, entities)
                }

            // Find the artist with matching ID
            val targetArtist = artists.find { it.second.id == artistId }?.second

            // Return albums by this artist
            if (targetArtist != null) {
                entities
                    .filter { it.artist == targetArtist.name }
                    .groupBy { it.album }
                    .map { (albumName, songs) ->
                        createAlbumFromSongs(albumName, songs)
                    }
                    .sortedBy { it.title.lowercase() }
            } else {
                emptyList()
            }
        }
    }

    /**
     * Create an Artist from a group of SongEntities.
     * Generates a consistent ID based on artist name hash.
     *
     * @param artistName The artist name
     * @param songs Songs by this artist
     * @param allSongs All songs (needed to calculate album count)
     */
    private fun createArtistFromSongs(
        artistName: String,
        songs: List<SongEntity>,
        allSongs: List<SongEntity>
    ): Artist {
        // Get unique albums by this artist
        val albums = songs.groupBy { it.album }
        val albumIds = albums.map { (albumName, _) -> albumName.hashCode().toLong() }

        return Artist(
            id = artistName.hashCode().toLong(),
            name = artistName,
            songCount = songs.size,
            albumCount = albums.size,
            totalDuration = songs.sumOf { it.duration },
            artworkUri = songs.firstOrNull()?.albumArtUri,
            songIds = songs.map { it.id },
            albumIds = albumIds
        )
    }

    // ============================================
    // FOLDERS
    // ============================================

    override fun getAllFolders(): Flow<List<com.sukoon.music.domain.model.Folder>> {
        return songDao.getAllSongs().map { entities ->
            // Group songs by folder path, filtering out songs without folder paths
            entities
                .filter { !it.folderPath.isNullOrEmpty() }
                .groupBy { it.folderPath!! }
                .map { (folderPath, songs) ->
                    createFolderFromSongs(folderPath, songs)
                }
                .sortedBy { it.name.lowercase() }
        }
    }

    override fun getFolderById(folderId: Long): Flow<com.sukoon.music.domain.model.Folder?> {
        return songDao.getAllSongs().map { entities ->
            // Group songs by folder path and find the one matching folderId
            entities
                .filter { !it.folderPath.isNullOrEmpty() }
                .groupBy { it.folderPath!! }
                .map { (folderPath, songs) ->
                    createFolderFromSongs(folderPath, songs)
                }
                .find { it.id == folderId }
        }
    }

    override fun getSongsByFolderId(folderId: Long): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            // First, find the folder by grouping
            val folders = entities
                .filter { !it.folderPath.isNullOrEmpty() }
                .groupBy { it.folderPath!! }
                .map { (folderPath, songs) ->
                    folderPath to createFolderFromSongs(folderPath, songs)
                }

            // Find the folder with matching ID
            val targetFolder = folders.find { it.second.id == folderId }?.second

            // Return songs in this folder
            if (targetFolder != null) {
                entities
                    .filter { it.folderPath == targetFolder.path }
                    .map { it.toSong() }
                    .sortedBy { it.title.lowercase() }
            } else {
                emptyList()
            }
        }
    }

    /**
     * Create a Folder from a group of SongEntities.
     * Generates a consistent ID based on folder path hash.
     *
     * @param folderPath The full folder path
     * @param songs Songs in this folder
     */
    private fun createFolderFromSongs(
        folderPath: String,
        songs: List<SongEntity>
    ): com.sukoon.music.domain.model.Folder {
        // Extract folder name (last segment of path)
        val folderName = folderPath.substringAfterLast('/').ifEmpty { folderPath }

        return com.sukoon.music.domain.model.Folder(
            id = folderPath.hashCode().toLong(),
            path = folderPath,
            name = folderName,
            songCount = songs.size,
            totalDuration = songs.sumOf { it.duration },
            albumArtUri = songs.firstOrNull()?.albumArtUri,
            songIds = songs.map { it.id }
        )
    }

    // ============================================
    // SMART PLAYLIST METHODS
    // ============================================

    override fun getLastAddedSongs(): Flow<List<Song>> {
        // getAllSongs() already returns songs sorted by dateAdded DESC
        return songDao.getAllSongs().map { entities ->
            entities.map { it.toSong() }
        }
    }

    override fun getMostPlayedSongs(): Flow<List<Song>> {
        return recentlyPlayedDao.getMostPlayed().map { recentlyPlayedEntities ->
            // Get the song IDs sorted by play count
            val songIds = recentlyPlayedEntities.map { it.songId }

            // Fetch full song data for those IDs, preserving the order
            songIds.mapNotNull { songId ->
                songDao.getSongById(songId)?.toSong()
            }
        }
    }

    override fun getLikedSongsCount(): Flow<Int> {
        return songDao.getLikedSongsCount()
    }

    override fun getRecentlyPlayedCount(): Flow<Int> {
        return recentlyPlayedDao.getRecentlyPlayedCount()
    }

    override fun getMostPlayedCount(): Flow<Int> {
        return recentlyPlayedDao.getMostPlayedCount()
    }
}
