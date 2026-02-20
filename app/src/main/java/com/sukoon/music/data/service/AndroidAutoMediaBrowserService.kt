package com.sukoon.music.data.service

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.session.MediaSession
import com.sukoon.music.data.browsertree.BrowseTree
import com.sukoon.music.util.DevLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var isSessionTokenReady = false

    companion object {
        private const val BROWSE_TIMEOUT_MS = 2500L
        private const val EXTRA_CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
        private const val EXTRA_CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        private const val EXTRA_CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        private const val CONTENT_STYLE_LIST = 1
        private const val CONTENT_STYLE_GRID = 2
    }

    override fun onCreate() {
        super.onCreate()
        AndroidAutoConnectionTracker.markInteraction()
        try {
            setSessionToken(mediaSession.sessionCompatToken)
            isSessionTokenReady = true
            DevLogger.d("AndroidAutoMediaBrowserService", "Service initialized with MediaSession token")
        } catch (e: Exception) {
            isSessionTokenReady = false
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
        AndroidAutoConnectionTracker.markInteraction()
        ensureSessionTokenReady()
        if (!isSessionTokenReady) {
            DevLogger.d(
                "AndroidAutoMediaBrowserService",
                "ROOT_REQUEST_BEFORE_TOKEN_READY: package=$clientPackageName uid=$clientUid"
            )
        }
        return BrowserRoot(BrowseTree.ROOT_ID, buildRootExtras())
    }

    /**
     * Android Auto calls this to load children of a media item.
     */
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        AndroidAutoConnectionTracker.markInteraction()
        result.detach()
        serviceScope.launch {
            ensureSessionTokenReady()
            var resultDelivered = false
            fun sendResultOnce(items: MutableList<MediaBrowserCompat.MediaItem>) {
                if (resultDelivered) return
                resultDelivered = true
                result.sendResult(items)
            }

            if (!isSessionTokenReady) {
                DevLogger.d(
                    "AndroidAutoMediaBrowserService",
                    "CHILDREN_REQUEST_BEFORE_TOKEN_READY: parentId=$parentId"
                )
                sendResultOnce(if (parentId == BrowseTree.ROOT_ID) fallbackRootChildren() else mutableListOf())
                return@launch
            }

            try {
                val children = withContext(Dispatchers.IO) {
                    withTimeoutOrNull(BROWSE_TIMEOUT_MS) {
                        browseTree.getChildren(parentId)
                    }
                }
                val resolvedChildren = when {
                    children == null -> {
                        DevLogger.d(
                            "AndroidAutoMediaBrowserService",
                            "BROWSE_TIMEOUT: parentId=$parentId timeoutMs=$BROWSE_TIMEOUT_MS"
                        )
                        if (parentId == BrowseTree.ROOT_ID) browseTree.getRootFallbackChildren() else emptyList()
                    }
                    children.isEmpty() && parentId == BrowseTree.ROOT_ID -> {
                        DevLogger.d(
                            "AndroidAutoMediaBrowserService",
                            "BROWSE_EMPTY_ROOT_FALLBACK: parentId=$parentId"
                        )
                        browseTree.getRootFallbackChildren()
                    }
                    else -> children
                }

                sendResultOnce(resolvedChildren.map { it.toBrowserItem() }.toMutableList())
            } catch (e: Exception) {
                DevLogger.e("AndroidAutoMediaBrowserService", "Failed to load children for $parentId", e)
                sendResultOnce(if (parentId == BrowseTree.ROOT_ID) fallbackRootChildren() else mutableListOf())
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
        DevLogger.d("AndroidAutoMediaBrowserService", "Service destroyed")
    }

    private fun buildRootExtras(): Bundle {
        return Bundle().apply {
            putBoolean(EXTRA_CONTENT_STYLE_SUPPORTED, true)
            putInt(EXTRA_CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID)
            putInt(EXTRA_CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
        }
    }

    private fun fallbackRootChildren(): MutableList<MediaBrowserCompat.MediaItem> {
        return browseTree.getRootFallbackChildren()
            .map { it.toBrowserItem() }
            .toMutableList()
    }

    private fun ensureSessionTokenReady() {
        if (isSessionTokenReady) return
        try {
            setSessionToken(mediaSession.sessionCompatToken)
            isSessionTokenReady = true
            DevLogger.d("AndroidAutoMediaBrowserService", "Session token re-initialized")
        } catch (e: Exception) {
            isSessionTokenReady = false
            DevLogger.e("AndroidAutoMediaBrowserService", "Session token initialization retry failed", e)
        }
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
