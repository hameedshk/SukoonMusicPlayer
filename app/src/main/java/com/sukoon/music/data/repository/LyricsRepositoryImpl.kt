package com.sukoon.music.data.repository

import android.util.Log
import com.sukoon.music.data.local.dao.LyricsDao
import com.sukoon.music.data.local.entity.LyricsEntity
import com.sukoon.music.data.lyrics.LrcParser
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
    private val id3LyricsExtractor: com.sukoon.music.data.lyrics.Id3LyricsExtractor
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
        Log.d(TAG, "=== LYRICS FETCH START ===")
        Log.d(TAG, "Track ID: $trackId")
        Log.d(TAG, "Artist: $artist")
        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Album: $album")
        Log.d(TAG, "Duration: ${duration}s")
        Log.d(TAG, "Audio URI: $audioUri")

        emit(LyricsState.Loading)

        try {
            // Step 1: Check cache first
            Log.d(TAG, "Step 1: Checking cache...")
            val cached = lyricsDao.getLyricsByTrackId(trackId)
            if (cached != null) {
                Log.d(TAG, "✓ Found cached lyrics (source: ${cached.source})")
                val lyrics = cached.toDomainModel()
                val parsedLines = LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)
                Log.d(TAG, "✓ Parsed ${parsedLines.size} lyric lines")
                emit(LyricsState.Success(lyrics, parsedLines))
                return@flow
            }
            Log.d(TAG, "✗ No cached lyrics found")

            // Step 2: Try local .lrc file (highest priority offline source)
            Log.d(TAG, "Step 2: Scanning for local .lrc file...")
            val localLrcContent = offlineLyricsScanner.findLrcFile(audioUri, title, artist)
            if (localLrcContent != null) {
                Log.d(TAG, "✓ Found local .lrc file")
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
                Log.d(TAG, "✓ Cached local lyrics, parsed ${parsedLines.size} lines")
                emit(LyricsState.Success(lyrics, parsedLines))
                return@flow
            }
            Log.d(TAG, "✗ No local .lrc file found")

            // Step 3: Try embedded lyrics from ID3 tags
            Log.d(TAG, "Step 3: Extracting ID3 lyrics...")
            val (embeddedSynced, embeddedPlain) = id3LyricsExtractor.extractLyrics(audioUri)
            if (embeddedSynced != null || embeddedPlain != null) {
                Log.d(TAG, "✓ Found embedded lyrics (synced: ${embeddedSynced != null}, plain: ${embeddedPlain != null})")
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
                Log.d(TAG, "✓ Cached embedded lyrics, parsed ${parsedLines.size} lines")
                emit(LyricsState.Success(lyrics, parsedLines))
                return@flow
            }
            Log.d(TAG, "✗ No embedded lyrics found")

            // Step 4: Fetch from LRCLIB - try precise lookup first
            Log.d(TAG, "Step 4: Fetching from LRCLIB API...")
            Log.d(TAG, "API Request: artist='$artist', track='$title', album='$album', duration=${duration}s")
            val cleanTitle = title
                .replace(Regex("\\s*-\\s*.*$"), "") // remove "- DJMaza.Life"
                .replace(Regex("\\(.*?\\)"), "")    // remove "(Official Video)"
                .trim()
            val safeDuration = duration.takeIf { it > 0 }
            val lyricsResponse = try {
                val response = lyricsApi.getLyrics(
                    artistName = artist,
                    trackName = cleanTitle,
                    albumName = album,
                    duration = safeDuration
                )
                Log.d(TAG, "✓ LRCLIB API success - precise lookup")
                response
            } catch (e: HttpException) {
                Log.d(TAG, "✗ LRCLIB precise lookup failed: ${e.code()} ${e.message()}")
                if (e.code() == 404) {
                    // Step 5: Fallback to search if not found
                    Log.d(TAG, "Step 5: Trying LRCLIB search fallback...")
                    try {
                        val searchResults = lyricsApi.searchLyrics("$artist $cleanTitle")
                        Log.d(TAG, "✓ LRCLIB search returned ${searchResults.size} results")
                        searchResults.firstOrNull()
                    } catch (searchError: Exception) {
                        Log.e(TAG, "✗ LRCLIB search failed", searchError)
                        null
                    }
                } else {
                    Log.e(TAG, "✗ LRCLIB API error", e)
                    throw e
                }
            } catch (e: IOException) {
                Log.e(TAG, "✗ IOException fetching lyrics: ${e.message}", e)
                // Treat IO errors (connection drops, timeouts) as "not found" rather than fatal errors
                // This prevents showing error UI for transient network issues
                when (e) {
                    is SocketTimeoutException -> Log.d(TAG, "Request timed out - treating as not found")
                    is UnknownHostException -> Log.d(TAG, "No internet connection - treating as not found")
                    else -> Log.d(TAG, "Network issue (${e.javaClass.simpleName}) - treating as not found")
                }
                null // Continue to "not found" handling below
            } catch (e: Exception) {
                Log.e(TAG, "✗ Unexpected error fetching lyrics", e)
                throw e
            }

            if (lyricsResponse == null) {
                Log.d(TAG, "✗ No lyrics found from any source")
                emit(LyricsState.NotFound)
                return@flow
            }

            // Validate that the response has usable lyrics
            val hasLyrics = !lyricsResponse.syncedLyrics.isNullOrBlank() ||
                           !lyricsResponse.plainLyrics.isNullOrBlank()

            if (!hasLyrics) {
                Log.d(TAG, "✗ API returned response but lyrics fields are empty")
                emit(LyricsState.NotFound)
                return@flow
            }

            Log.d(TAG, "✓ API returned valid lyrics (synced: ${!lyricsResponse.syncedLyrics.isNullOrBlank()}, plain: ${!lyricsResponse.plainLyrics.isNullOrBlank()})")

            // Step 6: Cache the result from online source
            Log.d(TAG, "Step 6: Caching online lyrics...")
            val entity = LyricsEntity(
                trackId = trackId,
                syncedLyrics = lyricsResponse.syncedLyrics,
                plainLyrics = lyricsResponse.plainLyrics,
                syncOffset = 0,
                source = com.sukoon.music.domain.model.LyricsSource.ONLINE.name,
                lastFetched = System.currentTimeMillis()
            )
            lyricsDao.insertLyrics(entity)
            Log.d(TAG, "✓ Cached online lyrics")

            // Step 7: Parse and emit
            val lyrics = entity.toDomainModel()
            val parsedLines = LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)
            Log.d(TAG, "✓ Parsed ${parsedLines.size} lines from online lyrics")

            if (lyrics.syncedLyrics.isNullOrBlank() && lyrics.plainLyrics.isNullOrBlank()) {
                Log.d(TAG, "✗ Online lyrics were empty")
                emit(LyricsState.NotFound)
            } else {
                Log.d(TAG, "✓ SUCCESS - Emitting lyrics (synced: ${!lyrics.syncedLyrics.isNullOrBlank()}, plain: ${!lyrics.plainLyrics.isNullOrBlank()})")
                emit(LyricsState.Success(lyrics, parsedLines))
            }

        } catch (e: Exception) {
            Log.e(TAG, "✗ FATAL ERROR in lyrics fetch", e)
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
