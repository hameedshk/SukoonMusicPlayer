package com.sukoon.music.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.sukoon.music.data.local.dao.LyricsDao
import com.sukoon.music.data.local.entity.LyricsEntity
import com.sukoon.music.data.lyrics.LrcParser
import com.sukoon.music.data.metadata.GeminiMetadataCorrector
import com.sukoon.music.data.metadata.MetadataCorrectionResult
import com.sukoon.music.data.analytics.AnalyticsTracker
import com.sukoon.music.data.preferences.PreferencesManager
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.security.MessageDigest
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
    private val geminiMetadataCorrector: GeminiMetadataCorrector,
    private val preferencesManager: PreferencesManager,
    private val analyticsTracker: AnalyticsTracker
) : LyricsRepository {

    companion object {
        private const val TAG = "LyricsRepository"
        private const val RETRY_BACKOFF_MS = 350L
        private const val MIN_LYRICS_LENGTH = 8
        private const val EVENT_LYRICS_FETCH_STARTED = "lyrics_fetch_started"
        private const val EVENT_LYRICS_STAGE_RESULT = "lyrics_fetch_stage_result"
        private const val EVENT_LYRICS_FETCH_COMPLETED = "lyrics_fetch_completed"
        private const val EVENT_GEMINI_METADATA_RESULT = "gemini_metadata_result"
        private const val EVENT_GEMINI_QUOTA_LIMITED = "gemini_quota_limited"
        private const val EVENT_LYRICS_NOT_FOUND_DIAGNOSTICS = "lyrics_not_found_diagnostics"
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

        val startedAtMs = System.currentTimeMillis()
        val analyticsEnabled = runCatching { preferencesManager.analyticsEnabledFlow.first() }.getOrDefault(false)
        val trackFingerprint = hashForAnalytics("$trackId|$audioUri|$artist|$title")
        val stageAttempts = linkedSetOf<String>()

        logLyricsAnalytics(
            analyticsEnabled,
            EVENT_LYRICS_FETCH_STARTED,
            mapOf(
                "track_hash" to trackFingerprint,
                "has_artist" to artist.isNotBlank(),
                "has_album" to !album.isNullOrBlank(),
                "title_len_bucket" to lengthBucket(title.length)
            )
        )

        fun stageResult(stage: String, result: String, source: String? = null, extra: Map<String, Any?> = emptyMap()) {
            stageAttempts += stage
            logLyricsAnalytics(
                enabled = analyticsEnabled,
                name = EVENT_LYRICS_STAGE_RESULT,
                params = buildMap {
                    put("stage", stage)
                    put("result", result)
                    source?.let { put("source", it) }
                    putAll(extra)
                }
            )
        }

        fun completion(finalResult: String, finalSource: LyricsSource?) {
            val elapsedMs = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L)
            logLyricsAnalytics(
                enabled = analyticsEnabled,
                name = EVENT_LYRICS_FETCH_COMPLETED,
                params = mapOf(
                    "final_result" to finalResult,
                    "final_source" to finalSource?.name?.lowercase().orEmpty(),
                    "attempted_stage_count" to stageAttempts.size,
                    "duration_ms_bucket" to durationBucket(elapsedMs)
                )
            )
        }

        try {
            // 1) Manual cache first
            stageAttempts += "manual_cache"
            val cached = lyricsDao.getLyricsByTrackId(trackId)
            if (cached?.isManual == true) {
                DevLogger.d(TAG, "lyrics_stage=manual_hit trackId=$trackId")
                val lyrics = cached.toDomainModel()
                stageResult("manual_cache", "hit", source = cached.source.lowercase())
                completion("success", lyrics.source)
                emit(LyricsState.Success(lyrics, LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)))
                return@flow
            }
            stageResult("manual_cache", "miss")

            // 2) Regular cache
            stageAttempts += "cache"
            if (cached != null) {
                DevLogger.d(TAG, "lyrics_stage=cache_hit trackId=$trackId source=${cached.source}")
                val lyrics = cached.toDomainModel()
                stageResult("cache", "hit", source = cached.source.lowercase())
                completion("success", lyrics.source)
                emit(LyricsState.Success(lyrics, LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)))
                return@flow
            }
            stageResult("cache", "miss")
            DevLogger.d(TAG, "lyrics_stage=cache_miss trackId=$trackId")

            // 3) Local .lrc
            stageAttempts += "local"
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
                stageResult("local", "hit", source = cachedEntity.source.lowercase())
                completion("success", lyrics.source)
                emit(LyricsState.Success(lyrics, LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)))
                return@flow
            }
            stageResult("local", "miss")
            DevLogger.d(TAG, "lyrics_stage=local_miss trackId=$trackId")

            // 4) Embedded tags
            stageAttempts += "embedded"
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
                stageResult("embedded", "hit", source = cachedEntity.source.lowercase())
                completion("success", lyrics.source)
                emit(LyricsState.Success(lyrics, LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)))
                return@flow
            }
            stageResult("embedded", "miss")
            DevLogger.d(TAG, "lyrics_stage=embedded_miss trackId=$trackId")

            val normalizedArtist = normalizeArtist(artist.trim())
            val normalizedTitle = normalizeTrackTitle(title.trim())
            val normalizedAlbum = normalize(album)
            val safeDuration = duration.takeIf { it > 0 }
            val rawArtist = artist.trim().takeIf { it.isNotBlank() && !isPlaceholderArtist(it) }
            val rawTitle = title.trim()

            if (normalizedTitle.isNullOrBlank()) {
                DevLogger.d(TAG, "lyrics_stage=invalid_title trackId=$trackId")
                stageResult("input_validation", "invalid_title")
                logNotFoundDiagnostics(analyticsEnabled, stageAttempts)
                completion("not_found", null)
                emit(LyricsState.NotFound)
                return@flow
            }

            // 5) LRCLIB lookup + search
            stageAttempts += "lrclib_primary"
            var finalResponse = fetchOnlineLyricsFromLrclib(
                artistName = normalizedArtist,
                trackName = normalizedTitle,
                albumName = normalizedAlbum,
                duration = safeDuration
            )
            stageResult("lrclib_primary", if (hasUsableLyrics(finalResponse)) "hit" else "miss")
            var source = LyricsSource.ONLINE

            // Coverage fallback: retry LRCLIB with raw metadata when normalized query misses.
            if (!hasUsableLyrics(finalResponse) &&
                rawTitle.isNotBlank() &&
                (rawTitle != normalizedTitle || rawArtist != normalizedArtist)
            ) {
                stageAttempts += "lrclib_raw_retry"
                finalResponse = fetchOnlineLyricsFromLrclib(
                    artistName = rawArtist,
                    trackName = rawTitle,
                    albumName = album?.trim(),
                    duration = safeDuration
                )
                stageResult("lrclib_raw_retry", if (hasUsableLyrics(finalResponse)) "hit" else "miss")
            }

            // 6) Gemini-corrected LRCLIB retry
            if (!hasUsableLyrics(finalResponse)) {
                stageAttempts += "gemini"
                val geminiLookup = tryGeminiCorrectedLookup(
                    originalArtist = artist,
                    originalTitle = title,
                    originalAlbum = album,
                    duration = safeDuration,
                    analyticsEnabled = analyticsEnabled
                )
                finalResponse = geminiLookup.response
                stageResult("gemini", geminiLookup.stageResult, extra = geminiLookup.extraAnalytics)
                if (hasUsableLyrics(finalResponse)) {
                    source = LyricsSource.ONLINE
                }
            }

            // 7) Free fallback provider
            if (!hasUsableLyrics(finalResponse)) {
                stageAttempts += "secondary"
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
                stageResult("secondary", if (hasUsableLyrics(finalResponse)) "hit" else "miss")
                if (hasUsableLyrics(finalResponse)) {
                    source = LyricsSource.ONLINE_FALLBACK_FREE
                }
            }

            if (!hasUsableLyrics(finalResponse)) {
                DevLogger.d(TAG, "lyrics_stage=final_not_found trackId=$trackId")
                logNotFoundDiagnostics(analyticsEnabled, stageAttempts)
                completion("not_found", null)
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
            stageResult("online_cache_write", "hit", source = cachedEntity.source.lowercase())
            completion("success", lyrics.source)
            emit(LyricsState.Success(lyrics, LrcParser.parse(lyrics.syncedLyrics, lyrics.syncOffset)))
        } catch (e: Exception) {
            DevLogger.e(TAG, "lyrics_stage=fatal_error trackId=$trackId", e)
            stageResult(
                stage = "fatal",
                result = "error",
                extra = mapOf("error_type" to e.javaClass.simpleName.lowercase())
            )
            completion("error", null)
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

    private data class GeminiLookupResult(
        val response: LyricsResponse?,
        val stageResult: String,
        val extraAnalytics: Map<String, Any?> = emptyMap()
    )

    private suspend fun tryGeminiCorrectedLookup(
        originalArtist: String,
        originalTitle: String,
        originalAlbum: String?,
        duration: Int?,
        analyticsEnabled: Boolean
    ): GeminiLookupResult {
        DevLogger.d(TAG, "lyrics_stage=gemini_metadata_correction_start")

        val correctionResult = geminiMetadataCorrector.correctMetadata(
            originalArtist = originalArtist,
            originalTitle = originalTitle,
            originalAlbum = originalAlbum
        )

        val correctionType = correctionResultType(correctionResult)

        logLyricsAnalytics(
            analyticsEnabled,
            EVENT_GEMINI_METADATA_RESULT,
            mapOf(
                "result_type" to correctionType,
                "cooldown_active" to (correctionResult is MetadataCorrectionResult.QuotaLimited)
            )
        )

        return when (correctionResult) {
            is MetadataCorrectionResult.Corrected -> {
                val correctedArtist = normalizeArtist(correctionResult.metadata.artist)
                val correctedTitle = normalizeTrackTitle(correctionResult.metadata.title)
                val correctedAlbum = normalize(correctionResult.metadata.album)

                if (correctedTitle.isNullOrBlank()) {
                    DevLogger.d(TAG, "lyrics_stage=gemini_invalid_corrected_title")
                    GeminiLookupResult(
                        response = null,
                        stageResult = "miss",
                        extraAnalytics = mapOf("gemini_result_type" to "invalid_corrected_title")
                    )
                } else {
                    val correctedResponse = fetchOnlineLyricsFromLrclib(
                        artistName = correctedArtist,
                        trackName = correctedTitle,
                        albumName = correctedAlbum,
                        duration = duration
                    )

                    if (hasUsableLyrics(correctedResponse)) {
                        DevLogger.d(TAG, "lyrics_stage=gemini_retry_hit")
                        GeminiLookupResult(
                            response = correctedResponse,
                            stageResult = "hit",
                            extraAnalytics = mapOf("gemini_result_type" to correctionType)
                        )
                    } else {
                        DevLogger.d(TAG, "lyrics_stage=gemini_retry_miss")
                        GeminiLookupResult(
                            response = null,
                            stageResult = "miss",
                            extraAnalytics = mapOf("gemini_result_type" to correctionType)
                        )
                    }
                }
            }

            is MetadataCorrectionResult.QuotaLimited -> {
                logLyricsAnalytics(
                    analyticsEnabled,
                    EVENT_GEMINI_QUOTA_LIMITED,
                    mapOf(
                        "http_code" to correctionResult.httpCode,
                        "reason" to correctionResult.reason,
                        "cooldown_minutes" to ((correctionResult.cooldownUntilMs - System.currentTimeMillis()) / 60000L).coerceAtLeast(0L)
                    )
                )
                DevLogger.d(TAG, "lyrics_stage=gemini_quota_limited")
                GeminiLookupResult(
                    response = null,
                    stageResult = "skipped",
                    extraAnalytics = mapOf("gemini_result_type" to correctionType)
                )
            }

            else -> {
                DevLogger.d(TAG, "lyrics_stage=gemini_metadata_correction_miss type=$correctionType")
                GeminiLookupResult(
                    response = null,
                    stageResult = "miss",
                    extraAnalytics = mapOf("gemini_result_type" to correctionType)
                )
            }
        }
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
        val (bestCandidate, bestScore) = scored.maxByOrNull { (_, score) -> score } ?: return null

        val bestTitleNormalized = normalizeForMatch(bestCandidate.trackName)
        val targetTitleNormalized = normalizeForMatch(queryTitle)
        val hasTitleContainment = bestTitleNormalized.contains(targetTitleNormalized) ||
            targetTitleNormalized.contains(bestTitleNormalized)
        val titleOverlap = tokenOverlapRatio(bestTitleNormalized, targetTitleNormalized)

        // Guardrail: reject obviously wrong candidates when title overlap is extremely low.
        if (!hasTitleContainment && titleOverlap < 0.34f) {
            DevLogger.d(
                TAG,
                "lyrics_stage=candidate_rejected_low_confidence artist='${bestCandidate.artistName}' title='${bestCandidate.trackName}' score=$bestScore"
            )
            return null
        }

        if (bestScore <= 0) {
            DevLogger.d(
                TAG,
                "lyrics_stage=search_low_confidence_fallback artist='${bestCandidate.artistName}' title='${bestCandidate.trackName}'"
            )
        }
        return bestCandidate
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

    private fun buildSearchQueries(artistName: String?, trackName: String): List<String> {
        val titleClean = normalizeWhitespace(stripSearchNoise(stripFeaturing(trackName)))
        val titleNoBrackets = normalizeWhitespace(stripBracketedSegments(titleClean))
        val artistClean = normalizeWhitespace(stripFeaturing(artistName.orEmpty()))
        val candidates = mutableListOf<String>()

        if (artistName.isNullOrBlank()) {
            candidates += trackName
            if (titleClean != trackName) candidates += titleClean
            if (titleNoBrackets.isNotBlank() && titleNoBrackets != titleClean) candidates += titleNoBrackets
        } else {
            candidates += "$artistName $trackName"
            candidates += "$trackName $artistName"
            candidates += trackName
            if (titleClean != trackName) {
                candidates += "$artistName $titleClean"
                candidates += titleClean
            }
            if (titleNoBrackets.isNotBlank() && titleNoBrackets != titleClean) {
                candidates += "$artistName $titleNoBrackets"
                candidates += titleNoBrackets
            }
            if (artistClean.isNotBlank() && artistClean != artistName) {
                candidates += "$artistClean $trackName"
                if (titleClean != trackName) {
                    candidates += "$artistClean $titleClean"
                }
                if (titleNoBrackets.isNotBlank() && titleNoBrackets != titleClean) {
                    candidates += "$artistClean $titleNoBrackets"
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

    private fun normalizeArtist(artist: String?): String? {
        val normalized = normalize(artist)
        if (normalized.isNullOrBlank()) return null
        return normalized.takeUnless { isPlaceholderArtist(it) }
    }

    private fun normalizeTrackTitle(title: String?): String? {
        return normalize(stripSearchNoise(title.orEmpty()))
    }

    private fun stripFeaturing(value: String): String {
        return value
            .replace(Regex("\\b(feat\\.?|ft\\.?|featuring)\\b.*$", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun stripSearchNoise(value: String): String {
        return value
            .replace(Regex("\\b(official|video|audio|lyrics?|hd|4k|8k)\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\b(remastered|remaster|version|edit|mix)\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\b(full\\s+song|music\\s+video)\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun stripBracketedSegments(value: String): String {
        return value.replace(Regex("\\s*[\\(\\[].*?[\\)\\]]\\s*"), " ").replace(Regex("\\s+"), " ").trim()
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

    private fun isPlaceholderArtist(value: String): Boolean {
        val normalized = value.trim().lowercase()
        return normalized.isBlank() ||
            normalized == "unknown" ||
            normalized == "<unknown>" ||
            normalized == "unknown artist" ||
            normalized == "various artists" ||
            normalized == "va"
    }

    private fun logLyricsAnalytics(enabled: Boolean, name: String, params: Map<String, Any?> = emptyMap()) {
        if (!enabled) return
        analyticsTracker.logEvent(name = name, params = params)
    }

    private fun logNotFoundDiagnostics(enabled: Boolean, stageAttempts: Set<String>) {
        logLyricsAnalytics(
            enabled = enabled,
            name = EVENT_LYRICS_NOT_FOUND_DIAGNOSTICS,
            params = mapOf(
                "manual_attempted" to stageAttempts.contains("manual_cache"),
                "cache_attempted" to stageAttempts.contains("cache"),
                "local_attempted" to stageAttempts.contains("local"),
                "embedded_attempted" to stageAttempts.contains("embedded"),
                "lrclib_attempted" to stageAttempts.contains("lrclib_primary"),
                "lrclib_raw_attempted" to stageAttempts.contains("lrclib_raw_retry"),
                "gemini_attempted" to stageAttempts.contains("gemini"),
                "secondary_attempted" to stageAttempts.contains("secondary")
            )
        )
    }

    private fun correctionResultType(result: MetadataCorrectionResult): String {
        return when {
            result is MetadataCorrectionResult.Corrected -> "corrected"
            result is MetadataCorrectionResult.QuotaLimited -> "quota_limited"
            result == MetadataCorrectionResult.Unchanged -> "unchanged"
            result == MetadataCorrectionResult.Disabled -> "disabled"
            result == MetadataCorrectionResult.NotConfigured -> "not_configured"
            result == MetadataCorrectionResult.NetworkError -> "network_error"
            else -> "parse_error"
        }
    }

    private fun hashForAnalytics(input: String): String {
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashed = digest.digest(input.toByteArray(Charsets.UTF_8))
            hashed.joinToString("") { "%02x".format(it) }.take(12)
        }.getOrDefault(input.hashCode().toString())
    }

    private fun lengthBucket(length: Int): String {
        return when {
            length <= 0 -> "empty"
            length <= 16 -> "short"
            length <= 48 -> "medium"
            else -> "long"
        }
    }

    private fun durationBucket(durationMs: Long): String {
        return when {
            durationMs < 500 -> "lt_500ms"
            durationMs < 1500 -> "lt_1500ms"
            durationMs < 4000 -> "lt_4000ms"
            else -> "gte_4000ms"
        }
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

        // Remove common upload noise in track titles
        .replace(Regex("\\b(official|video|audio|lyrics?|hd|4k|8k)\\b", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("\\b(remastered|remaster|version|edit|mix)\\b", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("\\b(full\\s+song|music\\s+video)\\b", RegexOption.IGNORE_CASE), " ")

        // Remove file extensions if present
        .replace(Regex("\\.(mp3|m4a|flac|wav|ogg|aac|wma)$", RegexOption.IGNORE_CASE), "")

        // Remove standalone site names in parentheses (words with unusual caps or all caps)
        .replace(Regex("[\\(\\[](?:[A-Z][a-z]*){2,}[\\)\\]]"), "") // PagalWorld, DjMaza
        .replace(Regex("[\\(\\[][A-Z-]{4,}[\\)\\]]"), "") // MR-JATT, SONGS
        .replace(Regex("\\s*[\\(\\[].*?(official|lyrics?|audio|video|remaster).*?[\\)\\]]", RegexOption.IGNORE_CASE), " ")

        // Clean up extra whitespace and multiple spaces
        .replace(Regex("\\s+"), " ")
        .trim()
        .takeIf { it.isNotBlank() }
}
