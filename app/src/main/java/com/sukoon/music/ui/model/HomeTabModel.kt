package com.sukoon.music.ui.model

import androidx.compose.ui.graphics.vector.ImageVector

enum class HomeTabKey(val storageToken: String) {
    HOME("home"),
    SONGS("songs"),
    PLAYLISTS("playlists"),
    FOLDERS("folders"),
    ALBUMS("albums"),
    ARTISTS("artists"),
    GENRES("genres");

    companion object {
        fun fromStoredValue(value: String?): HomeTabKey {
            val normalized = value?.trim().orEmpty()
            if (normalized.isEmpty()) return HOME

            values().firstOrNull { it.storageToken.equals(normalized, ignoreCase = true) }?.let {
                return it
            }

            return when {
                normalized.startsWith("Hi ", ignoreCase = true) -> HOME
                normalized.equals("Songs", ignoreCase = true) -> SONGS
                normalized.equals("Playlist", ignoreCase = true) -> PLAYLISTS
                normalized.equals("Playlists", ignoreCase = true) -> PLAYLISTS
                normalized.equals("Folders", ignoreCase = true) -> FOLDERS
                normalized.equals("Albums", ignoreCase = true) -> ALBUMS
                normalized.equals("Artists", ignoreCase = true) -> ARTISTS
                normalized.equals("Genres", ignoreCase = true) -> GENRES
                else -> HOME
            }
        }
    }
}

data class HomeTabSpec(
    val key: HomeTabKey,
    val label: String,
    val icon: ImageVector
)
