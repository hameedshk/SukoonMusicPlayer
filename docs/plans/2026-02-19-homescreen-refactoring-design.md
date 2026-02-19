# HomeScreen Refactoring Design Document

**Date:** 2026-02-19
**Status:** APPROVED
**Scope:** Decompose HomeScreen (1199 lines) into modular architecture with SongsScreen and PlaylistsScreen
**Goals:**
1. Extract SongsScreen with SongsViewModel (self-contained)
2. Extract PlaylistsScreen with PlaylistsScreenViewModel (self-contained)
3. Eliminate tight coupling to HomeViewModel
4. Fix 23 identified bugs and edge cases
5. Improve maintainability and testability

---

## Architecture Overview

### Current Problem
```
HomeScreen (1199 lines)
├─ Manages: 3 ViewModels + 15+ local states
├─ Contains: Embedded SongsContent (350 lines)
├─ Contains: Embedded PlaylistsLogic (250 lines)
└─ Problem: Fragile, tightly coupled, hard to modify
```

### Target Architecture
```
HomeScreen (reduced to ~350 lines - coordinator only)
├─ Manages: Tab coordination, single ViewModel instances
├─ Consolidates: Delete launchers (1, not duplicated)
├─ Passes: ViewModels to all screens (shared instances)
└─ Delegates to:
    ├─ HomeTabScreen (renamed from HomeTab)
    ├─ SongsScreen (new, self-contained)
    │  ├─ Owns: SongsViewModel
    │  ├─ Lifecycle: Safe teardown on tab switch
    │  └─ SongsContent (pure presentation)
    └─ PlaylistsScreen (new, self-contained)
       ├─ Owns: PlaylistsScreenViewModel
       ├─ Lifecycle: Safe teardown on tab switch
       └─ PlaylistsContent (pure presentation)
```

---

## Component Design

### SongsViewModel

**Responsibilities:**
- Owns all UI state: sortMode, dialogs, scroll position, delete operations
- Manages delete operations with atomic Mutex lock (prevents race with scan)
- Tracks screen active state (cleanup on tab switch)
- Persists sort preferences to DataStore
- Manages lifecycle safely with proper cleanup

**Key StateFlows:**
- `songs: StateFlow<List<Song>>` - from SongRepository
- `playbackState: StateFlow<PlaybackState>` - from PlaybackRepository
- `sortMode, sortOrder` - UI state, persisted
- `showSortDialog, showMenu, selectedSongId` - UI dialogs
- `songToDelete, isDeleting` - delete operation state
- `scrollPosition` - preserved across tab switches
- `deleteError, requiresDeletePermission` - error handling
- `isScreenActive` - lifecycle signal

**Critical Methods:**
```kotlin
fun confirmDelete(song: Song, callback: (DeleteResult) -> Unit)
  // Atomic delete with Mutex lock
  // Pauses playback if deleting currently playing song
  // Only callbacks if screen still active
  // Triggers refresh scan on success

fun setSortMode(mode: String, order: String)
  // Updates state AND persists to DataStore

fun setScreenActive(active: Boolean)
  // Called on screen lifecycle to prevent orphaned operations
```

### PlaylistsScreenViewModel

**Same pattern as SongsViewModel:**
- Owns all playlist UI state
- Atomic delete with Mutex
- Lifecycle management
- Error handling

### HomeScreen

**Responsibilities (simplified):**
- Tab coordination (selection, pager sync)
- Consolidate ViewModel instances (one PlaylistViewModel for all screens)
- Consolidate delete launcher (one for all screens)
- Handle delete permission results
- Delegate to each screen (pass ViewModels + launcher callback)

**Not responsible for:**
- Sort state (moved to SongsViewModel)
- Delete dialog state (moved to SongsViewModel/PlaylistsScreenViewModel)
- Playlist filter state (moved to PlaylistsScreenViewModel)
- Ad logic per-screen (passed to screens as parameters)

### SongsScreen

**Responsibilities:**
- Container/Coordinator for songs tab
- Injects SongsViewModel
- Collects all StateFlows from ViewModel
- Manages screen lifecycle (DisposableEffect: active → inactive)
- Receives delete launcher from parent
- Memoizes callbacks (prevent duplicate navigation)
- Passes data + callbacks to SongsContent

**Critical Pattern:**
```kotlin
DisposableEffect(Unit) {
    viewModel.setScreenActive(true)
    onDispose {
        viewModel.setScreenActive(false)  // ← Cleanup signal
    }
}

// When operations complete, check isScreenActive before showing UI
if (isScreenActive) {
    Toast.makeText(context, "Song deleted", Toast.LENGTH_SHORT).show()
}
```

### SongsContent

**Pure presentation:**
- Receives ALL data as parameters (no ViewModel)
- Renders LazyColumn, dialogs, menus
- Calls callbacks for user actions
- Memoized callbacks prevent duplicate calls
- No direct repository access

---

## Data Flow

