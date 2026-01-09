package com.sukoon.music.data.remote.dto

/**
 * Request DTOs for Gemini AI API.
 */
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = GenerationConfig()
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val temperature: Float = 0.1f,  // Low temp for consistent output
    val maxOutputTokens: Int = 100,  // Limit output (only need 3 fields)
    val topP: Float = 0.8f
)
