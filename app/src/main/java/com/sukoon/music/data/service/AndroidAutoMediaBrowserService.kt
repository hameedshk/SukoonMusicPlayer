package com.sukoon.music.data.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.sukoon.music.util.DevLogger

/**
 * Placeholder for future Android Auto MediaBrowser support.
 *
 * Currently, Android Auto integration is handled by MusicPlaybackService which extends MediaSessionService.
 * This service is retained for future enhancement to provide rich browsing support.
 */
class AndroidAutoMediaBrowserService : Service() {

    override fun onCreate() {
        super.onCreate()
        DevLogger.d("AndroidAutoMediaBrowserService", "Service created")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        DevLogger.d("AndroidAutoMediaBrowserService", "Service destroyed")
    }
}
