package com.sukoon.music.data.remote

import com.sukoon.music.data.remote.dto.GeminiRequest
import com.sukoon.music.data.remote.dto.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface for Google Gemini AI API.
 * Used for metadata correction only - NEVER for lyrics generation.
 */
interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/"
    }
}
