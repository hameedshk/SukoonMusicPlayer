package com.sukoon.music.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
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
    private val TAG = "FeedbackRepositoryImpl"

    override suspend fun submitFeedback(
        category: FeedbackCategory,
        details: String,
        consentGiven: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Submitting feedback: $category")

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

            Log.d(TAG, "Firebase data prepared, calling add()")

            val task = feedbackCollection.add(feedbackData)
            Log.d(TAG, "Firebase task created - isComplete: ${task.isComplete}, isSuccessful: ${task.isSuccessful}, isCanceled: ${task.isCanceled}")
            Log.d(TAG, "Firestore path: feedback collection")
            Log.d(TAG, "Waiting for Firebase listeners to fire or timeout...")

            // Wrap with timeout — suspendCancellableCoroutine ensures exception handling
            val timeoutResult = withTimeoutOrNull(15000L) {
                suspendCancellableCoroutine { continuation ->
                    Log.d(TAG, "Adding Firebase listeners, task state: ${task.isComplete}, isSuccessful: ${task.isSuccessful}")

                    task.addOnSuccessListener { documentRef ->
                        Log.d(TAG, "✓ Firebase task succeeded: ${documentRef.id}")
                        continuation.resume(true)
                    }
                    task.addOnFailureListener { exception ->
                        Log.e(TAG, "✗ Firebase task failed: ${exception.message}", exception)
                        continuation.resumeWithException(exception)
                    }
                    task.addOnCanceledListener {
                        Log.e(TAG, "⊘ Firebase task was canceled")
                        continuation.cancel()
                    }

                    Log.d(TAG, "Listeners attached, awaiting task completion or timeout...")
                }
            }

            if (timeoutResult == true) {
                Log.d(TAG, "Feedback submitted successfully")
                Result.success(Unit)
            } else {
                Log.e(TAG, "Timeout after 15 seconds waiting for Firebase")
                Result.failure(Exception("Firebase request timed out after 15 seconds"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "submitFeedback exception: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }
}