### Delete Operation (Safe)
```
User clicks delete
  ↓
SongsContent calls: onShowDeleteConfirmation(song)
  ↓
SongsScreen: viewModel.showDeleteConfirmation(song)
  ↓
SongsViewModel updates: _songToDelete.value = song
  ↓
SongsContent shows delete dialog
  ↓
User confirms delete
  ↓
SongsContent calls: onConfirmDelete(song)
  ↓
SongsScreen: viewModel.confirmDelete(song) { result ->
    if (isScreenActive) { /* show result */ }
}
  ↓
SongsViewModel:
  1. Check if deleting currently playing song → pause playback
  2. Acquire Mutex lock (prevent race with scan)
  3. Call deleteHelper.deleteSongs()
  4. On success: trigger scan refresh
  5. Update state only if isScreenActive
  ↓
Result propagates back via StateFlows
  ↓
SongsScreen shows success toast (only if still active)
```

### Scan + Delete Race Prevention
```
// In SongsViewModel
private val DATA_MODIFICATION_MUTEX = Mutex()

fun confirmDelete(song: Song) {
    viewModelScope.launch {
        DATA_MODIFICATION_MUTEX.withLock {
            // Only delete can proceed while holding lock
            val result = deleteHelper.deleteSongs(...)
        }
    }
}

// In SongRepository.scanLocalMusic()
suspend fun scanLocalMusic() {
    DATA_MODIFICATION_MUTEX.withLock {
        // Only scan can proceed while holding lock
        // Prevents race: scan won't overwrite delete result
    }
}
```

---

## Lifecycle Management

### Screen Active → Inactive
```
HorizontalPager swipes away SongsScreen
  ↓
DisposableEffect.onDispose() fires
  ↓
viewModel.setScreenActive(false)
  ↓
SongsViewModel sets: _isScreenActive.value = false
  ↓
If delete operation in flight:
  - Completes but doesn't show UI (checks isScreenActive)
  - Coroutine cleanup via viewModelScope
  - Orphaned operations prevented ✓
```

### Screen Inactive → Active
```
HorizontalPager swipes back to SongsScreen
  ↓
DisposableEffect runs again
  ↓
viewModel.setScreenActive(true)
  ↓
Scroll position restored (from SongsViewModel.scrollPosition)
  ↓
Sort mode restored (persisted in DataStore)
  ↓
All state preserved ✓
```

---

## Error Handling & Edge Cases

### Delete Currently Playing Song
```kotlin
fun confirmDelete(song: Song) {
    if (playbackState.value.currentSong?.id == song.id) {
        playbackRepository.pause()  // ← Stop playback first
    }
    // Then proceed with delete
}
```

### Delete During Permission Dialog
```kotlin
// If user navigates away while permission dialog pending:
val deleteLauncher = rememberLauncherForActivityResult(...) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        // Only proceed if screen still active
        if (isScreenActive) {
            finalizeDeletion()
        }
    }
}
```

### Empty Songs List
```kotlin
// Don't show sort button if no songs
if (songs.isEmpty()) {
    Box { Text("No songs found") }
    return  // Don't render UI
}

// Disable sort button if empty
IconButton(
    onClick = onShowSortDialog,
    enabled = songs.isNotEmpty()
)
```

### Navigation During Delete
```kotlin
// Memoize navigation callbacks to prevent duplicate calls
val memoizedNavigate = remember(onNavigateToAlbum) {
    { albumId: Long ->
        if (isScreenActive) onNavigateToAlbum(albumId)
    }
}

SongItem(onClick = { memoizedNavigate(albumId) })
```

---

## State Persistence

### Sort Preferences
```kotlin
// In SongsViewModel.init
init {
    viewModelScope.launch {
        val (mode, order) = songRepository.getSongSortPreferences()
        _sortMode.value = mode
        _sortOrder.value = order
    }
}

// On change
fun setSortMode(mode: String, order: String) {
    _sortMode.value = mode
    _sortOrder.value = order
    viewModelScope.launch {
        settingsRepository.setSongSortMode(mode, order)
    }
}
```

### Scroll Position
```kotlin
// Saved in SongsViewModel (survives tab switch)
val scrollPosition: StateFlow<Int>

// In SongsContent
val scrollState = rememberLazyListState(
    initialFirstVisibleItemIndex = scrollPosition
)

LaunchedEffect(scrollState) {
    snapshotFlow { scrollState.firstVisibleItemIndex }
        .collect { index -> viewModel.saveScrollPosition(index) }
}
```

---

## File Structure

