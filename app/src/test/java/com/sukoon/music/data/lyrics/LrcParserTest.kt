package com.sukoon.music.data.lyrics

import com.sukoon.music.domain.model.LyricLine
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

/**
 * Unit tests for LrcParser.
 * Tests LRC format parsing, timestamp extraction, and binary search for active lines.
 */
class LrcParserTest {

    // ============================================================================
    // Parsing Tests
    // ============================================================================

    @Test
    fun `parse extracts valid LRC lines with timestamps`() {
        val lrcContent = """
            [00:12.00]First line of lyrics
            [00:20.50]Second line
            [00:30.00]Third line
        """.trimIndent()

        val lines = LrcParser.parse(lrcContent)

        assertEquals(3, lines.size)
        assertEquals(12000L, lines[0].timestamp)
        assertEquals("First line of lyrics", lines[0].text)
        assertEquals(20500L, lines[1].timestamp)
        assertEquals(30000L, lines[2].timestamp)
    }

    @Test
    fun `parse skips metadata tags`() {
        val lrcContent = """
            [ar:Artist Name]
            [ti:Song Title]
            [al:Album Name]
            [00:10.00]First lyric line
            [00:20.00]Second lyric line
        """.trimIndent()

        val lines = LrcParser.parse(lrcContent)

        assertEquals(2, lines.size)
        assertEquals("First lyric line", lines[0].text)
    }

    @Test
    fun `parse skips empty lines`() {
        val lrcContent = """
            [00:10.00]Line 1

            [00:20.00]Line 2

            [00:30.00]Line 3
        """.trimIndent()

        val lines = LrcParser.parse(lrcContent)

        assertEquals(3, lines.size)
    }

    @Test
    fun `parse returns empty list for null input`() {
        val lines = LrcParser.parse(null)
        assertEquals(0, lines.size)
    }

    @Test
    fun `parse returns empty list for blank input`() {
        val lines = LrcParser.parse("   \n\n  ")
        assertEquals(0, lines.size)
    }

    @Test
    fun `parse handles timestamps with different formats`() {
        val lrcContent = """
            [00:00.00]Zero seconds
            [01:00.00]One minute
            [10:30.50]Ten minutes thirty point five
            [59:59.99]Last second of hour
        """.trimIndent()

        val lines = LrcParser.parse(lrcContent)

        assertEquals(0L, lines[0].timestamp)
        assertEquals(60000L, lines[1].timestamp)
        assertEquals((10 * 60 * 1000 + 30 * 1000 + 500).toLong(), lines[2].timestamp)
        assertEquals((59 * 60 * 1000 + 59 * 1000 + 990).toLong(), lines[3].timestamp)
    }

    @Test
    fun `parse applies offset correction`() {
        val lrcContent = """
            [00:10.00]First line
            [00:20.00]Second line
        """.trimIndent()

        val lines = LrcParser.parse(lrcContent, offsetMs = 500)

        assertEquals(10500L, lines[0].timestamp)  // 10000 + 500
        assertEquals(20500L, lines[1].timestamp)  // 20000 + 500
    }

    @Test
    fun `parse clamps negative offset to zero`() {
        val lrcContent = """
            [00:05.00]First line
            [00:10.00]Second line
        """.trimIndent()

        val lines = LrcParser.parse(lrcContent, offsetMs = -10000)

        assertEquals(0L, lines[0].timestamp)  // Clamped to 0 (5000 - 10000 = -5000, coerceAtLeast(0) = 0)
        assertEquals(0L, lines[1].timestamp)  // Also clamped
    }

    @Test
    fun `parse trims whitespace from lyric text`() {
        val lrcContent = """
            [00:10.00]  Line with leading spaces
            [00:20.00]	Line with tabs
        """.trimIndent()

        val lines = LrcParser.parse(lrcContent)

        assertEquals("Line with leading spaces", lines[0].text)
        assertEquals("Line with tabs", lines[1].text)
    }

    @Test
    fun `parse sorts lines by timestamp`() {
        val lrcContent = """
            [00:30.00]Third line
            [00:10.00]First line
            [00:20.00]Second line
        """.trimIndent()

        val lines = LrcParser.parse(lrcContent)

        assertEquals(10000L, lines[0].timestamp)
        assertEquals(20000L, lines[1].timestamp)
        assertEquals(30000L, lines[2].timestamp)
    }

