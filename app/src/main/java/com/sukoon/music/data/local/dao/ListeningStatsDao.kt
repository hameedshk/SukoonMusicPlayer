package com.sukoon.music.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sukoon.music.data.local.entity.ListeningStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningStatsDao {

    /**
     * Insert or replace a daily stats record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: ListeningStatsEntity)

    /**
     * Update an existing stats record.
     */
    @Update
    suspend fun update(stats: ListeningStatsEntity)

    /**
     * Get stats for a specific date.
     */
    @Query("SELECT * FROM listening_stats WHERE dateMs = :dateMs")
    suspend fun getStatsByDate(dateMs: Long): ListeningStatsEntity?

    /**
     * Get stats for the last N days (ordered by date, most recent first).
     */
    @Query("""
        SELECT * FROM listening_stats
        WHERE dateMs >= :fromDateMs
        ORDER BY dateMs DESC
    """)
    fun getStatsRange(fromDateMs: Long): Flow<List<ListeningStatsEntity>>

    /**
     * Get stats for the last 7 days as a Flow (for UI observation).
     */
    @Query("""
        SELECT * FROM listening_stats
        ORDER BY dateMs DESC
        LIMIT 7
    """)
    fun getLastSevenDays(): Flow<List<ListeningStatsEntity>>

    /**
     * Delete stats older than a given date.
     */
    @Query("DELETE FROM listening_stats WHERE dateMs < :beforeDateMs")
    suspend fun deleteOlderThan(beforeDateMs: Long)

    /**
     * Get the total listening time across the last 7 days in milliseconds.
     */
    @Query("""
        SELECT COALESCE(SUM(totalDurationMs), 0)
        FROM listening_stats
        WHERE dateMs >= :fromDateMs
    """)
    suspend fun getTotalDuration7Days(fromDateMs: Long): Long

    /**
     * Get the top artist across the last 7 days.
     */
    @Query("""
        SELECT topArtist, SUM(topArtistCount) as totalCount
        FROM listening_stats
        WHERE dateMs >= :fromDateMs AND topArtist IS NOT NULL
        GROUP BY topArtist
        ORDER BY totalCount DESC
        LIMIT 1
    """)
    suspend fun getTopArtist7Days(fromDateMs: Long): TopArtistResult?

    /**
     * Get the peak time-of-day across the last 7 days.
     */
    @Query("""
        SELECT peakTimeOfDay, COUNT(*) as frequency
        FROM listening_stats
        WHERE dateMs >= :fromDateMs AND peakTimeOfDay != 'unknown'
        GROUP BY peakTimeOfDay
        ORDER BY frequency DESC
        LIMIT 1
    """)
    suspend fun getPeakTimeOfDay7Days(fromDateMs: Long): PeakTimeResult?

    /**
     * Get all stats for the last 7 days (for manual aggregation if needed).
     */
    @Query("""
        SELECT * FROM listening_stats
        WHERE dateMs >= :fromDateMs
        ORDER BY dateMs DESC
    """)
    suspend fun getAll7DayStats(fromDateMs: Long): List<ListeningStatsEntity>
}

/**
 * Result class for top artist query.
 */
data class TopArtistResult(
    val topArtist: String?,
    val totalCount: Int
)

/**
 * Result class for peak time-of-day query.
 */
data class PeakTimeResult(
    val peakTimeOfDay: String,
    val frequency: Int
)
