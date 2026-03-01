package com.sukoon.music.domain.repository

import com.sukoon.music.domain.model.Lyrics
import com.sukoon.music.domain.model.LyricsState
import kotlinx.coroutines.flow.Flow

/**
 * Repository for fetching and caching song lyrics from LRCLIB.
 */
interface LyricsRepository {

    /**
     * Get lyrics for a track. Checks cache first, then offline sources, then online API.
     *
     * @param trackId MediaStore audio ID
     * @param audioUri URI of the audio file for offline lyrics scanning
     * @param artist Artist name for LRCLIB query
     * @param title Song title for LRCLIB query
     * @param album Album name for LRCLIB query (optional)
     * @param duration Track duration in seconds for precision matching
     * @return Flow of LyricsState
     */
    fun getLyrics(
        trackId: Long,
        audioUri: String,
        artist: String,
        title: String,
        album: String?,
        duration: Int
    ): Flow<LyricsState>

    /**
     * Update manual sync offset for a track.
     *
     * @param trackId MediaStore audio ID
     * @param offsetMs Offset in milliseconds (±500ms tolerance)
     */
    suspend fun updateSyncOffset(trackId: Long, offsetMs: Long)

    /**
     * Clear cached lyrics for a track (force refresh).
     */
    suspend fun clearLyrics(trackId: Long)

    /**
     * Save user-provided lyrics for a track. Manual lyrics always take priority.
     */
    suspend fun saveManualLyrics(
        trackId: Long,
        syncedLyrics: String?,
        plainLyrics: String?
    )

    /**
     * Remove manually saved lyrics for a track.
     */
    suspend fun clearManualLyrics(trackId: Long)

    /**
     * Import lyrics from a local file Uri (supports .lrc and .txt).
     */
    suspend fun importLyricsForTrack(trackId: Long, fileUri: String): Result<Unit>
}
