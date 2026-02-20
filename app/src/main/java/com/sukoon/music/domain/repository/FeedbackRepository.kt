package com.sukoon.music.domain.repository

import com.sukoon.music.domain.model.FeedbackCategory
import com.sukoon.music.domain.model.FeedbackAttachment

interface FeedbackRepository {
    suspend fun submitFeedback(
        category: FeedbackCategory,
        details: String,
        consentGiven: Boolean,
        attachment: FeedbackAttachment?
    ): Result<Unit>
}
