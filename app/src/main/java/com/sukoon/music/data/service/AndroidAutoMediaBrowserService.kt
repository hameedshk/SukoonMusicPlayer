package com.sukoon.music.data.service

import androidx.media3.session.MediaSession
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.sukoon.music.util.DevLogger
import dagger.hilt.EntryPoints
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Android Auto integration stub.
 *
 * Note: Full MediaBrowserService-based browse tree support requires Media3 updates.
 * For now, Android Auto playback control is provided through MusicPlaybackService's MediaSession.
 *
 * This service maintains the injected MediaSession singleton for future browse tree implementation.
 */
class AndroidAutoMediaBrowserService : Service() {

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
            DevLogger.d("AndroidAutoMediaBrowserService", "Service initialized with MediaSession")
        } catch (e: Exception) {
            DevLogger.e("AndroidAutoMediaBrowserService", "Failed to initialize", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        DevLogger.d("AndroidAutoMediaBrowserService", "Service destroyed")
    }
}
