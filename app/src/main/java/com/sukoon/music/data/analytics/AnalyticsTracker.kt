package com.sukoon.music.data.analytics

interface AnalyticsTracker {
    fun logScreenView(screenName: String, screenClass: String? = null)

    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())

    fun setUserProperty(name: String, value: String?)
}

