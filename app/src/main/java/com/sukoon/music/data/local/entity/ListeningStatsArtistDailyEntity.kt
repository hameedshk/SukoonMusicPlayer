package com.sukoon.music.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Per-day artist aggregate used to compute accurate weekly top artist.
 */
@Entity(
    tableName = "listening_stats_artist_daily",
    primaryKeys = ["dateMs", "artistName"],
    indices = [
        Index(value = ["dateMs"]),
        Index(value = ["artistName"])
    ]
)
data class ListeningStatsArtistDailyEntity(
    val dateMs: Long,
    val artistName: String,
    val totalDurationMs: Long = 0,
    val playCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
