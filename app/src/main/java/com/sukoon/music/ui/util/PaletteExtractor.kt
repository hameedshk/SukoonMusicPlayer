package com.sukoon.music.ui.util

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Data class holding extracted colors from album art.
 */
data class AlbumPalette(
    val vibrant: Color = Color(0xFF1DB954), // Spotify green as fallback
    val vibrantDark: Color = Color(0xFF1AAE4B),
    val vibrantLight: Color = Color(0xFF21E35C),
    val muted: Color = Color(0xFF2E2E2E),
    val mutedDark: Color = Color(0xFF1E1E1E),
    val mutedLight: Color = Color(0xFF3E3E3E),
    val dominant: Color = Color(0xFF1DB954)
)

/**
 * Remembers and extracts color palette from album art URI.
 * Returns AlbumPalette with extracted colors or default Spotify green if extraction fails.
 *
 * @param albumArtUri URI of the album art image
 * @param onPaletteExtracted Callback when palette is extracted (optional)
 * @return AlbumPalette with extracted colors
 */
@Composable
fun rememberAlbumPalette(
    albumArtUri: String?,
    onPaletteExtracted: ((AlbumPalette) -> Unit)? = null
): AlbumPalette {
    val context = LocalContext.current
    var palette by remember(albumArtUri) { mutableStateOf(AlbumPalette()) }

    LaunchedEffect(albumArtUri) {
        if (albumArtUri.isNullOrBlank()) {
            android.util.Log.d("PaletteExtractor", "Album art URI is null or blank, using default colors")
            return@LaunchedEffect
        }

        try {
            android.util.Log.d("PaletteExtractor", "Extracting palette from: $albumArtUri")

            val bitmap = withContext(Dispatchers.IO) {
                // Load bitmap from URI using Coil with proper configuration
                val imageLoader = ImageLoader.Builder(context)
                    .crossfade(false) // Disable crossfade for faster loading
                    .build()

                val request = ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .allowHardware(false) // Disable hardware bitmaps for Palette
                    .size(512) // Limit size for faster palette extraction
                    .build()

                val result = imageLoader.execute(request)
                when (result) {
                    is SuccessResult -> {
                        val drawable = result.drawable

                        // Handle BitmapDrawable directly
                        if (drawable is BitmapDrawable) {
                            drawable.bitmap
                        } else {
                            // Convert any other drawable to bitmap
                            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 512
                            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 512
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            drawable.setBounds(0, 0, canvas.width, canvas.height)
                            drawable.draw(canvas)
                            bitmap
                        }
                    }
                    else -> {
                        android.util.Log.w("PaletteExtractor", "Failed to load image: ${result.javaClass.simpleName}")
                        null
                    }
                }
            }

            if (bitmap != null) {
                android.util.Log.d("PaletteExtractor", "Bitmap loaded, extracting palette...")

                // Extract palette on Default dispatcher
                val extractedPalette = withContext(Dispatchers.Default) {
                    Palette.from(bitmap)
                        .maximumColorCount(16)
                        .generate()
                }

                // Convert to AlbumPalette
                palette = AlbumPalette(
                    vibrant = extractedPalette.vibrantSwatch?.let { Color(it.rgb) }
                        ?: palette.vibrant,
                    vibrantDark = extractedPalette.darkVibrantSwatch?.let { Color(it.rgb) }
                        ?: palette.vibrantDark,
                    vibrantLight = extractedPalette.lightVibrantSwatch?.let { Color(it.rgb) }
                        ?: palette.vibrantLight,
                    muted = extractedPalette.mutedSwatch?.let { Color(it.rgb) }
                        ?: palette.muted,
                    mutedDark = extractedPalette.darkMutedSwatch?.let { Color(it.rgb) }
                        ?: palette.mutedDark,
                    mutedLight = extractedPalette.lightMutedSwatch?.let { Color(it.rgb) }
                        ?: palette.mutedLight,
                    dominant = extractedPalette.dominantSwatch?.let { Color(it.rgb) }
                        ?: palette.dominant
                )

                android.util.Log.d("PaletteExtractor", "Palette extracted - Vibrant: ${palette.vibrant}, Dominant: ${palette.dominant}")
                onPaletteExtracted?.invoke(palette)
            } else {
                android.util.Log.w("PaletteExtractor", "Bitmap is null, using default colors")
            }
        } catch (e: Exception) {
            // If extraction fails, use default Spotify green
            android.util.Log.e("PaletteExtractor", "Error extracting palette", e)
        }
    }

    return palette
}

/**
 * Helper extension to get a suitable accent color from palette.
 * Prefers vibrant colors for UI accents.
 */
val AlbumPalette.accentColor: Color
    get() = vibrant

/**
 * Helper extension to get a suitable background color from palette.
 * Prefers muted dark colors for backgrounds.
 */
val AlbumPalette.backgroundColor: Color
    get() = mutedDark
