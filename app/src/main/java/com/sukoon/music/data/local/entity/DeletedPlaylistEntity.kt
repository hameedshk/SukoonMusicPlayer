package com.sukoon.music.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing deleted playlists in a trash system.
 * Allows users to restore recently deleted playlists.
 *
 * Deleted playlists are kept for 30 days before permanent deletion.
 */
@Entity(
    tableName = "deleted_playlists",
    indices = [Index(value = ["deletedAt"])]
)
data class DeletedPlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Original playlist ID before deletion.
     */
    val originalPlaylistId: Long,

    /**
     * Playlist name.
     */
    val name: String,

    /**
     * Playlist description (optional).
     */
    val description: String? = null,

    /**
     * Cover image URI (optional).
     */
    val coverImageUri: String? = null,

    /**
     * Original creation timestamp.
     */
    val originalCreatedAt: Long,

    /**
     * Deletion timestamp.
     */
    val deletedAt: Long = System.currentTimeMillis(),

    /**
     * JSON string containing the full playlist data for restoration.
     * Includes song information for matching during restore.
     */
    val playlistDataJson: String
)
