package com.sukoon.music

import android.app.Application
import com.sukoon.music.domain.repository.PlaybackRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SukoonApplication : Application() {

    @Inject
    lateinit var playbackRepository: PlaybackRepository

    override fun onCreate() {
        super.onCreate()

        // Connect to MediaController on app start
        // This ensures the controller is ready before any UI is displayed
        CoroutineScope(Dispatchers.Main).launch {
            playbackRepository.connect()
        }
    }
}
