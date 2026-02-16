package com.sukoon.music.data.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import javax.inject.Inject

class FirebaseAnalyticsTracker @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) : AnalyticsTracker {

    override fun logScreenView(screenName: String, screenClass: String?) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            if (!screenClass.isNullOrBlank()) {
                param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }
        }
    }

    override fun logEvent(name: String, params: Map<String, Any?>) {
        firebaseAnalytics.logEvent(name) {
            params.forEach { (key, value) ->
                if (key.isBlank() || value == null) return@forEach
                when (value) {
                    is String -> param(key, value)
                    is Int -> param(key, value.toLong())
                    is Long -> param(key, value)
                    is Double -> param(key, value)
                    is Float -> param(key, value.toDouble())
                    is Boolean -> param(key, if (value) 1L else 0L)
                    else -> param(key, value.toString().take(100))
                }
            }
        }
    }

    override fun setUserProperty(name: String, value: String?) {
        if (name.isBlank()) return
        firebaseAnalytics.setUserProperty(name, value)
    }
}

