package com.sukoon.music.data.repository

import com.sukoon.music.data.local.dao.LyricsDao
import com.sukoon.music.data.local.entity.LyricsEntity
import com.sukoon.music.data.lyrics.LrcParser
import com.sukoon.music.data.metadata.GeminiMetadataCorrector
import com.sukoon.music.data.remote.LyricsApi
import com.sukoon.music.data.remote.dto.LyricsResponse
import com.sukoon.music.domain.model.Lyrics
import com.sukoon.music.domain.model.LyricsSource
import com.sukoon.music.domain.model.LyricsState
import com.sukoon.music.domain.repository.LyricsRepository
import com.sukoon.music.util.DevLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of LyricsRepository following clean architecture.
 *
 * Offline-First Strategy:
 * 1. Check Room cache first
 * 2. Try local .lrc file in same directory as audio file
 * 3. Try embedded lyrics from audio file ID3 tags
 * 4. Fetch from LRCLIB with precise lookup + search fallback
 * 5. Retry once for transient network issues
 * 6. Try Gemini metadata correction and retry lookup
 * 7. Cache successful results
 */
@Singleton
class LyricsRepositoryImpl @Inject constructor(
    private val lyricsDao: LyricsDao,
    private val lyricsApi: LyricsApi,
    private val offlineLyricsScanner: com.sukoon.music.data.lyrics.OfflineLyricsScanner,
    private val id3LyricsExtractor: com.sukoon.music.data.lyrics.Id3LyricsExtractor,
    private val geminiMetadataCorrector: GeminiMetadataCorrector
) : LyricsRepository {

    companion object {
        private const val TAG = "LyricsRepository"
        private const val RETRY_BACKOFF_MS = 350L
    }

    override fun getLyrics(
        trackId: Long,
        audioUri: String,
        artist: String,
        title: String,
        album: String?,
        duration: Int
    ): Flow<LyricsState> = flow {
        DevLogger.d(TAG, "lyrics_stage=start trackId=$trackId")
        emit(LyricsState.Loading)

        try {
            // 1) Cache
            val cached = lyricsDao.getLyricsByTrackId(trackId)
            if (cached != null) {
                DevLogger.d(TAG, "lyrics_stage=cache_hit trackId=$trackId source=${cached.source}")
                val lyrics = cached.toDomainModel()
                emit(LyricsState.Success(lyrics, LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)))
                return@flow
            }
            DevLogger.d(TAG, "lyrics_stage=cache_miss trackId=$trackId")

            // 2) Local .lrc
            val localLrc = offlineLyricsScanner.findLrcFile(audioUri, title, artist)
            if (!localLrc.isNullOrBlank()) {
                DevLogger.d(TAG, "lyrics_stage=local_hit trackId=$trackId")
                val entity = LyricsEntity(
                    trackId = trackId,
                    syncedLyrics = localLrc,
                    plainLyrics = null,
                    syncOffset = 0,
                    source = LyricsSource.LOCAL_FILE.name,
                    lastFetched = System.currentTimeMillis()
                )
                lyricsDao.insertLyrics(entity)
                val lyrics = entity.toDomainModel()
                emit(LyricsState.Success(lyrics, LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)))
                return@flow
            }
            DevLogger.d(TAG, "lyrics_stage=local_miss trackId=$trackId")

            // 3) Embedded tags
            val (embeddedSynced, embeddedPlain) = id3LyricsExtractor.extractLyrics(audioUri)
            if (!embeddedSynced.isNullOrBlank() || !embeddedPlain.isNullOrBlank()) {
                DevLogger.d(TAG, "lyrics_stage=embedded_hit trackId=$trackId")
                val entity = LyricsEntity(
                    trackId = trackId,
                    syncedLyrics = embeddedSynced,
                    plainLyrics = embeddedPlain,
                    syncOffset = 0,
                    source = LyricsSource.EMBEDDED.name,
                    lastFetched = System.currentTimeMillis()
                )
                lyricsDao.insertLyrics(entity)
                val lyrics = entity.toDomainModel()
                emit(LyricsState.Success(lyrics, LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)))
                return@flow
            }
            DevLogger.d(TAG, "lyrics_stage=embedded_miss trackId=$trackId")

            val normalizedArtist = normalize(artist.trim())
            val normalizedTitle = normalize(title.trim())
            val normalizedAlbum = normalize(album)
            val safeDuration = duration.takeIf { it > 0 }

            if (normalizedTitle.isNullOrBlank()) {
                DevLogger.d(TAG, "lyrics_stage=invalid_title trackId=$trackId")
                emit(LyricsState.NotFound)
                return@flow
            }

            // 4-5) Remote lookup + search + retry
            var finalResponse = fetchOnlineLyrics(
                artistName = normalizedArtist,
                trackName = normalizedTitle,
                albumName = normalizedAlbum,
                duration = safeDuration
            )

            // 6) Gemini metadata correction retry
            if (!hasUsableLyrics(finalResponse)) {
                finalResponse = tryGeminiCorrectedLookup(
                    originalArtist = artist,
                    originalTitle = title,
                    originalAlbum = album,
                    duration = safeDuration
                )
            }

            if (!hasUsableLyrics(finalResponse)) {
                DevLogger.d(TAG, "lyrics_stage=final_not_found trackId=$trackId")
                emit(LyricsState.NotFound)
                return@flow
            }

            // 7) Cache + emit
            val entity = LyricsEntity(
                trackId = trackId,
                syncedLyrics = finalResponse?.syncedLyrics,
                plainLyrics = finalResponse?.plainLyrics,
                syncOffset = 0,
                source = LyricsSource.ONLINE.name,
                lastFetched = System.currentTimeMillis()
            )
            lyricsDao.insertLyrics(entity)
            DevLogger.d(TAG, "lyrics_stage=online_cached trackId=$trackId")

            val lyrics = entity.toDomainModel()
            emit(LyricsState.Success(lyrics, LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)))
        } catch (e: Exception) {
            DevLogger.e(TAG, "lyrics_stage=fatal_error trackId=$trackId", e)
            emit(LyricsState.Error("Failed to fetch lyrics: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun updateSyncOffset(trackId: Long, offsetMs: Long) {
        lyricsDao.updateSyncOffset(trackId, offsetMs)
    }

    override suspend fun clearLyrics(trackId: Long) {
        val lyrics = lyricsDao.getLyricsByTrackId(trackId)
        if (lyrics != null) {
            lyricsDao.deleteLyrics(lyrics)
        }
    }

    private suspend fun fetchOnlineLyrics(
        artistName: String?,
        trackName: String,
        albumName: String?,
        duration: Int?
    ): LyricsResponse? {
        var attempt = 1
        while (attempt <= 2) {
            try {
                return fetchOnlineLyricsSingleAttempt(
                    artistName = artistName,
                    trackName = trackName,
                    albumName = albumName,
                    duration = duration
                )
            } catch (e: IOException) {
                val transient = isTransientNetworkError(e)
                DevLogger.d(
                    TAG,
                    "lyrics_stage=remote_io_failure attempt=$attempt transient=$transient error=${e.javaClass.simpleName}"
                )
                if (!transient || attempt == 2) {
                    return null
                }
                delay(RETRY_BACKOFF_MS)
            }
            attempt++
        }
        return null
    }

    private suspend fun fetchOnlineLyricsSingleAttempt(
        artistName: String?,
        trackName: String,
        albumName: String?,
        duration: Int?
    ): LyricsResponse? {
        val precise = tryPreciseLookup(
            artistName = artistName,
            trackName = trackName,
            albumName = albumName,
            duration = duration
        )
        if (hasUsableLyrics(precise)) {
            return precise
        }

        val searchQuery = if (!artistName.isNullOrBlank()) "$artistName $trackName" else trackName
        val searchResults = trySearchLookup(searchQuery)
        val searchHit = searchResults.firstOrNull { hasUsableLyrics(it) }
        if (searchHit != null) {
            DevLogger.d(TAG, "lyrics_stage=remote_search_hit query='$searchQuery'")
        } else {
            DevLogger.d(TAG, "lyrics_stage=remote_search_miss query='$searchQuery'")
        }
        return searchHit
    }

    private suspend fun tryPreciseLookup(
        artistName: String?,
        trackName: String,
        albumName: String?,
        duration: Int?
    ): LyricsResponse? {
        return try {
            val response = lyricsApi.getLyrics(
                artistName = artistName,
                trackName = trackName,
                albumName = albumName,
                duration = duration
            )
            if (hasUsableLyrics(response)) {
                DevLogger.d(TAG, "lyrics_stage=remote_precise_hit artist='$artistName' track='$trackName'")
            } else {
                DevLogger.d(TAG, "lyrics_stage=remote_precise_empty artist='$artistName' track='$trackName'")
            }
            response
        } catch (e: HttpException) {
            DevLogger.d(TAG, "lyrics_stage=remote_precise_http code=${e.code()} artist='$artistName' track='$trackName'")
            null
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            DevLogger.e(TAG, "lyrics_stage=remote_precise_unexpected", e)
            throw e
        }
    }

    private suspend fun trySearchLookup(query: String): List<LyricsResponse> {
        return try {
            val results = lyricsApi.searchLyrics(query)
            DevLogger.d(TAG, "lyrics_stage=remote_search_result_count count=${results.size} query='$query'")
            results
        } catch (e: HttpException) {
            DevLogger.d(TAG, "lyrics_stage=remote_search_http code=${e.code()} query='$query'")
            emptyList()
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            DevLogger.e(TAG, "lyrics_stage=remote_search_unexpected query='$query'", e)
            throw e
        }
    }

    private suspend fun tryGeminiCorrectedLookup(
        originalArtist: String,
        originalTitle: String,
        originalAlbum: String?,
        duration: Int?
    ): LyricsResponse? {
        DevLogger.d(TAG, "lyrics_stage=gemini_metadata_correction_start")

        val corrected = geminiMetadataCorrector.correctMetadata(
            originalArtist = originalArtist,
            originalTitle = originalTitle,
            originalAlbum = originalAlbum
        ) ?: run {
            DevLogger.d(TAG, "lyrics_stage=gemini_metadata_correction_miss")
            return null
        }

        val correctedArtist = normalize(corrected.artist)
        val correctedTitle = normalize(corrected.title)
        val correctedAlbum = normalize(corrected.album)

        if (correctedTitle.isNullOrBlank()) {
            DevLogger.d(TAG, "lyrics_stage=gemini_invalid_corrected_title")
            return null
        }

        val correctedResponse = fetchOnlineLyrics(
            artistName = correctedArtist,
            trackName = correctedTitle,
            albumName = correctedAlbum,
            duration = duration
        )

        if (hasUsableLyrics(correctedResponse)) {
            DevLogger.d(TAG, "lyrics_stage=gemini_retry_hit")
        } else {
            DevLogger.d(TAG, "lyrics_stage=gemini_retry_miss")
        }

        return correctedResponse
    }

    private fun isTransientNetworkError(error: IOException): Boolean {
        return error is SocketTimeoutException ||
            error is UnknownHostException ||
            error is ConnectException ||
            error is InterruptedIOException
    }

    private fun hasUsableLyrics(response: LyricsResponse?): Boolean {
        if (response == null) return false
        return !response.syncedLyrics.isNullOrBlank() || !response.plainLyrics.isNullOrBlank()
    }

    /**
     * Convert LyricsEntity to Lyrics domain model.
     */
    private fun LyricsEntity.toDomainModel(): Lyrics {
        return Lyrics(
            trackId = trackId,
            syncedLyrics = syncedLyrics,
            plainLyrics = plainLyrics,
            syncOffset = syncOffset,
            source = try {
                LyricsSource.valueOf(source)
            } catch (e: Exception) {
                LyricsSource.UNKNOWN
            },
            lastFetched = lastFetched
        )
    }
}

/**
 * Normalize track/artist metadata by removing common junk patterns.
 * Uses generic patterns to catch watermarks, site tags, and quality indicators.
 */
private fun normalize(text: String?): String? {
    if (text.isNullOrBlank()) return null

    return text
        // Remove website watermarks: (SiteName.com), (www.site.com), [SiteName.pk]
        .replace(Regex("[\\(\\[](?:www\\.)?[a-zA-Z0-9-]+\\.[a-z]{2,}[\\)\\]]", RegexOption.IGNORE_CASE), "")

        // Remove quality/bitrate indicators: (320Kbps), [128kbps], (High Quality)
        .replace(Regex("[\\(\\[]\\s*\\d+\\s*[kK]bps\\s*[\\)\\]]", RegexOption.IGNORE_CASE), "")
        .replace(Regex("[\\(\\[]\\s*(?:High|Low|Medium|Best)\\s+Quality\\s*[\\)\\]]", RegexOption.IGNORE_CASE), "")

        // Remove YouTube Music "- Topic" suffix
        .replace(Regex("\\s*-\\s*Topic$", RegexOption.IGNORE_CASE), "")

        // Remove file extensions if present
        .replace(Regex("\\.(mp3|m4a|flac|wav|ogg|aac|wma)$", RegexOption.IGNORE_CASE), "")

        // Remove standalone site names in parentheses (words with unusual caps or all caps)
        .replace(Regex("[\\(\\[](?:[A-Z][a-z]*){2,}[\\)\\]]"), "") // PagalWorld, DjMaza
        .replace(Regex("[\\(\\[][A-Z-]{4,}[\\)\\]]"), "") // MR-JATT, SONGS

        // Clean up extra whitespace and multiple spaces
        .replace(Regex("\\s+"), " ")
        .trim()
        .takeIf { it.isNotBlank() }
}
