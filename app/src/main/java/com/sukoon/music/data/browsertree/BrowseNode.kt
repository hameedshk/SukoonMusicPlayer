package com.sukoon.music.data.browsertree

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class BrowseNode(
    val mediaId: String,
    val title: String,
    val subtitle: String? = null,
    val mediaUri: String? = null,
    val artworkUri: String? = null,
    val isPlayable: Boolean = false,
    val isBrowsable: Boolean = true
) {
    fun toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setArtworkUri(artworkUri?.let { android.net.Uri.parse(it) })
            .build()

        return MediaItem.Builder()
            .setMediaId(mediaId)
            .apply { if (isPlayable && mediaUri != null) setUri(android.net.Uri.parse(mediaUri)) }
            .setMediaMetadata(metadata)
            .build()
    }
}
