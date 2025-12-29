package com.sukoon.music.data.repository

import android.content.Context
import coil.ImageLoader
import com.sukoon.music.data.local.SukoonDatabase
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.domain.model.AudioQuality
import com.sukoon.music.domain.model.UserPreferences
import com.sukoon.music.domain.repository.SettingsRepository
import com.sukoon.music.domain.repository.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SettingsRepository.
 *
 * Manages user preferences via PreferencesManager (DataStore)
 * and provides storage management operations.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val database: SukoonDatabase,
    private val imageLoader: ImageLoader
) : SettingsRepository {

    override val userPreferences: Flow<UserPreferences> =
        preferencesManager.userPreferencesFlow

    override suspend fun setPrivateSessionEnabled(enabled: Boolean) {
        preferencesManager.setPrivateSessionEnabled(enabled)
    }

    override suspend fun setTheme(theme: AppTheme) {
        preferencesManager.setTheme(theme)
    }

    override suspend fun setScanOnStartup(enabled: Boolean) {
        preferencesManager.setScanOnStartup(enabled)
    }

    override suspend fun setShowNotificationControls(enabled: Boolean) {
        preferencesManager.setShowNotificationControls(enabled)
    }

    override suspend fun setGaplessPlaybackEnabled(enabled: Boolean) {
        preferencesManager.setGaplessPlaybackEnabled(enabled)
    }

    override suspend fun setCrossfadeDuration(durationMs: Int) {
        preferencesManager.setCrossfadeDuration(durationMs)
    }

    override suspend fun setPauseOnAudioNoisy(enabled: Boolean) {
        preferencesManager.setPauseOnAudioNoisy(enabled)
    }

    override suspend fun setResumeOnAudioFocus(enabled: Boolean) {
        preferencesManager.setResumeOnAudioFocus(enabled)
    }

    override suspend fun setAudioQuality(quality: AudioQuality) {
        preferencesManager.setAudioQuality(quality)
    }

    override suspend fun setAudioBuffer(bufferMs: Int) {
        preferencesManager.setAudioBuffer(bufferMs)
    }

    override suspend fun setAudioNormalizationEnabled(enabled: Boolean) {
        preferencesManager.setAudioNormalizationEnabled(enabled)
    }

    override suspend fun setMinimumAudioDuration(seconds: Int) {
        preferencesManager.setMinimumAudioDuration(seconds)
    }

    override suspend fun setShowAllAudioFiles(enabled: Boolean) {
        preferencesManager.setShowAllAudioFiles(enabled)
    }

    override suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val databaseSize = getDatabaseSize()
        val cacheSize = getCacheSize()

        StorageStats(
            databaseSizeBytes = databaseSize,
            cacheSizeBytes = cacheSize
        )
    }

    override suspend fun clearImageCache() {
        withContext(Dispatchers.IO) {
            // Clear Coil's disk and memory cache
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
        }
    }

    override suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            // Clear Room database
            database.clearAllTables()

            // Clear DataStore preferences
            preferencesManager.clearAllPreferences()

            // Clear image cache
            clearImageCache()
        }
    }

    /**
     * Get database file size in bytes.
     */
    private fun getDatabaseSize(): Long {
        val dbFile = context.getDatabasePath(SukoonDatabase.DATABASE_NAME)
        return if (dbFile.exists()) dbFile.length() else 0L
    }

    /**
     * Get cache directory size in bytes.
     */
    private fun getCacheSize(): Long {
        return context.cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }
}
