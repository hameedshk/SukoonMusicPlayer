package com.sukoon.music.domain.model

/**
 * Sealed class representing items in a folder view.
 * Can be either a subfolder or a song file.
 */
sealed class FolderItem {
    /**
     * Represents a folder (directory) item.
     */
    data class FolderType(val folder: Folder) : FolderItem()

    /**
     * Represents a song (file) item.
     */
    data class SongType(val song: Song) : FolderItem()
}
