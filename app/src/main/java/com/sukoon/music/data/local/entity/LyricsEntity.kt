package com.sukoon.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching lyrics from multiple sources.
 *
 * Caches both synced (timestamped) and plain lyrics separately to support
 * fallback when sync drift exceeds tolerance.
 * Tracks source for debugging and UI display.
 */
@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val trackId: Long,           // MediaStore audio ID
    val syncedLyrics: String?,               // LRC format: [mm:ss.xx]Lyric line
    val plainLyrics: String?,                // Plain text lyrics
    val syncOffset: Long = 0,                // Manual offset correction in ms (±500ms tolerance)
    val source: String = "UNKNOWN",          // LyricsSource enum value (stored as string)
    val lastFetched: Long = System.currentTimeMillis()
)
