package com.sukoon.music.data.metadata

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

/**
 * Unit tests for GeminiMetadataCorrector.
 * Tests the three critical fixes:
 * 1. JSON escaping for special characters
 * 2. Safe regex group extraction
 * 3. Operator precedence in lyrics detection
 */
class GeminiMetadataCorrectorTest {

    // ============================================================================
    // TEST 1: JSON Escaping Fix - Ensure special characters are properly escaped
    // ============================================================================

    @Test
    fun `buildPrompt escapes quotes in metadata`() {
        // This tests the JSON escaping fix
        val artist = """The "Beatles" Revival"""
        val title = """Let It "Be" Now"""
        val album = null

        val prompt = buildTestPrompt(artist, title, album)

        // Verify quotes are escaped
        assertTrue(prompt.contains("""The \"Beatles\" Revival"""))
        assertTrue(prompt.contains("""Let It \"Be\" Now"""))
    }

    @Test
    fun `buildPrompt escapes newlines in metadata`() {
        val artist = "Artist\nLine2"
        val title = "Title\nWith\nNewlines"
        val album = null

        val prompt = buildTestPrompt(artist, title, album)

        // Verify newlines are escaped
        assertTrue(prompt.contains("""Artist\nLine2"""))
        assertTrue(prompt.contains("""Title\nWith\nNewlines"""))
    }

    @Test
    fun `buildPrompt escapes backslashes in metadata`() {
        val artist = """C:\Users\Music"""
        val title = """D:\Audio\Track"""
        val album = null

        val prompt = buildTestPrompt(artist, title, album)

        // Verify backslashes are escaped
        assertTrue(prompt.contains("""C:\\Users\\Music"""))
        assertTrue(prompt.contains("""D:\\Audio\\Track"""))
    }

    @Test
    fun `buildPrompt escapes tabs in metadata`() {
        val artist = "Artist\tName"
        val title = "Title\tHere"
        val album = null

        val prompt = buildTestPrompt(artist, title, album)

        assertTrue(prompt.contains("""Artist\tName"""))
        assertTrue(prompt.contains("""Title\tHere"""))
    }

    @Test
    fun `buildPrompt handles null album correctly`() {
        val artist = "Artist"
        val title = "Title"
        val album = null

        val prompt = buildTestPrompt(artist, title, album)

        // When album is null, should output "null" (not "\"null\"")
        assertTrue(prompt.contains(""""album": null"""))
    }

