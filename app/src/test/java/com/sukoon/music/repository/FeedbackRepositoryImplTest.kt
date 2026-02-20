package com.sukoon.music.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.data.repository.FeedbackRepositoryImpl
import com.sukoon.music.domain.model.FeedbackAttachment
import com.sukoon.music.domain.model.FeedbackCategory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedbackRepositoryImplTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var feedbackCollection: CollectionReference
    private lateinit var repository: FeedbackRepositoryImpl

    @Before
    fun setUp() {
        firestore = mockk(relaxed = true)
        storage = mockk(relaxed = true)
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        preferencesManager = mockk(relaxed = true)
        feedbackCollection = mockk(relaxed = true)

        every { context.contentResolver } returns contentResolver
        every { firestore.collection("feedback") } returns feedbackCollection
        coEvery { preferencesManager.getOrCreateAnonymousUserId() } returns "test-user"

        repository = FeedbackRepositoryImpl(
            firestore = firestore,
            storage = storage,
            context = context,
            preferencesManager = preferencesManager
        )
    }

    @Test
    fun `submitFeedback fails when attachment is not an image mime type`() = runTest {
        every { contentResolver.getType(any()) } returns "application/pdf"

        val result = repository.submitFeedback(
            category = FeedbackCategory.OTHER,
            details = "Not working",
            consentGiven = true,
            attachment = FeedbackAttachment(uri = "content://feedback/file.pdf")
        )

        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("Only image screenshots are supported", ignoreCase = true) == true
        )
        verify(exactly = 0) { feedbackCollection.add(any()) }
    }

    @Test
    fun `submitFeedback fails when attachment size is above 10MB`() = runTest {
        every { contentResolver.getType(any()) } returns "image/png"

        val result = repository.submitFeedback(
            category = FeedbackCategory.APP_SLOW_CRASHES,
            details = "Heavy screenshot",
            consentGiven = true,
            attachment = FeedbackAttachment(
                uri = "content://feedback/huge.png",
                fileName = "huge.png",
                sizeBytes = 11L * 1024L * 1024L
            )
        )

        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("too large", ignoreCase = true) == true
        )
        verify(exactly = 0) { feedbackCollection.add(any()) }
    }

    @Test
    fun `fetchDownloadUrlWithRetry returns null when storage reports object does not exist`() = runTest {
        val storageReference = mockk<StorageReference>(relaxed = true)
        every { storageReference.downloadUrl } returns Tasks.forException(
            Exception("Object does not exist at location.")
        )

        val result = repository.fetchDownloadUrlWithRetry(storageReference)

        assertTrue(result == null)
    }

    @Test
    fun `fetchDownloadUrlWithRetry returns uri when available`() = runTest {
        val storageReference = mockk<StorageReference>(relaxed = true)
        val expected = Uri.parse("https://example.com/screenshot.png")
        every { storageReference.downloadUrl } returns Tasks.forResult(expected)

        val result = repository.fetchDownloadUrlWithRetry(storageReference)

        assertTrue(result == expected)
    }
}
