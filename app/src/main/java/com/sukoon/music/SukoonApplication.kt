package com.sukoon.music

import android.app.Application
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.domain.repository.PlaybackRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class SukoonApplication : Application() {

    @Inject
    lateinit var playbackRepository: PlaybackRepository

    @Inject
    lateinit var adMobManager: AdMobManager

    override fun onCreate() {
        super.onCreate()

        // Initialize AdMob early (demo/test ad units enabled in AdMobManager)
        adMobManager.initialize()

        // Connect to MediaController on app start
        // This ensures the controller is ready before any UI is displayed
        CoroutineScope(Dispatchers.Main).launch {
            playbackRepository.connect()
        }
    }
}
