package com.sukoon.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for tracking recently played albums.
 */
@Entity(tableName = "recently_played_albums")
data class RecentlyPlayedAlbumEntity(
    /**
     * The album name (used as unique identifier for albums in this project).
     */
    @PrimaryKey
    val albumName: String,

    /**
     * Timestamp when a song from this album was last played.
     */
    val lastPlayedAt: Long = System.currentTimeMillis()
)
