package com.sukoon.music.data.repository

import com.sukoon.music.data.local.dao.PlaylistDao
import com.sukoon.music.data.local.dao.RecentlyPlayedDao
import com.sukoon.music.data.local.dao.SongDao
import com.sukoon.music.data.local.entity.PlaylistSongCrossRef
import com.sukoon.music.data.local.entity.SongEntity
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.data.source.MediaStoreScanner
import com.sukoon.music.di.ApplicationScope
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.domain.model.FolderSortMode
import com.sukoon.music.domain.model.ScanState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.model.UserPreferences
import com.sukoon.music.domain.repository.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SongRepository following the architecture established by PlaybackRepositoryImpl.
 */
@Singleton
class SongRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val recentlyPlayedDao: RecentlyPlayedDao,
    private val playlistDao: PlaylistDao,
    private val mediaStoreScanner: MediaStoreScanner,
    private val preferencesManager: PreferencesManager,
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
                if (!mediaStoreScanner.hasAudioPermission()) {
                    _scanState.update {
                        ScanState.Error("Storage permission not granted.")
                    }
                    return@withContext false
                }

                _scanState.update { ScanState.Scanning(scannedCount = 0) }

                val songs = mediaStoreScanner.scanAudioFiles { count, title ->
                    _scanState.update {
                        ScanState.Scanning(scannedCount = count, message = title)
                    }
                }

                if (songs.isNotEmpty()) {
                    songDao.insertSongs(songs)
                }

                _scanState.update {
                    ScanState.Success(totalSongs = songs.size)
                }

                true

            } catch (e: Exception) {
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
            val songIds = recentlyPlayedEntities.map { it.songId }
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
            entities
                .groupBy { it.album }
                .map { (albumName, songs) -> createAlbumFromSongs(albumName, songs) }
                .sortedBy { it.title.lowercase() }
        }
    }

    override fun getAlbumById(albumId: Long): Flow<Album?> {
        return songDao.getAllSongs().map { entities ->
            entities
                .groupBy { it.album }
                .map { (albumName, songs) -> createAlbumFromSongs(albumName, songs) }
                .find { it.id == albumId }
        }
    }

    override fun getSongsByAlbumId(albumId: Long): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            val albums = entities.groupBy { it.album }
            val targetAlbumTitle = albums.keys.find { it.hashCode().toLong() == albumId }
            
            if (targetAlbumTitle != null) {
                entities
                    .filter { it.album == targetAlbumTitle }
                    .map { it.toSong() }
                    .sortedBy { it.title.lowercase() }
            } else {
                emptyList()
            }
        }
    }

    // Artists

    override fun getAllArtists(): Flow<List<Artist>> {
        return songDao.getAllSongs().map { entities ->
            entities
                .groupBy { it.artist }
                .map { (artistName, songs) -> createArtistFromSongs(artistName, songs) }
                .sortedBy { it.name.lowercase() }
        }
    }

    override fun getArtistById(artistId: Long): Flow<Artist?> {
        return songDao.getAllSongs().map { entities ->
            entities
                .groupBy { it.artist }
                .map { (artistName, songs) -> createArtistFromSongs(artistName, songs) }
                .find { it.id == artistId }
        }
    }

    override fun getSongsByArtistId(artistId: Long): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            val artists = entities.groupBy { it.artist }
            val targetArtistName = artists.keys.find { it.hashCode().toLong() == artistId }

            if (targetArtistName != null) {
                entities
                    .filter { it.artist == targetArtistName }
                    .map { it.toSong() }
                    .sortedBy { it.title.lowercase() }
            } else {
                emptyList()
            }
        }
    }

    override fun getAlbumsByArtistId(artistId: Long): Flow<List<Album>> {
        return songDao.getAllSongs().map { entities ->
            val artists = entities.groupBy { it.artist }
            val targetArtistName = artists.keys.find { it.hashCode().toLong() == artistId }

            if (targetArtistName != null) {
                entities
                    .filter { it.artist == targetArtistName }
                    .groupBy { it.album }
                    .map { (albumName, songs) -> createAlbumFromSongs(albumName, songs) }
                    .sortedBy { it.title.lowercase() }
            } else {
                emptyList()
            }
        }
    }

    // ============================================
    // FOLDERS
    // ============================================

    override fun getAllFolders(): Flow<List<com.sukoon.music.domain.model.Folder>> {
        return combine(
            songDao.getAllSongs(),
            preferencesManager.userPreferencesFlow
        ) { entities, preferences ->
            val folders = entities
                .filter { !it.folderPath.isNullOrEmpty() && shouldIncludeSong(it, preferences) }
                .groupBy { it.folderPath!! }
                .map { (folderPath, songs) -> createFolderFromSongs(folderPath, songs) }

            applySorting(folders, preferences.folderSortMode, entities)
        }
    }

    override fun getExcludedFolders(): Flow<List<com.sukoon.music.domain.model.Folder>> {
        return combine(
            songDao.getAllSongs(),
            preferencesManager.userPreferencesFlow
        ) { entities, preferences ->
            val excludedPaths = preferences.excludedFolderPaths
            val folders = entities
                .filter { it.folderPath in excludedPaths }
                .groupBy { it.folderPath!! }
                .map { (folderPath, songs) -> createFolderFromSongs(folderPath, songs) }

            applySorting(folders, preferences.folderSortMode, entities)
        }
    }

    override fun getFolderById(folderId: Long): Flow<com.sukoon.music.domain.model.Folder?> {
        return combine(
            songDao.getAllSongs(),
            preferencesManager.userPreferencesFlow
        ) { entities, preferences ->
            entities
                .filter { !it.folderPath.isNullOrEmpty() && (shouldIncludeSong(it, preferences) || preferences.excludedFolderPaths.contains(it.folderPath)) }
                .groupBy { it.folderPath!! }
                .map { (folderPath, songs) -> createFolderFromSongs(folderPath, songs) }
                .find { it.id == folderId }
        }
    }

    override fun getSongsByFolderId(folderId: Long): Flow<List<Song>> {
        return combine(
            songDao.getAllSongs(),
            preferencesManager.userPreferencesFlow
        ) { entities, preferences ->
            val folders = entities
                .filter { !it.folderPath.isNullOrEmpty() }
                .groupBy { it.folderPath!! }
                .map { (path, _) -> path to path.hashCode().toLong() }

            val targetPath = folders.find { it.second == folderId }?.first

            if (targetPath != null) {
                entities
                    .filter { it.folderPath == targetPath }
                    .map { it.toSong() }
                    .sortedBy { it.title.lowercase() }
            } else {
                emptyList()
            }
        }
    }

    // ============================================
    // PLAYLIST METHODS
    // ============================================

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        withContext(Dispatchers.IO) {
            playlistDao.addSongToPlaylist(
                PlaylistSongCrossRef(
                    playlistId = playlistId,
                    songId = songId,
                    position = playlistDao.getPlaylistSongCount(playlistId)
                )
            )
        }
    }

    // --- Helpers ---

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

    private fun createArtistFromSongs(artistName: String, songs: List<SongEntity>): Artist {
        val albums = songs.groupBy { it.album }
        return Artist(
            id = artistName.hashCode().toLong(),
            name = artistName,
            songCount = songs.size,
            albumCount = albums.size,
            totalDuration = songs.sumOf { it.duration },
            artworkUri = songs.firstOrNull()?.albumArtUri,
            songIds = songs.map { it.id },
            albumIds = albums.keys.map { it.hashCode().toLong() }
        )
    }

    private fun createFolderFromSongs(folderPath: String, songs: List<SongEntity>): com.sukoon.music.domain.model.Folder {
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

    private fun shouldIncludeSong(song: SongEntity, preferences: UserPreferences): Boolean {
        if (preferences.excludedFolderPaths.contains(song.folderPath)) return false
        if (!preferences.showAllAudioFiles && (song.duration < preferences.minimumAudioDuration * 1000)) return false
        return true
    }

    private fun applySorting(
        folders: List<com.sukoon.music.domain.model.Folder>,
        sortMode: FolderSortMode,
        allSongs: List<SongEntity>
    ): List<com.sukoon.music.domain.model.Folder> {
        return when (sortMode) {
            FolderSortMode.NAME_ASC -> folders.sortedBy { it.name.lowercase() }
            FolderSortMode.NAME_DESC -> folders.sortedByDescending { it.name.lowercase() }
            FolderSortMode.TRACK_COUNT -> folders.sortedByDescending { it.songCount }
            FolderSortMode.RECENTLY_MODIFIED -> {
                folders.sortedByDescending { folder ->
                    allSongs.filter { it.folderPath == folder.path }.maxOfOrNull { it.dateAdded } ?: 0L
                }
            }
            FolderSortMode.DURATION -> folders.sortedByDescending { it.totalDuration }
        }
    }

    // --- Smart Playlists ---

    override fun getLastAddedSongs(): Flow<List<Song>> = getAllSongs()
    override fun getMostPlayedSongs(): Flow<List<Song>> = getAllSongs() // Placeholder
    override fun getLikedSongsCount(): Flow<Int> = songDao.getLikedSongsCount()
    override fun getRecentlyPlayedCount(): Flow<Int> = songDao.getLikedSongsCount() // Placeholder
    override fun getMostPlayedCount(): Flow<Int> = songDao.getLikedSongsCount() // Placeholder
}
