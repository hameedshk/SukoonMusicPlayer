package com.sukoon.music.data.repository

import android.content.Context
import android.os.Build
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.sukoon.music.BuildConfig
import com.sukoon.music.data.preferences.PreferencesManager
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
        consentGiven: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = preferencesManager.getOrCreateAnonymousUserId()
            val feedbackData = mapOf(
                "userId" to userId,
                "category" to category.displayName,
                "details" to details,
                "appVersion" to BuildConfig.VERSION_NAME,
                "androidVersion" to Build.VERSION.SDK_INT,
                "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "timestamp" to FieldValue.serverTimestamp(),
                "status" to "NEW",
                "consentGiven" to consentGiven
            )

            val task = feedbackCollection.add(feedbackData)

            // Wrap with timeout — suspendCancellableCoroutine ensures exception handling
            val timeoutResult = withTimeoutOrNull(15000L) {
                suspendCancellableCoroutine { continuation ->
                    task.addOnSuccessListener { _ ->
                        continuation.resume(true)
                    }
                    task.addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
                    task.addOnCanceledListener {
                        continuation.cancel()
                    }
                }
            }

            if (timeoutResult == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Firebase request timed out after 15 seconds"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
