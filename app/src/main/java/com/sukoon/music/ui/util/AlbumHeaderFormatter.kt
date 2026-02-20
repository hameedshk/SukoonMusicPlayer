package com.sukoon.music.ui.util

import com.sukoon.music.domain.model.Album

enum class AlbumSourceType {
    TAGGED_ALBUM,
    FOLDER_INFERRED,
    ARTIST_SCOPED
}

data class AlbumHeaderModel(
    val title: String,
    val artist: String?,
    val yearLabel: String?,
    val songCountLabel: String,
    val sourceType: AlbumSourceType
) {
    val metadataLine: String
        get() = listOfNotNull(
            artist?.takeIf { it.isNotBlank() },
            yearLabel?.takeIf { it.isNotBlank() },
            songCountLabel.takeIf { it.isNotBlank() }
        ).joinToString(" | ")
}

fun buildAlbumHeaderModel(
    album: Album,
    songCountLabel: String,
    unknownAlbumLabel: String,
    unknownArtistLabel: String
): AlbumHeaderModel {
    val normalizedTitle = normalizeMetadataValue(album.title, unknownAlbumLabel)
    val normalizedArtist = normalizeMetadataValue(album.artist, unknownArtistLabel)
    val resolvedArtist = if (normalizedArtist.equals(normalizedTitle, ignoreCase = true)) {
        null
    } else if (normalizedArtist.equals(unknownArtistLabel, ignoreCase = true)) {
        null
    } else {
        normalizedArtist
    }
    val yearLabel = album.year
        ?.takeIf { it > 0 }
        ?.toString()

    return AlbumHeaderModel(
        title = normalizedTitle,
        artist = resolvedArtist,
        yearLabel = yearLabel,
        songCountLabel = songCountLabel,
        sourceType = resolveAlbumSourceType(album.title)
    )
}

internal fun normalizeMetadataValue(
    value: String?,
    fallback: String
): String {
    val sanitized = value?.trim().orEmpty()
    return if (isUnknownToken(sanitized)) fallback else sanitized
}

private fun resolveAlbumSourceType(rawAlbumTitle: String?): AlbumSourceType {
    val title = rawAlbumTitle?.trim().orEmpty()
    return if (isUnknownToken(title)) AlbumSourceType.FOLDER_INFERRED else AlbumSourceType.TAGGED_ALBUM
}

private fun isUnknownToken(value: String): Boolean {
    if (value.isEmpty()) return true

    return value.equals("<unknown>", ignoreCase = true) ||
        value.equals("unknown", ignoreCase = true) ||
        value.equals("unknown album", ignoreCase = true) ||
        value.equals("null", ignoreCase = true)
}

