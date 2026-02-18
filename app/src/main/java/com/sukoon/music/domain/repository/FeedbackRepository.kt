package com.sukoon.music.domain.repository

import com.sukoon.music.domain.model.FeedbackCategory

interface FeedbackRepository {
    suspend fun submitFeedback(
        category: FeedbackCategory,
        details: String,
        consentGiven: Boolean
    ): Result<Unit>
}
