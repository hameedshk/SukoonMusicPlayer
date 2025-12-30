package com.sukoon.music.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Relation
import com.sukoon.music.domain.model.QueueWithSongs
import com.sukoon.music.domain.model.Song

/**
 * Junction table for many-to-many relationship between queues and songs.
 *
 * Features:
 * - CASCADE delete: Removing a queue or song automatically removes junction entries
 * - Ordered songs: Position field allows manual reordering within queues
 * - No duplicates: Same song can be added multiple times to a queue but at different positions
 */
@Entity(
    tableName = "queue_items",
    primaryKeys = ["queueId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = QueueEntity::class,
            parentColumns = ["id"],
            childColumns = ["queueId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("songId"),  // Index for faster reverse lookups
        Index("queueId", "position")  // Composite index for ordered queries
    ]
)
data class QueueItemEntity(
    val queueId: Long,
    val songId: Long,
    val position: Int  // For manual ordering within queue (0 = first)
)

/**
 * Room relation data class for fetching a queue with all its songs.
 * Used by QueueDao for efficient JOIN queries.
 */
data class QueueWithSongsEntity(
    @Embedded val queue: QueueEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = androidx.room.Junction(
            value = QueueItemEntity::class,
            parentColumn = "queueId",
            entityColumn = "songId"
        )
    )
    val songs: List<SongEntity>
)

/**
 * Convert QueueWithSongsEntity to domain QueueWithSongs model.
 */
fun QueueWithSongsEntity.toDomain(): QueueWithSongs {
    val songList = songs.map { it.toDomain() }
    val totalDuration = songList.sumOf { it.duration }

    return QueueWithSongs(
        queue = queue.toDomain(
            songCount = songList.size,
            totalDuration = totalDuration
        ),
        songs = songList
    )
}
