package com.sukoon.music.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SecondaryLyricsResponse(
    @SerializedName("lyrics") val lyrics: String? = null,
    @SerializedName("error") val error: String? = null
)

