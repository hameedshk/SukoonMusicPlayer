package com.sukoon.music.domain.model

data class Playlist(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val coverImageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val songCount: Int = 0
)
