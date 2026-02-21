package com.sukoon.music.ui.viewmodel

import com.sukoon.music.R

enum class PlaylistSortMode(val labelResId: Int) {
    RECENT(R.string.playlists_screen_sort_recent),
    NAME(R.string.playlists_screen_sort_name),
    SONG_COUNT(R.string.playlists_screen_sort_songs)
}

enum class PlaylistSortOrder {
    ASCENDING,
    DESCENDING
}
