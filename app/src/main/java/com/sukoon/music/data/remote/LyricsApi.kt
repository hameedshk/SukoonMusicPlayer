package com.sukoon.music.data.remote

import com.sukoon.music.data.remote.dto.LyricsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface LyricsApi {

    @GET("api/get")
    suspend fun getLyrics(
        @Query("artist_name") artistName: String,
        @Query("track_name") trackName: String,
        @Query("album_name") albumName: String? = null,
        @Query("duration") duration: Int? = null
    ): LyricsResponse

    @GET("api/search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): List<LyricsResponse>

    companion object {
        const val BASE_URL = "https://lrclib.net/"
    }
}
