package com.sukoon.music.data.browsertree

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.domain.model.Playlist

@UnstableApi
fun Song.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setDurationMs(duration)
        .setArtworkUri(albumArtUri?.let { Uri.parse(it) })
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .build()

    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(Uri.parse(path))
        .setMediaMetadata(metadata)
        .build()
}

@UnstableApi
fun Album.toMediaItem(withUri: Boolean = false): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setArtworkUri(albumArtUri?.let { Uri.parse(it) })
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .build()

    return MediaItem.Builder()
        .setMediaId("album_$id")
        .apply { if (withUri && albumArtUri != null) setUri(Uri.parse(albumArtUri)) }
        .setMediaMetadata(metadata)
        .build()
}

@UnstableApi
fun Artist.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setArtworkUri(artworkUri?.let { Uri.parse(it) })
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .build()

    return MediaItem.Builder()
        .setMediaId("artist_$id")
        .setMediaMetadata(metadata)
        .build()
}

@UnstableApi
fun Playlist.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .build()

    return MediaItem.Builder()
        .setMediaId("playlist_$id")
        .setMediaMetadata(metadata)
        .build()
}
