package com.sukoon.music.domain.model

/**
 * Clean, normalized input for lyrics search.
 * This MUST be prepared before calling the repository.
 */
data class LyricsQuery(
    val trackId: Long,
    val audioUri: String,
    val artist: String?,
    val title: String,
    val durationSec: Int?
)
