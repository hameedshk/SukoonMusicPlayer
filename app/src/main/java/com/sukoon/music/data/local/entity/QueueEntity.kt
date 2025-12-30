package com.sukoon.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sukoon.music.domain.model.Queue

/**
 * Room entity for storing saved queues.
 *
 * Features:
 * - Auto-incrementing primary key
 * - Timestamp tracking for created and modified dates
 * - Current queue marker for restoring playback state
 * - Metadata: name, song count, total duration
 */
@Entity(tableName = "queues")
data class QueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val isCurrent: Boolean = false
)

/**
 * Convert QueueEntity to domain Queue model.
 * Note: songCount and totalDuration must be calculated separately from queue items.
 */
fun QueueEntity.toDomain(songCount: Int = 0, totalDuration: Long = 0): Queue {
    return Queue(
        id = id,
        name = name,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        isCurrent = isCurrent,
        songCount = songCount,
        totalDuration = totalDuration
    )
}

/**
 * Convert domain Queue model to QueueEntity.
 */
fun Queue.toEntity(): QueueEntity {
    return QueueEntity(
        id = id,
        name = name,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        isCurrent = isCurrent
    )
}
