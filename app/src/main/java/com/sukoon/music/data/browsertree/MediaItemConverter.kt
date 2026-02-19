package com.sukoon.music.data.browsertree

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.domain.model.Playlist

fun Song.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setDurationMs(duration)
        .setArtworkUri(albumArtUri?.let { Uri.parse(it) })
        .build()

    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(Uri.parse(path))
        .setMediaMetadata(metadata)
        .build()
}

fun Album.toMediaItem(withUri: Boolean = false): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setArtworkUri(albumArtUri?.let { Uri.parse(it) })
        .build()

    return MediaItem.Builder()
        .setMediaId("album_$id")
        .apply { if (withUri && albumArtUri != null) setUri(Uri.parse(albumArtUri)) }
        .setMediaMetadata(metadata)
        .build()
}

fun Artist.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setArtworkUri(artworkUri?.let { Uri.parse(it) })
        .build()

    return MediaItem.Builder()
        .setMediaId("artist_$id")
        .setMediaMetadata(metadata)
        .build()
}

fun Playlist.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .build()

    return MediaItem.Builder()
        .setMediaId("playlist_$id")
        .setMediaMetadata(metadata)
        .build()
}
