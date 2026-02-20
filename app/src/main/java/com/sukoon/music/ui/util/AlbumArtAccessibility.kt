package com.sukoon.music.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sukoon.music.R

@Composable
fun albumArtContentDescription(
    albumTitle: String?,
    artistName: String?
): String {
    val normalizedTitle = albumTitle
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: stringResource(R.string.library_album_detail_unknown_album)
    val normalizedArtist = artistName
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: stringResource(R.string.now_playing_unknown_artist)

    return stringResource(
        R.string.common_ui_album_art_description,
        normalizedTitle,
        normalizedArtist
    )
}
