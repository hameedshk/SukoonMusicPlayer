package com.sukoon.music.data.remote.dto

import kotlinx.serialization.Serializable
/**
 * Request DTOs for Gemini AI API.
 */
data class GeminiRequest(
    val contents: List<Content>,
    val tools: List<Tool>? = null, // For Google Search Grounding
    val generationConfig: GenerationConfig? = GenerationConfig()
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val temperature: Float = 0.3f,   // Balanced for complete responses
    val maxOutputTokens: Int = 200,  // Sufficient for 3 fields
    val topP: Float = 0.9f
)

@Serializable
data class Tool(
    val googleSearchRetrieval: GoogleSearchRetrieval? = null
)

@Serializable
data class GoogleSearchRetrieval(
    val dynamicRetrievalConfig: DynamicRetrievalConfig? = null
)

@Serializable
data class DynamicRetrievalConfig(
    val mode: String = "MODE_DYNAMIC",           // AI decides when to search
    val dynamicThreshold: Float = 0.3f           // Sensitivity of the search
)