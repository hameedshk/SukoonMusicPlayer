package com.sukoon.music.data.repository

import com.sukoon.music.data.local.dao.QueueDao
import com.sukoon.music.data.local.dao.SongDao
import com.sukoon.music.data.local.entity.QueueEntity
import com.sukoon.music.data.local.entity.QueueItemEntity
import com.sukoon.music.data.local.entity.toDomain
import com.sukoon.music.data.local.entity.toEntity
import com.sukoon.music.di.ApplicationScope
import com.sukoon.music.domain.model.Queue
import com.sukoon.music.domain.model.QueueWithSongs
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.QueueRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of QueueRepository using Room database.
 *
 * Responsibilities:
 * - Map between database entities (QueueEntity, QueueItemEntity) and domain models (Queue, Song)
 * - Combine multiple data sources (queues + song counts + total duration) for reactive updates
 * - Execute database operations on IO dispatcher
 * - Provide reactive Flows for UI observation
 * - Manage queue auto-save and current queue tracking
 */
@Singleton
class QueueRepositoryImpl @Inject constructor(
    private val queueDao: QueueDao,
    private val songDao: SongDao,
    @ApplicationScope private val scope: CoroutineScope
) : QueueRepository {

    // ============================================
    // QUEUE CRUD
    // ============================================

    override fun getAllQueues(): Flow<List<Queue>> {
        return queueDao.getAllQueues()
            .combine(flow { emit(getAllQueueMetadata()) }) { queues, metadata ->
                queues.map { entity ->
                    val meta = metadata[entity.id]
                    entity.toDomain(
                        songCount = meta?.first ?: 0,
                        totalDuration = meta?.second ?: 0L
                    )
                }
            }
    }

    override fun getQueue(queueId: Long): Flow<Queue?> {
        return queueDao.getQueue(queueId)
            .combine(queueDao.getSongsInQueue(queueId)) { queueEntity, songs ->
                queueEntity?.let {
                    val totalDuration = songs.sumOf { song -> song.duration }
                    it.toDomain(
                        songCount = songs.size,
                        totalDuration = totalDuration
                    )
                }
            }
    }

    override suspend fun getQueueById(queueId: Long): Queue? {
        return withContext(Dispatchers.IO) {
            val entity = queueDao.getQueueById(queueId)
            if (entity != null) {
                val songs = queueDao.getSongsInQueue(queueId)
                val songCount = queueDao.getQueueSongCount(queueId)
                // Calculate total duration by summing up song durations
                // Since getSongsInQueue is a Flow, we need a different approach
                val items = queueDao.getQueueItems(queueId)
                var totalDuration = 0L
                items.forEach { item ->
                    val song = songDao.getSongById(item.songId)
                    totalDuration += song?.duration ?: 0L
                }
                entity.toDomain(songCount = songCount, totalDuration = totalDuration)
            } else {
                null
            }
        }
    }

    override suspend fun getCurrentQueue(): Queue? {
        return withContext(Dispatchers.IO) {
            val entity = queueDao.getCurrentQueue()
            if (entity != null) {
                val songCount = queueDao.getQueueSongCount(entity.id)
                val items = queueDao.getQueueItems(entity.id)
                var totalDuration = 0L
                items.forEach { item ->
                    val song = songDao.getSongById(item.songId)
                    totalDuration += song?.duration ?: 0L
                }
                entity.toDomain(songCount = songCount, totalDuration = totalDuration)
            } else {
                null
            }
        }
    }

    override suspend fun getCurrentQueueWithSongs(): QueueWithSongs? {
        return withContext(Dispatchers.IO) {
            val entity = queueDao.getCurrentQueueWithSongs()
            entity?.toDomain()
        }
    }

    override suspend fun createQueue(name: String, songs: List<Song>): Long {
        return withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val entity = QueueEntity(
                name = name,
                createdAt = now,
                modifiedAt = now,
                isCurrent = false
            )
            val queueId = queueDao.insertQueue(entity)

            // Add songs to the queue
            if (songs.isNotEmpty()) {
                val items = songs.mapIndexed { index, song ->
                    QueueItemEntity(
                        queueId = queueId,
                        songId = song.id,
                        position = index
                    )
                }
                queueDao.addSongsToQueue(items)
            }

            queueId
        }
    }

    override suspend fun updateQueue(queue: Queue) {
        withContext(Dispatchers.IO) {
            val entity = queue.toEntity()
            queueDao.updateQueue(entity.copy(modifiedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun deleteQueue(queueId: Long) {
        withContext(Dispatchers.IO) {
            queueDao.deleteQueueById(queueId)
        }
    }

    override suspend fun setCurrentQueue(queueId: Long) {
        withContext(Dispatchers.IO) {
            queueDao.setCurrentQueue(queueId)
            // Update modified timestamp
            val queue = queueDao.getQueueById(queueId)
            queue?.let {
                queueDao.updateQueue(it.copy(modifiedAt = System.currentTimeMillis()))
            }
        }
    }

    override suspend fun clearCurrentQueue() {
        withContext(Dispatchers.IO) {
            queueDao.clearCurrentQueueMarker()
        }
    }

    // ============================================
    // QUEUE-SONG MANAGEMENT
    // ============================================

    override fun getSongsInQueue(queueId: Long): Flow<List<Song>> {
        return queueDao.getSongsInQueue(queueId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getQueueWithSongs(queueId: Long): QueueWithSongs? {
        return withContext(Dispatchers.IO) {
            val entity = queueDao.getQueueWithSongs(queueId)
            entity?.toDomain()
        }
    }

    override suspend fun addSongToQueue(queueId: Long, songId: Long, position: Int) {
        withContext(Dispatchers.IO) {
            val actualPosition = if (position == -1) {
                queueDao.getMaxPosition(queueId) + 1
            } else {
                // Shift existing items if inserting in the middle
                queueDao.shiftPositions(queueId, position, 1)
                position
            }

            val item = QueueItemEntity(
                queueId = queueId,
                songId = songId,
                position = actualPosition
            )
            queueDao.addSongToQueue(item)

            // Update queue modified timestamp
            val queue = queueDao.getQueueById(queueId)
            queue?.let {
                queueDao.updateQueue(it.copy(modifiedAt = System.currentTimeMillis()))
            }
        }
    }

    override suspend fun addSongsToQueue(queueId: Long, songIds: List<Long>, startPosition: Int) {
        withContext(Dispatchers.IO) {
            val actualStartPosition = if (startPosition == -1) {
                queueDao.getMaxPosition(queueId) + 1
            } else {
                // Shift existing items
                queueDao.shiftPositions(queueId, startPosition, songIds.size)
                startPosition
            }

            val items = songIds.mapIndexed { index, songId ->
                QueueItemEntity(
                    queueId = queueId,
                    songId = songId,
                    position = actualStartPosition + index
                )
            }
            queueDao.addSongsToQueue(items)

            // Update queue modified timestamp
            val queue = queueDao.getQueueById(queueId)
            queue?.let {
                queueDao.updateQueue(it.copy(modifiedAt = System.currentTimeMillis()))
            }
        }
    }

    override suspend fun removeSongFromQueue(queueId: Long, position: Int) {
        withContext(Dispatchers.IO) {
            queueDao.removeSongFromQueue(queueId, position)

            // Shift items after the removed position down
            queueDao.shiftPositions(queueId, position + 1, -1)

            // Update queue modified timestamp
            val queue = queueDao.getQueueById(queueId)
            queue?.let {
                queueDao.updateQueue(it.copy(modifiedAt = System.currentTimeMillis()))
            }
        }
    }

    override suspend fun updateSongPosition(queueId: Long, fromPosition: Int, toPosition: Int) {
        withContext(Dispatchers.IO) {
            if (fromPosition == toPosition) return@withContext

            val items = queueDao.getQueueItems(queueId).toMutableList()

            // Perform the reordering
            val movedItem = items.find { it.position == fromPosition } ?: return@withContext
            items.removeIf { it.position == fromPosition }

            // Adjust positions
            val adjustedItems = items.map { item ->
                when {
                    fromPosition < toPosition && item.position in (fromPosition + 1)..toPosition ->
                        item.copy(position = item.position - 1)
                    fromPosition > toPosition && item.position in toPosition until fromPosition ->
                        item.copy(position = item.position + 1)
                    else -> item
                }
            }.toMutableList()

            // Insert the moved item at the new position
            adjustedItems.add(movedItem.copy(position = toPosition))

            // Clear and re-insert all items
            queueDao.clearQueue(queueId)
            queueDao.addSongsToQueue(adjustedItems)

            // Update queue modified timestamp
            val queue = queueDao.getQueueById(queueId)
            queue?.let {
                queueDao.updateQueue(it.copy(modifiedAt = System.currentTimeMillis()))
            }
        }
    }

    override suspend fun replaceQueueSongs(queueId: Long, songs: List<Song>) {
        withContext(Dispatchers.IO) {
            // Clear existing songs
            queueDao.clearQueue(queueId)

            // Add new songs
            if (songs.isNotEmpty()) {
                val items = songs.mapIndexed { index, song ->
                    QueueItemEntity(
                        queueId = queueId,
                        songId = song.id,
                        position = index
                    )
                }
                queueDao.addSongsToQueue(items)
            }

            // Update queue modified timestamp
            val queue = queueDao.getQueueById(queueId)
            queue?.let {
                queueDao.updateQueue(it.copy(modifiedAt = System.currentTimeMillis()))
            }
        }
    }

    override suspend fun clearQueue(queueId: Long) {
        withContext(Dispatchers.IO) {
            queueDao.clearQueue(queueId)

            // Update queue modified timestamp
            val queue = queueDao.getQueueById(queueId)
            queue?.let {
                queueDao.updateQueue(it.copy(modifiedAt = System.currentTimeMillis()))
            }
        }
    }

    // ============================================
    // AUTO-SAVE OPERATIONS
    // ============================================

    override suspend fun saveCurrentPlaybackQueue(
        name: String,
        songs: List<Song>,
        markAsCurrent: Boolean
    ): Long {
        return withContext(Dispatchers.IO) {
            // Check if there's an existing current queue
            val existingCurrent = queueDao.getCurrentQueue()

            val queueId = if (existingCurrent != null) {
                // Update existing current queue
                replaceQueueSongs(existingCurrent.id, songs)
                queueDao.updateQueue(
                    existingCurrent.copy(
                        name = name,
                        modifiedAt = System.currentTimeMillis()
                    )
                )
                existingCurrent.id
            } else {
                // Create new queue
                val newQueueId = createQueue(name, songs)
                if (markAsCurrent) {
                    setCurrentQueue(newQueueId)
                }
                newQueueId
            }

            queueId
        }
    }

    override suspend fun restoreQueue(queueId: Long): List<Song>? {
        return withContext(Dispatchers.IO) {
            val queueWithSongs = queueDao.getQueueWithSongs(queueId)
            queueWithSongs?.toDomain()?.songs
        }
    }

    // ============================================
    // CLEANUP OPERATIONS
    // ============================================

    override suspend fun cleanupOldQueues(olderThanDays: Int) {
        withContext(Dispatchers.IO) {
            val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
            queueDao.deleteQueuesOlderThan(cutoffTime)
        }
    }

    override suspend fun getQueueCount(): Int {
        return withContext(Dispatchers.IO) {
            queueDao.getQueueCount()
        }
    }

    // ============================================
    // HELPER FUNCTIONS
    // ============================================

    /**
     * Get metadata (song count and total duration) for all queues.
     * Returns a map of queueId to Pair(songCount, totalDuration).
     */
    private suspend fun getAllQueueMetadata(): Map<Long, Pair<Int, Long>> {
        return withContext(Dispatchers.IO) {
            val queues = queueDao.getAllQueues()
            val metadata = mutableMapOf<Long, Pair<Int, Long>>()

            // This is not efficient but works for now
            // In production, consider adding a view or optimized query
            queueDao.getAllQueues().collect { entities ->
                entities.forEach { queue ->
                    val items = queueDao.getQueueItems(queue.id)
                    var totalDuration = 0L
                    items.forEach { item ->
                        val song = songDao.getSongById(item.songId)
                        totalDuration += song?.duration ?: 0L
                    }
                    metadata[queue.id] = Pair(items.size, totalDuration)
                }
            }

            metadata
        }
    }
}
