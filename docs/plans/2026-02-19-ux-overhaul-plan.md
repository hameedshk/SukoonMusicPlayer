# Sukoon UX Overhaul Plan (Research-Backed, Decision-Complete)

## Summary
This plan redesigns the full app UX with a retention-first, expressive direction while keeping the current moderate IA model (tab-based home structure). It prioritizes clarity, premium feel, reduced friction, and less intrusive monetization. Delivery is phased for safety, but scope covers whole-app standards and all major surfaces.

## Deep-Dive Findings (Current State)
1. Visual system feels generic in key places.
2. Navigation discoverability is overloaded.
3. Monetization competes with core flow.
4. Localization and accessibility maturity is uneven.
5. Onboarding feels functional, not delightful.

## UX North Star
- Primary outcome: retention + delight.
- Style direction: expressive + dynamic, with calm readability.
- IA approach: moderate IA update (keep home tab model, improve hierarchy and wayfinding).
- Monetization: aggressive cleanup of interruptiveness.
- Delivery: phased release with KPIs.

## Public API / Interface / Type Changes
1. Add design-token interfaces.
   - `ui/theme/tokens/SpacingTokens`
   - `ui/theme/tokens/ShapeTokens`
   - `ui/theme/tokens/MotionTokens`
   - `ui/theme/tokens/ElevationTokens`
   - `ui/theme/tokens/TypeScaleTokens`
   - `SukoonDesignSystem` composition local
2. Add UX state and UI model standardization.
   - `ui/model/ScreenState.kt`: `Loading`, `Empty`, `Error`, `Content`
3. Navigation metadata extension.
   - Extend `HomeTabSpec` with `priority`, `shortLabel`, `analyticsName`, `badgeCount`
4. Monetization surface contract.
   - `domain/model/MonetizationSurfacePolicy`
5. Copy/localization contract.
   - migrate user-facing text to resources

## Full UX Redesign Spec (By Surface)
1. Onboarding
   - 2-card progressive flow, stronger hierarchy, micro-feedback
   - permission rationale expander
   - progress indicator
   - success transition to Home
2. Home + Tab Pill System
   - keep tab model
   - primary tabs always visible: Home, Songs, Playlists, Albums
   - secondary tabs in "More": Folders, Artists, Genres
   - contextual top bar subtitle/action states
   - personalized empty state with quick-start actions
3. Now Playing
   - reduce visual noise around critical controls
   - 3 clarity modes: Immersive, Focused Controls, Lyrics Focus
   - first-session gesture hinting
   - collapse lower-priority actions under a single "More" affordance
4. Search
   - segmented scopes: All, Songs, Albums, Artists
   - suggestions: recent + top entities
   - stronger no-results recovery actions
5. Settings
   - reorganize into Playback, Library and Privacy, Personalization, Support and Premium
   - one premium card per screen max
   - compact "Upcoming" section for coming-soon rows
6. Library/Detail Screens
   - unify row anatomy and action hierarchy
   - standardize headers, sorting/filter chips, empty states
   - consistent multi-select entry/exit behavior
7. Ads and Premium UX
   - remove persistent bottom banner from core discovery path
   - only non-intrusive inline placements at natural breaks
   - no ad during onboarding or playback-critical moments
   - contextual premium upsell at meaningful friction points
8. Motion and Haptics
   - motion tokens + duration caps by interaction class
   - state-change choreography only
   - haptics only for high-importance actions

## Implementation Plan (Phased, Decision-Complete)
1. Phase 1: Foundations
   - add token system and screen state models
   - reusable primitives: `SukoonScaffold`, `SukoonTopBar`, `SukoonEmptyState`, `SukoonActionRow`
   - UX analytics taxonomy
2. Phase 2: Core Journeys
   - Onboarding, Home, Now Playing, Search, Settings
   - localization migration for touched screens
   - monetization policy resolver for these screens
3. Phase 3: Library Surfaces
   - Songs, Albums, Artists, Folders, Genres, Playlists and detail/selection screens
   - unify multi-select UX and context menus
4. Phase 4: Stabilization
   - accessibility pass
   - performance pass
   - visual QA matrix
   - A/B guardrails for monetization changes

## Test Cases and Scenarios
1. UX behavior tests
   - primary tabs visible on compact screens
   - now playing controls one-thumb reachable
   - search-to-play in <=2 interactions from search view
2. Accessibility tests
   - actionable controls have labels
   - touch targets meet minimum size
   - contrast compliance for light/dark/amoled and accents
3. Localization tests
   - no hardcoded user-facing text in migrated screens
   - truncation/overflow checks for long strings
4. Monetization UX tests
   - no ads during onboarding or critical playback moments
   - frequency caps enforced
   - premium CTA only at allowed policy points
5. Performance tests
   - smooth long-list scroll
   - no transition jank on mid-tier devices
   - no startup/home render regressions

## Acceptance Criteria
1. Core journey task time reduced by at least 20 percent for play song, open now playing, search-and-play, and changing key settings.
2. User-reported clutter/confusion decreases in release feedback.
3. Retention and session depth improve without increasing accidental-ad interactions.
4. Accessibility audit passes for migrated screens.
5. Localization-ready architecture is enforced for all new UI copy.

## Assumptions and Defaults
1. Goal is retention + delight.
2. Aggressive cleanup of intrusive ad placements is allowed.
3. Scope is whole-app redesign blueprint.
4. Visual direction is expressive + dynamic.
5. IA change is moderate, not a full nav paradigm replacement.
6. Delivery is phased releases.

## Research Sources
- https://m3.material.io/foundations/understanding-navigation
- https://m3.material.io/styles/typography/overview
- https://developer.android.com/develop/ui/compose/accessibility/api-defaults
- https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes
- https://developer.android.com/guide/playcore/in-app-review
- https://support.google.com/admob/answer/6201362
