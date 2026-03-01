package com.sukoon.music.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Per-day time-bucket aggregate used to compute accurate weekly peak time.
 */
@Entity(
    tableName = "listening_stats_bucket_daily",
    primaryKeys = ["dateMs", "bucket"],
    indices = [
        Index(value = ["dateMs"]),
        Index(value = ["bucket"])
    ]
)
data class ListeningStatsBucketDailyEntity(
    val dateMs: Long,
    val bucket: String, // "morning" | "afternoon" | "night"
    val totalDurationMs: Long = 0,
    val playCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
