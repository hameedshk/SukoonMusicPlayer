package com.sukoon.music.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.sukoon.music.data.local.dao.LyricsDao
import com.sukoon.music.data.local.entity.LyricsEntity
import com.sukoon.music.data.lyrics.LrcParser
import com.sukoon.music.data.metadata.GeminiMetadataCorrector
import com.sukoon.music.data.remote.LyricsApi
import com.sukoon.music.data.remote.SecondaryLyricsApi
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
import kotlin.math.abs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of LyricsRepository following clean architecture.
 *
 * Offline-First + Manual Priority Strategy:
 * 1. Manual cache
 * 2. Regular cache
 * 3. Local .lrc file
 * 4. Embedded metadata lyrics
 * 5. LRCLIB (precise + search)
 * 6. Gemini-corrected LRCLIB retry
 * 7. Free no-auth fallback provider
 */
@Singleton
class LyricsRepositoryImpl @Inject constructor(
    private val context: Context,
    private val lyricsDao: LyricsDao,
    private val lyricsApi: LyricsApi,
    private val secondaryLyricsApi: SecondaryLyricsApi,
    private val offlineLyricsScanner: com.sukoon.music.data.lyrics.OfflineLyricsScanner,
    private val id3LyricsExtractor: com.sukoon.music.data.lyrics.Id3LyricsExtractor,
    private val geminiMetadataCorrector: GeminiMetadataCorrector
) : LyricsRepository {

    companion object {
        private const val TAG = "LyricsRepository"
        private const val RETRY_BACKOFF_MS = 350L
        private const val MIN_LYRICS_LENGTH = 8
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
            // 1) Manual cache first
            val cached = lyricsDao.getLyricsByTrackId(trackId)
            if (cached?.isManual == true) {
                DevLogger.d(TAG, "lyrics_stage=manual_hit trackId=$trackId")
                val lyrics = cached.toDomainModel()
                emit(LyricsState.Success(lyrics, LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)))
                return@flow
            }

            // 2) Regular cache
            if (cached != null) {
                DevLogger.d(TAG, "lyrics_stage=cache_hit trackId=$trackId source=${cached.source}")
                val lyrics = cached.toDomainModel()
                emit(LyricsState.Success(lyrics, LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)))
                return@flow
            }
            DevLogger.d(TAG, "lyrics_stage=cache_miss trackId=$trackId")

            // 3) Local .lrc
            val localLrc = offlineLyricsScanner.findLrcFile(audioUri, title, artist)
            if (!localLrc.isNullOrBlank()) {
                DevLogger.d(TAG, "lyrics_stage=local_hit trackId=$trackId")
                val entity = LyricsEntity(
                    trackId = trackId,
                    syncedLyrics = localLrc,
                    plainLyrics = null,
                    syncOffset = 0,
                    source = LyricsSource.LOCAL_FILE.name,
                    isManual = false,
                    manualUpdatedAt = null,
                    lastFetched = System.currentTimeMillis()
                )
                val cachedEntity = cacheAutoLyrics(entity)
                val lyrics = cachedEntity.toDomainModel()
                emit(LyricsState.Success(lyrics, LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)))
                return@flow
            }
            DevLogger.d(TAG, "lyrics_stage=local_miss trackId=$trackId")

            // 4) Embedded tags
            val (embeddedSynced, embeddedPlain) = id3LyricsExtractor.extractLyrics(audioUri)
            if (!embeddedSynced.isNullOrBlank() || !embeddedPlain.isNullOrBlank()) {
                DevLogger.d(TAG, "lyrics_stage=embedded_hit trackId=$trackId")
                val entity = LyricsEntity(
                    trackId = trackId,
                    syncedLyrics = embeddedSynced,
                    plainLyrics = embeddedPlain,
                    syncOffset = 0,
                    source = LyricsSource.EMBEDDED.name,
                    isManual = false,
                    manualUpdatedAt = null,
                    lastFetched = System.currentTimeMillis()
                )
                val cachedEntity = cacheAutoLyrics(entity)
                val lyrics = cachedEntity.toDomainModel()
                emit(LyricsState.Success(lyrics, LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)))
                return@flow
            }
            DevLogger.d(TAG, "lyrics_stage=embedded_miss trackId=$trackId")

            val normalizedArtist = normalize(artist.trim())
            val normalizedTitle = normalize(title.trim())
            val normalizedAlbum = normalize(album)
            val safeDuration = duration.takeIf { it > 0 }
            val rawArtist = artist.trim().takeIf { it.isNotBlank() }
            val rawTitle = title.trim()

            if (normalizedTitle.isNullOrBlank()) {
                DevLogger.d(TAG, "lyrics_stage=invalid_title trackId=$trackId")
                emit(LyricsState.NotFound)
                return@flow
            }

            // 5) LRCLIB lookup + search
            var finalResponse = fetchOnlineLyricsFromLrclib(
                artistName = normalizedArtist,
                trackName = normalizedTitle,
                albumName = normalizedAlbum,
                duration = safeDuration
            )
            var source = LyricsSource.ONLINE

            // Coverage fallback: retry LRCLIB with raw metadata when normalized query misses.
            if (!hasUsableLyrics(finalResponse) &&
                rawTitle.isNotBlank() &&
                (rawTitle != normalizedTitle || rawArtist != normalizedArtist)
            ) {
                finalResponse = fetchOnlineLyricsFromLrclib(
                    artistName = rawArtist,
                    trackName = rawTitle,
                    albumName = album?.trim(),
                    duration = safeDuration
                )
            }

            // 6) Gemini-corrected LRCLIB retry
            if (!hasUsableLyrics(finalResponse)) {
                finalResponse = tryGeminiCorrectedLookup(
                    originalArtist = artist,
                    originalTitle = title,
                    originalAlbum = album,
                    duration = safeDuration
                )
                if (hasUsableLyrics(finalResponse)) {
                    source = LyricsSource.ONLINE
                }
            }

            // 7) Free fallback provider
            if (!hasUsableLyrics(finalResponse)) {
                finalResponse = trySecondaryProvider(
                    artistName = normalizedArtist,
                    trackName = normalizedTitle
                )
                if (!hasUsableLyrics(finalResponse) && rawTitle.isNotBlank()) {
                    finalResponse = trySecondaryProvider(
                        artistName = rawArtist,
                        trackName = rawTitle
                    )
                }
                if (hasUsableLyrics(finalResponse)) {
                    source = LyricsSource.ONLINE_FALLBACK_FREE
                }
            }

            if (!hasUsableLyrics(finalResponse)) {
                DevLogger.d(TAG, "lyrics_stage=final_not_found trackId=$trackId")
                emit(LyricsState.NotFound)
                return@flow
            }

            val entity = LyricsEntity(
                trackId = trackId,
                syncedLyrics = finalResponse?.syncedLyrics,
                plainLyrics = finalResponse?.plainLyrics,
                syncOffset = 0,
                source = source.name,
                isManual = false,
                manualUpdatedAt = null,
                lastFetched = System.currentTimeMillis()
            )
            val cachedEntity = cacheAutoLyrics(entity)
            DevLogger.d(TAG, "lyrics_stage=online_cached trackId=$trackId source=${cachedEntity.source}")

            val lyrics = cachedEntity.toDomainModel()
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
        lyricsDao.deleteLyricsByTrackId(trackId)
    }

    override suspend fun saveManualLyrics(trackId: Long, syncedLyrics: String?, plainLyrics: String?) {
        val synced = syncedLyrics?.trim()?.takeIf { it.isNotBlank() }
        val plain = plainLyrics?.trim()?.takeIf { it.isNotBlank() }

        require(!(synced == null && plain == null)) { "Lyrics cannot be empty" }

        val existing = lyricsDao.getLyricsByTrackId(trackId)
        val now = System.currentTimeMillis()
        val entity = LyricsEntity(
            trackId = trackId,
            syncedLyrics = synced,
            plainLyrics = plain,
            syncOffset = existing?.syncOffset ?: 0L,
            source = LyricsSource.MANUAL.name,
            isManual = true,
            manualUpdatedAt = now,
            lastFetched = now
        )
        lyricsDao.insertLyrics(entity)
        DevLogger.d(TAG, "lyrics_stage=manual_save_success trackId=$trackId")
    }

    override suspend fun clearManualLyrics(trackId: Long) {
        val existing = lyricsDao.getLyricsByTrackId(trackId)
        if (existing?.isManual == true) {
            lyricsDao.deleteLyricsByTrackId(trackId)
            DevLogger.d(TAG, "lyrics_stage=manual_clear_success trackId=$trackId")
        }
    }

    override suspend fun importLyricsForTrack(trackId: Long, fileUri: String): Result<Unit> {
        return runCatching {
            val uri = Uri.parse(fileUri)
            val text = readTextFromUri(uri).trim()
            require(text.isNotBlank()) { "Imported file is empty" }

            val fileName = getDisplayName(uri).orEmpty()
            val isLrcFile = fileName.endsWith(".lrc", ignoreCase = true) || seemsLrc(text)

            if (isLrcFile) {
                saveManualLyrics(trackId = trackId, syncedLyrics = text, plainLyrics = null)
            } else {
                saveManualLyrics(trackId = trackId, syncedLyrics = null, plainLyrics = text)
            }
            DevLogger.d(TAG, "lyrics_stage=manual_import_success trackId=$trackId file='$fileName'")
        }.onFailure { e ->
            DevLogger.e(TAG, "lyrics_stage=manual_import_failed trackId=$trackId", e)
        }
    }

    private suspend fun fetchOnlineLyricsFromLrclib(
        artistName: String?,
        trackName: String,
        albumName: String?,
        duration: Int?
    ): LyricsResponse? {
        var attempt = 1
        while (attempt <= 2) {
            try {
                return fetchFromLrclibSingleAttempt(
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

    private suspend fun fetchFromLrclibSingleAttempt(
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
        if (hasUsableLyrics(precise) && hasMinimumContent(precise)) {
            return precise
        }

        val queries = buildSearchQueries(artistName, trackName)
        for (query in queries) {
            val searchResults = trySearchLookup(query)
            val searchHit = selectBestSearchCandidate(
                candidates = searchResults,
                queryArtist = artistName,
                queryTitle = trackName,
                queryDuration = duration
            )
            if (searchHit != null) {
                DevLogger.d(TAG, "lyrics_stage=remote_search_hit query='$query'")
                return searchHit
            }
        }

        DevLogger.d(TAG, "lyrics_stage=remote_search_miss track='$trackName'")
        return null
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

        val correctedResponse = fetchOnlineLyricsFromLrclib(
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

    private suspend fun trySecondaryProvider(
        artistName: String?,
        trackName: String
    ): LyricsResponse? {
        if (artistName.isNullOrBlank() || trackName.isBlank()) {
            DevLogger.d(TAG, "lyrics_stage=secondary_provider_skip_missing_metadata")
            return null
        }

        val variants = buildSecondaryProviderVariants(artistName, trackName)
        for ((artistVariant, titleVariant) in variants) {
            try {
                DevLogger.d(
                    TAG,
                    "lyrics_stage=secondary_provider_try artist='$artistVariant' track='$titleVariant'"
                )
                val response = secondaryLyricsApi.getLyrics(artist = artistVariant, title = titleVariant)
                val plain = response.lyrics?.trim().takeIf { !it.isNullOrBlank() }

                if (!plain.isNullOrBlank() && plain.length >= MIN_LYRICS_LENGTH) {
                    DevLogger.d(TAG, "lyrics_stage=secondary_provider_hit")
                    return LyricsResponse(
                        trackName = titleVariant,
                        artistName = artistVariant,
                        plainLyrics = plain,
                        syncedLyrics = null
                    )
                }
            } catch (e: HttpException) {
                DevLogger.d(TAG, "lyrics_stage=secondary_provider_http code=${e.code()}")
            } catch (e: IOException) {
                DevLogger.d(TAG, "lyrics_stage=secondary_provider_io ${e.javaClass.simpleName}")
            } catch (e: Exception) {
                DevLogger.e(TAG, "lyrics_stage=secondary_provider_unexpected", e)
            }
        }
        DevLogger.d(TAG, "lyrics_stage=secondary_provider_miss")
        return null
    }

    private fun selectBestSearchCandidate(
        candidates: List<LyricsResponse>,
        queryArtist: String?,
        queryTitle: String,
        queryDuration: Int?
    ): LyricsResponse? {
        val usableCandidates = candidates
            .filter { hasUsableLyrics(it) }
        if (usableCandidates.isEmpty()) return null

        val scored = usableCandidates
            .map { candidate -> candidate to candidateMatchScore(candidate, queryArtist, queryTitle, queryDuration) }

        scored.forEach { (candidate, score) ->
            if (score < minimumScoreThreshold(queryArtist)) {
                DevLogger.d(
                    TAG,
                    "lyrics_stage=candidate_rejected_low_confidence artist='${candidate.artistName}' title='${candidate.trackName}' score=$score"
                )
            }
        }

        val passing = scored
            .filter { (_, score) -> score >= minimumScoreThreshold(queryArtist) }
            .maxByOrNull { (_, score) -> score }
            ?.first

        if (passing != null) {
            return passing
        }

        // Coverage-first fallback: use best available candidate even if confidence is low.
        val fallback = scored.maxByOrNull { (_, score) -> score }?.first
        if (fallback != null) {
            DevLogger.d(
                TAG,
                "lyrics_stage=search_low_confidence_fallback artist='${fallback.artistName}' title='${fallback.trackName}'"
            )
            return fallback
        }
        return usableCandidates.firstOrNull()
    }

    private fun candidateMatchScore(
        candidate: LyricsResponse,
        queryArtist: String?,
        queryTitle: String,
        queryDuration: Int?
    ): Int {
        var score = 0

        val candidateTitle = normalizeForMatch(candidate.trackName)
        val targetTitle = normalizeForMatch(queryTitle)
        if (candidateTitle.isNotBlank() && targetTitle.isNotBlank()) {
            if (candidateTitle.contains(targetTitle) || targetTitle.contains(candidateTitle)) {
                score += 3
            } else if (tokenOverlapRatio(candidateTitle, targetTitle) >= 0.5f) {
                score += 2
            }
        }

        val targetArtist = normalizeForMatch(queryArtist)
        val candidateArtist = normalizeForMatch(candidate.artistName)
        if (targetArtist.isNotBlank() && candidateArtist.isNotBlank()) {
            if (candidateArtist.contains(targetArtist) || targetArtist.contains(candidateArtist)) {
                score += 3
            } else if (tokenOverlapRatio(candidateArtist, targetArtist) >= 0.4f) {
                score += 2
            }
        }

        if (queryDuration != null && candidate.duration != null) {
            val delta = abs(queryDuration - candidate.duration)
            when {
                delta <= 8 -> score += 1
                delta > 25 -> score -= 1
            }
        }

        return score
    }

    private fun minimumScoreThreshold(queryArtist: String?): Int {
        return 0
    }

    private fun buildSearchQueries(artistName: String?, trackName: String): List<String> {
        val titleClean = normalizeWhitespace(stripFeaturing(trackName))
        val artistClean = normalizeWhitespace(stripFeaturing(artistName.orEmpty()))
        val candidates = mutableListOf<String>()

        if (artistName.isNullOrBlank()) {
            candidates += trackName
            if (titleClean != trackName) candidates += titleClean
        } else {
            candidates += "$artistName $trackName"
            candidates += "$trackName $artistName"
            candidates += trackName
            if (titleClean != trackName) {
                candidates += "$artistName $titleClean"
                candidates += titleClean
            }
            if (artistClean.isNotBlank() && artistClean != artistName) {
                candidates += "$artistClean $trackName"
                if (titleClean != trackName) {
                    candidates += "$artistClean $titleClean"
                }
            }
        }

        return candidates
            .map { normalizeWhitespace(it) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun buildSecondaryProviderVariants(artistName: String, trackName: String): List<Pair<String, String>> {
        val primaryArtist = normalizeWhitespace(artistName)
        val cleanArtist = normalizeWhitespace(stripFeaturing(primaryArtist))
        val splitArtist = normalizeWhitespace(primaryArtist.split(",", "&", " and ").firstOrNull().orEmpty())
        val primaryTitle = normalizeWhitespace(trackName)
        val cleanTitle = normalizeWhitespace(stripFeaturing(primaryTitle))
        val parenRemovedTitle = normalizeWhitespace(primaryTitle.replace(Regex("\\s*[\\(\\[].*?[\\)\\]]\\s*"), " "))

        return listOf(
            primaryArtist to primaryTitle,
            cleanArtist to primaryTitle,
            primaryArtist to cleanTitle,
            cleanArtist to cleanTitle,
            splitArtist to cleanTitle,
            splitArtist to parenRemovedTitle
        ).map { (a, t) -> normalizeWhitespace(a) to normalizeWhitespace(t) }
            .filter { (a, t) -> a.isNotBlank() && t.isNotBlank() }
            .distinct()
    }

    private fun stripFeaturing(value: String): String {
        return value
            .replace(Regex("\\b(feat\\.?|ft\\.?|featuring)\\b.*$", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun normalizeWhitespace(value: String): String {
        return value.replace(Regex("\\s+"), " ").trim()
    }

    private fun tokenOverlapRatio(a: String, b: String): Float {
        val aTokens = a.split(" ").filter { it.isNotBlank() }.toSet()
        val bTokens = b.split(" ").filter { it.isNotBlank() }.toSet()
        if (aTokens.isEmpty() || bTokens.isEmpty()) return 0f
        val overlap = aTokens.intersect(bTokens).size.toFloat()
        return overlap / minOf(aTokens.size, bTokens.size)
    }

    private fun normalizeForMatch(text: String?): String {
        return text
            ?.lowercase()
            ?.replace(Regex("[^a-z0-9\\s]"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()
    }

    private fun hasMinimumContent(response: LyricsResponse?): Boolean {
        if (response == null) return false
        val totalLength = (response.syncedLyrics?.length ?: 0) + (response.plainLyrics?.length ?: 0)
        return totalLength >= MIN_LYRICS_LENGTH
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

    private suspend fun cacheAutoLyrics(entity: LyricsEntity): LyricsEntity {
        val existing = lyricsDao.getLyricsByTrackId(entity.trackId)
        if (existing?.isManual == true) {
            DevLogger.d(TAG, "lyrics_stage=manual_override_preserved trackId=${entity.trackId}")
            return existing
        }
        lyricsDao.insertLyrics(entity)
        return entity
    }

    private fun readTextFromUri(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: throw IllegalStateException("Unable to open file")
    }

    private fun getDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun seemsLrc(text: String): Boolean {
        return Regex("""\\[\\d{1,2}:\\d{2}(?:\\.\\d{1,2})?\\]""").containsMatchIn(text)
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
