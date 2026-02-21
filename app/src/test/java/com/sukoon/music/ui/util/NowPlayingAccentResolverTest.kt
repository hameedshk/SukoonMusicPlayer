package com.sukoon.music.ui.util

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingAccentResolverTest {

    @Test
    fun `resolveNowPlayingAccentColors returns fallback colors when album art is unavailable`() {
        val fallback = Color(0.2f, 0.4f, 0.6f, 1f)
        val palette = buildPalette(vibrant = Color(1f, 0f, 0f, 1f))

        val resolved = resolveNowPlayingAccentColors(
            palette = palette,
            hasAlbumArt = false,
            fallbackAccent = fallback
        )

        assertEquals(fallback, resolved.controlsColor)
        assertEquals(fallback, resolved.sliderColor)
    }

    @Test
    fun `hasUsableAlbumArt validates expected URI forms`() {
        assertFalse(hasUsableAlbumArt(null))
        assertFalse(hasUsableAlbumArt("   "))
        assertFalse(hasUsableAlbumArt("cover_art"))
        assertTrue(hasUsableAlbumArt("content://media/external/audio/albumart/42"))
        assertTrue(hasUsableAlbumArt("/storage/emulated/0/Music/cover.jpg"))
        assertTrue(hasUsableAlbumArt("file:///storage/emulated/0/Music/cover.jpg"))
    }

    @Test
    fun `resolveNowPlayingAccentColors clamps out of range channels`() {
        val fallback = Color(2f, -1f, 0.5f, 1.2f)
        val palette = buildPalette(
            vibrant = Color(2f, -0.6f, 3f, 1f),
            vibrantDark = Color(-1f, 0.3f, 4f, 1f),
            vibrantLight = Color(0.2f, 2.4f, -2f, 1f),
            dominant = Color(5f, 0.5f, -3f, 1f)
        )

        val resolved = resolveNowPlayingAccentColors(
            palette = palette,
            hasAlbumArt = true,
            fallbackAccent = fallback
        )

        assertColorChannelsAreValid(resolved.controlsColor)
        assertColorChannelsAreValid(resolved.sliderColor)
    }

    private fun buildPalette(
        vibrant: Color = Color(0.5f, 0.3f, 0.2f, 1f),
        vibrantDark: Color = Color(0.4f, 0.2f, 0.1f, 1f),
        vibrantLight: Color = Color(0.7f, 0.5f, 0.3f, 1f),
        muted: Color = Color(0.3f, 0.3f, 0.3f, 1f),
        mutedDark: Color = Color(0.2f, 0.2f, 0.2f, 1f),
        mutedLight: Color = Color(0.6f, 0.6f, 0.6f, 1f),
        dominant: Color = Color(0.5f, 0.3f, 0.2f, 1f)
    ): AlbumPalette = AlbumPalette(
        vibrant = vibrant,
        vibrantDark = vibrantDark,
        vibrantLight = vibrantLight,
        muted = muted,
        mutedDark = mutedDark,
        mutedLight = mutedLight,
        dominant = dominant
    )

    private fun assertColorChannelsAreValid(color: Color) {
        assertTrue(color.red in 0f..1f)
        assertTrue(color.green in 0f..1f)
        assertTrue(color.blue in 0f..1f)
        assertTrue(color.alpha in 0f..1f)
    }
}
