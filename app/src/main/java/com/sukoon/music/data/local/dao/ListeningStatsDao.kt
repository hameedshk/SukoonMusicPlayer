package com.sukoon.music.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sukoon.music.data.local.entity.ListeningStatsArtistDailyEntity
import com.sukoon.music.data.local.entity.ListeningStatsBucketDailyEntity
import com.sukoon.music.data.local.entity.ListeningStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: ListeningStatsEntity)

    @Update
    suspend fun update(stats: ListeningStatsEntity)

    @Query("SELECT * FROM listening_stats WHERE dateMs = :dateMs")
    suspend fun getStatsByDate(dateMs: Long): ListeningStatsEntity?

    @Query(
        """
        INSERT INTO listening_stats (dateMs, totalDurationMs, topArtist, topArtistCount, peakTimeOfDay, playCount, updatedAt)
        VALUES (:dateMs, :durationMs, NULL, 0, 'unknown', 1, :updatedAt)
        ON CONFLICT(dateMs) DO UPDATE SET
            totalDurationMs = totalDurationMs + :durationMs,
            playCount = playCount + 1,
            updatedAt = :updatedAt
        """
    )
    suspend fun incrementDailyTotals(dateMs: Long, durationMs: Long, updatedAt: Long)

    @Query(
        """
        UPDATE listening_stats
        SET topArtist = :topArtist,
            topArtistCount = :topArtistCount,
            peakTimeOfDay = :peakTimeOfDay,
            updatedAt = :updatedAt
        WHERE dateMs = :dateMs
        """
    )
    suspend fun updateDailyDerivedStats(
        dateMs: Long,
        topArtist: String?,
        topArtistCount: Int,
        peakTimeOfDay: String,
        updatedAt: Long
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtistDaily(stats: ListeningStatsArtistDailyEntity)

    @Query(
        """
        INSERT INTO listening_stats_artist_daily (dateMs, artistName, totalDurationMs, playCount, updatedAt)
        VALUES (:dateMs, :artistName, :durationMs, 1, :updatedAt)
        ON CONFLICT(dateMs, artistName) DO UPDATE SET
            totalDurationMs = totalDurationMs + :durationMs,
            playCount = playCount + 1,
            updatedAt = :updatedAt
        """
    )
    suspend fun incrementArtistDaily(dateMs: Long, artistName: String, durationMs: Long, updatedAt: Long)

    @Query(
        """
        SELECT artistName, totalDurationMs, playCount
        FROM listening_stats_artist_daily
        WHERE dateMs = :dateMs
        ORDER BY totalDurationMs DESC, playCount DESC, artistName COLLATE NOCASE ASC
        LIMIT 1
        """
    )
    suspend fun getTopArtistForDate(dateMs: Long): DailyArtistAggregate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBucketDaily(stats: ListeningStatsBucketDailyEntity)

    @Query(
        """
        INSERT INTO listening_stats_bucket_daily (dateMs, bucket, totalDurationMs, playCount, updatedAt)
        VALUES (:dateMs, :bucket, :durationMs, 1, :updatedAt)
        ON CONFLICT(dateMs, bucket) DO UPDATE SET
            totalDurationMs = totalDurationMs + :durationMs,
            playCount = playCount + 1,
            updatedAt = :updatedAt
        """
    )
    suspend fun incrementBucketDaily(dateMs: Long, bucket: String, durationMs: Long, updatedAt: Long)

    @Query(
        """
        SELECT bucket, totalDurationMs, playCount
        FROM listening_stats_bucket_daily
        WHERE dateMs = :dateMs
        ORDER BY totalDurationMs DESC, playCount DESC,
            CASE bucket
                WHEN 'morning' THEN 0
                WHEN 'afternoon' THEN 1
                WHEN 'night' THEN 2
                ELSE 3
            END ASC
        LIMIT 1
        """
    )
    suspend fun getPeakBucketForDate(dateMs: Long): DailyBucketAggregate?

    @Query(
        """
        SELECT * FROM listening_stats
        WHERE dateMs >= :fromDateMs
        ORDER BY dateMs DESC
        """
    )
    fun getStatsRange(fromDateMs: Long): Flow<List<ListeningStatsEntity>>

    @Query(
        """
        SELECT * FROM listening_stats
        ORDER BY dateMs DESC
        LIMIT 7
        """
    )
    fun getLastSevenDays(): Flow<List<ListeningStatsEntity>>

    @Query("DELETE FROM listening_stats WHERE dateMs < :beforeDateMs")
    suspend fun deleteOlderThan(beforeDateMs: Long)

    @Query("DELETE FROM listening_stats_artist_daily WHERE dateMs < :beforeDateMs")
    suspend fun deleteArtistDailyOlderThan(beforeDateMs: Long)

    @Query("DELETE FROM listening_stats_bucket_daily WHERE dateMs < :beforeDateMs")
    suspend fun deleteBucketDailyOlderThan(beforeDateMs: Long)

    @Query(
        """
        SELECT COALESCE(SUM(totalDurationMs), 0)
        FROM listening_stats
        WHERE dateMs >= :fromDateMs
        """
    )
    suspend fun getTotalDuration7Days(fromDateMs: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(totalDurationMs), 0)
        FROM listening_stats
        WHERE dateMs >= :fromDateMs
        """
    )
    fun observeTotalDuration7Days(fromDateMs: Long): Flow<Long>

    @Query(
        """
        SELECT artistName, SUM(totalDurationMs) AS totalDurationMs, SUM(playCount) AS totalPlayCount
        FROM listening_stats_artist_daily
        WHERE dateMs >= :fromDateMs
        GROUP BY artistName
        ORDER BY totalDurationMs DESC, totalPlayCount DESC, artistName COLLATE NOCASE ASC
        LIMIT 1
        """
    )
    suspend fun getTopArtistByDuration7Days(fromDateMs: Long): TopArtistDurationResult?

    @Query(
        """
        SELECT artistName, SUM(totalDurationMs) AS totalDurationMs, SUM(playCount) AS totalPlayCount
        FROM listening_stats_artist_daily
        WHERE dateMs >= :fromDateMs
        GROUP BY artistName
        ORDER BY totalDurationMs DESC, totalPlayCount DESC, artistName COLLATE NOCASE ASC
        LIMIT 1
        """
    )
    fun observeTopArtistByDuration7Days(fromDateMs: Long): Flow<TopArtistDurationResult?>

    @Query(
        """
        SELECT bucket AS peakTimeOfDay, SUM(totalDurationMs) AS totalDurationMs, SUM(playCount) AS totalPlayCount
        FROM listening_stats_bucket_daily
        WHERE dateMs >= :fromDateMs AND bucket != 'unknown'
        GROUP BY bucket
        ORDER BY totalDurationMs DESC, totalPlayCount DESC,
            CASE bucket
                WHEN 'morning' THEN 0
                WHEN 'afternoon' THEN 1
                WHEN 'night' THEN 2
                ELSE 3
            END ASC
        LIMIT 1
        """
    )
    suspend fun getPeakTimeOfDayByDuration7Days(fromDateMs: Long): PeakTimeDurationResult?

    @Query(
        """
        SELECT bucket AS peakTimeOfDay, SUM(totalDurationMs) AS totalDurationMs, SUM(playCount) AS totalPlayCount
        FROM listening_stats_bucket_daily
        WHERE dateMs >= :fromDateMs AND bucket != 'unknown'
        GROUP BY bucket
        ORDER BY totalDurationMs DESC, totalPlayCount DESC,
            CASE bucket
                WHEN 'morning' THEN 0
                WHEN 'afternoon' THEN 1
                WHEN 'night' THEN 2
                ELSE 3
            END ASC
        LIMIT 1
        """
    )
    fun observePeakTimeOfDayByDuration7Days(fromDateMs: Long): Flow<PeakTimeDurationResult?>

    @Query(
        """
        SELECT * FROM listening_stats
        WHERE dateMs >= :fromDateMs
        ORDER BY dateMs DESC
        """
    )
    suspend fun getAll7DayStats(fromDateMs: Long): List<ListeningStatsEntity>
}

data class DailyArtistAggregate(
    val artistName: String,
    val totalDurationMs: Long,
    val playCount: Int
)

data class DailyBucketAggregate(
    val bucket: String,
    val totalDurationMs: Long,
    val playCount: Int
)

data class TopArtistDurationResult(
    val artistName: String,
    val totalDurationMs: Long,
    val totalPlayCount: Int
)

data class PeakTimeDurationResult(
    val peakTimeOfDay: String,
    val totalDurationMs: Long,
    val totalPlayCount: Int
)
