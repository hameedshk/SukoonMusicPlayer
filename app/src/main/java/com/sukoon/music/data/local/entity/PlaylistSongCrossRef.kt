package com.sukoon.music.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table for many-to-many relationship between playlists and songs.
 *
 * Features:
 * - CASCADE delete: Removing a playlist or song automatically removes junction entries
 * - Prevents duplicates: Same song can't be added to same playlist twice
 * - Ordered songs: Position field allows manual reordering within playlists
 * - Timestamp tracking: Track when song was added to playlist
 */
@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("songId")]  // Index for faster reverse lookups (find playlists containing a song)
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val addedAt: Long = System.currentTimeMillis(),
    val position: Int = 0  // For manual ordering within playlist (0 = first)
)
