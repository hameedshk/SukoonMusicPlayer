package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.data.analytics.AnalyticsTracker
import com.sukoon.music.domain.model.FeedbackAttachment
import com.sukoon.music.domain.model.FeedbackCategory
import com.sukoon.music.domain.model.FeedbackResult
import com.sukoon.music.domain.repository.FeedbackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val feedbackRepository: FeedbackRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _submitState = MutableStateFlow<FeedbackResult>(FeedbackResult.Idle)
    val submitState: StateFlow<FeedbackResult> = _submitState.asStateFlow()

    fun submitFeedback(
        category: FeedbackCategory,
        details: String,
        consentGiven: Boolean,
        attachment: FeedbackAttachment?
    ) {
        viewModelScope.launch {
            _submitState.value = FeedbackResult.Loading
            val result = feedbackRepository.submitFeedback(category, details, consentGiven, attachment)
            _submitState.value = if (result.isSuccess) {
                analyticsTracker.logEvent(
                    "feedback_submitted",
                    mapOf(
                        "category" to category.displayName,
                        "has_attachment" to (attachment != null)
                    )
                )
                FeedbackResult.Success
            } else {
                FeedbackResult.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun resetSubmitState() {
        _submitState.value = FeedbackResult.Idle
    }
}
