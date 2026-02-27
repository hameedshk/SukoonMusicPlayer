package com.sukoon.music.data.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteConfigStateTest {

    @Test
    fun shouldShowAds_freeUserHomeScreen_enabled_returnsTrue() {
        val state = RemoteConfigState(
            adsEnabledForFreeUsers = true,
            adsPlacementHomeScreen = true
        )

        assertTrue(state.shouldShowAds(isPremiumUser = false, AdPlacement.HOME_SCREEN))
    }

    @Test
    fun shouldShowAds_freeUserButTierDisabled_returnsFalse() {
        val state = RemoteConfigState(
            adsEnabledForFreeUsers = false,
            adsPlacementHomeScreen = true
        )

        assertFalse(state.shouldShowAds(isPremiumUser = false, AdPlacement.HOME_SCREEN))
    }

    @Test
    fun shouldShowAds_freeUserButPlacementDisabled_returnsFalse() {
        val state = RemoteConfigState(
            adsEnabledForFreeUsers = true,
            adsPlacementHomeScreen = false
        )

        assertFalse(state.shouldShowAds(isPremiumUser = false, AdPlacement.HOME_SCREEN))
    }

    @Test
    fun shouldShowAds_premiumUserWithAllEnabled_returnsFalse() {
        val state = RemoteConfigState(
            adsEnabledForPremiumUsers = false,
            adsPlacementHomeScreen = true
        )

        assertFalse(state.shouldShowAds(isPremiumUser = true, AdPlacement.HOME_SCREEN))
    }

    @Test
    fun shouldShowAds_premiumUserEnabled_stillReturnsTrue() {
        val state = RemoteConfigState(
            adsEnabledForPremiumUsers = true,
            adsPlacementHomeScreen = true
        )

        assertTrue(state.shouldShowAds(isPremiumUser = true, AdPlacement.HOME_SCREEN))
    }

    @Test
    fun shouldShowAds_freeUserNowPlayingDisabled_returnsFalse() {
        val state = RemoteConfigState(
            adsEnabledForFreeUsers = true,
            adsPlacementNowPlaying = false
        )

        assertFalse(state.shouldShowAds(isPremiumUser = false, AdPlacement.NOW_PLAYING))
    }

    @Test
    fun shouldShowAds_freeUserNowPlayingEnabled_returnsTrue() {
        val state = RemoteConfigState(
            adsEnabledForFreeUsers = true,
            adsPlacementNowPlaying = true
        )

        assertTrue(state.shouldShowAds(isPremiumUser = false, AdPlacement.NOW_PLAYING))
    }

    @Test
    fun shouldShowAds_allDisabled_returnsFalse() {
        val state = RemoteConfigState(
            adsEnabledForFreeUsers = false,
            adsEnabledForPremiumUsers = false,
            adsPlacementHomeScreen = false,
            adsPlacementNowPlaying = false
        )

        assertFalse(state.shouldShowAds(isPremiumUser = false, AdPlacement.HOME_SCREEN))
        assertFalse(state.shouldShowAds(isPremiumUser = true, AdPlacement.HOME_SCREEN))
        assertFalse(state.shouldShowAds(isPremiumUser = false, AdPlacement.NOW_PLAYING))
        assertFalse(state.shouldShowAds(isPremiumUser = true, AdPlacement.NOW_PLAYING))
    }

    @Test
    fun shouldShowAds_allEnabled_multipleScenarios() {
        val state = RemoteConfigState(
            adsEnabledForFreeUsers = true,
            adsEnabledForPremiumUsers = true,
            adsPlacementHomeScreen = true,
            adsPlacementNowPlaying = true
        )

        assertTrue(state.shouldShowAds(isPremiumUser = false, AdPlacement.HOME_SCREEN))
        assertTrue(state.shouldShowAds(isPremiumUser = true, AdPlacement.HOME_SCREEN))
        assertTrue(state.shouldShowAds(isPremiumUser = false, AdPlacement.NOW_PLAYING))
        assertTrue(state.shouldShowAds(isPremiumUser = true, AdPlacement.NOW_PLAYING))
    }
}
