package com.sukoon.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for tracking recently played artists.
 */
@Entity(tableName = "recently_played_artists")
data class RecentlyPlayedArtistEntity(
    /**
     * The artist name (used as unique identifier for artists in this project).
     */
    @PrimaryKey
    val artistName: String,

    /**
     * Timestamp when a song by this artist was last played.
     */
    val lastPlayedAt: Long = System.currentTimeMillis()
)
