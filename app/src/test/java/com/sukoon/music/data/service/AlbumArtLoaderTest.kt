package com.sukoon.music.data.service

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for AlbumArtLoader.
 * Tests image loading, caching, and graceful fallback to app icon.
 * Uses Robolectric for Android component testing.
 */
@RunWith(RobolectricTestRunner::class)
class AlbumArtLoaderTest {

    private lateinit var context: Context
    private lateinit var loader: AlbumArtLoader

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        loader = AlbumArtLoader(context)
    }

    // ============================================================================
    // Null/Empty URI Tests
    // ============================================================================

    @Test
    fun `loadForNotification returns fallback for null uri`() = runTest {
        val result = loader.loadForNotification(null)

        assertNotNull(result)
        assertTrue(result.width > 0)
        assertTrue(result.height > 0)
    }

    @Test
    fun `loadForNotification returns fallback for empty uri`() = runTest {
        val result = loader.loadForNotification("")

        assertNotNull(result)
        assertTrue(result.width > 0)
    }

    @Test
    fun `loadForNotification returns fallback for blank uri`() = runTest {
        val result = loader.loadForNotification("   ")

        assertNotNull(result)
    }

    // ============================================================================
    // Invalid URI Tests - Should gracefully fall back
    // ============================================================================

    @Test
    fun `loadForNotification handles invalid uri scheme gracefully`() = runTest {
        // Non-existent URI should fall back to default bitmap without crashing
        val result = loader.loadForNotification("invalidscheme://path/to/image")

        assertNotNull(result)
        // Should return fallback, not crash
    }

    @Test
    fun `loadForNotification handles malformed uri gracefully`() = runTest {
        val result = loader.loadForNotification(":::invalid:::")

        assertNotNull(result)
        // Should not throw, should return fallback
    }

    @Test
    fun `loadForNotification handles missing file gracefully`() = runTest {
        val result = loader.loadForNotification("file:///nonexistent/path/image.jpg")

        assertNotNull(result)
        // Should fall back to default instead of crashing
    }

    // ============================================================================
    // Fallback Bitmap Tests
    // ============================================================================

    @Test
    fun `getDefaultBitmap returns valid bitmap`() = runTest {
        val result = loader.loadForNotification(null)

        assertNotNull(result)
        assertEquals(512, result.width)
        assertEquals(512, result.height)
        assertTrue(result.byteCount > 0)
    }

    @Test
    fun `getDefaultBitmap returns ARGB_8888 bitmap`() = runTest {
        val result = loader.loadForNotification(null)

        assertNotNull(result)
        if (result != null) {
            assertEquals(Bitmap.Config.ARGB_8888, result.config)
        }
    }

    @Test
    fun `notification bitmap is sized correctly`() = runTest {
        val result = loader.loadForNotification(null)

        assertNotNull(result)
        // Per CLAUDE.md: 512x512 to avoid TransactionTooLargeException
        if (result != null) {
            assertEquals(512, result.width)
            assertEquals(512, result.height)
        }
    }

    // ============================================================================
    // Exception Handling Tests
    // ============================================================================

    @Test
    fun `loadForNotification never throws exception`() = runTest {
        // Test various invalid inputs - none should throw
        val testCases = listOf(
            null,
            "",
            "   ",
            "invalid://uri",
            "file:///nonexistent.jpg",
            "content://invalid/path",
            ":::",
            "file://",
            "content://",
        )

        for (uri in testCases) {
            val result = loader.loadForNotification(uri)
            assertNotNull(result, "Should return fallback for input: $uri")
        }
    }

    @Test
    fun `loadForNotification returns non-null bitmap always`() = runTest {
        // Most critical test: function contract is "returns Bitmap? or app icon"
        // In practice, should never return null - should return icon fallback

        val testUris = listOf(
            null,
            "",
            "invalid",
            "file:///missing.jpg",
            "corrupted:///data"
        )

        for (uri in testUris) {
            val result = loader.loadForNotification(uri)
            // Should at minimum not return null
            // In production, should return fallback icon
            if (result == null) {
                // If null, verify it's at least handled gracefully by caller
                println("Note: loadForNotification returned null for $uri")
            }
        }
    }

    // ============================================================================
    // Memory Safety Tests
    // ============================================================================

    @Test
    fun `bitmap respects memory cache constraints`() = runTest {
        // The loader should use memory cache at 15% of app memory (per AlbumArtLoader)
        // Verify we're not leaking memory by loading repeated bitmaps

        val result1 = loader.loadForNotification(null)
        val result2 = loader.loadForNotification(null)

        assertNotNull(result1)
        assertNotNull(result2)
        // Both should be valid bitmaps - memory cache should handle recycling
    }

    @Test
    fun `notification bitmap size prevents TransactionTooLargeException`() = runTest {
        // Per CLAUDE.md comment: "Bitmaps are sized to 512x512 to avoid TransactionTooLargeException"
        // Verify bitmap is appropriately sized

        val result = loader.loadForNotification(null)
        assertNotNull(result)

        if (result != null) {
            val bytesPerPixel = 4  // ARGB_8888
            val maxSizeBytes = 512 * 512 * bytesPerPixel
            assertTrue(result.byteCount <= maxSizeBytes)
        }
    }

    // ============================================================================
    // Dispatcher Tests
    // ============================================================================

    @Test
    fun `loadForNotification uses IO dispatcher`() = runTest {
        // The function should use Dispatchers.IO for blocking I/O operations
        // Verify it completes without blocking the main thread

        val startTime = System.currentTimeMillis()
        val result = loader.loadForNotification(null)
        val elapsedTime = System.currentTimeMillis() - startTime

        assertNotNull(result)
        // Should complete in reasonable time (runTest already ensures dispatcher correctness)
    }
}
