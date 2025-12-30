package com.sukoon.music.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sukoon.music.data.local.entity.QueueEntity
import com.sukoon.music.data.local.entity.QueueItemEntity
import com.sukoon.music.data.local.entity.QueueWithSongsEntity
import com.sukoon.music.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Queue operations.
 *
 * Provides CRUD operations for:
 * - Queues (create, read, update, delete)
 * - Queue-Song relationships (add/remove songs from queues)
 * - Advanced queries (song counts, position updates, current queue tracking)
 *
 * Uses Flow for reactive queries and suspend functions for single operations.
 */
@Dao
interface QueueDao {

    // ============================================
    // QUEUE CRUD OPERATIONS
    // ============================================

    /**
     * Get all queues ordered by modified date (most recently modified first).
     * Returns a reactive Flow that emits whenever queues are added/removed/updated.
     */
    @Query("SELECT * FROM queues ORDER BY modifiedAt DESC")
    fun getAllQueues(): Flow<List<QueueEntity>>

    /**
     * Get a specific queue by ID.
     * Returns null if queue doesn't exist.
     */
    @Query("SELECT * FROM queues WHERE id = :queueId")
    suspend fun getQueueById(queueId: Long): QueueEntity?

    /**
     * Get a reactive flow for a specific queue by ID.
     * Emits updates when the queue is modified.
     */
    @Query("SELECT * FROM queues WHERE id = :queueId")
    fun getQueue(queueId: Long): Flow<QueueEntity?>

    /**
     * Get the current active queue.
     * Returns null if no queue is marked as current.
     */
    @Query("SELECT * FROM queues WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentQueue(): QueueEntity?

    /**
     * Get the current active queue with all its songs.
     * Returns null if no queue is marked as current.
     */
    @Transaction
    @Query("SELECT * FROM queues WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentQueueWithSongs(): QueueWithSongsEntity?

    /**
     * Insert a new queue or replace if exists.
     * Returns the row ID of the inserted queue.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueue(queue: QueueEntity): Long

    /**
     * Update an existing queue.
     */
    @Update
    suspend fun updateQueue(queue: QueueEntity)

    /**
     * Delete a queue.
     * CASCADE constraint automatically removes all queue-song associations.
     */
    @Delete
    suspend fun deleteQueue(queue: QueueEntity)

    /**
     * Delete a queue by ID.
     * CASCADE constraint automatically removes all queue-song associations.
     */
    @Query("DELETE FROM queues WHERE id = :queueId")
    suspend fun deleteQueueById(queueId: Long)

    /**
     * Mark a specific queue as current and unmark all others.
     * Ensures only one queue is marked as current at a time.
     */
    @Transaction
    suspend fun setCurrentQueue(queueId: Long) {
        clearCurrentQueueMarker()
        markQueueAsCurrent(queueId)
    }

    /**
     * Clear the current queue marker (no queue is active).
     */
    @Query("UPDATE queues SET isCurrent = 0")
    suspend fun clearCurrentQueueMarker()

    /**
     * Mark a specific queue as current.
     */
    @Query("UPDATE queues SET isCurrent = 1 WHERE id = :queueId")
    suspend fun markQueueAsCurrent(queueId: Long)

    /**
     * Get the total number of saved queues.
     */
    @Query("SELECT COUNT(*) FROM queues")
    suspend fun getQueueCount(): Int

    // ============================================
    // QUEUE WITH SONGS (JOIN QUERIES)
    // ============================================

    /**
     * Get a queue with all its songs.
     * Uses @Transaction to ensure the queue and songs are loaded atomically.
     */
    @Transaction
    @Query("SELECT * FROM queues WHERE id = :queueId")
    suspend fun getQueueWithSongs(queueId: Long): QueueWithSongsEntity?

    /**
     * Get all queues with their songs.
     * Reactive Flow that updates when queues or songs change.
     */
    @Transaction
    @Query("SELECT * FROM queues ORDER BY modifiedAt DESC")
    fun getAllQueuesWithSongs(): Flow<List<QueueWithSongsEntity>>

    // ============================================
    // QUEUE-SONG MANAGEMENT
    // ============================================

    /**
     * Add a song to a queue at a specific position.
     * Uses REPLACE conflict strategy to allow updating position.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToQueue(item: QueueItemEntity)

    /**
     * Add multiple songs to a queue.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongsToQueue(items: List<QueueItemEntity>)

    /**
     * Remove a song from a queue at a specific position.
     */
    @Query("DELETE FROM queue_items WHERE queueId = :queueId AND position = :position")
    suspend fun removeSongFromQueue(queueId: Long, position: Int)

    /**
     * Remove all songs from a queue.
     */
    @Query("DELETE FROM queue_items WHERE queueId = :queueId")
    suspend fun clearQueue(queueId: Long)

    /**
     * Get the number of songs in a queue.
     */
    @Query("SELECT COUNT(*) FROM queue_items WHERE queueId = :queueId")
    suspend fun getQueueSongCount(queueId: Long): Int

    /**
     * Get all songs in a queue ordered by position.
     * Returns a reactive Flow that updates when songs are added/removed/reordered.
     */
    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN queue_items qi ON s.id = qi.songId
        WHERE qi.queueId = :queueId
        ORDER BY qi.position ASC
    """)
    fun getSongsInQueue(queueId: Long): Flow<List<SongEntity>>

    /**
     * Get all queue items for a specific queue ordered by position.
     * Used for position management operations.
     */
    @Query("SELECT * FROM queue_items WHERE queueId = :queueId ORDER BY position ASC")
    suspend fun getQueueItems(queueId: Long): List<QueueItemEntity>

    /**
     * Get the maximum position in a queue.
     * Returns -1 if the queue is empty.
     */
    @Query("SELECT COALESCE(MAX(position), -1) FROM queue_items WHERE queueId = :queueId")
    suspend fun getMaxPosition(queueId: Long): Int

    /**
     * Update a song's position within a queue.
     * Used for drag-and-drop reordering.
     */
    @Query("""
        UPDATE queue_items
        SET position = :newPosition
        WHERE queueId = :queueId AND position = :oldPosition
    """)
    suspend fun updateItemPosition(queueId: Long, oldPosition: Int, newPosition: Int)

    /**
     * Shift positions for items between two positions.
     * Used when inserting or removing items to maintain order.
     */
    @Query("""
        UPDATE queue_items
        SET position = position + :shift
        WHERE queueId = :queueId AND position >= :startPosition
    """)
    suspend fun shiftPositions(queueId: Long, startPosition: Int, shift: Int)

    // ============================================
    // CLEANUP OPERATIONS
    // ============================================

    /**
     * Delete queues older than the specified timestamp.
     * Does not delete queues marked as current.
     *
     * @param olderThanTimestamp Timestamp (millis since epoch)
     */
    @Query("DELETE FROM queues WHERE modifiedAt < :olderThanTimestamp AND isCurrent = 0")
    suspend fun deleteQueuesOlderThan(olderThanTimestamp: Long)
}
