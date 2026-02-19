package com.sukoon.music.data.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionControllerImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var controller: SessionControllerImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        controller = SessionControllerImpl()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `session auto-expires after inactivity timeout`() = runTest {
        controller.startPrivateSession()
        assertTrue(controller.sessionState().first().isActive)

        advanceTimeBy(300_000L)
        runCurrent()

        assertFalse(controller.sessionState().first().isActive)
    }

    @Test
    fun `refreshing inactivity timer keeps session active until refreshed timeout elapses`() = runTest {
        controller.startPrivateSession()
        val firstStartTime = controller.sessionState().first().startedAtMs

        advanceTimeBy(240_000L)
        controller.refreshInactivityTimer()
        val refreshedStartTime = controller.sessionState().first().startedAtMs
        assertNotEquals(firstStartTime, refreshedStartTime)

        advanceTimeBy(70_000L)
        runCurrent()
        assertTrue(controller.sessionState().first().isActive)

        advanceTimeBy(230_000L)
        runCurrent()
        assertFalse(controller.sessionState().first().isActive)
    }
}
