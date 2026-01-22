package com.sukoon.music.data.repository

import android.util.Log
import com.sukoon.music.data.local.dao.ListeningStatsDao
import com.sukoon.music.data.local.entity.ListeningStatsEntity
import com.sukoon.music.domain.repository.ListeningStatsRepository
import com.sukoon.music.domain.repository.ListeningStatsSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Implementation of ListeningStatsRepository.
 * Tracks listening activity, computes aggregates, and enforces 7-day retention.
 */
class ListeningStatsRepositoryImpl(
    private val listeningStatsDao: ListeningStatsDao
) : ListeningStatsRepository {

    companion object {
        private const val TAG = "ListeningStatsRepository"
        private const val SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000L
    }

    /**
     * Get normalized start-of-day timestamp for any given time.
     */
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
     * Get time-of-day bucket for a given hour (0-23).
     */
    private fun getTimeOfDayBucket(hour: Int): String = when (hour) {
        in 5..10 -> "morning"
        in 11..16 -> "afternoon"
        in 17..23, in 0..4 -> "night"
        else -> "unknown"
    }

    /**
     * Get the current hour (0-23) in device timezone.
     */
    private fun getCurrentHour(): Int {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }

    override fun getLast7DaysStats(): Flow<List<ListeningStatsSnapshot>> {
        val sevenDaysAgo = normalizeToStartOfDay() - SEVEN_DAYS_MS
        return listeningStatsDao.getStatsRange(sevenDaysAgo)
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

    override suspend fun getTotalListeningTime7Days(): Long = withContext(Dispatchers.IO) {
        val sevenDaysAgo = normalizeToStartOfDay() - SEVEN_DAYS_MS
        val totalMs = listeningStatsDao.getTotalDuration7Days(sevenDaysAgo)
        totalMs / 60000  // Convert to minutes
    }

    override suspend fun getTopArtist7Days(): String? = withContext(Dispatchers.IO) {
        val sevenDaysAgo = normalizeToStartOfDay() - SEVEN_DAYS_MS
        listeningStatsDao.getTopArtist7Days(sevenDaysAgo)?.topArtist
    }

    override suspend fun getPeakTimeOfDay7Days(): String? = withContext(Dispatchers.IO) {
        val sevenDaysAgo = normalizeToStartOfDay() - SEVEN_DAYS_MS
        val result = listeningStatsDao.getPeakTimeOfDay7Days(sevenDaysAgo)
        if (result != null && result.peakTimeOfDay != "unknown") {
            result.peakTimeOfDay
        } else {
            null
        }
    }

    override suspend fun cleanupOldStats(): Unit = withContext(Dispatchers.IO) {
        val sevenDaysAgo = normalizeToStartOfDay() - SEVEN_DAYS_MS
        try {
            listeningStatsDao.deleteOlderThan(sevenDaysAgo)
            Log.d(TAG, "Cleaned up stats older than 7 days")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old stats", e)
        }
    }

    override suspend fun recordPlayEvent(artistName: String, durationMs: Long): Unit = withContext(Dispatchers.IO) {
        try {
            val today = normalizeToStartOfDay()
            val currentHour = getCurrentHour()
            val timeOfDay = getTimeOfDayBucket(currentHour)

            // Get or create today's stats
            val existing = listeningStatsDao.getStatsByDate(today)

            if (existing != null) {
                // Update existing record
                val updated = existing.copy(
                    totalDurationMs = existing.totalDurationMs + durationMs,
                    playCount = existing.playCount + 1,
                    peakTimeOfDay = timeOfDay,  // Always update to current time bucket
                    updatedAt = System.currentTimeMillis()
                )
                listeningStatsDao.update(updated)
            } else {
                // Create new record
                val newStats = ListeningStatsEntity(
                    dateMs = today,
                    totalDurationMs = durationMs,
                    topArtist = artistName,
                    topArtistCount = 1,
                    peakTimeOfDay = timeOfDay,
                    playCount = 1,
                    updatedAt = System.currentTimeMillis()
                )
                listeningStatsDao.upsert(newStats)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recording play event", e)
        }
    }
}
