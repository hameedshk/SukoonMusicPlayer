# Settings Screen Reorder Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Reorganize SettingsScreen UI into 6 categorical sections (Listening, Go Premium, Music Library, Appearance, Notifications & Language, Help & About) without modifying business logic or state management.

**Architecture:** Pure UI layer reorganization. All state flows, callbacks, and dialogs remain unchanged. Six `SettingsGroupCard` items will be rearranged into the new category order with renamed group headers (currently no headers are shown in grouped cards).

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, existing SettingsViewModel, StateFlow bindings unchanged.

---

### Task 1: Create Listening Section Card

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/ui/screen/SettingsScreen.kt`

**Step 1: Identify current Listening items**
- Equalizer (line 396-401)
- Sleep Timer (line 220-224)
- Gapless Playback (line 364-376)
- Crossfade (line 377-382)
- Audio Buffer (line 358-363)
- Private Session (line 319-331)

**Step 2: Create new Listening SettingsGroupCard**

Add after Premium Banner (after line 179), before any other settings cards:

```kotlin
item {
    SettingsGroupCard(
        modifier = Modifier.padding(horizontal = SpacingLarge),
        rows = listOf(
            SettingsRowModel(
                icon = Icons.Default.Equalizer,
                title = stringResource(R.string.settings_screen_equalizer_title),
                value = stringResource(R.string.settings_screen_equalizer_description),
                onClick = onNavigateToEqualizer
            ),
            SettingsRowModel(
                icon = Icons.Default.Timer,
                title = stringResource(R.string.settings_screen_sleep_timer_title),
                value = getSleepTimerLabel(context, userPreferences.sleepTimerTargetTimeMs),
                onClick = { showSleepTimerDialog = true }
            ),
            SettingsRowModel(
                icon = Icons.Default.Speed,
                title = stringResource(R.string.settings_screen_gapless_playback_title),
                value = stringResource(R.string.settings_screen_gapless_playback_description),
                valuePlacement = ValuePlacement.Below,
                onClick = { viewModel.toggleGaplessPlayback() },
                trailingContent = {
                    Switch(
                        checked = userPreferences.gaplessPlaybackEnabled,
                        onCheckedChange = { viewModel.toggleGaplessPlayback() },
                    )
                }
            ),
            SettingsRowModel(
                icon = Icons.Default.AutoAwesome,
                title = stringResource(R.string.label_crossfade_duration),
                value = getCrossfadeLabel(context, userPreferences.crossfadeDurationMs),
                onClick = { showCrossfadeDialog = true }
            ),
            SettingsRowModel(
                icon = Icons.Default.Storage,
                title = stringResource(R.string.settings_screen_audio_buffer_title),
                value = stringResource(R.string.settings_screen_duration_ms_value, userPreferences.audioBufferMs),
                onClick = { showBufferDialog = true }
            ),
            SettingsRowModel(
                icon = Icons.Default.VisibilityOff,
                title = stringResource(R.string.settings_screen_private_session_title),
                valuePlacement = ValuePlacement.Below,
                onClick = null,
                trailingContent = {
                    Switch(
                        checked = userPreferences.isPrivateSessionEnabled,
                        onCheckedChange = { viewModel.togglePrivateSession() },
                    )
                }
            )
        )
    )
}
```

**Step 3: Run build to verify no compilation errors**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 2: Create Go Premium Section Card

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/ui/screen/SettingsScreen.kt`

**Step 1: Extract Remove Ads item and create Go Premium card**

Add after Listening card (after Task 1):

```kotlin
item {
    SettingsGroupCard(
        modifier = Modifier.padding(horizontal = SpacingLarge),
        rows = listOf(
            SettingsRowModel(
                icon = Icons.Default.WorkspacePremium,
                title = stringResource(R.string.settings_screen_remove_ads_title),
                value = if (isPremium) {
                    stringResource(R.string.settings_screen_premium_active)
                } else {
                    stringResource(R.string.settings_screen_discount_48_off)
                },
                valueColor = accentTokens.active,
                onClick = {
                    analyticsTracker?.logEvent(
                        name = "remove_ads_tap",
                        params = mapOf("source" to "settings")
                    )
                    showPremiumDialog = true
                }
            )
        )
    )
}
```

