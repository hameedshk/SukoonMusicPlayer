package com.sukoon.music.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.tasks.await

/**
 * Helper class for Google Play In-App Review API.
 *
 * Handles the in-app review flow with graceful fallback to Play Store listing
 * if the API fails or quota is exceeded.
 */
class InAppReviewHelper(private val activity: Activity) {

    private val reviewManager = ReviewManagerFactory.create(activity)

    /**
     * Request an in-app review from the user.
     *
     * If the In-App Review API fails (e.g., quota exceeded), falls back to
     * opening the Play Store listing for the app.
     *
     * @return Result<Unit> indicating success or failure
     */
    suspend fun requestReview(): Result<Unit> = runCatching {
        val reviewInfo = reviewManager.requestReviewFlow().await()
        reviewManager.launchReviewFlow(activity, reviewInfo).await()
        Unit
    }.recoverCatching { error ->
        // Fallback: Open Play Store directly
        openPlayStoreListing()
        throw error // Re-throw to let caller know it was fallback
    }

    /**
     * Open the app's Play Store listing as a fallback.
     */
    private fun openPlayStoreListing() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=${activity.packageName}")
            // Fallback to browser if Play Store not installed
            if (resolveActivity(activity.packageManager) == null) {
                data = Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")
            }
        }
        activity.startActivity(intent)
    }
}
