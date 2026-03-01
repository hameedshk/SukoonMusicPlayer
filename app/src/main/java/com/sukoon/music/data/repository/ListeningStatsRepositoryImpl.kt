package com.sukoon.music.data.repository

import android.util.Log
import com.sukoon.music.data.local.dao.ListeningStatsDao
import com.sukoon.music.data.local.entity.ListeningStatsEntity
import com.sukoon.music.domain.repository.ListeningStatsRepository
import com.sukoon.music.domain.repository.ListeningStatsSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.max

/**
 * Implementation of ListeningStatsRepository.
 * Tracks listening activity, computes aggregates, and enforces 7-day retention.
 */
class ListeningStatsRepositoryImpl(
    private val listeningStatsDao: ListeningStatsDao
) : ListeningStatsRepository {

    companion object {
        private const val TAG = "ListeningStatsRepository"
        private const val DAY_MS = 24 * 60 * 60 * 1000L
        private const val WINDOW_DAYS = 7L
        private const val MIN_CARD_MINUTES = 30L
    }

    private fun normalizeToStartOfDay(timeMs: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timeMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * Rolling 7-day window including today.
     * Ex: if today is D, include D, D-1, ... D-6.
     */
    private fun rollingWindowStart(timeMs: Long = System.currentTimeMillis()): Long {
        val daysBack = WINDOW_DAYS - 1
        return normalizeToStartOfDay(timeMs) - (daysBack * DAY_MS)
    }

    private fun getTimeOfDayBucket(hour: Int): String = when (hour) {
        in 5..10 -> "morning"
        in 11..16 -> "afternoon"
        in 17..23, in 0..4 -> "night"
        else -> "unknown"
    }

    private fun getCurrentHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    private fun currentWindowStartTicker(): Flow<Long> = flow {
        while (true) {
            val now = System.currentTimeMillis()
            emit(rollingWindowStart(now))
            val startOfNextDay = normalizeToStartOfDay(now) + DAY_MS
            val waitMs = max(1_000L, startOfNextDay - now + 1_000L)
            delay(waitMs)
        }
    }.distinctUntilChanged()

    override fun getLast7DaysStats(): Flow<List<ListeningStatsSnapshot>> {
        return currentWindowStartTicker().flatMapLatest { windowStart ->
            listeningStatsDao.getStatsRange(windowStart)
                .map { entities ->
                    entities.map { entity ->
                        ListeningStatsSnapshot(
                            totalListeningTimeMinutes = entity.totalDurationMs / 60000,
                            topArtist = entity.topArtist,
                            peakTimeOfDay = entity.peakTimeOfDay
                        )
                    }
                }
        }
    }

    override fun observeWeeklySnapshot(): Flow<ListeningStatsSnapshot?> {
        return currentWindowStartTicker().flatMapLatest { windowStart ->
            combine(
                listeningStatsDao.observeTotalDuration7Days(windowStart),
                listeningStatsDao.observeTopArtistByDuration7Days(windowStart),
                listeningStatsDao.observePeakTimeOfDayByDuration7Days(windowStart)
            ) { totalMs, topArtist, peakTime ->
                val totalMinutes = totalMs / 60000
                if (totalMinutes < MIN_CARD_MINUTES) {
                    null
                } else {
                    ListeningStatsSnapshot(
                        totalListeningTimeMinutes = totalMinutes,
                        topArtist = topArtist?.artistName,
                        peakTimeOfDay = peakTime?.peakTimeOfDay ?: "unknown"
                    )
                }
            }
        }
    }

    override suspend fun getTotalListeningTime7Days(): Long = withContext(Dispatchers.IO) {
        val windowStart = rollingWindowStart()
        val totalMs = listeningStatsDao.getTotalDuration7Days(windowStart)
        totalMs / 60000
    }

    override suspend fun getTopArtist7Days(): String? = withContext(Dispatchers.IO) {
        val windowStart = rollingWindowStart()
        listeningStatsDao.getTopArtistByDuration7Days(windowStart)?.artistName
    }

    override suspend fun getPeakTimeOfDay7Days(): String? = withContext(Dispatchers.IO) {
        val windowStart = rollingWindowStart()
        listeningStatsDao.getPeakTimeOfDayByDuration7Days(windowStart)?.peakTimeOfDay
    }

    override suspend fun cleanupOldStats(): Unit = withContext(Dispatchers.IO) {
        val windowStart = rollingWindowStart()
        try {
            listeningStatsDao.deleteOlderThan(windowStart)
            listeningStatsDao.deleteArtistDailyOlderThan(windowStart)
            listeningStatsDao.deleteBucketDailyOlderThan(windowStart)
            Log.d(TAG, "Cleaned up stats older than rolling 7-day window")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old stats", e)
        }
    }

    override suspend fun recordPlayEvent(artistName: String, durationMs: Long): Unit = withContext(Dispatchers.IO) {
        try {
            val today = normalizeToStartOfDay()
            val currentHour = getCurrentHour()
            val timeOfDay = getTimeOfDayBucket(currentHour)
            val normalizedArtist = artistName.trim().ifBlank { "Unknown Artist" }
            val updatedAt = System.currentTimeMillis()

            listeningStatsDao.incrementDailyTotals(
                dateMs = today,
                durationMs = durationMs,
                updatedAt = updatedAt
            )
            listeningStatsDao.incrementArtistDaily(
                dateMs = today,
                artistName = normalizedArtist,
                durationMs = durationMs,
                updatedAt = updatedAt
            )
            listeningStatsDao.incrementBucketDaily(
                dateMs = today,
                bucket = timeOfDay,
                durationMs = durationMs,
                updatedAt = updatedAt
            )

            val topArtistForDay = listeningStatsDao.getTopArtistForDate(today)
            val peakBucketForDay = listeningStatsDao.getPeakBucketForDate(today)

            listeningStatsDao.updateDailyDerivedStats(
                dateMs = today,
                topArtist = topArtistForDay?.artistName,
                topArtistCount = topArtistForDay?.playCount ?: 0,
                peakTimeOfDay = peakBucketForDay?.bucket ?: "unknown",
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error recording play event", e)
        }
    }
}
