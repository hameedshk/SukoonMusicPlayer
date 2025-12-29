package com.sukoon.music.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LyricsResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("trackName") val trackName: String,
    @SerializedName("artistName") val artistName: String,
    @SerializedName("albumName") val albumName: String?,
    @SerializedName("duration") val duration: Int,
    @SerializedName("plainLyrics") val plainLyrics: String?,
    @SerializedName("syncedLyrics") val syncedLyrics: String?
)
