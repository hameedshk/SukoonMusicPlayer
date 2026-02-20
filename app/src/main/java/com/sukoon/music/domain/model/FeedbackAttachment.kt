package com.sukoon.music.domain.model

data class FeedbackAttachment(
    val uri: String,
    val mimeType: String? = null,
    val fileName: String? = null,
    val sizeBytes: Long? = null
)
