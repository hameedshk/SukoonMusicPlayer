package com.sukoon.music.domain.model

data class QueueItem(
    val queueId: Long,
    val song: Song,
    val position: Int
)

data class QueueWithSongs(
    val queue: Queue,
    val songs: List<Song>
)
