package com.sukoon.music.data.repository

import com.sukoon.music.data.local.dao.GenreCoverDao
import com.sukoon.music.data.local.dao.PlaylistDao
import com.sukoon.music.data.local.dao.RecentlyPlayedAlbumDao
import com.sukoon.music.data.local.dao.RecentlyPlayedArtistDao
import com.sukoon.music.data.local.dao.RecentlyPlayedDao
import com.sukoon.music.data.local.dao.SongDao
import com.sukoon.music.data.local.entity.GenreCoverEntity
import com.sukoon.music.data.local.entity.PlaylistSongCrossRef
import com.sukoon.music.data.local.entity.RecentlyPlayedAlbumEntity
import com.sukoon.music.data.local.entity.RecentlyPlayedArtistEntity
import com.sukoon.music.data.local.entity.RecentlyPlayedEntity
import com.sukoon.music.data.local.entity.SongEntity
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.data.source.MediaStoreScanner
import com.sukoon.music.di.ApplicationScope
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.domain.model.FolderItem
import com.sukoon.music.domain.model.FolderSortMode
import com.sukoon.music.domain.model.Genre
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
import kotlinx.coroutines.flow.first
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
    private val recentlyPlayedArtistDao: RecentlyPlayedArtistDao,
    private val recentlyPlayedAlbumDao: RecentlyPlayedAlbumDao,
    private val playlistDao: PlaylistDao,
    private val genreCoverDao: GenreCoverDao,
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
                    // Remove songs from DB that no longer exist in MediaStore
                    val scannedSongIds = songs.map { it.id }
                    songDao.deleteSongsNotIn(scannedSongIds)
                } else {
                    // If scan found no songs, clear the database
                    songDao.deleteAllSongs()
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
        return combine(
            recentlyPlayedDao.getRecentlyPlayed(),
            getAllSongs()
        ) { history, allSongs ->
            val songMap = allSongs.associateBy { it.id }
            history.mapNotNull { songMap[it.songId] }
        }
    }

    override suspend fun logSongPlay(songId: Long) {
        withContext(Dispatchers.IO) {
            recentlyPlayedDao.logPlay(songId, System.currentTimeMillis())
            // Also log album play for rediscover feature
            songDao.getSongById(songId)?.let { song ->
                recentlyPlayedAlbumDao.logAlbumPlay(
                    RecentlyPlayedAlbumEntity(song.album, System.currentTimeMillis())
                )
            }
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

    override fun getRecentlyPlayedArtists(): Flow<List<Artist>> {
        return combine(
            recentlyPlayedArtistDao.getRecentlyPlayedArtists(),
            songDao.getAllSongs()
        ) { history, allSongs ->
            val artistGroups = allSongs.groupBy { it.artist }
            history.mapNotNull { record ->
                artistGroups[record.artistName]?.let { songs ->
                    createArtistFromSongs(record.artistName, songs)
                }
            }
        }
    }

    override suspend fun logArtistPlay(artistName: String) {
        withContext(Dispatchers.IO) {
            recentlyPlayedArtistDao.logArtistPlay(
                RecentlyPlayedArtistEntity(artistName, System.currentTimeMillis())
            )
        }
    }

    override fun getAllGenres(): Flow<List<Genre>> {
        return songDao.getAllSongs().map { entities ->
            entities
                .groupBy { it.genre }
                .map { (genreName, songs) -> createGenreFromSongs(genreName, songs) }
                .sortedBy { it.name.lowercase() }
        }
    }

    override fun getGenreById(genreId: Long): Flow<Genre?> {
        return songDao.getAllSongs().map { entities ->
            entities
                .groupBy { it.genre }
                .map { (genreName, songs) -> createGenreFromSongs(genreName, songs) }
                .find { it.id == genreId }
        }
    }

    override fun getGenreByName(genreName: String): Flow<Genre?> {
        return songDao.getAllSongs().map { entities ->
            entities
                .groupBy { it.genre }
                .map { (name, songs) -> createGenreFromSongs(name, songs) }
                .find { it.name.equals(genreName, ignoreCase = true) }
        }
    }

    override fun getSongsByGenreId(genreId: Long): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            val genres = entities.groupBy { it.genre }
            val targetGenreName = genres.keys.find { Genre.generateId(it) == genreId }

            if (targetGenreName != null) {
                entities
                    .filter { it.genre == targetGenreName }
                    .map { it.toSong() }
                    .sortedBy { it.title.lowercase() }
            } else {
                emptyList()
            }
        }
    }

    override suspend fun updateGenreTags(oldGenre: String, newGenre: String) {
        withContext(Dispatchers.IO) {
            val songs = songDao.getAllSongs().first()
            val songsToUpdate = songs.filter { it.genre.equals(oldGenre, ignoreCase = true) }
            songsToUpdate.forEach { song ->
                songDao.updateSong(song.copy(genre = newGenre))
            }
        }
    }

    override suspend fun setGenreCover(genreId: Long, artworkUri: String) {
        withContext(Dispatchers.IO) {
            genreCoverDao.upsert(GenreCoverEntity(genreId, artworkUri))
        }
    }

    override suspend fun getGenreCover(genreId: Long): String? {
        return withContext(Dispatchers.IO) {
            genreCoverDao.getByGenreId(genreId)?.customArtworkUri
        }
    }

    override suspend fun removeGenreCover(genreId: Long) {
        withContext(Dispatchers.IO) {
            genreCoverDao.delete(genreId)
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
                .filter { it.songCount > 0 }  // Exclude empty folders

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

    override fun getSubfoldersAndSongsByPath(folderPath: String): Flow<List<FolderItem>> {
        return combine(
            songDao.getAllSongs(),
            preferencesManager.userPreferencesFlow
        ) { entities, preferences ->
            val normalizedPath = folderPath.trimEnd('/')

            // Get all folder paths
            val allPaths = entities
                .filter { !it.folderPath.isNullOrEmpty() }
                .map { it.folderPath!! }
                .distinct()

            // Find immediate subfolders (one level down)
            val subfolders = allPaths
                .filter { path ->
                    val parent = java.io.File(path).parent ?: return@filter false
                    // Match parent path AND ensure we're not showing storage root folders
                    parent == normalizedPath && !isStorageRootPath(parent)
                }
                .map { subPath ->
                    val songsInSubfolder = entities.filter { it.folderPath == subPath }
                    FolderItem.FolderType(createFolderFromSongs(subPath, songsInSubfolder))
                }

            // Get songs directly in this folder
            val songs = entities
                .filter { it.folderPath == normalizedPath }
                .map { FolderItem.SongType(it.toSong()) }

            // Combine: folders first, then songs
            subfolders + songs
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
            path = folderPath ?: "",
            playCount = playCount,
            year = year,
            size = size,
            albumArtUri = albumArtUri,
            dateAdded = dateAdded,
            isLiked = isLiked,
            genre = genre
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

    private fun createGenreFromSongs(genreName: String, songs: List<SongEntity>): Genre {
        return Genre(
            id = Genre.generateId(genreName),
            name = genreName,
            songCount = songs.size,
            totalDuration = songs.sumOf { it.duration },
            artworkUri = songs.firstOrNull()?.albumArtUri,
            songIds = songs.map { it.id }
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

    /**
     * Check if a path is a storage root that should not be navigable.
     * Prevents showing /storage, /storage/emulated, /storage/emulated/0, etc.
     */
    private fun isStorageRootPath(path: String): Boolean {
        val normalizedPath = path.trimEnd('/')
        // Storage root patterns to exclude
        return normalizedPath.isEmpty() ||
                normalizedPath == "/storage" ||
                normalizedPath == "/storage/emulated" ||
                normalizedPath.matches(Regex("^/storage/emulated/\\d+$"))
    }

    // --- Smart Playlists ---

    override fun getLastAddedSongs(): Flow<List<Song>> = getAllSongs().map { it.take(50) }

    override fun getMostPlayedSongs(): Flow<List<Song>> {
        return combine(
            recentlyPlayedDao.getMostPlayed(),
            getAllSongs()
        ) { history, allSongs ->
            val songMap = allSongs.associateBy { it.id }
            history.mapNotNull { songMap[it.songId] }
        }
    }

    override fun getLikedSongsCount(): Flow<Int> = songDao.getLikedSongsCount()

    override fun getRecentlyPlayedCount(): Flow<Int> = recentlyPlayedDao.getRecentlyPlayedCount()

    override fun getMostPlayedCount(): Flow<Int> = recentlyPlayedDao.getMostPlayedCount()

    override fun getRediscoverAlbums(): Flow<List<Album>> {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        return combine(
            recentlyPlayedAlbumDao.getRediscoverAlbumNames(thirtyDaysAgo),
            recentlyPlayedAlbumDao.getAllPlayedAlbumNames(),
            getAllAlbums()
        ) { rediscoverNames, allPlayedNames, allAlbums ->
            // Tier 1: Albums played before but not in last 30 days (sorted by oldest)
            val rediscoverList = rediscoverNames.toList()
            val rediscovered = rediscoverList.mapNotNull { name ->
                allAlbums.find { it.title == name }
            }.take(10)

            if (rediscovered.isNotEmpty()) {
                return@combine rediscovered
            }

            // Tier 2: Albums never played (not in play history)
            val playedSet = allPlayedNames.toSet()
            val neverPlayed = allAlbums
                .filter { it.title !in playedSet }
                .take(10)

            if (neverPlayed.isNotEmpty()) {
                return@combine neverPlayed
            }

            // Tier 3: Random albums as final fallback
            allAlbums.shuffled().take(10)
        }
    }
}
