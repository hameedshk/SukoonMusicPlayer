package com.sukoon.music.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import com.sukoon.music.BuildConfig
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.data.remote.dto.FeedbackDocument
import com.sukoon.music.data.remote.dto.FeedbackSnapshot
import com.sukoon.music.domain.model.FeedbackAttachment
import com.sukoon.music.domain.model.FeedbackCategory
import com.sukoon.music.domain.repository.FeedbackRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : FeedbackRepository {

    private val feedbackCollection = firestore.collection("feedback")

    override suspend fun submitFeedback(
        category: FeedbackCategory,
        details: String,
        consentGiven: Boolean,
        attachment: FeedbackAttachment?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = preferencesManager.getOrCreateAnonymousUserId()

            val uploadedAttachment = attachment?.let { uploadAttachment(userId, it) }

            val snapshot = uploadedAttachment?.let {
                FeedbackSnapshot(
                    imageBase64 = it.imageBase64,
                    fileName = it.fileName,
                    mimeType = it.mimeType,
                    sizeBytes = it.sizeBytes
                )
            }

            val feedbackData = FeedbackDocument(
                userId = userId,
                category = category.displayName,
                details = details,
                appVersion = BuildConfig.VERSION_NAME,
                androidVersion = Build.VERSION.SDK_INT,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                status = "NEW",
                consentGiven = consentGiven,
                hasAttachment = snapshot != null,
                snapshot = snapshot
            )
            val feedbackPayload = feedbackData.toFirestoreMap(
                timestampValue = FieldValue.serverTimestamp()
            )

            val writeResult = withTimeoutOrNull(FIRESTORE_TIMEOUT_MS) {
                awaitTask(feedbackCollection.add(feedbackPayload))
            }
            if (writeResult == null) {
                Result.failure(Exception("Feedback submission timed out. Please try again."))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("too large", ignoreCase = true) == true ->
                    e.message ?: "Image is too large. Please select a smaller image."
                e.message?.contains("permission", ignoreCase = true) == true ->
                    "Permission denied. Please check app permissions."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection and try again."
                e.message?.contains("timed out", ignoreCase = true) == true ->
                    "Request took too long. Please check your connection and try again."
                else -> e.message ?: "Failed to submit feedback. Please try again."
            }
            Result.failure(Exception(errorMessage))
        }
    }

    private suspend fun uploadAttachment(
        userId: String,
        attachment: FeedbackAttachment
    ): UploadedAttachment = withContext(Dispatchers.Default) {
        val uri = Uri.parse(attachment.uri)
        val resolver = context.contentResolver
        val mimeType = attachment.mimeType ?: resolver.getType(uri)

        if (mimeType.isNullOrBlank() || !mimeType.startsWith("image/")) {
            throw IllegalArgumentException("Only image screenshots are supported.")
        }

        // Read and validate original image
        val originalSizeBytes = attachment.sizeBytes ?: querySizeBytes(uri)

        if (originalSizeBytes == null || originalSizeBytes == 0L) {
            throw IllegalArgumentException("Cannot access the selected image. Please try selecting it again.")
        }
        if (originalSizeBytes > MAX_ATTACHMENT_BYTES) {
            throw IllegalArgumentException("Screenshot is too large. Please use an image under 10 MB.")
        }

        // Read image from URI
        val imageBytes = resolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: throw Exception("Cannot read the selected image. Please try selecting it again.")

        // Decode bitmap for compression
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw Exception("Failed to decode image. Please select a valid image.")

        // Compress to JPEG (quality 80) to reduce size
        val compressedBytes = ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
            output.toByteArray()
        }

        // Check compressed size is under 1MB (Firestore limit)
        if (compressedBytes.size > 1_000_000) {
            throw Exception("Compressed image is still too large (>${(compressedBytes.size / 1024 / 1024)}MB). Please select a smaller image.")
        }

        // Encode as Base64
        val imageBase64 = Base64.encodeToString(compressedBytes, Base64.DEFAULT)

        val extension = extensionForMimeType(mimeType)
        val fileName = attachment.fileName ?: queryDisplayName(uri) ?: "screenshot_${System.currentTimeMillis()}.$extension"
        val safeFileName = sanitizeFileName(fileName)

        return@withContext UploadedAttachment(
            imageBase64 = imageBase64,
            fileName = safeFileName,
            mimeType = "image/jpeg", // Always JPEG after compression
            sizeBytes = originalSizeBytes
        )
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun querySizeBytes(uri: Uri): Long? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun extensionForMimeType(mimeType: String): String {
        return when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
    }

    private fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(80)
            .ifBlank { "screenshot.jpg" }
    }

    private suspend fun <T> awaitTask(task: Task<T>): T {
        return suspendCancellableCoroutine { continuation ->
            task.addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result)
            }
            task.addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resumeWithException(exception)
            }
            task.addOnCanceledListener {
                if (continuation.isActive) continuation.cancel()
            }
        }
    }

    private data class UploadedAttachment(
        val imageBase64: String,
        val fileName: String,
        val mimeType: String,
        val sizeBytes: Long?
    )

    companion object {
        private const val FIRESTORE_TIMEOUT_MS = 15_000L
        private const val MAX_ATTACHMENT_BYTES = 10L * 1024L * 1024L  // Original file size limit
    }
}