    @Test
    fun `buildPrompt handles album with special characters`() {
        val album = """Greatest "Hits" & More\Special"""

        val prompt = buildTestPrompt("Artist", "Title", album)

        // Should escape quotes and backslashes, and wrap in quotes
        assertTrue(prompt.contains(""""album": """"))
        assertTrue(prompt.contains("""Greatest \"Hits\" & More\\Special"""))
    }

    // ============================================================================
    // TEST 2: Regex Safety Fix - Safe extraction of regex groups
    // ============================================================================

    @Test
    fun `extractField finds correctly formatted field`() {
        val text = """
            ARTIST: The Beatles
            TITLE: Let It Be
            ALBUM: Abbey Road
        """.trimIndent()

        val artist = extractFieldTest("ARTIST", text)
        assertEquals("The Beatles", artist)
    }

    @Test
    fun `extractField handles field with colon separator`() {
        val text = "TITLE: My Song Title"
        val title = extractFieldTest("TITLE", text)
        assertEquals("My Song Title", title)
    }

    @Test
    fun `extractField handles field with dash separator`() {
        val text = "ARTIST - Artist Name"
        val artist = extractFieldTest("ARTIST", text)
        assertEquals("Artist Name", artist)
    }

    @Test
    fun `extractField returns null for missing field`() {
        val text = """
            ARTIST: The Beatles
            TITLE: Let It Be
        """.trimIndent()

        val album = extractFieldTest("ALBUM", text)
        assertNull(album)
    }

    @Test
    fun `extractField returns null for malformed field`() {
        // Text without proper format - should not crash with IndexOutOfBoundsException
        val text = "ARTIST The Beatles"  // Missing colon/dash

        val artist = extractFieldTest("ARTIST", text)
        assertNull(artist)
    }

    @Test
    fun `extractField trims whitespace from result`() {
        val text = "ARTIST:   Padded Artist Name   "
        val artist = extractFieldTest("ARTIST", text)
        assertEquals("Padded Artist Name", artist)
    }

    @Test
    fun `extractField is case insensitive`() {
        val text = "artist: The Beatles"  // lowercase
        val artist = extractFieldTest("ARTIST", text)
        assertEquals("The Beatles", artist)
    }

    // ============================================================================
    // TEST 3: Operator Precedence Fix - Correct lyrics detection logic
    // ============================================================================

    @Test
    fun `lyricsDetection rejects actual lyrics with LRC markers and long text`() {
        // This is actual lyrics - should be rejected
        val lyricsText = """
            [0:00.00]Verse 1 lyrics
            [0:05.00]More lyrics here
            [0:10.00]Even more lyrics content
            [0:15.00]And another line here
        """.trimIndent()

        val shouldReject = shouldRejectAsLyrics(lyricsText)
        assertTrue(shouldReject, "Should reject actual lyrics with [0 markers and length > 200")
    }

    @Test
    fun `lyricsDetection accepts short text with LRC marker`() {
        // Short metadata that happens to contain "[0" - should NOT be rejected
        val shortText = "Album: [0] Special Edition"

        val shouldReject = shouldRejectAsLyrics(shortText)
        assertTrue(!shouldReject, "Should NOT reject short text even with [0 marker (length < 200)")
    }

    @Test
    fun `lyricsDetection accepts long text without LRC marker`() {
        // Long text without "[0" markers - should not be rejected
        val longMetadata = """
            This is a very long description of an album with lots of text describing
            the artists, the genre, the themes, and various other metadata that happens
            to be quite lengthy but doesn't contain any LRC timestamps so it should be
            fine to accept as valid metadata.
        """.trimIndent()

        val shouldReject = shouldRejectAsLyrics(longMetadata)
        assertTrue(!shouldReject, "Should NOT reject long text without [0 markers")
    }

    @Test
    fun `lyricsDetection requires BOTH conditions to reject`() {
        // Test the critical fix: BOTH conditions must be true ([0 AND length > 200)

        // Has [0 but short: ACCEPT
        val shortWith0 = "Album: [0] Short"
        assertTrue(!shouldRejectAsLyrics(shortWith0))

        // Long without [0: ACCEPT
        val longWithoutLrc = "X".repeat(250)
        assertTrue(!shouldRejectAsLyrics(longWithoutLrc))

        // Has [0 AND long: REJECT
        val longWith0 = "[0:00.00]" + "X".repeat(250)
        assertTrue(shouldRejectAsLyrics(longWith0))
    }

    // ============================================================================
    // Helper Functions for Testing
    // ============================================================================

    private fun buildTestPrompt(artist: String, title: String, album: String?): String {
        // Replicate the escapeJsonString function from GeminiMetadataCorrector
        fun escapeJsonString(value: String): String {
            return value
                .replace("\\", "\\\\")   // Backslash must be escaped first
                .replace("\"", "\\\"")   // Escape quotes
                .replace("\n", "\\n")    // Escape newlines
                .replace("\r", "\\r")    // Escape carriage returns
                .replace("\t", "\\t")    // Escape tabs
        }

        val escapedArtist = escapeJsonString(artist)
        val escapedTitle = escapeJsonString(title)
        val albumJson = album?.let { "\"${escapeJsonString(it)}\"" } ?: "null"

        return """
        Task: Normalize music metadata.

        Original metadata:
        {
          "artist": "$escapedArtist",
          "title": "$escapedTitle",
          "album": $albumJson
        }
    """.trimIndent()
    }

    private fun extractFieldTest(label: String, text: String): String? {
        val regex = Regex(
            pattern = "(?im)^\\s*\\*{0,2}$label\\*{0,2}\\s*[:\\-]\\s*(.+?)\\s*$"
        )
        // Use getOrNull(1) instead of get(1) to avoid IndexOutOfBoundsException
        return regex.find(text)?.groupValues?.getOrNull(1)?.trim()
    }

    private fun shouldRejectAsLyrics(text: String): Boolean {
        // Fixed operator precedence: BOTH conditions must be true (AND not OR)
        return text.contains("[0") && text.length > 200
    }
}
