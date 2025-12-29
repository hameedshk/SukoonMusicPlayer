package com.sukoon.music.data.repository

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

    override fun getLyrics(
        trackId: Long,
        audioUri: String,
        artist: String,
        title: String,
        album: String?,
        duration: Int
    ): Flow<LyricsState> = flow {
        emit(LyricsState.Loading)

        try {
            // Step 1: Check cache first
            val cached = lyricsDao.getLyricsByTrackId(trackId)
            if (cached != null) {
                val lyrics = cached.toDomainModel()
                val parsedLines = LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)
                emit(LyricsState.Success(lyrics, parsedLines))
                return@flow
            }

            // Step 2: Try local .lrc file (highest priority offline source)
            val localLrcContent = offlineLyricsScanner.findLrcFile(audioUri, title, artist)
            if (localLrcContent != null) {
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
                emit(LyricsState.Success(lyrics, parsedLines))
                return@flow
            }

            // Step 3: Try embedded lyrics from ID3 tags
            val (embeddedSynced, embeddedPlain) = id3LyricsExtractor.extractLyrics(audioUri)
            if (embeddedSynced != null || embeddedPlain != null) {
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
                emit(LyricsState.Success(lyrics, parsedLines))
                return@flow
            }

            // Step 4: Fetch from LRCLIB - try precise lookup first
            val lyricsResponse = try {
                lyricsApi.getLyrics(
                    artistName = artist,
                    trackName = title,
                    albumName = album,
                    duration = duration
                )
            } catch (e: HttpException) {
                if (e.code() == 404) {
                    // Step 3: Fallback to search if not found
                    val searchResults = lyricsApi.searchLyrics("$artist $title")
                    searchResults.firstOrNull()
                } else {
                    throw e
                }
            }

            if (lyricsResponse == null) {
                emit(LyricsState.NotFound)
                return@flow
            }

            // Step 5: Cache the result from online source
            val entity = LyricsEntity(
                trackId = trackId,
                syncedLyrics = lyricsResponse.syncedLyrics,
                plainLyrics = lyricsResponse.plainLyrics,
                syncOffset = 0,
                source = com.sukoon.music.domain.model.LyricsSource.ONLINE.name,
                lastFetched = System.currentTimeMillis()
            )
            lyricsDao.insertLyrics(entity)

            // Step 5: Parse and emit
            val lyrics = entity.toDomainModel()
            val parsedLines = LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)

            if (lyrics.syncedLyrics.isNullOrBlank() && lyrics.plainLyrics.isNullOrBlank()) {
                emit(LyricsState.NotFound)
            } else {
                emit(LyricsState.Success(lyrics, parsedLines))
            }

        } catch (e: Exception) {
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
