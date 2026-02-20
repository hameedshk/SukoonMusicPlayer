package com.sukoon.music.viewmodel

import com.sukoon.music.data.analytics.AnalyticsTracker
import com.sukoon.music.domain.model.FeedbackAttachment
import com.sukoon.music.domain.model.FeedbackCategory
import com.sukoon.music.domain.model.FeedbackResult
import com.sukoon.music.domain.repository.FeedbackRepository
import com.sukoon.music.ui.viewmodel.FeedbackViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedbackViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var feedbackRepository: FeedbackRepository
    private lateinit var analyticsTracker: AnalyticsTracker
    private lateinit var viewModel: FeedbackViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        feedbackRepository = mockk(relaxed = true)
        analyticsTracker = mockk(relaxed = true)
        viewModel = FeedbackViewModel(feedbackRepository, analyticsTracker)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submitFeedback with attachment emits success and logs analytics payload`() = runTest {
        val attachment = FeedbackAttachment(uri = "content://feedback/screenshot.png")
        coEvery {
            feedbackRepository.submitFeedback(
                category = FeedbackCategory.FEATURE_REQUEST,
                details = "Please add XYZ",
                consentGiven = true,
                attachment = attachment
            )
        } returns Result.success(Unit)

        viewModel.submitFeedback(
            category = FeedbackCategory.FEATURE_REQUEST,
            details = "Please add XYZ",
            consentGiven = true,
            attachment = attachment
        )
        advanceUntilIdle()

        assertEquals(FeedbackResult.Success, viewModel.submitState.value)
        coVerify(exactly = 1) {
            feedbackRepository.submitFeedback(
                category = FeedbackCategory.FEATURE_REQUEST,
                details = "Please add XYZ",
                consentGiven = true,
                attachment = attachment
            )
        }
        verify(exactly = 1) {
            analyticsTracker.logEvent(
                "feedback_submitted",
                match {
                    it["category"] == FeedbackCategory.FEATURE_REQUEST.displayName &&
                        it["has_attachment"] == true
                }
            )
        }
    }

    @Test
    fun `submitFeedback failure emits error and does not log analytics`() = runTest {
        coEvery {
            feedbackRepository.submitFeedback(
                category = FeedbackCategory.APP_SLOW_CRASHES,
                details = "App crashes",
                consentGiven = true,
                attachment = null
            )
        } returns Result.failure(Exception("upload failed"))

        viewModel.submitFeedback(
            category = FeedbackCategory.APP_SLOW_CRASHES,
            details = "App crashes",
            consentGiven = true,
            attachment = null
        )
        advanceUntilIdle()

        val state = viewModel.submitState.value
        assertTrue(state is FeedbackResult.Error)
        assertEquals("upload failed", (state as FeedbackResult.Error).message)
        verify(exactly = 0) { analyticsTracker.logEvent(any(), any()) }
    }

    @Test
    fun `resetSubmitState returns state to idle`() = runTest {
        coEvery {
            feedbackRepository.submitFeedback(any(), any(), any(), any())
        } returns Result.success(Unit)

        viewModel.submitFeedback(
            category = FeedbackCategory.OTHER,
            details = "Some details",
            consentGiven = true,
            attachment = null
        )
        advanceUntilIdle()
        assertEquals(FeedbackResult.Success, viewModel.submitState.value)

        viewModel.resetSubmitState()
        assertEquals(FeedbackResult.Idle, viewModel.submitState.value)
    }
}