**Step 2: Run build to verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 3: Create Music Library Section Card

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/ui/screen/SettingsScreen.kt`

**Step 1: Create Music Library card with proper item order**

Add after Go Premium card (after Task 2):

```kotlin
item {
    SettingsGroupCard(
        modifier = Modifier.padding(horizontal = SpacingLarge),
        rows = listOf(
            SettingsRowModel(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.settings_screen_rescan_library_title),
                value = stringResource(R.string.settings_screen_rescan_library_description),
                onClick = {
                    viewModel.resetScanState()
                    showRescanDialog = true
                },
                showLoading = isScanning
            ),
            SettingsRowModel(
                icon = Icons.Default.Search,
                title = stringResource(R.string.settings_screen_scan_on_startup_title),
                value = stringResource(R.string.settings_screen_scan_on_startup_description),
                onClick = { viewModel.toggleScanOnStartup() },
                trailingContent = {
                    Switch(
                        checked = userPreferences.scanOnStartup,
                        onCheckedChange = { viewModel.toggleScanOnStartup() },
                    )
                }
            ),
            SettingsRowModel(
                icon = Icons.Default.AudioFile,
                title = stringResource(R.string.settings_screen_show_all_audio_files_title),
                value = stringResource(R.string.settings_screen_show_all_audio_files_description),
                valuePlacement = ValuePlacement.Below,
                onClick = { viewModel.toggleShowAllAudioFiles() },
                trailingContent = {
                    Switch(
                        checked = userPreferences.showAllAudioFiles,
                        onCheckedChange = { viewModel.toggleShowAllAudioFiles() },
                    )
                }
            ),
            SettingsRowModel(
                icon = Icons.Default.Timer,
                title = stringResource(R.string.label_minimum_audio_duration),
                value = stringResource(R.string.settings_screen_duration_seconds_value, userPreferences.minimumAudioDuration),
                onClick = { showMinDurationDialog = true }
            ),
            SettingsRowModel(
                icon = Icons.Default.FolderOff,
                title = stringResource(R.string.settings_screen_excluded_folders_title),
                value = stringResource(R.string.settings_screen_excluded_folders_value),
                onClick = onNavigateToExcludedFolders
            )
        )
    )
}
```

**Step 2: Run build to verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 4: Create Appearance Section Card

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/ui/screen/SettingsScreen.kt`

**Step 1: Create Appearance card**

Add after Music Library card (after Task 3):

```kotlin
item {
    SettingsGroupCard(
        modifier = Modifier.padding(horizontal = SpacingLarge),
        rows = listOf(
            SettingsRowModel(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.settings_screen_theme_title),
                value = getThemeDescription(context, userPreferences.theme),
                onClick = { showThemeDialog = true }
            ),
            SettingsRowModel(
                icon = Icons.Default.ColorLens,
                title = stringResource(R.string.settings_screen_accent_color_title),
                value = userPreferences.accentProfile.label,
                onClick = { showAccentDialog = true }
            )
        )
    )
}
```

**Step 2: Run build to verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 5: Create Notifications & Language Section Card

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/ui/screen/SettingsScreen.kt`

**Step 1: Create Notifications & Language card**

Add after Appearance card (after Task 4):

```kotlin
item {
    SettingsGroupCard(
        modifier = Modifier.padding(horizontal = SpacingLarge),
        rows = listOf(
            SettingsRowModel(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.settings_screen_notification_settings_title),
                onClick = { showComingSoonToast(context, context.getString(R.string.settings_screen_notification_settings_coming_soon)) }
            ),
            SettingsRowModel(
                icon = Icons.Default.Language,
                title = stringResource(R.string.settings_screen_language_title),
                value = getLanguageDescription(context, appLanguageTag),
                onClick = { showLanguageDialog = true }
            )
        )
    )
}
```

**Step 2: Run build to verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 6: Create Help & About Section Card

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/ui/screen/SettingsScreen.kt`

