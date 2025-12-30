package com.sukoon.music.domain.repository

import com.sukoon.music.domain.model.Queue
import com.sukoon.music.domain.model.QueueWithSongs
import com.sukoon.music.domain.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Queue operations.
 *
 * Provides a clean API for the domain layer to interact with queue data,
 * abstracting away the implementation details (Room database).
 *
 * All Flow-based methods are reactive and emit new values when data changes.
 * All suspend functions are for single operations.
 */
interface QueueRepository {

    // ============================================
    // QUEUE CRUD
    // ============================================

    /**
     * Get all saved queues.
     * Returns a Flow that emits whenever queues are added/removed/updated.
     */
    fun getAllQueues(): Flow<List<Queue>>

    /**
     * Get a single queue with its metadata.
     * Returns a Flow that emits when the queue or its song count changes.
     */
    fun getQueue(queueId: Long): Flow<Queue?>

    /**
     * Get a queue by ID (single snapshot).
     */
    suspend fun getQueueById(queueId: Long): Queue?

    /**
     * Get the current active queue.
     * Returns null if no queue is marked as current.
     */
    suspend fun getCurrentQueue(): Queue?

    /**
     * Get the current active queue with all its songs.
     * Returns null if no queue is marked as current.
     */
    suspend fun getCurrentQueueWithSongs(): QueueWithSongs?

    /**
     * Create a new queue.
     * Returns the ID of the newly created queue.
     */
    suspend fun createQueue(name: String, songs: List<Song> = emptyList()): Long

    /**
     * Update an existing queue's metadata (name, modified timestamp).
     */
    suspend fun updateQueue(queue: Queue)

    /**
     * Delete a queue.
     * All songs in the queue are automatically removed (CASCADE).
     */
    suspend fun deleteQueue(queueId: Long)

    /**
     * Mark a queue as the current active queue.
     * Unmarks any previously current queue.
     */
    suspend fun setCurrentQueue(queueId: Long)

    /**
     * Clear the current queue marker (no queue is active).
     */
    suspend fun clearCurrentQueue()

    // ============================================
    // QUEUE-SONG MANAGEMENT
    // ============================================

    /**
     * Get all songs in a queue.
     * Returns a Flow that emits when songs are added/removed/reordered.
     */
    fun getSongsInQueue(queueId: Long): Flow<List<Song>>

    /**
     * Get a queue with all its songs (single snapshot).
     */
    suspend fun getQueueWithSongs(queueId: Long): QueueWithSongs?

    /**
     * Add a song to a queue at a specific position.
     * If position is -1, appends to the end.
     */
    suspend fun addSongToQueue(queueId: Long, songId: Long, position: Int = -1)

    /**
     * Add multiple songs to a queue.
     * Songs are added in order starting from the specified position.
     */
    suspend fun addSongsToQueue(queueId: Long, songIds: List<Long>, startPosition: Int = -1)

    /**
     * Remove a song from a queue at a specific position.
     */
    suspend fun removeSongFromQueue(queueId: Long, position: Int)

    /**
     * Update the position of a song within a queue.
     * Used for manual drag-and-drop reordering.
     */
    suspend fun updateSongPosition(queueId: Long, fromPosition: Int, toPosition: Int)

    /**
     * Replace all songs in a queue with a new list.
     * Efficiently replaces the entire queue contents.
     */
    suspend fun replaceQueueSongs(queueId: Long, songs: List<Song>)

    /**
     * Clear all songs from a queue.
     */
    suspend fun clearQueue(queueId: Long)

    // ============================================
    // AUTO-SAVE OPERATIONS
    // ============================================

    /**
     * Save the current playback queue.
     * Creates a new queue or updates the current one.
     * This is called automatically during playback.
     *
     * @param name Name for the queue (defaults to "Current Queue")
     * @param songs List of songs in the queue
     * @param markAsCurrent Whether to mark this as the active queue
     * @return ID of the saved queue
     */
    suspend fun saveCurrentPlaybackQueue(
        name: String = "Current Queue",
        songs: List<Song>,
        markAsCurrent: Boolean = true
    ): Long

    /**
     * Restore a saved queue to the playback system.
     * Returns the list of songs to be played.
     */
    suspend fun restoreQueue(queueId: Long): List<Song>?

    // ============================================
    // CLEANUP OPERATIONS
    // ============================================

    /**
     * Auto-cleanup: Delete queues older than the specified number of days.
     * Does not delete queues marked as current.
     *
     * @param olderThanDays Delete queues older than this many days (default: 30)
     */
    suspend fun cleanupOldQueues(olderThanDays: Int = 30)

    /**
     * Get the total number of saved queues.
     */
    suspend fun getQueueCount(): Int
}
