package com.sukoon.music.data.repository

import com.sukoon.music.util.DevLogger
import com.sukoon.music.data.local.dao.LyricsDao
import com.sukoon.music.data.local.entity.LyricsEntity
import com.sukoon.music.data.lyrics.LrcParser
import com.sukoon.music.data.metadata.GeminiMetadataCorrector
import com.sukoon.music.data.remote.LyricsApi
import com.sukoon.music.domain.model.Lyrics
import com.sukoon.music.domain.model.LyricsState
import com.sukoon.music.domain.repository.LyricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import java.io.IOException
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
 * 4. If offline sources fail, fetch from LRCLIB using precise lookup
 * 5. Fallback to LRCLIB search API if precise lookup fails
 * 6. Cache results for future use
 * 7. Parse synced lyrics with LrcParser
 */
@Singleton
class LyricsRepositoryImpl @Inject constructor(
    private val lyricsDao: LyricsDao,
    private val lyricsApi: LyricsApi,
    private val offlineLyricsScanner: com.sukoon.music.data.lyrics.OfflineLyricsScanner,
    private val id3LyricsExtractor: com.sukoon.music.data.lyrics.Id3LyricsExtractor,
    private val geminiMetadataCorrector: com.sukoon.music.data.metadata.GeminiMetadataCorrector
) : LyricsRepository {

    companion object {
        private const val TAG = "LyricsRepository"
    }

    override fun getLyrics(
        trackId: Long,
        audioUri: String,
        artist: String,
        title: String,
        album: String?,
        duration: Int
    ): Flow<LyricsState> = flow {
        DevLogger.d(TAG, "=== LYRICS FETCH START ===")
        DevLogger.d(TAG, "Track ID: $trackId")
        DevLogger.d(TAG, "Artist: $artist")
        DevLogger.d(TAG, "Title: $title")
        DevLogger.d(TAG, "Album: $album")
        DevLogger.d(TAG, "Duration: ${duration}s")
        DevLogger.d(TAG, "Audio URI: $audioUri")

        emit(LyricsState.Loading)

        try {
            // Step 1: Check cache first
            DevLogger.d(TAG, "Step 1: Checking cache...")
            val cached = lyricsDao.getLyricsByTrackId(trackId)
            if (cached != null) {
                DevLogger.d(TAG, "✓ Found cached lyrics (source: ${cached.source})")
                val lyrics = cached.toDomainModel()
                val parsedLines = LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)
                DevLogger.d(TAG, "✓ Parsed ${parsedLines.size} lyric lines")
                emit(LyricsState.Success(lyrics, parsedLines))
                return@flow
            }
            DevLogger.d(TAG, "✗ No cached lyrics found")

            // Step 2: Try local .lrc file (highest priority offline source)
            DevLogger.d(TAG, "Step 2: Scanning for local .lrc file...")
            val localLrcContent = offlineLyricsScanner.findLrcFile(audioUri, title, artist)
            if (localLrcContent != null) {
                DevLogger.d(TAG, "✓ Found local .lrc file")
                val entity = LyricsEntity(
                    trackId = trackId,
                    syncedLyrics = localLrcContent,
                    plainLyrics = null,
                    syncOffset = 0,
                    source = com.sukoon.music.domain.model.LyricsSource.LOCAL_FILE.name,
                    lastFetched = System.currentTimeMillis()
                )
                lyricsDao.insertLyrics(entity)

                val lyrics = entity.toDomainModel()
                val parsedLines = LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)
                DevLogger.d(TAG, "✓ Cached local lyrics, parsed ${parsedLines.size} lines")
                emit(LyricsState.Success(lyrics, parsedLines))
                return@flow
            }
            DevLogger.d(TAG, "✗ No local .lrc file found")

            // Step 3: Try embedded lyrics from ID3 tags
            DevLogger.d(TAG, "Step 3: Extracting ID3 lyrics...")
            val (embeddedSynced, embeddedPlain) = id3LyricsExtractor.extractLyrics(audioUri)
            if (embeddedSynced != null || embeddedPlain != null) {
                DevLogger.d(TAG, "✓ Found embedded lyrics (synced: ${embeddedSynced != null}, plain: ${embeddedPlain != null})")
                val entity = LyricsEntity(
                    trackId = trackId,
                    syncedLyrics = embeddedSynced,
                    plainLyrics = embeddedPlain,
                    syncOffset = 0,
                    source = com.sukoon.music.domain.model.LyricsSource.EMBEDDED.name,
                    lastFetched = System.currentTimeMillis()
                )
                lyricsDao.insertLyrics(entity)

                val lyrics = entity.toDomainModel()
                val parsedLines = LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)
                DevLogger.d(TAG, "✓ Cached embedded lyrics, parsed ${parsedLines.size} lines")
                emit(LyricsState.Success(lyrics, parsedLines))
                return@flow
            }
            DevLogger.d(TAG, "✗ No embedded lyrics found")

            // Step 4: Fetch from LRCLIB - try precise lookup first
            DevLogger.d(TAG, "Step 4: Fetching from LRCLIB API...")
            DevLogger.d(TAG, "API Request: artist='$artist', track='$title', album='$album', duration=${duration}s")


            val cleanArtist = artist.trim().takeIf { it.isNotBlank() }
            val cleanTitle  = title.trim()
            val safeDuration = duration.takeIf { it > 0 }

            // Validation: Title is mandatory for any lookup
            if (cleanTitle.isNullOrBlank()) {
                DevLogger.w(TAG, "Cannot fetch lyrics: title is blank after normalization (original: '$title')")
                emit(LyricsState.NotFound)
                return@flow
            }

            val lyricsResponse = try {
                // Try precise lookup if we have artist metadata
                if (!cleanArtist.isNullOrBlank()) {
                    DevLogger.d(TAG, "Trying precise lookup with artist='$cleanArtist', title='$cleanTitle'")
                    val response = lyricsApi.getLyrics(
                        artistName = cleanArtist,
                        trackName = cleanTitle,
                        albumName = null,
                        duration = null
                    )
                    DevLogger.d(TAG, "✓ LRCLIB API success - precise lookup for artist='$cleanArtist', title='$cleanTitle' ")
                    response
                } else {
                    DevLogger.d(TAG, "Artist missing/invalid, going straight to search fallback")
                    null // Skip to search fallback
                }
            } catch (e: HttpException) {
                DevLogger.d(TAG, "✗ LRCLIB precise lookup failed: ${e.code()} ${e.message()}")
                if (safeDuration != null) {
                    // Retry without duration
                    lyricsApi.getLyrics(
                        artistName = cleanArtist,
                        trackName = cleanTitle,
                        albumName = null,
                        duration = null
                    )
                }
               /* if (e.code() == 404) {
                    // Step 5: Fallback to search if not found
                    DevLogger.d(TAG, "Step 5: Trying LRCLIB search fallback...")
                    try {
                        // Try with normalized values first, then fallback to original if needed
                        val searchQuery = when {
                            cleanArtist != null && cleanTitle != null -> "$cleanArtist $cleanTitle"
                            cleanTitle != null -> cleanTitle
                            else -> "$artist $title"
                        }
                        DevLogger.d(TAG, "Search query: '$searchQuery'")
                        val searchResults = lyricsApi.searchLyrics(searchQuery)
                        DevLogger.d(TAG, "✓ LRCLIB search returned ${searchResults.size} results")
                        searchResults.firstOrNull()
                    } catch (searchError: Exception) {
                        DevLogger.e(TAG, "✗ LRCLIB search failed", searchError)
                        null
                    }
                }*/ else {
                    DevLogger.e(TAG, "✗ LRCLIB API error", e)
                    throw e
                }
            } catch (e: IOException) {
                DevLogger.e(TAG, "✗ IOException fetching lyrics: ${e.message}", e)
                // Treat IO errors (connection drops, timeouts) as "not found" rather than fatal errors
                // This prevents showing error UI for transient network issues
                when (e) {
                    is SocketTimeoutException -> DevLogger.d(TAG, "Request timed out - treating as not found")
                    is UnknownHostException -> DevLogger.d(TAG, "No internet connection - treating as not found")
                    else -> DevLogger.d(TAG, "Network issue (${e.javaClass.simpleName}) - treating as not found")
                }
                null // Continue to "not found" handling below
            } catch (e: Exception) {
                DevLogger.e(TAG, "✗ Unexpected error fetching lyrics", e)
                throw e
            }

            // If no response yet, try search API as last resort
            val finalLyricsResponse = if (lyricsResponse == null) {
                DevLogger.d(TAG, "Precise lookup returned null, trying search API as final fallback...")
                try {
                    val searchQuery = if (!cleanArtist.isNullOrBlank()) {
                        "$cleanArtist $cleanTitle"
                    } else {
                        cleanTitle
                    }
                    DevLogger.d(TAG, "Final search query: '$searchQuery'")
                    val searchResults = lyricsApi.searchLyrics(searchQuery)
                    DevLogger.d(TAG, "✓ Final search returned ${searchResults.size} results")
                    searchResults.firstOrNull{
                        !it.syncedLyrics.isNullOrBlank() || !it.plainLyrics.isNullOrBlank()
                    }
                } catch (searchError: Exception) {
                    DevLogger.e(TAG, "✗ Final search failed", searchError)
                    null
                }
            } else {
                lyricsResponse
            }

            // Step 6: Gemini metadata correction (if enabled and API key configured)
            val geminiCorrectedResponse = if (finalLyricsResponse == null) {
                DevLogger.d(TAG, "Attempting Gemini metadata correction")

                val correctedMetadata = geminiMetadataCorrector.correctMetadata(
                    originalArtist = artist,
                    originalTitle = title,
                    originalAlbum = album
                )

                if (correctedMetadata != null) {
                    DevLogger.d(TAG, "Retrying LRCLIB with corrected metadata")

                    // Normalize corrected metadata (same as original flow)
                    val normalizedArtist = normalize(correctedMetadata.artist) ?: ""
                    val normalizedTitle = normalize(correctedMetadata.title) ?: ""
                    val normalizedAlbum = normalize(correctedMetadata.album)

                    try {
                        // Retry precise lookup with corrected metadata
                        val retryResponse = lyricsApi.getLyrics(
                            artistName = normalizedArtist,
                            trackName = normalizedTitle,
                            albumName = null,
                            duration = duration?.toInt()
                        )

                        // Success - return for caching
                        if (retryResponse.syncedLyrics != null || retryResponse.plainLyrics != null) {
                            DevLogger.d(TAG, "✓ LRCLIB retry with corrected metadata succeeded")
                            retryResponse
                        } else {
                            null
                        }
                    } catch (e: HttpException) {
                        DevLogger.d(TAG, "✗ LRCLIB retry with corrected metadata failed: ${e.code()}")
                        null
                    } catch (e: IOException) {
                        DevLogger.d(TAG, "✗ LRCLIB retry network error: ${e.message}")
                        null
                    }
                } else {
                    null
                }
            } else {
                finalLyricsResponse
            }

            if (geminiCorrectedResponse == null) {
                DevLogger.d(TAG, "✗ No lyrics found from any source")
                emit(LyricsState.NotFound)
                return@flow
            }

            // Validate that the response has usable lyrics
            val hasLyrics = !geminiCorrectedResponse.syncedLyrics.isNullOrBlank() ||
                           !geminiCorrectedResponse.plainLyrics.isNullOrBlank()

            if (!hasLyrics) {
                DevLogger.d(TAG, "✗ API returned response but lyrics fields are empty")
                emit(LyricsState.NotFound)
                return@flow
            }

            DevLogger.d(TAG, "✓ API returned valid lyrics (synced: ${!geminiCorrectedResponse.syncedLyrics.isNullOrBlank()}, plain: ${!geminiCorrectedResponse.plainLyrics.isNullOrBlank()})")

            // Step 7: Cache the result from online source
            DevLogger.d(TAG, "Step 7: Caching online lyrics...")
            val entity = LyricsEntity(
                trackId = trackId,
                syncedLyrics = geminiCorrectedResponse.syncedLyrics,
                plainLyrics = geminiCorrectedResponse.plainLyrics,
                syncOffset = 0,
                source = com.sukoon.music.domain.model.LyricsSource.ONLINE.name,
                lastFetched = System.currentTimeMillis()
            )
            lyricsDao.insertLyrics(entity)
            DevLogger.d(TAG, "✓ Cached online lyrics")

            // Step 7: Parse and emit
            val lyrics = entity.toDomainModel()
            val parsedLines = LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)
            DevLogger.d(TAG, "✓ Parsed ${parsedLines.size} lines from online lyrics")

            if (lyrics.syncedLyrics.isNullOrBlank() && lyrics.plainLyrics.isNullOrBlank()) {
                DevLogger.d(TAG, "✗ Online lyrics were empty")
                emit(LyricsState.NotFound)
            } else {
                DevLogger.d(TAG, "✓ SUCCESS - Emitting lyrics (synced: ${!lyrics.syncedLyrics.isNullOrBlank()}, plain: ${!lyrics.plainLyrics.isNullOrBlank()})")
                emit(LyricsState.Success(lyrics, parsedLines))
            }

        } catch (e: Exception) {
            DevLogger.e(TAG, "✗ FATAL ERROR in lyrics fetch", e)
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
                com.sukoon.music.domain.model.LyricsSource.valueOf(source)
            } catch (e: Exception) {
                com.sukoon.music.domain.model.LyricsSource.UNKNOWN
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
