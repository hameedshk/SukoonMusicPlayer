package com.sukoon.music

import android.app.Application
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.util.AppLocaleManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class SukoonApplication : Application() {

    @Inject
    lateinit var playbackRepository: PlaybackRepository

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()

        // Apply persisted per-app language early in process startup.
        val savedLanguageTag = runBlocking {
            preferencesManager.getAppLanguageTag()
        }
        AppLocaleManager.applyLanguage(this, savedLanguageTag)

        // Initialize AdMob early (demo/test ad units enabled in AdMobManager)
        adMobManager.initialize()

        // Connect to MediaController on app start
        // This ensures the controller is ready before any UI is displayed
        CoroutineScope(Dispatchers.Main).launch {
            playbackRepository.connect()
        }
    }
}
