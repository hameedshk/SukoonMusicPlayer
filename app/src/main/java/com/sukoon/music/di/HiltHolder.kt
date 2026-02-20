package com.sukoon.music.di

import com.sukoon.music.SukoonApplication
import com.sukoon.music.domain.repository.PlaybackRepository

/**
 * Holder for accessing Hilt-injected instances outside of standard DI contexts.
 * Used by MediaSessionService to access PlaybackRepository for state synchronization.
 */
object HiltHolder {
    private var application: SukoonApplication? = null

    fun init(app: SukoonApplication) {
        application = app
    }

    fun getInstance(): SukoonApplication? = application

    fun getPlaybackRepository(): PlaybackRepository? =
        application?.getPlaybackRepositoryOrNull()
}
