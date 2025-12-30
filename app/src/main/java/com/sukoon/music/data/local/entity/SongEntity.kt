package com.sukoon.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sukoon.music.domain.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: String,
    val albumArtUri: String?,
    val dateAdded: Long,
    val isLiked: Boolean = false,
    val folderPath: String? = null,
    val genre: String = "Unknown Genre",
    val year: Int = 0,
    val size: Long = 0,
    val playCount: Int = 0
)

/**
 * Convert SongEntity to domain Song model.
 */
fun SongEntity.toDomain(): Song {
    return Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        uri = uri,
        path = folderPath ?: "",
        playCount = playCount,
        year = year,
        size = size,
        albumArtUri = albumArtUri,
        dateAdded = dateAdded,
        isLiked = isLiked,
        genre = genre
    )
}