```
ui/screen/
├─ HomeScreen.kt (reduced to ~350 lines)
├─ HomeTabScreen.kt (renamed from HomeTab, ~180 lines)
├─ SongsScreen.kt (new, ~400 lines)
│  ├─ fun SongsScreen() [coordinator]
│  └─ private fun SongsContent() [presentation]
├─ PlaylistsScreen.kt (new, ~350 lines)
│  ├─ fun PlaylistsScreen() [coordinator]
│  └─ private fun PlaylistsContent() [presentation]
└─ ...other screens (unchanged)

ui/viewmodel/
├─ HomeViewModel.kt (unchanged, still owns home tab data)
├─ SongsViewModel.kt (new, ~300 lines)
├─ PlaylistsScreenViewModel.kt (new, ~250 lines)
├─ PlaylistViewModel.kt (unchanged, shared instance)
└─ ...other viewmodels
```

---

## Phased Rollout

### Phase 1: Extract SongsScreen
- Create SongsViewModel with all song UI state
- Extract SongsScreen + SongsContent from HomeScreen
- Update HomeScreen to delegate to SongsScreen
- Test: All songs tab features work, sort persists, scroll preserved

### Phase 2: Extract PlaylistsScreen
- Create PlaylistsScreenViewModel with all playlist UI state
- Extract PlaylistsScreen + PlaylistsContent from HomeScreen
- Update HomeScreen to delegate to PlaylistsScreen
- Test: All playlists tab features work, filters work, delete works

### Phase 3: Verify & Simplify
- Verify HomeScreen is now ~350 lines (was 1199)
- Verify all tabs work independently
- Performance testing: Tab switching smooth, no leaks
- Final cleanup: Remove any remaining embedded functions

---

## Testing Strategy

### Unit Tests
- SongsViewModel: sort logic, delete with Mutex, cleanup
- PlaylistsScreenViewModel: filter state, delete operations
- Error scenarios: permission denied, delete during navigation

### Integration Tests
- Tab switching: Sort state preserved, scroll restored
- Delete: Currently playing song, playlist in use
- Navigation: Deep links during delete, rapid tab switching
- Lifecycle: Config rotation, app backgrounding

### Manual QA
- Delete song while another is playing → playback stops ✓
- Rapid tab switching → no crashes ✓
- Navigation away mid-delete → no orphaned toasts ✓
- Rotate device → all state preserved ✓
- Low memory scenarios → no leaks ✓

---

## Success Criteria

✅ **Architecture:**
- Each tab is independent, self-contained
- No tight coupling between tabs and HomeViewModel
- Clear data flow and responsibility boundaries

✅ **Functionality:**
- All features work exactly as before (no behavior changes)
- Delete, sort, filter, scroll, navigation all preserved
- Ads still display correctly

✅ **Code Quality:**
- HomeScreen reduced from 1199 → 350 lines
- Clear separation of concerns
- Safe lifecycle management
- All 23 bugs fixed

✅ **Maintainability:**
- New developer can understand SongsScreen without reading HomeScreen
- Pattern consistent across all tabs
- Future tabs follow same template

---

## Known Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Scroll position lost on crash | Use in-memory StateFlow (good enough) |
| Delete permission while backgrounded | Check isScreenActive before UI updates |
| Memory from multiple ViewModels | Hilt handles cleanup automatically |
| Rapid tab switching | DisposableEffect + setScreenActive handles |
| Data inconsistency between tabs | Single source via Repository flows |

---

## Timeline

- **Phase 1 (SongsScreen):** 2-3 hours
- **Phase 2 (PlaylistsScreen):** 2-3 hours
- **Phase 3 (Verification):** 1-2 hours
- **Total:** 5-8 hours of development

---

## Appendix: Bug Fixes Summary

| Bug # | Issue | Fixed By |
|-------|-------|----------|
| 1 | ViewModel Lifecycle | Hilt scoping + explicit cleanup |
| 2 | Orphaned Launcher | DisposableEffect + isScreenActive check |
| 3 | Coroutine Cleanup | Try/catch + atomic operations |
| 4 | Data Sync | Single source + refresh via scan |
| 5 | Sort Persistence | DataStore save/load |
| 6 | PlaylistViewModel Sharing | Pass single instance to all screens |
| 7 | Deps Chain | PlaylistViewModel passed from HomeScreen |
| 8 | AdMobManager | Passed to screens needing ads |
| 9 | Delete Launchers | Consolidated in HomeScreen |
| 10 | Dialog on Rotation | State in ViewModel (survives) |
| 11 | Excessive Recompose | SongsScreen owns its data |
| 12 | Scroll Lost | Persisted in SongsViewModel |
| 13 | Empty Sort Button | Disable if songs.isEmpty() |
| 14 | Delete Playing Song | Pause before delete |
| 15 | Delete Playing Playlist | Pause before delete |
| 16 | Rapid Tab Switch | Safe teardown + cleanup |
| 17 | Scan + Delete Race | Mutex lock |
| 18 | Listener Leaks | WhileSubscribed(1000) |
| 19 | Bitmap Leak | Compose keys + lifecycle |
| 20 | Deep Link + Delete | Close dialogs before nav |
| 21 | Nav Duplicate | Memoized callbacks |
| 22 | Permission Denied | Different UX (keep dialog) |
| 23 | Multi-API Support | Test on Android 12-15 |
