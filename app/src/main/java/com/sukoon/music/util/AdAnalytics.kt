package com.sukoon.music.util

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

/**
 * Firebase Analytics helper for ad tracking and optimization.
 * Logs all ad impressions, clicks, failures, and dismissals.
 */
object AdAnalytics {
    private const val TAG = "AdAnalytics"

    /**
     * Log successful ad impression (load and display).
     *
     * @param placement Ad placement type: "banner", "native", "now_playing"
     * @param screen Screen where ad appeared: "home", "albums", "artists", "now_playing"
     */
    fun logAdImpression(placement: String, screen: String) {
        val firebase = Firebase.analytics
        firebase.logEvent("ad_impression") {
            param("placement", placement.take(100))
            param("screen", screen.take(100))
        }
        DevLogger.d(TAG, "Ad impression: $placement on $screen")
    }

    /**
     * Log ad click event.
     *
     * @param placement Ad placement type: "banner", "native", "now_playing"
     */
    fun logAdClicked(placement: String) {
        val firebase = Firebase.analytics
        firebase.logEvent("ad_clicked") {
            param("placement", placement.take(100))
        }
        DevLogger.d(TAG, "Ad clicked: $placement")
    }

    /**
     * Log ad load failure.
     *
     * @param placement Ad placement type: "banner", "native", "now_playing"
     * @param errorCode AdMob error code
     */
    fun logAdFailed(placement: String, errorCode: Int) {
        val firebase = Firebase.analytics
        firebase.logEvent("ad_failed_to_load") {
            param("placement", placement.take(100))
            param("error_code", errorCode.toLong())
        }
        DevLogger.d(TAG, "Ad failed: $placement (code: $errorCode)")
    }

    /**
     * Log Now Playing overlay dismissal.
     *
     * @param autoDismiss true if auto-dismissed after 10s, false if user dismissed manually
     */
    fun logNowPlayingAdDismissed(autoDismiss: Boolean) {
        val firebase = Firebase.analytics
        firebase.logEvent("now_playing_ad_dismissed") {
            param("auto_dismiss", if (autoDismiss) 1L else 0L)
        }
        DevLogger.d(TAG, "Now Playing ad dismissed (auto: $autoDismiss)")
    }
}
