package com.sukoon.music.ui.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMatcherTest {

    @Test
    fun `normalize strips punctuation diacritics and collapses spaces`() {
        val normalized = SearchMatcher.normalize("  Beyoncé - Café!!  ")
        assertEquals("beyonce cafe", normalized)
    }

    @Test
    fun `title exact match scores higher than artist exact match`() {
        val context = SearchMatcher.createQueryContext("believer")

        val titleMatch = SearchMatcher.scoreSong(
            context = context,
            title = "Believer",
            artist = "Imagine Dragons",
            album = "Evolve"
        )
        val artistMatch = SearchMatcher.scoreSong(
            context = context,
            title = "Thunder",
            artist = "Believer",
            album = "Evolve"
        )

        assertTrue(titleMatch.score > artistMatch.score)
    }

    @Test
    fun `fuzzy matching allows small typos`() {
        val context = SearchMatcher.createQueryContext("beliver")

        val result = SearchMatcher.scoreSong(
            context = context,
            title = "Believer",
            artist = "Imagine Dragons",
            album = "Evolve"
        )

        assertTrue(result.isMatch)
    }

    @Test
    fun `tokens can match across title and album`() {
        val context = SearchMatcher.createQueryContext("night drive")

        val result = SearchMatcher.scoreSong(
            context = context,
            title = "Night",
            artist = "Artist",
            album = "Drive Sessions"
        )

        assertTrue(result.isMatch)
    }

    @Test
    fun `unrelated query does not pass match threshold`() {
        val context = SearchMatcher.createQueryContext("qzxv")

        val result = SearchMatcher.scoreSong(
            context = context,
            title = "Believer",
            artist = "Imagine Dragons",
            album = "Evolve"
        )

        assertFalse(result.isMatch)
    }
}
