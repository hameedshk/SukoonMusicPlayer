package com.sukoon.music.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * LRCLIB API response DTO.
 *
 * All fields are nullable to handle partial responses gracefully.
 * The API may return incomplete data or error responses that don't match the full schema.
 */
data class LyricsResponse(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("trackName") val trackName: String? = null,
    @SerializedName("artistName") val artistName: String? = null,
    @SerializedName("albumName") val albumName: String? = null,
    @SerializedName("duration") val duration: Int? = null,
    @SerializedName("plainLyrics") val plainLyrics: String? = null,
    @SerializedName("syncedLyrics") val syncedLyrics: String? = null
)