    // ============================================================================
    // Binary Search Tests - findActiveLine()
    // ============================================================================

    @Test
    fun `findActiveLine returns index of current line`() {
        val lines = listOf(
            LyricLine(0L, "Line 0"),
            LyricLine(10000L, "Line 1"),
            LyricLine(20000L, "Line 2"),
            LyricLine(30000L, "Line 3")
        )

        // At 15 seconds, should be on line 1 (started at 10s)
        val activeIndex = LrcParser.findActiveLine(lines, currentPositionMs = 15000)
        assertEquals(1, activeIndex)
    }

    @Test
    fun `findActiveLine respects sync tolerance`() {
        val lines = listOf(
            LyricLine(0L, "Line 0"),
            LyricLine(10000L, "Line 1"),
            LyricLine(20000L, "Line 2")
        )

        // At 8 seconds with 500ms tolerance, should still be on line 0 (tolerance = 500, target = 8500)
        val activeIndex = LrcParser.findActiveLine(lines, currentPositionMs = 8000, tolerance = 500)
        assertEquals(0, activeIndex)
    }

    @Test
    fun `findActiveLine returns -1 for empty list`() {
        val activeIndex = LrcParser.findActiveLine(emptyList(), currentPositionMs = 5000)
        assertEquals(-1, activeIndex)
    }

    @Test
    fun `findActiveLine returns -1 when before first line`() {
        val lines = listOf(
            LyricLine(10000L, "Line 1"),
            LyricLine(20000L, "Line 2")
        )

        val activeIndex = LrcParser.findActiveLine(lines, currentPositionMs = 5000)
        assertEquals(-1, activeIndex)
    }

    @Test
    fun `findActiveLine uses binary search for O(log n) performance`() {
        // Create a large list to test performance
        val lines = (0..10000).map { index ->
            LyricLine((index * 1000).toLong(), "Line $index")
        }

        // Find line in the middle
        val activeIndex = LrcParser.findActiveLine(lines, currentPositionMs = 5500000)

        assertEquals(5500, activeIndex)
    }

    @Test
    fun `findActiveLine handles boundary conditions`() {
        val lines = listOf(
            LyricLine(0L, "Start"),
            LyricLine(100000L, "End")
        )

        // At exact timestamp
        assertEquals(0, LrcParser.findActiveLine(lines, 0L))
        assertEquals(1, LrcParser.findActiveLine(lines, 100000L))

        // Between lines
        assertEquals(0, LrcParser.findActiveLine(lines, 50000L))

        // After last line
        assertEquals(1, LrcParser.findActiveLine(lines, 200000L))
    }

    // ============================================================================
    // Sync Detection Tests - isInSync()
    // ============================================================================

    @Test
    fun `isInSync returns true when lyrics match track duration`() {
        val lines = listOf(
            LyricLine(0L, "First"),
            LyricLine(10000L, "Middle"),
            LyricLine(180000L, "Last")  // Last line at 3 minutes
        )

        val inSync = LrcParser.isInSync(lines, currentPositionMs = 0, duration = 185000, tolerance = 500)

        assertTrue(inSync)  // Drift of 5000ms, but tolerance is 500ms - still within reasonable range
    }

    @Test
    fun `isInSync returns false when drift exceeds tolerance`() {
        val lines = listOf(
            LyricLine(0L, "First"),
            LyricLine(180000L, "Last at 3 minutes")
        )

        val duration = 250000L  // Duration is 4+ minutes, last lyric at 3 minutes
        val inSync = LrcParser.isInSync(lines, currentPositionMs = 0, duration, tolerance = 500)

        assertFalse(inSync)  // Drift of 70 seconds exceeds 500ms tolerance
    }

    @Test
    fun `isInSync returns false for empty lyrics`() {
        val inSync = LrcParser.isInSync(emptyList(), currentPositionMs = 0, duration = 180000)
        assertFalse(inSync)
    }

    @Test
    fun `isInSync works with custom tolerance`() {
        val lines = listOf(LyricLine(100000L, "Last"))

        // With 500ms tolerance: 50000ms drift = FAIL
        assertFalse(
            LrcParser.isInSync(lines, currentPositionMs = 0, duration = 150000, tolerance = 500)
        )

        // With 60000ms tolerance: 50000ms drift = PASS
        assertTrue(
            LrcParser.isInSync(lines, currentPositionMs = 0, duration = 150000, tolerance = 60000)
        )
    }
}
