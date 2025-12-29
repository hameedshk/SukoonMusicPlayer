package com.sukoon.music.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.sukoon.music.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads album art bitmaps for media notifications.
 *
 * Uses Coil for efficient image loading with memory and disk caching.
 * Bitmaps are sized to 512x512 to avoid TransactionTooLargeException.
 *
 * Thread-safe: All loading happens on IO dispatcher.
 */
@Singleton
class AlbumArtLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val imageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.15)  // Use 15% of app memory for cache
                .build()
        }
        .build()

    /**
     * Load album art bitmap for notification display.
     *
     * @param uri Album art URI string (from Song.albumArtUri)
     * @return Bitmap sized to 512x512, or app icon if loading fails
     */
    suspend fun loadForNotification(uri: String?): Bitmap? {
        if (uri.isNullOrEmpty()) return getDefaultBitmap()

        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .size(512, 512)  // Notification size limit to avoid TransactionTooLargeException
                    .allowHardware(false)  // Required for notifications (software bitmaps only)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()

                when (val result = imageLoader.execute(request)) {
                    is SuccessResult -> (result.drawable as? BitmapDrawable)?.bitmap
                        ?: getDefaultBitmap()
                    else -> getDefaultBitmap()
                }
            } catch (e: Exception) {
                // Fallback to default on any error (network, IO, etc.)
                getDefaultBitmap()
            }
        }
    }

    /**
     * Get default app icon as fallback bitmap.
     * Handles both BitmapDrawable and AdaptiveIconDrawable (Android 8.0+).
     */
    private fun getDefaultBitmap(): Bitmap {
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            ?: throw IllegalStateException("Failed to load launcher icon")

        // Handle BitmapDrawable directly
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        // Handle AdaptiveIconDrawable and other drawables by rendering to bitmap
        val bitmap = Bitmap.createBitmap(
            512,
            512,
            Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
