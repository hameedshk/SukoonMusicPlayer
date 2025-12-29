package com.sukoon.music.domain.model

enum class FolderSortMode {
    NAME_ASC,      // A-Z
    NAME_DESC,     // Z-A
    TRACK_COUNT,   // Most songs first
    RECENTLY_MODIFIED,  // Most recent song added
    DURATION       // Longest first
}
