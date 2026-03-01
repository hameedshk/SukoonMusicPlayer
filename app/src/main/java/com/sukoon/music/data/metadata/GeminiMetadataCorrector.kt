package com.sukoon.music.data.metadata

import android.content.Context
import com.sukoon.music.BuildConfig
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.data.remote.GeminiApi
import com.sukoon.music.data.remote.dto.Content
import com.sukoon.music.data.remote.dto.CorrectedMetadata
import com.sukoon.music.data.remote.dto.GeminiRequest
import com.sukoon.music.data.remote.dto.GeminiResponse
import com.sukoon.music.data.remote.dto.GenerationConfig
import com.sukoon.music.data.remote.dto.Part
import com.sukoon.music.util.DevLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Corrects music metadata using Gemini AI.
 * CRITICAL: Only corrects metadata (artist/title/album) - NEVER generates lyrics.
 * PRIVACY: User must opt-in to AI metadata correction; requires internet connection.
 */
class GeminiMetadataCorrector @Inject constructor(
    private val geminiApi: GeminiApi,
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    private val tag = "GeminiMetadataCorrector"

    /**
     * Attempts to correct metadata using Gemini AI.
     * Returns null if correction fails or produces invalid output.
     */
    suspend fun correctMetadata(
        originalArtist: String,
        originalTitle: String,
        originalAlbum: String?
    ): CorrectedMetadata? {
        // PRIVACY: Check if user has opted in to AI metadata correction
        val userPrefs = preferencesManager.userPreferencesFlow.first()
        if (!userPrefs.aiMetadataCorrectionEnabled) {
            DevLogger.d(tag, "AI metadata correction disabled by user")
            return null
        }

        // Check if Gemini feature is enabled globally in BuildConfig
        if (!BuildConfig.ENABLE_GEMINI_METADATA_CORRECTION) {
            DevLogger.d(tag, "Gemini metadata correction is disabled via BuildConfig")
            return null
        }

        // Get API key from BuildConfig
        val apiKey = BuildConfig.GEMINI_API_KEY

        // Validate API key
        if (apiKey.isNullOrBlank()) {
            DevLogger.d(tag, "Gemini API key not configured in local.properties")
            return null
        }

        // Build prompt with strict constraints
        val prompt = buildPrompt(originalArtist, originalTitle, originalAlbum)

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                temperature = 0.1f,
                maxOutputTokens = 1000,
                topP = 0.8f
            )
        )

        return try {
            DevLogger.d(tag, "Requesting metadata correction for: $originalArtist - $originalTitle")

            val response = withContext(Dispatchers.IO) {
                geminiApi.generateContent(
                    model = BuildConfig.GEMINI_MODEL,
                    apiKey = apiKey,
                    request = request
                )
            }

            // Parse and validate response
            parseAndValidate(response, originalArtist, originalTitle, originalAlbum)

        } catch (e: HttpException) {
            DevLogger.e(tag, "Gemini HTTP error: ${e.code()} - ${e.message()}")
            null
        } catch (e: IOException) {
            DevLogger.e(tag, "Gemini network error: ${e.message}")
            null
        } catch (e: Exception) {
            DevLogger.e(tag, "Gemini unexpected error: ${e.message}")
            null
        }
    }

    private fun buildPrompt(
        artist: String,
        title: String,
        album: String?
    ): String {
        fun escapeJsonString(value: String): String {
            return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
        }

        val escapedArtist = escapeJsonString(artist)
        val escapedTitle = escapeJsonString(title)
        val albumJson = album?.let { "\"${escapeJsonString(it)}\"" } ?: "null"

        return """
        Task: Normalize music metadata.

        Rules:
        - Do NOT generate lyrics
        - Do NOT add explanations
        - Do NOT use markdown
        - Do NOT guess missing information
        - Only fix spelling, casing, and remove junk text
        - If unsure, keep the original value unchanged

        Respond ONLY with valid JSON in this exact schema:

        {
          "artist": "string",
          "title": "string",
          "album": "string or null"
        }

        Original metadata:
        {
          "artist": "$escapedArtist",
          "title": "$escapedTitle",
          "album": $albumJson
        }
    """.trimIndent()
    }

    private fun parseAndValidate(
        response: GeminiResponse,
        originalArtist: String,
        originalTitle: String,
        originalAlbum: String?
    ): CorrectedMetadata? {
        val text = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text

        if (text.isNullOrBlank()) {
            DevLogger.d(tag, "Gemini returned empty response")
            return null
        }

        // Validation: Check for lyrics generation (forbidden)
        if (text.contains("[0") && text.length > 200) {
            DevLogger.w(tag, "Gemini appears to have generated lyrics - rejecting")
            return null
        }

        val parsed = parseFromJson(text) ?: parseFromLabeledText(text)
        if (parsed == null) {
            DevLogger.d(tag, "Gemini response could not be parsed")
            return null
        }

        val correctedArtist = parsed.artist.trim()
        val correctedTitle = parsed.title.trim()
        val correctedAlbum = parsed.album?.trim()?.takeIf { it.isNotBlank() }

        if (correctedArtist.isBlank() || correctedTitle.isBlank()) {
            DevLogger.d(tag, "Gemini response missing required fields")
            return null
        }

        if (correctedArtist == originalArtist.trim() &&
            correctedTitle == originalTitle.trim() &&
            correctedAlbum == originalAlbum?.trim()) {
            DevLogger.d(tag, "Gemini returned identical metadata")
            return null
        }

        DevLogger.d(tag, "Metadata corrected: $correctedArtist - $correctedTitle")
        return CorrectedMetadata(
            artist = correctedArtist,
            title = correctedTitle,
            album = correctedAlbum
        )
    }

    private fun parseFromJson(rawText: String): CorrectedMetadata? {
        val jsonPayload = extractJsonObject(rawText) ?: return null
        return try {
            val json = JSONObject(jsonPayload)
            val artist = json.optString("artist", "").trim()
            val title = json.optString("title", "").trim()
            val album = if (json.has("album") && !json.isNull("album")) {
                json.optString("album", "").trim().takeIf { it.isNotBlank() }
            } else {
                null
            }

            if (artist.isBlank() || title.isBlank()) {
                null
            } else {
                CorrectedMetadata(artist = artist, title = title, album = album)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseFromLabeledText(text: String): CorrectedMetadata? {
        val artist = extractField("ARTIST", text)?.trim().orEmpty()
        val title = extractField("TITLE", text)?.trim().orEmpty()
        val album = extractField("ALBUM", text)
            ?.trim()
            ?.takeIf { !it.equals("UNKNOWN", ignoreCase = true) && it.isNotBlank() }

        if (artist.isBlank() || title.isBlank()) {
            return null
        }

        return CorrectedMetadata(artist = artist, title = title, album = album)
    }

    private fun extractJsonObject(text: String): String? {
        val cleaned = text
            .replace("```json", "", ignoreCase = true)
            .replace("```", "")
            .trim()

        if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
            return cleaned
        }

        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) {
            return null
        }

        return cleaned.substring(start, end + 1)
    }
}

private fun extractField(label: String, text: String): String? {
    val regex = Regex(
        pattern = "(?im)^\\s*\\*{0,2}$label\\*{0,2}\\s*[:\\-]\\s*(.+?)\\s*$"
    )
    return regex.find(text)?.groupValues?.getOrNull(1)?.trim()
}
