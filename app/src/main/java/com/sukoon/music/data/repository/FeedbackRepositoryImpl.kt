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

            val feedbackData = mutableMapOf<String, Any?>(
                "userId" to userId,
                "category" to category.displayName,
                "details" to details,
                "appVersion" to BuildConfig.VERSION_NAME,
                "androidVersion" to Build.VERSION.SDK_INT,
                "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "timestamp" to FieldValue.serverTimestamp(),
                "status" to "NEW",
                "consentGiven" to consentGiven,
                "hasAttachment" to (uploadedAttachment != null)
            )

            if (uploadedAttachment != null) {
                feedbackData["screenshotPath"] = uploadedAttachment.path
                uploadedAttachment.downloadUrl?.let { feedbackData["screenshotDownloadUrl"] = it }
                feedbackData["screenshotFileName"] = uploadedAttachment.fileName
                feedbackData["screenshotMimeType"] = uploadedAttachment.mimeType
                uploadedAttachment.sizeBytes?.let { feedbackData["screenshotSizeBytes"] = it }
            }

            val writeResult = withTimeoutOrNull(FIRESTORE_TIMEOUT_MS) {
                awaitTask(feedbackCollection.add(feedbackData))
            }
            if (writeResult == null) {
                Result.failure(Exception("Feedback submission timed out. Please try again."))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to submit feedback"))
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

        val sizeBytes = attachment.sizeBytes ?: querySizeBytes(uri)
        if (sizeBytes != null && sizeBytes > MAX_ATTACHMENT_BYTES) {
            throw IllegalArgumentException("Screenshot is too large. Please use an image under 10 MB.")
        }

        val extension = extensionForMimeType(mimeType)
        val fileName = attachment.fileName ?: queryDisplayName(uri) ?: "screenshot_${System.currentTimeMillis()}.$extension"
        val safeFileName = sanitizeFileName(fileName)
        val storagePath = "feedback/$userId/${System.currentTimeMillis()}_${UUID.randomUUID()}_$safeFileName"
        val storageRef = storage.reference.child(storagePath)

        val uploadResult = withTimeoutOrNull(UPLOAD_TIMEOUT_MS) {
            awaitTask(storageRef.putFile(uri))
        } ?: throw Exception("Screenshot upload timed out. Please try again.")

        val downloadUri = fetchDownloadUrlWithRetry(storageRef)

        return UploadedAttachment(
            path = storagePath,
            downloadUrl = downloadUri?.toString(),
            fileName = safeFileName,
            mimeType = mimeType,
            sizeBytes = uploadResult.metadata?.sizeBytes ?: sizeBytes
        )
    }

    internal suspend fun fetchDownloadUrlWithRetry(storageRef: StorageReference): Uri? {
        repeat(DOWNLOAD_URL_RETRY_COUNT) { attempt ->
            val result = withTimeoutOrNull(UPLOAD_TIMEOUT_MS) {
                runCatching { awaitTask(storageRef.downloadUrl) }
            } ?: runCatching { throw Exception("Download URL request timed out.") }
            val downloadUri = result.getOrNull()
            if (downloadUri != null) return downloadUri

            val message = result.exceptionOrNull()?.message.orEmpty()
            val shouldRetry = message.contains("Object does not exist at location", ignoreCase = true)
            if (!shouldRetry || attempt == DOWNLOAD_URL_RETRY_COUNT - 1) return null
            delay((attempt + 1) * DOWNLOAD_URL_RETRY_DELAY_MS)
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
        val downloadUrl: String?,
        val fileName: String,
        val mimeType: String,
        val sizeBytes: Long?
    )

    companion object {
        private const val FIRESTORE_TIMEOUT_MS = 15_000L
        private const val UPLOAD_TIMEOUT_MS = 25_000L
        private const val MAX_ATTACHMENT_BYTES = 10L * 1024L * 1024L
        private const val DOWNLOAD_URL_RETRY_COUNT = 3
        private const val DOWNLOAD_URL_RETRY_DELAY_MS = 400L
    }
}
