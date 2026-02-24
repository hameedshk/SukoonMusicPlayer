package com.sukoon.music.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.sukoon.music.BuildConfig
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.data.remote.dto.FeedbackDocument
import com.sukoon.music.data.remote.dto.FeedbackSnapshot
import com.sukoon.music.domain.model.FeedbackAttachment
import com.sukoon.music.domain.model.FeedbackCategory
import com.sukoon.music.domain.repository.FeedbackRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
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
                    path = it.path,
                    downloadUrl = it.downloadUrl,
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
                e.message?.contains("quota", ignoreCase = true) == true ->
                    "Storage limit exceeded. Please try again later."
                e.message?.contains("permission", ignoreCase = true) == true ->
                    "Permission denied. Please check app permissions."
                e.message?.contains("not authorized", ignoreCase = true) == true ->
                    "Not authorized to submit feedback. Please sign in."
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
    ): UploadedAttachment {
        val uri = Uri.parse(attachment.uri)
        val resolver = context.contentResolver
        val mimeType = attachment.mimeType ?: resolver.getType(uri)
        if (mimeType.isNullOrBlank() || !mimeType.startsWith("image/")) {
            throw IllegalArgumentException("Only image screenshots are supported.")
        }

        // Validate file accessibility before upload
        val sizeBytes = attachment.sizeBytes ?: querySizeBytes(uri)
        if (sizeBytes == null || sizeBytes == 0L) {
            throw IllegalArgumentException("Cannot access the selected image. Please try selecting it again.")
        }
        if (sizeBytes > MAX_ATTACHMENT_BYTES) {
            throw IllegalArgumentException("Screenshot is too large. Please use an image under 10 MB.")
        }

        // Verify file is readable before attempting upload
        val isReadable = runCatching {
            resolver.openInputStream(uri)?.use { it.read() >= 0 }
        }.getOrNull() ?: false

        if (!isReadable) {
            throw IllegalArgumentException("Cannot read the selected image. The file may no longer be accessible. Please select it again.")
        }

        val extension = extensionForMimeType(mimeType)
        val fileName = attachment.fileName ?: queryDisplayName(uri) ?: "screenshot_${System.currentTimeMillis()}.$extension"
        val safeFileName = sanitizeFileName(fileName)
        val storagePath = "feedback/$userId/${System.currentTimeMillis()}_${UUID.randomUUID()}_$safeFileName"
        val storageRef = try {
            storage.reference.child(storagePath)
        } catch (e: Exception) {
            throw Exception("Firebase Storage not available. Please try again later.")
        }

        // Use putStream instead of putFile to read directly from ContentResolver
        // This avoids issues with file:// URIs and private cache directories
        val inputStream = resolver.openInputStream(uri) ?: throw Exception("Cannot read the image file. Please try selecting it again.")

        val uploadResult = withTimeoutOrNull(UPLOAD_TIMEOUT_MS) {
            try {
                val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                    .setContentType(mimeType ?: "image/jpeg") // Fallback to JPEG if type unknown
                    .build()
                awaitTask(storageRef.putStream(inputStream, metadata))
            } finally {
                inputStream.close()
            }
        } ?: throw Exception("Screenshot upload timed out. Please try again.")

        val downloadUri = fetchDownloadUrlWithRetry(storageRef)
        if (downloadUri == null) {
            throw Exception("Failed to process screenshot. Please try again.")
        }

        return UploadedAttachment(
            path = storagePath,
            downloadUrl = downloadUri.toString(),
            fileName = safeFileName,
            mimeType = mimeType,
            sizeBytes = uploadResult.metadata?.sizeBytes ?: sizeBytes
        )
    }

    internal suspend fun fetchDownloadUrlWithRetry(storageRef: StorageReference): Uri? {
        repeat(DOWNLOAD_URL_RETRY_COUNT) { attempt ->
            val result = withTimeoutOrNull(DOWNLOAD_URL_TIMEOUT_MS) {
                runCatching { awaitTask(storageRef.downloadUrl) }
            } ?: runCatching { throw Exception("Download URL request timed out.") }
            val downloadUri = result.getOrNull()
            if (downloadUri != null) return downloadUri

            val message = result.exceptionOrNull()?.message.orEmpty()
            val shouldRetry = message.contains("Object does not exist at location", ignoreCase = true)
            if (!shouldRetry || attempt == DOWNLOAD_URL_RETRY_COUNT - 1) return null

            // Exponential backoff with cap: 1s, 2s, 4s, 8s, 10s
            val delayMs = minOf((1L shl attempt) * 1000L, 10_000L)
            delay(delayMs)
        }
        return null
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
        val path: String,
        val downloadUrl: String,  // Always present: error thrown if fetch fails
        val fileName: String,
        val mimeType: String,
        val sizeBytes: Long?
    )

    companion object {
        private const val FIRESTORE_TIMEOUT_MS = 15_000L
        private const val UPLOAD_TIMEOUT_MS = 25_000L
        private const val DOWNLOAD_URL_TIMEOUT_MS = 5_000L
        private const val MAX_ATTACHMENT_BYTES = 10L * 1024L * 1024L
        private const val DOWNLOAD_URL_RETRY_COUNT = 5
    }
}
