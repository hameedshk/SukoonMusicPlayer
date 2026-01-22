package com.sukoon.music.data.repository

import com.google.gson.Gson
import com.sukoon.music.data.local.dao.DeletedPlaylistDao
import com.sukoon.music.data.local.dao.PlaylistDao
import com.sukoon.music.data.local.dao.SongDao
import com.sukoon.music.data.local.entity.DeletedPlaylistEntity
import com.sukoon.music.data.local.entity.PlaylistEntity
import com.sukoon.music.data.local.entity.PlaylistSongCrossRef
import com.sukoon.music.data.local.entity.SongEntity
import com.sukoon.music.di.ApplicationScope
import com.sukoon.music.domain.model.DeletedPlaylist
import com.sukoon.music.domain.model.Playlist
import com.sukoon.music.domain.model.PlaylistExport
import com.sukoon.music.domain.model.PlaylistExportData
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.model.SongExportData
import com.sukoon.music.domain.repository.PlaylistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PlaylistRepository using Room database.
 */
@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
    private val deletedPlaylistDao: DeletedPlaylistDao,
    @ApplicationScope private val scope: CoroutineScope
) : PlaylistRepository {

    private val gson = Gson()

    // ============================================
    // PLAYLIST CRUD
    // ============================================

    override fun getAllPlaylists(): Flow<List<Playlist>> {
        // Use the optimized query with subquery count for reliable reactive updates
        return playlistDao.getAllPlaylistsWithCount().map { list ->
            list.map { item ->
                item.playlist.toPlaylist(songCount = item.songCount)
            }
        }
    }

    override fun getPlaylistWithSongs(playlistId: Long): Flow<Playlist?> {
        // Use the reactive Flow from DAO that handles the relation
        return playlistDao.getPlaylistWithSongsFlow(playlistId).map { playlistWithSongs ->
            playlistWithSongs?.playlist?.toPlaylist(songCount = playlistWithSongs.songs.size)
        }
    }

    override suspend fun getPlaylistById(playlistId: Long): Playlist? {
        return withContext(Dispatchers.IO) {
            val entity = playlistDao.getPlaylistById(playlistId)
            if (entity != null) {
                val songCount = playlistDao.getPlaylistSongCount(playlistId)
                entity.toPlaylist(songCount = songCount)
            } else {
                null
            }
        }
    }

    override suspend fun createPlaylist(name: String, description: String?): Long {
        return withContext(Dispatchers.IO) {
            val entity = PlaylistEntity(
                name = name,
                description = description,
                coverImageUri = null,
                createdAt = System.currentTimeMillis()
            )
            playlistDao.insertPlaylist(entity)
        }
    }

    override suspend fun updatePlaylist(playlist: Playlist) {
        withContext(Dispatchers.IO) {
            val entity = PlaylistEntity(
                id = playlist.id,
                name = playlist.name,
                description = playlist.description,
                coverImageUri = playlist.coverImageUri,
                createdAt = playlist.createdAt
            )
            playlistDao.updatePlaylist(entity)
        }
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        withContext(Dispatchers.IO) {
            val playlistJson = exportPlaylist(playlistId)
            if (playlistJson != null) {
                val playlist = playlistDao.getPlaylistById(playlistId)
                if (playlist != null) {
                    val deletedPlaylist = DeletedPlaylistEntity(
                        originalPlaylistId = playlistId,
                        name = playlist.name,
                        description = playlist.description,
                        coverImageUri = playlist.coverImageUri,
                        originalCreatedAt = playlist.createdAt,
                        deletedAt = System.currentTimeMillis(),
                        playlistDataJson = playlistJson
                    )
                    deletedPlaylistDao.insertDeletedPlaylist(deletedPlaylist)
                }
            }
            playlistDao.deletePlaylistById(playlistId)
        }
    }

    // ============================================
    // PLAYLIST-SONG MANAGEMENT
    // ============================================

    override fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getSongsInPlaylist(playlistId).map { entities ->
            entities.map { it.toSong() }
        }
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        withContext(Dispatchers.IO) {
            playlistDao.addSongToPlaylistTransactional(playlistId, songId, System.currentTimeMillis())
        }
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        withContext(Dispatchers.IO) {
            playlistDao.removeSongFromPlaylist(playlistId, songId)
        }
    }

    override suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            playlistDao.isSongInPlaylist(playlistId, songId)
        }
    }

    override suspend fun updateSongPosition(playlistId: Long, songId: Long, newPosition: Int) {
        withContext(Dispatchers.IO) {
            playlistDao.updateSongPosition(playlistId, songId, newPosition)
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private fun PlaylistEntity.toPlaylist(songCount: Int): Playlist {
        return Playlist(
            id = id,
            name = name,
            description = description,
            coverImageUri = coverImageUri,
            createdAt = createdAt,
            songCount = songCount
        )
    }

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

    // ============================================
    // IMPORT/EXPORT OPERATIONS
    // ============================================

    override suspend fun exportPlaylist(playlistId: Long): String? {
        return withContext(Dispatchers.IO) {
            try {
                val playlist = playlistDao.getPlaylistById(playlistId) ?: return@withContext null
                val songs = playlistDao.getSongsInPlaylist(playlistId).first()
                val exportData = PlaylistExportData(
                    name = playlist.name,
                    description = playlist.description,
                    createdAt = playlist.createdAt,
                    songs = songs.map { song ->
                        SongExportData(song.title, song.artist, song.album, song.duration)
                    }
                )
                gson.toJson(PlaylistExport(version = 1, playlists = listOf(exportData)))
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun exportAllPlaylists(): String {
        return withContext(Dispatchers.IO) {
            try {
                val playlists = playlistDao.getAllPlaylists().first()
                val exportDataList = playlists.map { playlist ->
                    val songs = playlistDao.getSongsInPlaylist(playlist.id).first()
                    PlaylistExportData(
                        name = playlist.name,
                        description = playlist.description,
                        createdAt = playlist.createdAt,
                        songs = songs.map { song ->
                            SongExportData(song.title, song.artist, song.album, song.duration)
                        }
                    )
                }
                gson.toJson(PlaylistExport(version = 1, playlists = exportDataList))
            } catch (e: Exception) {
                "{\"version\":1,\"playlists\":[]}"
            }
        }
    }

    override suspend fun importPlaylists(json: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                val export = gson.fromJson(json, PlaylistExport::class.java)
                var importedCount = 0
                val allSongs = songDao.getAllSongs().first()

                export.playlists.forEach { playlistData ->
                    val playlistId = playlistDao.insertPlaylist(
                        PlaylistEntity(
                            name = playlistData.name,
                            description = playlistData.description,
                            coverImageUri = null,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    var position = 0
                    playlistData.songs.forEach { songData ->
                        val matchingSong = allSongs.find { song ->
                            song.title.equals(songData.title, ignoreCase = true) &&
                            song.artist.equals(songData.artist, ignoreCase = true)
                        }
                        if (matchingSong != null) {
                            playlistDao.addSongToPlaylist(
                                PlaylistSongCrossRef(playlistId, matchingSong.id, System.currentTimeMillis(), position++)
                            )
                        }
                    }
                    importedCount++
                }
                importedCount
            } catch (e: Exception) {
                0
            }
        }
    }

    // ============================================
    // TRASH/RESTORE OPERATIONS
    // ============================================

    override fun getDeletedPlaylists(): Flow<List<DeletedPlaylist>> {
        return deletedPlaylistDao.getAllDeletedPlaylists().map { entities ->
            entities.map { it.toDeletedPlaylist() }
        }
    }

    override fun getDeletedPlaylistsCount(): Flow<Int> {
        return deletedPlaylistDao.getDeletedPlaylistsCount()
    }

    override suspend fun restorePlaylist(deletedPlaylistId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val deletedPlaylist = deletedPlaylistDao.getDeletedPlaylistById(deletedPlaylistId)
                    ?: return@withContext false
                if (importPlaylists(deletedPlaylist.playlistDataJson) > 0) {
                    deletedPlaylistDao.permanentlyDelete(deletedPlaylistId)
                    true
                } else false
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun permanentlyDeletePlaylist(deletedPlaylistId: Long) {
        withContext(Dispatchers.IO) { deletedPlaylistDao.permanentlyDelete(deletedPlaylistId) }
    }

    override suspend fun clearTrash() {
        withContext(Dispatchers.IO) { deletedPlaylistDao.clearTrash() }
    }

    override suspend fun cleanupOldDeletedPlaylists() {
        withContext(Dispatchers.IO) {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            deletedPlaylistDao.deleteOlderThan(thirtyDaysAgo)
        }
    }

    private fun DeletedPlaylistEntity.toDeletedPlaylist(): DeletedPlaylist {
        return DeletedPlaylist(id, name, description, deletedAt, originalCreatedAt, emptyList())
    }
}
