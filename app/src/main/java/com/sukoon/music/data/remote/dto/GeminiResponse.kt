package com.sukoon.music.data.remote.dto

/**
 * Response DTOs for Gemini AI API.
 */
data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: ContentResponse?,
    val finishReason: String?
)

data class ContentResponse(
    val parts: List<PartResponse>?
)

data class PartResponse(
    val text: String?
)

/**
 * Parsed metadata from Gemini's response.
 * Represents corrected artist/title/album fields.
 */
data class CorrectedMetadata(
    val artist: String,
    val title: String,
    val album: String?
)
