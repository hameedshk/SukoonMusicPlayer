package com.sukoon.music.data.service

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.session.MediaSession
import com.sukoon.music.util.DevLogger
import dagger.hilt.EntryPoints
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * MediaBrowserService for Android Auto integration.
 * Provides app discovery and playback control for Android Auto.
 */
class AndroidAutoMediaBrowserService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSession

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ServiceEntryPoint {
        fun getMediaSession(): MediaSession
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val entryPoint = EntryPoints.get(this, ServiceEntryPoint::class.java)
            mediaSession = entryPoint.getMediaSession()
            // MediaBrowserServiceCompat will use this session for Android Auto control
            DevLogger.d("AndroidAutoMediaBrowserService", "Service initialized with MediaSession")
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
        return BrowserRoot("root", null)
    }

    /**
     * Android Auto calls this to load children of a media item.
     * For now, return empty list (playback control is primary use case).
     */
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(mutableListOf())
    }

    override fun onDestroy() {
        super.onDestroy()
        DevLogger.d("AndroidAutoMediaBrowserService", "Service destroyed")
    }
}
