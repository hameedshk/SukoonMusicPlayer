package com.sukoon.music.data.remote.dto

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FeedbackSnapshot(
    val path: String = "",
    val downloadUrl: String? = null,
    val fileName: String = "",
    val mimeType: String = "",
    val sizeBytes: Long? = null
)

data class FeedbackDocument(
    val userId: String = "",
    val category: String = "",
    val details: String = "",
    val appVersion: String = "",
    val androidVersion: Int = 0,
    val deviceModel: String = "",
    @ServerTimestamp var timestamp: Date? = null,
    val status: String = "NEW",
    val consentGiven: Boolean = false,
    val hasAttachment: Boolean = false,
    val snapshot: FeedbackSnapshot? = null
)
