# Firebase Remote Config for Ad Control - Design Document

**Date**: 2026-02-27
**Feature**: Dynamic ad control via Firebase Remote Config
**Status**: Approved for implementation

## Overview

Integrate Firebase Remote Config to dynamically control:
- Global enable/disable of ads (without app update)
- Per-placement ad visibility (HomeScreen, NowPlayingScreen)
- Ad unit IDs (enable A/B testing different AdMob units)
- Premium vs free user differentiation

## Design Goals

1. **Revenue control**: Enable/disable ads instantly without releasing new APK
2. **Premium differentiation**: Show ads only to free users, not premium subscribers
3. **Placement flexibility**: Show ads in some screens but not others
4. **A/B testing**: Swap ad unit IDs remotely to test different ad formats
5. **Offline resilience**: Cache Remote Config so app works offline

## Remote Config Parameters

All parameters stored in Firebase Console with defaults:

| Parameter | Type | Default | Purpose |
|-----------|------|---------|---------|
| `ads_enabled_free_users` | Boolean | `true` | Show ads to free tier users |
| `ads_enabled_premium_users` | Boolean | `false` | Show ads to premium users |
| `ads_placement_home_screen` | Boolean | `true` | Enable banner on HomeScreen |
| `ads_placement_now_playing` | Boolean | `false` | Enable overlay on NowPlayingScreen |
| `ads_banner_unit_id` | String | `ca-app-pub-3940256099942544/6300978111` | Banner ad unit ID |
| `ads_native_unit_id` | String | `ca-app-pub-3940256099942544/2247696110` | Native ad unit ID |

## Architecture

### Components

**RemoteConfigManager** (New)
- Singleton responsible for Firebase Remote Config lifecycle
- Fetches config on app startup (async, non-blocking)
- Caches in memory + DataStore for offline fallback
- Exposes `StateFlow<RemoteConfigState>` for UI observation
- Handles premium user context

**AdMobManager** (Modified)
- Accepts `RemoteConfigManager` as dependency
- Uses Remote Config values instead of hardcoded BuildConfig
- Provides helper: `shouldShowAds(placement: AdPlacement, isPremium: Boolean): Boolean`

**Ad Composables** (Modified)
- `BannerAdView`, `NativeAdCard`, `NowPlayingAdOverlay`
- Conditionally render based on `RemoteConfigManager.shouldShowAds()`

**MainActivity** (Modified)
- Initialize `RemoteConfigManager` on startup

### Data Flow

```
App Launch (MainActivity)
  ↓
RemoteConfigManager.initialize()
  ├─ Determine if current user is premium (via BillingClient or cached preference)
  ├─ Fetch from Firebase (async, 12hr minimum cache)
  ├─ On success: cache in DataStore
  ├─ On failure: use DataStore cached values or hardcoded defaults
  └─ Emit RemoteConfigState via StateFlow
  ↓
AdMobManager observes RemoteConfigState
  ├─ Updates ad unit IDs if changed
  └─ Notifies listeners of visibility changes
  ↓
HomeScreen observes should-show-ads
  ├─ Conditionally render BannerAdView
  └─ Pass RemoteConfigManager to BannerAdView
  ↓
NowPlayingScreen observes should-show-ads
  └─ Conditionally render NowPlayingAdOverlay
```

### Premium User Detection

1. **On app startup**: Query `BillingClient` for active premium purchases
2. **Cache locally**: Store in DataStore preference `is_premium_user`
3. **Update periodically**: Refresh whenever app enters foreground
4. **Pass to RemoteConfigManager**: Used to determine ad eligibility

### Ad Visibility Logic

```kotlin
fun shouldShowAds(placement: AdPlacement, isPremium: Boolean): Boolean {
    return when {
        !isAdsEnabledForUserTier(isPremium) → false
        !isPlacementEnabled(placement) → false
        else → true
    }
}

private fun isAdsEnabledForUserTier(isPremium: Boolean): Boolean {
    return if (isPremium) {
        remoteConfig.getBoolean("ads_enabled_premium_users")
    } else {
        remoteConfig.getBoolean("ads_enabled_free_users")
    }
}

private fun isPlacementEnabled(placement: AdPlacement): Boolean {
    return when (placement) {
        AdPlacement.HOME_SCREEN → remoteConfig.getBoolean("ads_placement_home_screen")
        AdPlacement.NOW_PLAYING → remoteConfig.getBoolean("ads_placement_now_playing")
    }
}
```

## Failure Handling (Smart Hybrid Strategy)

| Scenario | Behavior |
|----------|----------|
| **First install, no network** | Use hardcoded defaults, show ads |
| **Network available** | Fetch from Firebase, cache in DataStore |
| **Fetch fails, cached values exist** | Use last successful cached values |
| **Remote Config disables ads** | Respected even if subsequent fetches fail |
| **Offline after successful fetch** | Use cached values from last fetch |

## Implementation Files

### New Files
- `app/src/main/java/com/sukoon/music/data/config/RemoteConfigManager.kt`
- `app/src/main/java/com/sukoon/music/data/config/RemoteConfigState.kt`

### Modified Files
- `app/src/main/java/com/sukoon/music/data/ads/AdMobManager.kt`
- `app/src/main/java/com/sukoon/music/ui/components/BannerAdView.kt`
- `app/src/main/java/com/sukoon/music/ui/components/NowPlayingAdOverlay.kt`
- `app/src/main/java/com/sukoon/music/ui/MainActivity.kt`

## Testing Strategy

1. **Unit tests**: Mock RemoteConfigManager, test shouldShowAds() logic
2. **Integration tests**: Test caching behavior offline
3. **Manual testing**:
   - Fetch Remote Config successfully, verify ads show/hide correctly
   - Disable network, verify cached values are used
   - Set ads_enabled_free_users=false in Firebase, verify ads disappear
   - Test premium vs free user differentiation

## Firebase Console Setup

1. Create parameters in Firebase Console → Remote Config
2. Set defaults (see Remote Config Parameters table above)
3. (Optional) Create targeting rule: "Free users" → set ads_enabled_free_users=true
4. (Optional) Create targeting rule: "Premium users" → set ads_enabled_premium_users=false
5. Deploy configuration

## CLAUDE.md Compliance

- ✅ Maintains offline-only constraint (ads controlled locally after fetch)
- ✅ Respects "banner/native ads only" rule
- ✅ No changes to MediaSessionService
- ✅ Uses DataStore for persistence (existing pattern)
- ✅ Follows MVVM architecture
- ✅ Firebase already in dependencies

## Success Criteria

1. Remote Config fetched on app startup within 3 seconds (non-blocking)
2. Ad placement visibility controlled by Remote Config parameters
3. Premium users never see ads regardless of Remote Config
4. Offline users see cached config values (no crashes)
5. Ad unit IDs can be swapped via Remote Config
6. All ad placements respect Remote Config without app update
