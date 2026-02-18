package com.sukoon.music.domain.model

sealed class FeedbackResult {
    data object Success : FeedbackResult()
    data class Error(val message: String) : FeedbackResult()
    data object Loading : FeedbackResult()
    data object Idle : FeedbackResult()
}
