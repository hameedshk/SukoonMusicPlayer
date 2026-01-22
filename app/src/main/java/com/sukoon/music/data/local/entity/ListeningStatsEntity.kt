package com.sukoon.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for tracking daily listening statistics.
 * Used to display the "Your Week" stats card on HomeScreen.
 *
 * Design:
 * - Records aggregate stats per day (total duration, top artist, time-of-day patterns).
 * - Data is stored for the last 7 days.
 * - Automatically rolled off via scheduled cleanup or lazy computation.
 * - All calculations are done locally; no cloud sync.
 */
@Entity(tableName = "listening_stats")
data class ListeningStatsEntity(
    /**
     * Date in milliseconds (normalized to start of day).
     * Used as primary key to ensure one record per day.
     */
    @PrimaryKey
    val dateMs: Long,

    /**
     * Total listening duration in milliseconds for this day.
     */
    val totalDurationMs: Long = 0,

    /**
     * Most played artist name for this day.
     * Can be null if no songs were played.
     */
    val topArtist: String? = null,

    /**
     * Play count of the top artist for the day.
     */
    val topArtistCount: Int = 0,

    /**
     * Time-of-day bucket when most music was played.
     * Values: "morning" (5-11), "afternoon" (11-17), "night" (17-24).
     */
    val peakTimeOfDay: String = "unknown",

    /**
     * Total play count for the day (number of songs started).
     */
    val playCount: Int = 0,

    /**
     * Timestamp when this record was created/updated (for debugging).
     */
    val updatedAt: Long = System.currentTimeMillis()
)
