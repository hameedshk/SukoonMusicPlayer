package com.sukoon.music.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Domain-level listening statistics repository interface.
 * Handles fetching aggregated, read-only stats for UI presentation.
 * All stats are derived from existing playback events and stored locally.
 */
interface ListeningStatsRepository {

    /**
     * Observe the last 7 days of listening statistics.
     * Returns empty list if no data available.
     *
     * @return Flow that emits list of daily stats (most recent first)
     */
    fun getLast7DaysStats(): Flow<List<ListeningStatsSnapshot>>

    /**
     * Get total listening time (in minutes) for the last 7 days.
     * Computed lazily when HomeScreen loads.
     */
    suspend fun getTotalListeningTime7Days(): Long

    /**
     * Get the top artist across the last 7 days (by play count).
     * Returns null if no listening data available.
     */
    suspend fun getTopArtist7Days(): String?

    /**
     * Get the peak time-of-day across the last 7 days.
     * Returns: "morning" (5-11), "afternoon" (11-17), "night" (17-24), or null
     */
    suspend fun getPeakTimeOfDay7Days(): String?

    /**
     * Cleanup: Delete stats older than 7 days.
     * Should be called lazily on HomeScreen load.
     */
    suspend fun cleanupOldStats()

    /**
     * Record a play event (called from PlaybackRepository).
     * Updates daily aggregates asynchronously.
     */
    suspend fun recordPlayEvent(artistName: String, durationMs: Long)
}

/**
 * Data class for UI presentation of listening stats.
 * Aggregates stats for a single day.
 */
data class ListeningStatsSnapshot(
    val totalListeningTimeMinutes: Long,
    val topArtist: String?,
    val peakTimeOfDay: String  // "morning", "afternoon", "night", or "unknown"
)
