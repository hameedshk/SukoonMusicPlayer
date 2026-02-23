# Plan: Decision-Complete Voice Song Search via Google Speech

## Summary
Add voice-to-text search on `SearchScreen` using `RecognizerIntent` and a mic icon in `ModernSearchBar`. Spoken text is normalized, safely applied to the query, and searched in the existing local library flow. Failures always degrade to manual typing with clear toasts. This plan closes all known edge cases and leaves no open implementation decisions.

## Locked Product Decisions
1. Scope: Voice search only fills local library query; no external Google song-identification/humming flow.
2. Placement: Mic icon inside existing `ModernSearchBar`.
3. Transcript choice: Use first non-blank recognition candidate only.
4. Query replace policy: Voice result replaces existing query.
5. Normalization policy: `trim`, collapse repeated whitespace, remove trailing punctuation (`.,!?;:`), then trim again.
6. Query length cap: Max 80 chars after normalization.
7. History policy: Save only when normalized query length is `>= 2`.
8. Failure UX: Toast + keep typing flow.
9. Analytics: Track `input_method` explicitly (`typed`, `history`, `voice`).
10. Re-entry behavior: Ignore repeated mic taps while recognition launch/result is in progress.

## Implementation Changes

### 1. `SearchScreen` voice launch and lifecycle-safe result handling
- File: `app/src/main/java/com/sukoon/music/ui/screen/SearchScreen.kt`
- Add `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())`.
- Build speech intent:
  - `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`
  - `EXTRA_LANGUAGE_MODEL = LANGUAGE_MODEL_FREE_FORM`
  - `EXTRA_LANGUAGE = Locale.getDefault().toLanguageTag()`
  - `EXTRA_PROMPT = stringResource(R.string.search_voice_prompt)`
  - `EXTRA_MAX_RESULTS = 3`
- Pre-launch checks:
  - `resolveActivity(packageManager) != null`
  - `isVoiceCaptureInProgress == false`
- State:
  - `isVoiceCaptureInProgress` (`rememberSaveable`) to debounce taps.
  - `isScreenActive` (`DisposableEffect`) to ignore late callback after screen disposal.
- Callback handling:
  - Clear `isVoiceCaptureInProgress`.
  - If screen inactive, return immediately.
  - If `RESULT_OK`, parse `EXTRA_RESULTS`, select first non-blank, normalize, clamp to 80.
  - If normalized length `< 2`, show `search_voice_no_match` toast and do not update history.
  - Else `viewModel.updateSearchQuery(normalized)` and `viewModel.saveSearchToHistory(inputMethod = VOICE)`.
  - For cancel/empty/error, show specific toast and keep current query unchanged.
- Launch exception handling:
  - Catch `ActivityNotFoundException` and `SecurityException` with `search_voice_not_available` toast.

### 2. `ModernSearchBar` API and UI updates
- File: `app/src/main/java/com/sukoon/music/ui/components/ModernSearchBar.kt`
- Signature additions:
  - `onVoiceClick: (() -> Unit)? = null`
  - `voiceEnabled: Boolean = true`
  - `textFieldFocusRequester: FocusRequester? = null`
- UI:
  - Keep existing clear behavior.
  - Show mic icon whenever `voiceEnabled && onVoiceClick != null`.
  - Mic remains visible even when query is non-empty.
  - Disable mic semantics visually when `voiceEnabled == false`.
- Accessibility:
  - Mic icon `contentDescription = stringResource(R.string.search_voice_content_description)`.
- Focus:
  - Apply `focusRequester` to text field when provided.

### 3. Focus and accessibility after voice result
- File: `app/src/main/java/com/sukoon/music/ui/screen/SearchScreen.kt`
- Create `FocusRequester` and pass to `ModernSearchBar`.
- After successful voice query apply:
  - Request text field focus.
  - Trigger accessibility announcement with updated query text if `LocalAccessibilityManager` is available.

### 4. ViewModel analytics and source-aware save
- File: `app/src/main/java/com/sukoon/music/ui/viewmodel/SearchViewModel.kt`
- Add:
  - `enum class SearchInputMethod { TYPED, HISTORY, VOICE }`
- Update methods:
  - `saveSearchToHistory(inputMethod: SearchInputMethod = TYPED)`
  - `applySearchFromHistory(...)` logs `input_method = "history"`.
- Analytics payload for search event:
  - `query_length`
  - `results_count`
  - `input_method`
- Keep repository behavior unchanged.

### 5. String resources
- Files:
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-hi/strings.xml` (or existing Hindi split resource file used by search/common UI)
- Add:
  - `search_voice_content_description`
  - `search_voice_prompt`
  - `search_voice_not_available`
  - `search_voice_cancelled`
  - `search_voice_no_match`
- Reuse existing toast style and localization patterns.

## Public API / Interface Changes
1. `ModernSearchBar` composable gains optional voice/focus parameters.
2. `SearchViewModel.saveSearchToHistory` gains `inputMethod` parameter with default.
3. New `SearchInputMethod` enum in `SearchViewModel` scope.
4. No database/schema/repository interfaces change.
5. No manifest permission changes.

## Edge Cases Covered
1. No recognizer app installed.
2. Recognizer installed but returns empty list.
3. User cancels recognizer dialog.
4. Rapid repeated mic taps.
5. Result callback after navigation away.
6. Very long transcript.
7. Whitespace/punctuation-heavy transcript.
8. Very short transcript (`< 2`) to prevent history pollution.
9. Existing query replacement behavior.
10. OEMs returning `RESULT_OK` with invalid payload.
11. Accessibility re-focus after voice completion.
12. Analytics distinguish voice from typed/history.

## Test Plan

### Unit tests
1. `SearchViewModel.saveSearchToHistory(VOICE)` logs `input_method=voice`.
2. `SearchViewModel.saveSearchToHistory` does not save blank query.
3. `applySearchFromHistory` logs `input_method=history`.

### Compose/UI tests
1. `ModernSearchBar` shows mic when callback provided.
2. `ModernSearchBar` hides/disables mic when `voiceEnabled=false`.
3. Clear button still works with mic present.
4. Focus requester is attached to text field.

### Instrumentation/manual scenarios
1. Successful speech result updates query and list.
2. Empty result shows `search_voice_no_match`.
3. Cancel shows `search_voice_cancelled`.
4. Unsupported recognizer shows `search_voice_not_available`.
5. Rapid double tap launches once.
6. Rotate device during recognizer flow; no crash and state recovers.
7. Hindi locale validates prompt/toast localization.

## Assumptions and Defaults
1. Minimum SDK and current dependencies remain unchanged.
2. Intent-based recognizer is preferred over embedded speech APIs for minimal permission and maintenance cost.
3. Query normalization and 80-char cap are product defaults for this release.
4. Voice feature is Search-screen-only in this iteration.
