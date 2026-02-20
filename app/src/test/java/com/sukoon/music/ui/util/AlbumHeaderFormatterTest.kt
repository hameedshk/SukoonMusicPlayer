package com.sukoon.music.ui.util

import com.sukoon.music.domain.model.Album
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumHeaderFormatterTest {

    @Test
    fun `buildAlbumHeaderModel replaces duplicate artist with unknown label`() {
        val album = Album(
            id = 1L,
            title = "18",
            artist = "18",
            songCount = 18,
            totalDuration = 1000L,
            albumArtUri = null,
            year = 2011
        )

        val model = buildAlbumHeaderModel(
            album = album,
            songCountLabel = "18 songs",
            unknownAlbumLabel = "Unknown album",
            unknownArtistLabel = "Unknown Artist",
            unknownYearLabel = "Unknown year"
        )

        assertEquals("18", model.title)
        assertEquals("Unknown Artist", model.artist)
        assertEquals("2011", model.yearLabel)
        assertEquals(AlbumSourceType.TAGGED_ALBUM, model.sourceType)
    }

    @Test
    fun `buildAlbumHeaderModel marks unknown album title as folder inferred`() {
        val album = Album(
            id = 2L,
            title = "<unknown>",
            artist = "Artist",
            songCount = 3,
            totalDuration = 1000L,
            albumArtUri = null,
            year = null
        )

        val model = buildAlbumHeaderModel(
            album = album,
            songCountLabel = "3 songs",
            unknownAlbumLabel = "Unknown album",
            unknownArtistLabel = "Unknown Artist",
            unknownYearLabel = "Unknown year"
        )

        assertEquals("Unknown album", model.title)
        assertEquals("Artist", model.artist)
        assertEquals("Unknown year", model.yearLabel)
        assertEquals(AlbumSourceType.FOLDER_INFERRED, model.sourceType)
    }

    @Test
    fun `normalizeMetadataValue uses fallback for unknown tokens`() {
        assertEquals("Fallback", normalizeMetadataValue("", "Fallback"))
        assertEquals("Fallback", normalizeMetadataValue("  <unknown> ", "Fallback"))
        assertEquals("Fallback", normalizeMetadataValue("unknown", "Fallback"))
        assertEquals("Fallback", normalizeMetadataValue("null", "Fallback"))
        assertEquals("Album", normalizeMetadataValue("Album", "Fallback"))
    }
}