**Step 1: Create Help & About card**

Add after Notifications & Language card (after Task 5):

```kotlin
item {
    SettingsGroupCard(
        modifier = Modifier.padding(horizontal = SpacingLarge),
        rows = listOf(
            SettingsRowModel(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_screen_about_title),
                onClick = onNavigateToAbout
            ),
            SettingsRowModel(
                icon = Icons.Default.Email,
                title = stringResource(R.string.settings_screen_feedback_title),
                value = stringResource(R.string.settings_screen_feedback_google_play),
                valuePlacement = ValuePlacement.Below,
                valueColor = accentTokens.active,
                onClick = {
                    analyticsTracker?.logEvent("feedback_tap", mapOf("source" to "settings"))
                    onNavigateToFeedbackReport()
                }
            ),
            SettingsRowModel(
                icon = Icons.Default.Star,
                title = stringResource(R.string.settings_screen_rate_us_title),
                value = stringResource(R.string.settings_screen_feedback_google_play),
                onClick = {
                    analyticsTracker?.logEvent("rate_us_item_tap", mapOf("source" to "settings"))
                    val activity = context as? ComponentActivity
                    if (activity != null) {
                        viewModel.triggerInAppReview(activity)
                    }
                }
            )
        )
    )
}
```

**Step 2: Run build to verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 7: Remove Old Settings Cards

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/ui/screen/SettingsScreen.kt` (lines 203-468)

**Step 1: Delete all old card items**
- Remove old Theme/Accent/Sleep/Ads card (lines 203-244)
- Remove old Notifications/Language card (lines 245-279)
- Remove old Feedback/About/Rate card (lines 280-314)
- Remove old Private Session card (lines 315-347)
- Remove old Audio Quality/Buffer/Gapless/Crossfade/Equalizer card (lines 348-404)
- Remove old Library Scan card (lines 405-468)

**Step 2: Verify LazyColumn structure**

Final structure:
- PremiumBanner item
- Listening card
- Go Premium card
- Music Library card
- Appearance card
- Notifications & Language card
- Help & About card
- Dialog definitions (unchanged)

**Step 3: Compile and verify no errors**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 8: Manual Testing

**Files:**
- No code changes

**Step 1: Build APK**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 2: Install APK**

Run: `./gradlew installDebug`
Expected: App installs without crashes

**Step 3: Manual Testing Checklist**

- [ ] Open SettingsScreen from HomeScreen
- [ ] Verify Premium Banner at top
- [ ] Scroll through 6 sections in order: Listening → Go Premium → Music Library → Appearance → Notifications & Language → Help & About
- [ ] Tap Equalizer (Listening) → navigates to EqualizerScreen
- [ ] Tap Sleep Timer (Listening) → dialog opens
- [ ] Tap Gapless Playback toggle (Listening) → switch works, state persists
- [ ] Tap Remove Ads (Go Premium) → premium dialog opens
- [ ] Tap Rescan Library (Music Library) → rescan dialog opens
- [ ] Tap Scan on Startup toggle (Music Library) → switch works
- [ ] Tap Theme (Appearance) → theme dialog opens
- [ ] Tap Accent Color (Appearance) → accent dialog opens
- [ ] Tap Language (Notifications & Language) → language dialog opens
- [ ] Tap About (Help & About) → navigates to AboutScreen
- [ ] Tap Send Feedback (Help & About) → navigates to FeedbackScreen
- [ ] Tap Rate on Google Play (Help & About) → in-app review opens

**Step 4: Verify no regressions**

- [ ] All dialogs still function (state management unchanged)
- [ ] All callbacks to viewModel still work
- [ ] All navigation callbacks (onNavigateTo*) still work
- [ ] Premium dialog still triggers analytics
- [ ] No UI crashes or layout issues
- [ ] Scrolling is smooth (LazyColumn optimization maintained)
