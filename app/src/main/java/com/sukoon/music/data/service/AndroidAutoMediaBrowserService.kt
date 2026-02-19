package com.sukoon.music.data.service

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.sukoon.music.data.browsertree.BrowseTree
import com.sukoon.music.di.ApplicationScope
import com.sukoon.music.util.DevLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MediaBrowserService for Android Auto integration.
 * Provides app discovery and playback control for Android Auto.
 */
@AndroidEntryPoint
class AndroidAutoMediaBrowserService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var mediaSession: MediaSession

    @Inject
    lateinit var browseTree: BrowseTree

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        try {
            setSessionToken(mediaSession.sessionCompatToken)
            DevLogger.d("AndroidAutoMediaBrowserService", "Service initialized with MediaSession token")
        } catch (e: Exception) {
            DevLogger.e("AndroidAutoMediaBrowserService", "Failed to initialize", e)
        }
    }

    /**
     * Returns the root media item for Android Auto browsing.
     */
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(BrowseTree.ROOT_ID, null)
    }

    /**
     * Android Auto calls this to load children of a media item.
     */
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.detach()
        scope.launch {
            try {
                val children = browseTree.getChildren(parentId)
                    .map { it.toBrowserItem() }
                    .toMutableList()
                result.sendResult(children)
            } catch (e: Exception) {
                DevLogger.e("AndroidAutoMediaBrowserService", "Failed to load children for $parentId", e)
                result.sendResult(mutableListOf())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        DevLogger.d("AndroidAutoMediaBrowserService", "Service destroyed")
    }

    private fun androidx.media3.common.MediaItem.toBrowserItem(): MediaBrowserCompat.MediaItem {
        val metadata = mediaMetadata
        val flags = when {
            metadata.isBrowsable == true && metadata.isPlayable == true ->
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE or MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            metadata.isBrowsable == true -> MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            metadata.isPlayable == true -> MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            else -> MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        }

        val description = MediaDescriptionCompat.Builder()
            .setMediaId(mediaId)
            .setTitle(metadata.title)
            .setSubtitle(metadata.artist ?: metadata.subtitle)
            .setDescription(metadata.albumTitle)
            .setIconUri(metadata.artworkUri)
            .setMediaUri(localConfiguration?.uri)
            .build()

        return MediaBrowserCompat.MediaItem(description, flags)
    }
}
