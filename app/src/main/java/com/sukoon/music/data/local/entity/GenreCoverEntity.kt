package com.sukoon.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing custom genre artwork.
 * Maps genre ID to a custom artwork URI.
 */
@Entity(tableName = "genre_covers")
data class GenreCoverEntity(
    @PrimaryKey
    val genreId: Long,

    /**
     * Custom artwork URI.
     * Can be a file URI (content://) from gallery or a local path.
     */
    val customArtworkUri: String
)
