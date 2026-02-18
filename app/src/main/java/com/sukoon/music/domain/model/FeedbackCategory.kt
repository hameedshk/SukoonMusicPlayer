package com.sukoon.music.domain.model

enum class FeedbackCategory(val displayName: String) {
    SONGS_MISSING("Songs missing"),
    PLAYBACK_PROBLEMS("Playback problems"),
    LYRICS_ISSUES("Lyrics issues"),
    AD_PROBLEMS("Ad problems"),
    APP_SLOW_CRASHES("App slow/crashes"),
    FEATURE_REQUEST("Feature request"),
    OTHER("Other")
}
