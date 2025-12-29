package com.sukoon.music.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Room relation data class for querying playlists with their associated songs.
 *
 * This uses Room's @Relation annotation with @Junction to handle the many-to-many
 * relationship between playlists and songs through the PlaylistSongCrossRef table.
 *
 * Usage:
 * ```
 * @Transaction
 * @Query("SELECT * FROM playlists WHERE id = :playlistId")
 * suspend fun getPlaylistWithSongs(playlistId: Long): PlaylistWithSongs?
 * ```
 */
data class PlaylistWithSongs(
    @Embedded
    val playlist: PlaylistEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songs: List<SongEntity>
)
