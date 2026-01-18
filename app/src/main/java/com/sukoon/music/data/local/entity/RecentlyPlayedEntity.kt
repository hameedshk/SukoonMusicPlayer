package com.sukoon.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for tracking recently played songs.
 * Used to display the "Recently Played" 2x3 grid on the Home screen (S-style).
 *
 * Design:
 * - Each time a song is played (not just started, but actually listened to for a significant duration),
 *   we log it with a timestamp.
 * - We keep track of the last time each song was played.
 * - The 2x3 grid shows the 6 most recently played songs.
 * - When private session is active, we don't log plays.
 */
@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    /**
     * The song ID (matches Song.id from MediaStore).
     */
    @PrimaryKey
    val songId: Long,

    /**
     * Timestamp when the song was last played (milliseconds since epoch).
     * Used for sorting to show most recent at the top.
     */
    val lastPlayedAt: Long = System.currentTimeMillis(),

    /**
     * Total number of times this song has been played.
     * Can be used for "most played" features later.
     */
    val playCount: Int = 1
)
