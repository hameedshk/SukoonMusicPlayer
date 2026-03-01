package com.sukoon.music.data.remote

import com.sukoon.music.data.remote.dto.SecondaryLyricsResponse
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Free no-auth fallback lyrics provider.
 *
 * Uses lyrics.ovh public API:
 * GET /v1/{artist}/{title}
 */
interface SecondaryLyricsApi {

    @GET("v1/{artist}/{title}")
    suspend fun getLyrics(
        @Path("artist") artist: String,
        @Path("title") title: String
    ): SecondaryLyricsResponse

    companion object {
        const val BASE_URL = "https://api.lyrics.ovh/"
    }
}

