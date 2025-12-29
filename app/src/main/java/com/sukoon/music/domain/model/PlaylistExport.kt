package com.sukoon.music.domain.model

/**
 * Data class for exporting/importing playlists in JSON format.
 *
 * Export format:
 * {
 *   "version": 1,
 *   "playlists": [
 *     {
 *       "name": "My Playlist",
 *       "description": "Cool songs",
 *       "createdAt": 1234567890,
 *       "songs": [
 *         { "title": "Song 1", "artist": "Artist 1", "album": "Album 1" },
 *         { "title": "Song 2", "artist": "Artist 2", "album": "Album 2" }
 *       ]
 *     }
 *   ]
 * }
 */
data class PlaylistExport(
    val version: Int = 1,
    val playlists: List<PlaylistExportData>
)

data class PlaylistExportData(
    val name: String,
    val description: String? = null,
    val createdAt: Long,
    val songs: List<SongExportData>
)

data class SongExportData(
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long = 0
)

/**
 * Data class for deleted playlist backup (restore functionality).
 * Stored in a separate "trash" table or file.
 */
data class DeletedPlaylist(
    val id: Long,
    val name: String,
    val description: String? = null,
    val deletedAt: Long,
    val originalCreatedAt: Long,
    val songIds: List<Long>
)
