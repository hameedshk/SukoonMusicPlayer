package com.sukoon.music.data.remote.dto

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FeedbackSnapshot(
    val imageBase64: String = "",  // Base64 encoded compressed image (stored in Firestore)
    val fileName: String = "",
    val mimeType: String = "image/jpeg",
    val sizeBytes: Long? = null  // Original file size for logging
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
) {
    fun toFirestoreMap(timestampValue: Any): Map<String, Any?> {
        val snapshotMap = snapshot?.let {
            mapOf(
                "imageBase64" to it.imageBase64,
                "fileName" to it.fileName,
                "mimeType" to it.mimeType,
                "sizeBytes" to it.sizeBytes
            )
        }

        return mapOf(
            "userId" to userId,
            "category" to category,
            "details" to details,
            "appVersion" to appVersion,
            "androidVersion" to androidVersion,
            "deviceModel" to deviceModel,
            "timestamp" to timestampValue,
            "status" to status,
            "consentGiven" to consentGiven,
            "hasAttachment" to hasAttachment,
            "snapshot" to snapshotMap
        )
    }
}
