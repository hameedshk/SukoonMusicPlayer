package com.sukoon.music.data.metadata

import android.content.Context
import android.util.Log
import com.sukoon.music.BuildConfig
import com.sukoon.music.data.remote.GeminiApi
import com.sukoon.music.data.remote.dto.Content
import com.sukoon.music.data.remote.dto.CorrectedMetadata
import com.sukoon.music.data.remote.dto.GeminiRequest
import com.sukoon.music.data.remote.dto.GeminiResponse
import com.sukoon.music.data.remote.dto.GenerationConfig
import com.sukoon.music.data.remote.dto.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Corrects music metadata using Gemini AI.
 * CRITICAL: Only corrects metadata (artist/title/album) - NEVER generates lyrics.
 */
class GeminiMetadataCorrector @Inject constructor(
    private val geminiApi: GeminiApi,
    private val context: Context
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
        // Check if Gemini feature is enabled globally
        if (!BuildConfig.ENABLE_GEMINI_METADATA_CORRECTION) {
            Log.d(tag, "Gemini metadata correction is disabled via BuildConfig")
            return null
        }

        // Get API key from BuildConfig
        val apiKey = BuildConfig.GEMINI_API_KEY

        // Validate API key
        if (apiKey.isNullOrBlank()) {
            Log.d(tag, "Gemini API key not configured in local.properties")
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
                maxOutputTokens = 100,
                topP = 0.8f
            )
        )

        return try {
            Log.d(tag, "Requesting metadata correction for: $originalArtist - $originalTitle")

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
            Log.e(tag, "Gemini HTTP error: ${e.code()} - ${e.message()}")
            null
        } catch (e: IOException) {
            Log.e(tag, "Gemini network error: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(tag, "Gemini unexpected error: ${e.message}")
            null
        }
    }

    private fun buildPrompt(artist: String, title: String, album: String?): String {
        return """
            Task: Correct music metadata for lyrics lookup.

            CRITICAL RULES:
            - You MUST NOT generate or write lyrics
            - You MUST ONLY correct artist name, song title, and album name
            - Fix spelling errors, expand abbreviations, remove junk text
            - Respond ONLY in this exact format:

            ARTIST: [corrected artist name]
            TITLE: [corrected song title]
            ALBUM: [corrected album name or UNKNOWN]

            Original Metadata:
            Artist: $artist
            Title: $title
            Album: ${album ?: "UNKNOWN"}

            Provide corrected metadata now:
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
            Log.d(tag, "Gemini returned empty response")
            return null
        }

        Log.d(tag, "Gemini response: $text")

        // Parse structured output
        val artistMatch = Regex("ARTIST:\\s*(.+)").find(text)
        val titleMatch = Regex("TITLE:\\s*(.+)").find(text)
        val albumMatch = Regex("ALBUM:\\s*(.+)").find(text)

        val correctedArtist = artistMatch?.groupValues?.getOrNull(1)?.trim()
        val correctedTitle = titleMatch?.groupValues?.getOrNull(1)?.trim()
        val correctedAlbum = albumMatch?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it != "UNKNOWN" }

        // Validation: Must have artist and title
        if (correctedArtist.isNullOrBlank() || correctedTitle.isNullOrBlank()) {
            Log.d(tag, "Gemini response missing required fields")
            return null
        }

        // Validation: Must be different from original (otherwise no point)
        if (correctedArtist == originalArtist &&
            correctedTitle == originalTitle &&
            correctedAlbum == originalAlbum) {
            Log.d(tag, "Gemini returned identical metadata")
            return null
        }

        // Validation: Check for lyrics generation (forbidden)
        if (text.contains("[0") || text.contains(":") && text.length > 200) {
            Log.w(tag, "Gemini appears to have generated lyrics - rejecting")
            return null
        }

        Log.d(tag, "Metadata corrected: $correctedArtist - $correctedTitle")
        return CorrectedMetadata(
            artist = correctedArtist,
            title = correctedTitle,
            album = correctedAlbum
        )
    }
}
