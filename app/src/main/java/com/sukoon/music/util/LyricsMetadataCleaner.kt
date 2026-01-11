package com.sukoon.music.domain.util

fun cleanArtist(artist: String?): String? {
    if (artist.isNullOrBlank()) return null
    return artist
        .substringBefore("feat.")
        .substringBefore("ft.")
        .substringBefore("&")
        .substringBefore(",")
        .trim()
        .takeIf { it.isNotBlank() }
}

fun cleanTitle(title: String): String {
    return title
        .replace(Regex("\\(.*?\\)"), "")
        .replace(Regex("\\[.*?]"), "")
        .replace(
            Regex("(?i)remastered|live|version|edit|mono|stereo"),
            ""
        )
        .trim()
}
