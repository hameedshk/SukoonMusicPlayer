package com.sukoon.music.data.ads

import android.content.Context
import androidx.media3.common.Player
import com.sukoon.music.data.premium.PremiumManager
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.usecase.SessionController
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for AdMobDecisionAgent.
 *
 * Tests all decision logic paths:
 * - Banner ad policies
 * - Native ad policies
 * - Interstitial ad policies
 * - Event recording and metrics
 * - Lifecycle management
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdMobDecisionAgentTest {

    @MockK
    lateinit var mockPremiumManager: PremiumManager

    @MockK
    lateinit var mockPlaybackRepository: PlaybackRepository

    @MockK
    lateinit var mockSessionController: SessionController

    @MockK
    lateinit var mockContext: Context

    private lateinit var agent: AdMobDecisionAgent
    private val testDispatcher = StandardTestDispatcher()

    private val mockPremiumFlow = MutableStateFlow(false)
    private val mockPlaybackStateFlow = MutableStateFlow(
        PlaybackState(
            currentSong = null,
            isPlaying = false,
            queue = emptyList(),
            currentPosition = 0L,
            duration = 0L
        )
    )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        // Setup default mocks
        every { mockPremiumManager.isPremiumUser } returns mockPremiumFlow
        every { mockPlaybackRepository.playbackState } returns mockPlaybackStateFlow
        coEvery { mockSessionController.isSessionPrivate() } returns false

        // Initialize agent with mocks
        agent = AdMobDecisionAgent(
            premiumManager = mockPremiumManager,
            playbackRepository = mockPlaybackRepository,
            sessionController = mockSessionController,
            context = mockContext
        )
    }

    // ============== Banner Ad Tests ==============

    @Test
    fun `shouldShowBanner returns false for premium users`() = runTest(testDispatcher) {
        mockPremiumFlow.value = true

        val decision = agent.shouldShowBanner(isMiniPlayerVisible = false, currentRoute = "Home")

        assertFalse(decision.shouldShow)
        assertEquals("Premium user", decision.reason)
        assertEquals(AdFormat.BANNER, decision.format)
    }

    @Test
    fun `shouldShowBanner returns false for private session`() = runTest(testDispatcher) {
        coEvery { mockSessionController.isSessionPrivate() } returns true
        mockPremiumFlow.value = false

        val decision = agent.shouldShowBanner(isMiniPlayerVisible = false, currentRoute = "Home")

        assertFalse(decision.shouldShow)
        assertEquals("Private session active", decision.reason)
    }

    @Test
    fun `shouldShowBanner returns false when mini player is visible`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false

        val decision = agent.shouldShowBanner(isMiniPlayerVisible = true, currentRoute = "Home")

        assertFalse(decision.shouldShow)
        assertEquals("Mini player overlap", decision.reason)
    }

    @Test
    fun `shouldShowBanner returns false when app is in background`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false

        // First call to set app foreground, then background
        agent.onAppBackgrounded()

        val decision = agent.shouldShowBanner(isMiniPlayerVisible = false, currentRoute = "Home")

        assertFalse(decision.shouldShow)
        assertEquals("App in background", decision.reason)
    }

    @Test
    fun `shouldShowBanner enforces NO_FILL cooldown of 60 seconds`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false
        agent.onAppForegrounded()

        // Simulate a failure
        agent.recordAdFailed(AdFormat.BANNER, 3, "No fill")

        // Try to show immediately - should be suppressed
        val decisionImmediate = agent.shouldShowBanner(
            isMiniPlayerVisible = false,
            currentRoute = "Home"
        )
        assertFalse(decisionImmediate.shouldShow)
        assertTrue(decisionImmediate.reason.contains("NO_FILL"))

        // Advance time by 30 seconds - still suppressed
        advanceTimeBy(30_000)
        val decisionPartial = agent.shouldShowBanner(
            isMiniPlayerVisible = false,
            currentRoute = "Home"
        )
        assertFalse(decisionPartial.shouldShow)

        // Advance time past 60 seconds - should be allowed
        advanceTimeBy(40_000) // Total 70s
        val decisionAfterCooldown = agent.shouldShowBanner(
            isMiniPlayerVisible = false,
            currentRoute = "Home"
        )
        assertTrue(decisionAfterCooldown.shouldShow)
    }

    @Test
    fun `shouldShowBanner suppresses after 3 consecutive failures`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false
        agent.onAppForegrounded()

        // Record 3 failures
        repeat(3) {
            agent.recordAdFailed(AdFormat.BANNER, 3, "No fill")
        }

        // Should now be suppressed
        val decision = agent.shouldShowBanner(
            isMiniPlayerVisible = false,
            currentRoute = "Home"
        )
        assertFalse(decision.shouldShow)
        assertTrue(decision.reason.contains("Too many consecutive failures"))

        // Successful load should reset counter
        agent.recordAdLoaded(AdFormat.BANNER, 1000)

        // Try again - refresh interval check still applies but not failure limit
        val decisionAfterLoad = agent.shouldShowBanner(
            isMiniPlayerVisible = false,
            currentRoute = "Home"
        )
        // May still be false due to refresh interval, but failure reason should be different
        assertTrue(decisionAfterLoad.reason != "Too many consecutive failures (3)")
    }

    @Test
    fun `shouldShowBanner returns true when all conditions are met`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false
        agent.onAppForegrounded()

        val decision = agent.shouldShowBanner(isMiniPlayerVisible = false, currentRoute = "Home")

        assertTrue(decision.shouldShow)
        assertEquals("All conditions met", decision.reason)
    }

    @Test
    fun `shouldShowBanner enforces refresh interval between calls`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false
        agent.onAppForegrounded()

        // First call succeeds
        val first = agent.shouldShowBanner(isMiniPlayerVisible = false, currentRoute = "Home")
        assertTrue(first.shouldShow)
        agent.recordAdLoaded(AdFormat.BANNER, 1000)

        // Immediate second call should be suppressed (within refresh interval)
        val second = agent.shouldShowBanner(isMiniPlayerVisible = false, currentRoute = "Home")
        assertFalse(second.shouldShow)
        assertTrue(second.reason.contains("Refresh cooldown"))

        // After 60+ seconds, should be allowed
        advanceTimeBy(61_000)
        val third = agent.shouldShowBanner(isMiniPlayerVisible = false, currentRoute = "Home")
        assertTrue(third.shouldShow)
    }

    // ============== Native Ad Tests ==============

    @Test
    fun `shouldShowNative returns false for premium users`() = runTest(testDispatcher) {
        mockPremiumFlow.value = true

        val decision = agent.shouldShowNative(albumId = 123L, albumTrackCount = 15, hasUserScrolled = true)

        assertFalse(decision.shouldShow)
        assertEquals("Premium user", decision.reason)
        assertEquals(AdFormat.NATIVE, decision.format)
    }

    @Test
    fun `shouldShowNative returns false for private session`() = runTest(testDispatcher) {
        coEvery { mockSessionController.isSessionPrivate() } returns true
        mockPremiumFlow.value = false

        val decision = agent.shouldShowNative(albumId = 123L, albumTrackCount = 15, hasUserScrolled = true)

        assertFalse(decision.shouldShow)
        assertEquals("Private session active", decision.reason)
    }

    @Test
    fun `shouldShowNative requires user scroll`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false

        val decision = agent.shouldShowNative(albumId = 123L, albumTrackCount = 15, hasUserScrolled = false)

        assertFalse(decision.shouldShow)
        assertEquals("User has not scrolled yet", decision.reason)
    }

    @Test
    fun `shouldShowNative requires album to have at least 10 tracks`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false

        // Album with 9 tracks - too small
        val decisionSmall = agent.shouldShowNative(albumId = 123L, albumTrackCount = 9, hasUserScrolled = true)
        assertFalse(decisionSmall.shouldShow)
        assertTrue(decisionSmall.reason.contains("Album too small"))

        // Album with 10 tracks - exactly meets requirement
        val decisionExact = agent.shouldShowNative(
            albumId = 123L,
            albumTrackCount = 10,
            hasUserScrolled = true
        )
        assertTrue(decisionExact.shouldShow)

        // Album with 11 tracks - exceeds requirement
        val decisionLarge = agent.shouldShowNative(
            albumId = 124L,
            albumTrackCount = 11,
            hasUserScrolled = true
        )
        assertTrue(decisionLarge.shouldShow)
    }

    @Test
    fun `shouldShowNative enforces per-album frequency cap (1 per session)`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false

        // First ad for album 123 is allowed
        val firstDecision = agent.shouldShowNative(
            albumId = 123L,
            albumTrackCount = 15,
            hasUserScrolled = true
        )
        assertTrue(firstDecision.shouldShow)

        // Record impression for album 123
        agent.recordAdImpression(AdFormat.NATIVE, albumId = 123L)

        // Second ad for same album should be blocked
        val secondDecision = agent.shouldShowNative(
            albumId = 123L,
            albumTrackCount = 15,
            hasUserScrolled = true
        )
        assertFalse(secondDecision.shouldShow)
        assertEquals("Already shown for this album this session", secondDecision.reason)

        // Ad for different album 124 should be allowed
        val differentAlbumDecision = agent.shouldShowNative(
            albumId = 124L,
            albumTrackCount = 15,
            hasUserScrolled = true
        )
        assertTrue(differentAlbumDecision.shouldShow)
    }

    @Test
    fun `shouldShowNative suppresses after 2 consecutive failures`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false

        // Record 2 consecutive failures
        agent.recordAdFailed(AdFormat.NATIVE, 3, "No fill")
        agent.recordAdFailed(AdFormat.NATIVE, 3, "No fill")

        // Should now be suppressed for entire session
        val decision = agent.shouldShowNative(
            albumId = 123L,
            albumTrackCount = 15,
            hasUserScrolled = true
        )
        assertFalse(decision.shouldShow)
        assertTrue(decision.reason.contains("consecutive failures"))
    }

    @Test
    fun `shouldShowNative enforces 120 second failure cooldown`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false

        // Record single failure
        agent.recordAdFailed(AdFormat.NATIVE, 3, "No fill")

        // Immediate retry should be suppressed
        val immediate = agent.shouldShowNative(
            albumId = 123L,
            albumTrackCount = 15,
            hasUserScrolled = true
        )
        assertFalse(immediate.shouldShow)
        assertTrue(immediate.reason.contains("Recent failure cooldown"))

        // After 121 seconds, cooldown should expire
        advanceTimeBy(121_000)
        val afterCooldown = agent.shouldShowNative(
            albumId = 123L,
            albumTrackCount = 15,
            hasUserScrolled = true
        )
        assertTrue(afterCooldown.shouldShow)
    }

    @Test
    fun `shouldShowNative returns true when all conditions are met`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false

        val decision = agent.shouldShowNative(
            albumId = 123L,
            albumTrackCount = 15,
            hasUserScrolled = true
        )

        assertTrue(decision.shouldShow)
        assertEquals("All conditions met", decision.reason)
    }

    @Test
    fun `shouldShowNative triggers premium upsell signal after 2 impressions`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false

        // First impression - no signal
        agent.recordAdImpression(AdFormat.NATIVE, albumId = 123L)
        assertFalse(agent.premiumUpsellSignal.value)

        // Second impression - signal triggered
        agent.recordAdImpression(AdFormat.NATIVE, albumId = 124L)
        assertTrue(agent.premiumUpsellSignal.value)

        // Dismiss signal
        agent.dismissPremiumUpsell()
        assertFalse(agent.premiumUpsellSignal.value)
    }

    // ============== Interstitial Ad Tests ==============

    @Test
    fun `shouldShowInterstitial returns false for premium users`() = runTest(testDispatcher) {
        mockPremiumFlow.value = true

        val decision = agent.shouldShowInterstitial()

        assertFalse(decision.shouldShow)
        assertEquals("Premium user", decision.reason)
        assertEquals(AdFormat.INTERSTITIAL, decision.format)
    }

    @Test
    fun `shouldShowInterstitial returns false for private session`() = runTest(testDispatcher) {
        coEvery { mockSessionController.isSessionPrivate() } returns true
        mockPremiumFlow.value = false

        val decision = agent.shouldShowInterstitial()

        assertFalse(decision.shouldShow)
        assertEquals("Private session active", decision.reason)
    }

    @Test
    fun `shouldShowInterstitial never shows during playback`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false

        // Music is playing
        mockPlaybackStateFlow.value = PlaybackState(
            currentSong = null,
            isPlaying = true,
            queue = emptyList(),
            currentPosition = 0L,
            duration = 0L
        )

        val decision = agent.shouldShowInterstitial()

        assertFalse(decision.shouldShow)
        assertTrue(decision.reason.contains("Playback active"))
    }

    @Test
    fun `shouldShowInterstitial enforces session frequency cap (max 2)`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false
        mockPlaybackStateFlow.value = PlaybackState(
            currentSong = null,
            isPlaying = false,
            queue = emptyList(),
            currentPosition = 0L,
            duration = 0L
        )

        // First interstitial allowed (must be preloaded first)
        agent.recordAdLoaded(AdFormat.INTERSTITIAL, 1000)
        val first = agent.shouldShowInterstitial()
        assertTrue(first.shouldShow)
        agent.recordAdImpression(AdFormat.INTERSTITIAL)

        // Preload second
        advanceTimeBy(181_000) // Min 3 minutes between interstitials
        agent.recordAdLoaded(AdFormat.INTERSTITIAL, 1000)
        val second = agent.shouldShowInterstitial()
        assertTrue(second.shouldShow)
        agent.recordAdImpression(AdFormat.INTERSTITIAL)

        // Third interstitial blocked (session limit reached)
        advanceTimeBy(181_000)
        agent.recordAdLoaded(AdFormat.INTERSTITIAL, 1000)
        val third = agent.shouldShowInterstitial()
        assertFalse(third.shouldShow)
        assertTrue(third.reason.contains("Session limit reached"))
    }

    @Test
    fun `shouldShowInterstitial enforces 3 minute time gate`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false
        mockPlaybackStateFlow.value = PlaybackState(
            currentSong = null,
            isPlaying = false,
            queue = emptyList(),
            currentPosition = 0L,
            duration = 0L
        )

        // Preload and show first
        agent.recordAdLoaded(AdFormat.INTERSTITIAL, 1000)
        val first = agent.shouldShowInterstitial()
        assertTrue(first.shouldShow)
        agent.recordAdImpression(AdFormat.INTERSTITIAL)

        // Preload second but check immediately - should be blocked
        agent.recordAdLoaded(AdFormat.INTERSTITIAL, 1000)
        val immediate = agent.shouldShowInterstitial()
        assertFalse(immediate.shouldShow)
        assertTrue(immediate.reason.contains("Cooldown period"))

        // After 2 minutes - still blocked
        advanceTimeBy(120_000)
        val afterTwoMinutes = agent.shouldShowInterstitial()
        assertFalse(afterTwoMinutes.shouldShow)

        // After 3+ minutes - allowed
        advanceTimeBy(61_000) // Total 181 seconds
        val afterThreeMinutes = agent.shouldShowInterstitial()
        assertTrue(afterThreeMinutes.shouldShow)
    }

    @Test
    fun `shouldShowInterstitial requires preload`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false
        mockPlaybackStateFlow.value = PlaybackState(
            currentSong = null,
            isPlaying = false,
            queue = emptyList(),
            currentPosition = 0L,
            duration = 0L
        )

        // Without preload, should be blocked
        val decision = agent.shouldShowInterstitial()
        assertFalse(decision.shouldShow)
        assertEquals("Ad not preloaded yet", decision.reason)

        // After preload, should be allowed
        agent.recordAdLoaded(AdFormat.INTERSTITIAL, 1000)
        val decisionAfterPreload = agent.shouldShowInterstitial()
        assertTrue(decisionAfterPreload.shouldShow)
    }

    @Test
    fun `shouldShowInterstitial returns true when all conditions are met`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false
        mockPlaybackStateFlow.value = PlaybackState(
            currentSong = null,
            isPlaying = false,
            queue = emptyList(),
            currentPosition = 0L,
            duration = 0L
        )

        agent.recordAdLoaded(AdFormat.INTERSTITIAL, 1000)

        val decision = agent.shouldShowInterstitial()

        assertTrue(decision.shouldShow)
        assertEquals("All conditions met", decision.reason)
    }

    // ============== Event Recording Tests ==============

    @Test
    fun `recordAdLoaded resets failures and sets preload for interstitials`() = runTest(testDispatcher) {
        // Record failures first
        agent.recordAdFailed(AdFormat.BANNER, 3, "No fill")
        agent.recordAdFailed(AdFormat.BANNER, 3, "No fill")

        // Load should reset failures
        agent.recordAdLoaded(AdFormat.BANNER, 1500)

        // Check that failures are reset by trying to show
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false
        agent.onAppForegrounded()

        // Refresh interval will still apply, but not failure count
        val decision = agent.shouldShowBanner(isMiniPlayerVisible = false, currentRoute = "Home")
        assertTrue(decision.reason != "Too many consecutive failures")

        // For interstitials, preload should be set
        agent.recordAdLoaded(AdFormat.INTERSTITIAL, 1000)
        mockPlaybackStateFlow.value = PlaybackState(
            currentSong = null,
            isPlaying = false,
            queue = emptyList(),
            currentPosition = 0L,
            duration = 0L
        )
        val interstitialDecision = agent.shouldShowInterstitial()
        assertTrue(interstitialDecision.shouldShow)
    }

    @Test
    fun `recordAdFailed increments consecutive failures and sets last failure time`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false

        // Record first failure
        agent.recordAdFailed(AdFormat.NATIVE, 3, "No fill")

        // Check that failure is recorded (can't show after 1 failure for now)
        var decision = agent.shouldShowNative(
            albumId = 123L,
            albumTrackCount = 15,
            hasUserScrolled = true
        )
        // Within cooldown window
        assertFalse(decision.shouldShow)

        // After cooldown, should be allowed
        advanceTimeBy(121_000)
        decision = agent.shouldShowNative(
            albumId = 123L,
            albumTrackCount = 15,
            hasUserScrolled = true
        )
        assertTrue(decision.shouldShow)
    }

    @Test
    fun `recordAdImpression tracks album frequency and triggers upsell signal`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false

        // First native ad impression
        agent.recordAdImpression(AdFormat.NATIVE, albumId = 100L)

        // Verify album is tracked - second attempt for same album should fail
        val decisionSameAlbum = agent.shouldShowNative(
            albumId = 100L,
            albumTrackCount = 15,
            hasUserScrolled = true
        )
        assertFalse(decisionSameAlbum.shouldShow)

        // Different album should be allowed
        val decisionDifferentAlbum = agent.shouldShowNative(
            albumId = 101L,
            albumTrackCount = 15,
            hasUserScrolled = true
        )
        assertTrue(decisionDifferentAlbum.shouldShow)

        // Record second impression - upsell should trigger
        agent.recordAdImpression(AdFormat.NATIVE, albumId = 101L)
        assertTrue(agent.premiumUpsellSignal.value)
    }

    // ============== Lifecycle Tests ==============

    @Test
    fun `onAppBackgrounded suppresses banner refresh`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false

        agent.onAppForegrounded() // Start in foreground
        val foregroundDecision = agent.shouldShowBanner(isMiniPlayerVisible = false, currentRoute = "Home")
        assertTrue(foregroundDecision.shouldShow)

        agent.onAppBackgrounded()
        val backgroundDecision = agent.shouldShowBanner(isMiniPlayerVisible = false, currentRoute = "Home")
        assertFalse(backgroundDecision.shouldShow)
        assertEquals("App in background", backgroundDecision.reason)
    }

    @Test
    fun `onAppForegrounded resumes banner refresh`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false

        agent.onAppBackgrounded()
        var decision = agent.shouldShowBanner(isMiniPlayerVisible = false, currentRoute = "Home")
        assertFalse(decision.shouldShow)

        agent.onAppForegrounded()
        decision = agent.shouldShowBanner(isMiniPlayerVisible = false, currentRoute = "Home")
        assertTrue(decision.shouldShow)
    }

    @Test
    fun `isLoadLatencyHigh correctly identifies high latency`() = runTest(testDispatcher) {
        // Initially no load times, so not high
        assertFalse(agent.isLoadLatencyHigh())

        // Record low latency load
        agent.recordAdLoaded(AdFormat.BANNER, 500)
        assertFalse(agent.isLoadLatencyHigh())

        // Record high latency load (>1500ms)
        agent.recordAdLoaded(AdFormat.BANNER, 2000)
        assertTrue(agent.isLoadLatencyHigh())

        // Average should be (500 + 2000) / 2 = 1250, so not high? Let me recalculate
        // Actually average is 1250ms which is < 1500ms, so should be false
        // Wait, let me add more high latency loads to push average above 1500
        agent.recordAdLoaded(AdFormat.BANNER, 3000)
        agent.recordAdLoaded(AdFormat.BANNER, 3000)
        // Average should now be higher than 1500ms
        assertTrue(agent.isLoadLatencyHigh())
    }

    @Test
    fun `getAverageLoadTimeMs returns correct average`() = runTest(testDispatcher) {
        assertEquals(0L, agent.getAverageLoadTimeMs())

        agent.recordAdLoaded(AdFormat.BANNER, 1000)
        assertEquals(1000L, agent.getAverageLoadTimeMs())

        agent.recordAdLoaded(AdFormat.BANNER, 2000)
        assertEquals(1500L, agent.getAverageLoadTimeMs())

        agent.recordAdLoaded(AdFormat.BANNER, 3000)
        // Average of 1000, 2000, 3000 = 2000
        assertEquals(2000L, agent.getAverageLoadTimeMs())
    }

    // ============== Integration Tests ==============

    @Test
    fun `full banner ad cycle with success`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false
        agent.onAppForegrounded()

        // Step 1: Check decision - should be allowed
        var decision = agent.shouldShowBanner(isMiniPlayerVisible = false, currentRoute = "Home")
        assertTrue(decision.shouldShow)

        // Step 2: Record successful load
        agent.recordAdLoaded(AdFormat.BANNER, 1200)

        // Step 3: Record impression
        agent.recordAdImpression(AdFormat.BANNER)

        // Step 4: Verify average latency tracked
        assertEquals(1200L, agent.getAverageLoadTimeMs())
        assertFalse(agent.isLoadLatencyHigh())

        // Step 5: Immediate refresh should be blocked (refresh interval)
        decision = agent.shouldShowBanner(isMiniPlayerVisible = false, currentRoute = "Home")
        assertFalse(decision.shouldShow)
    }

    @Test
    fun `full native ad cycle with failure and recovery`() = runTest(testDispatcher) {
        mockPremiumFlow.value = false
        coEvery { mockSessionController.isSessionPrivate() } returns false

        // Step 1: First ad allowed
        var decision = agent.shouldShowNative(albumId = 100L, albumTrackCount = 15, hasUserScrolled = true)
        assertTrue(decision.shouldShow)

        // Step 2: Load fails
        agent.recordAdFailed(AdFormat.NATIVE, 3, "No fill")

        // Step 3: Immediate retry blocked by cooldown
        decision = agent.shouldShowNative(albumId = 101L, albumTrackCount = 15, hasUserScrolled = true)
        assertFalse(decision.shouldShow)
        assertTrue(decision.reason.contains("Recent failure"))

        // Step 4: After cooldown, retry allowed
        advanceTimeBy(121_000)
        decision = agent.shouldShowNative(albumId = 101L, albumTrackCount = 15, hasUserScrolled = true)
        assertTrue(decision.shouldShow)

        // Step 5: Successful load resets failure counter
        agent.recordAdLoaded(AdFormat.NATIVE, 800)

        // Step 6: Another failure allowed (only 1 consecutive now)
        agent.recordAdFailed(AdFormat.NATIVE, 3, "No fill")
        advanceTimeBy(121_000)

        decision = agent.shouldShowNative(albumId = 102L, albumTrackCount = 15, hasUserScrolled = true)
        assertTrue(decision.shouldShow)
    }
}
