package com.sukoon.music.data.config

/**
 * Represents the current state of Remote Config loaded from Firebase.
 *
 * @param adsEnabledForFreeUsers Whether ads should show for free tier users
 * @param adsEnabledForPremiumUsers Whether ads should show for premium tier users
 * @param adsPlacementHomeScreen Whether banner ad should show on HomeScreen
 * @param adsPlacementNowPlaying Whether ad should show on NowPlayingScreen
 * @param bannerAdUnitId AdMob banner ad unit ID (from Remote Config)
 * @param nativeAdUnitId AdMob native ad unit ID (from Remote Config)
 * @param lastFetchTimestamp Timestamp of last successful fetch (for debugging)
 * @param isLoading Whether Remote Config is currently fetching
 */
data class RemoteConfigState(
    val adsEnabledForFreeUsers: Boolean = true,
    val adsEnabledForPremiumUsers: Boolean = false,
    val adsPlacementHomeScreen: Boolean = true,
    val adsPlacementNowPlaying: Boolean = false,
    val bannerAdUnitId: String = "ca-app-pub-3940256099942544/6300978111",
    val nativeAdUnitId: String = "ca-app-pub-3940256099942544/2247696110",
    val lastFetchTimestamp: Long = 0,
    val isLoading: Boolean = false
) {
    /**
     * Determine if ads should be shown for the given user tier and placement.
     */
    fun shouldShowAds(isPremiumUser: Boolean, placement: AdPlacement): Boolean {
        val tierAllows = if (isPremiumUser) {
            adsEnabledForPremiumUsers
        } else {
            adsEnabledForFreeUsers
        }

        if (!tierAllows) return false

        val placementAllows = when (placement) {
            AdPlacement.HOME_SCREEN -> adsPlacementHomeScreen
            AdPlacement.NOW_PLAYING -> adsPlacementNowPlaying
        }

        return placementAllows
    }
}

enum class AdPlacement {
    HOME_SCREEN,
    NOW_PLAYING
}
