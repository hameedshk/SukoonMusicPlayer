package com.sukoon.music.ui.util

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.collection.LruCache
import androidx.compose.material3.MaterialTheme
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
import com.sukoon.music.util.DevLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LRU cache for extracted album palettes.
 * Caches up to 50 palettes (approx 10KB total memory).
 */
private val paletteCache = LruCache<String, AlbumPalette>(50)

/**
 * Data class holding extracted colors from album art.
 * Fallback colors should be provided from MaterialTheme.colorScheme.
 */
data class AlbumPalette(
    val vibrant: Color,
    val vibrantDark: Color,
    val vibrantLight: Color,
    val muted: Color,
    val mutedDark: Color,
    val mutedLight: Color,
    val dominant: Color
)

/**
 * Remembers and extracts color palette from album art URI.
 * Returns AlbumPalette with extracted colors or theme colors if extraction fails.
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
    val colorScheme = MaterialTheme.colorScheme

    // Default palette using theme colors
    val defaultPalette = AlbumPalette(
        vibrant = colorScheme.primary,
        vibrantDark = colorScheme.primaryContainer,
        vibrantLight = colorScheme.secondary,
        muted = colorScheme.surfaceVariant,
        mutedDark = colorScheme.surface,
        mutedLight = colorScheme.surfaceContainer,
        dominant = colorScheme.primary
    )

    var palette by remember(albumArtUri) { mutableStateOf(defaultPalette) }

    LaunchedEffect(albumArtUri) {
        if (albumArtUri.isNullOrBlank()) {
            palette = defaultPalette
            return@LaunchedEffect
        }

        // Check cache first
        val cachedPalette = paletteCache.get(albumArtUri)
        if (cachedPalette != null) {
            DevLogger.d("PaletteExtractor", "Using cached palette for: $albumArtUri")
            palette = cachedPalette
            onPaletteExtracted?.invoke(cachedPalette)
            return@LaunchedEffect
        }

        try {
            DevLogger.d("PaletteExtractor", "Extracting palette from: $albumArtUri")

            val bitmap = withContext(Dispatchers.IO) {
                // Load bitmap from URI using Coil with proper configuration
                val imageLoader = ImageLoader.Builder(context)
                    .crossfade(false) // Disable crossfade for faster loading
                    .build()

                val request = ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .allowHardware(false) // Disable hardware bitmaps for Palette
                    .size(256) // Reduced from 512 for 4x faster extraction
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
                        DevLogger.w("PaletteExtractor", "Failed to load image: ${result.javaClass.simpleName}")
                        null
                    }
                }
            }

            if (bitmap != null) {
                DevLogger.d("PaletteExtractor", "Bitmap loaded, extracting palette...")

                // Extract palette on Default dispatcher - reduced color count for performance
                val extractedPalette = withContext(Dispatchers.Default) {
                    Palette.from(bitmap)
                        .maximumColorCount(8) // Reduced from 16 for 2x faster extraction
                        .generate()
                }

                // Convert to AlbumPalette, falling back to theme colors
                val extractedAlbumPalette = AlbumPalette(
                    vibrant = extractedPalette.vibrantSwatch?.let { Color(it.rgb) }
                        ?: defaultPalette.vibrant,
                    vibrantDark = extractedPalette.darkVibrantSwatch?.let { Color(it.rgb) }
                        ?: defaultPalette.vibrantDark,
                    vibrantLight = extractedPalette.lightVibrantSwatch?.let { Color(it.rgb) }
                        ?: defaultPalette.vibrantLight,
                    muted = extractedPalette.mutedSwatch?.let { Color(it.rgb) }
                        ?: defaultPalette.muted,
                    mutedDark = extractedPalette.darkMutedSwatch?.let { Color(it.rgb) }
                        ?: defaultPalette.mutedDark,
                    mutedLight = extractedPalette.lightMutedSwatch?.let { Color(it.rgb) }
                        ?: defaultPalette.mutedLight,
                    dominant = extractedPalette.dominantSwatch?.let { Color(it.rgb) }
                        ?: defaultPalette.dominant
                )

                // Cache the extracted palette for reuse
                paletteCache.put(albumArtUri, extractedAlbumPalette)

                palette = extractedAlbumPalette
                DevLogger.d("PaletteExtractor", "Palette extracted and cached - Vibrant: ${palette.vibrant}, Dominant: ${palette.dominant}")
                onPaletteExtracted?.invoke(palette)
            } else {
                DevLogger.w("PaletteExtractor", "Bitmap is null, using theme colors")
                palette = defaultPalette
            }
        } catch (e: Exception) {
            // If extraction fails, use theme colors
            DevLogger.e("PaletteExtractor", "Error extracting palette", e)
            palette = defaultPalette
        }
    }

    return palette
}

/**
 * Helper extension to get a suitable accent color from palette.
 * Prefers vibrant colors for UI accents.
 */
val AlbumPalette.candidateAccent: Color
    get() = vibrant

/**
 * Helper extension to get a desaturated slider color for the progress bar.
 * Reduces saturation by 40% to balance vibrancy with subtlety.
 * Returns a muted version of the vibrant accent color (60% saturation).
 */
val AlbumPalette.desaturatedSliderColor: Color
    get() = vibrant.desaturate(0.40f)

/**
 * Helper extension to get a suitable background color from palette.
 * Prefers muted dark colors for backgrounds.
 */
val AlbumPalette.backgroundColor: Color
    get() = mutedDark

/**
 * Desaturate a color by reducing its saturation value.
 * Desaturation amount: 0f = fully saturated, 1f = grayscale.
 *
 * @param desaturationAmount Amount to reduce saturation (0f-1f)
 * @return Desaturated color
 */
private fun Color.desaturate(desaturationAmount: Float): Color {
    // Convert sRGB to HSV
    val r = this.red
    val g = this.green
    val b = this.blue
    val a = this.alpha

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    // Compute hue
    val hue = when {
        delta == 0f -> 0f
        max == r -> (60f * ((g - b) / delta) + 360f) % 360f
        max == g -> (60f * ((b - r) / delta) + 120f) % 360f
        else -> (60f * ((r - g) / delta) + 240f) % 360f
    }

    // Compute saturation
    val saturation = if (max == 0f) 0f else delta / max

    // Compute value
    val value = max

    // Apply desaturation by reducing saturation
    val newSaturation = (saturation * (1f - desaturationAmount)).coerceIn(0f, 1f)

    // Convert back to RGB using HSV
    return Color.hsv(hue, newSaturation, value, a)
}
