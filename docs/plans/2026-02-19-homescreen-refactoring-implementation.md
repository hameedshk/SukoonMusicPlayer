# HomeScreen Refactoring Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Decompose HomeScreen (1199 lines) into modular SongsScreen and PlaylistsScreen with independent ViewModels, fixing 23 identified bugs and improving maintainability.

**Architecture:** Phased extraction (Songs first, then Playlists, then verify). Each screen owns its ViewModel and state. Lifecycle management prevents orphaned operations. Mutex prevents scan/delete race conditions. All 23 bugs addressed.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt DI, StateFlow, Room, Coroutines

**Phases:**
1. **Phase 1 (SongsScreen):** 10 tasks, ~1.5 hours
2. **Phase 2 (PlaylistsScreen):** 10 tasks, ~1.5 hours
3. **Phase 3 (Verification):** 3 tasks, ~1 hour
4. **Total:** 23 tasks, ~4 hours

---

## PHASE 1: SONGS SCREEN EXTRACTION

### Task 1: Create SongsViewModel Skeleton

**Files:**
- Create: `app/src/main/java/com/sukoon/music/ui/viewmodel/SongsViewModel.kt`

**Step 1: Write the file with basic ViewModel structure**

Create `SongsViewModel.kt`:

```kotlin
package com.sukoon.music.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Songs Screen.
 *
 * Owns all UI state for the songs tab: sorting, dialogs, delete operations.
 * Manages lifecycle safely with DisposableEffect cleanup.
 * Prevents race conditions with Mutex during delete + scan operations.
 */
@HiltViewModel
class SongsViewModel @Inject constructor(
    private val songRepository: SongRepository,
    val playbackRepository: PlaybackRepository,
    private val deleteHelper: DeleteHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "SongsViewModel"
    }

    // ========== DATA FLOWS ==========

    val songs: StateFlow<List<Song>> = songRepository.getAllSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1000),  // 1s not 5s (Bug #18)
            initialValue = emptyList()
        )

    val playbackState = playbackRepository.playbackState

    // ========== UI STATE FLOWS ==========

    private val _sortMode = MutableStateFlow("Song name")
    val sortMode: StateFlow<String> = _sortMode.asStateFlow()

    private val _sortOrder = MutableStateFlow("A to Z")
    val sortOrder: StateFlow<String> = _sortOrder.asStateFlow()

    private val _showSortDialog = MutableStateFlow(false)
    val showSortDialog: StateFlow<Boolean> = _showSortDialog.asStateFlow()

    private val _selectedSongId = MutableStateFlow<Long?>(null)
    val selectedSongId: StateFlow<Long?> = _selectedSongId.asStateFlow()

    private val _showMenu = MutableStateFlow(false)
    val showMenu: StateFlow<Boolean> = _showMenu.asStateFlow()

    private val _songToDelete = MutableStateFlow<Song?>(null)
    val songToDelete: StateFlow<Song?> = _songToDelete.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _showInfoForSong = MutableStateFlow<Song?>(null)
    val showInfoForSong: StateFlow<Song?> = _showInfoForSong.asStateFlow()

    private val _scrollPosition = MutableStateFlow(0)
    val scrollPosition: StateFlow<Int> = _scrollPosition.asStateFlow()

    // ========== ERROR HANDLING FLOWS ==========

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    private val _requiresDeletePermission = MutableStateFlow<android.content.IntentSender?>(null)
    val requiresDeletePermission: StateFlow<android.content.IntentSender?> = _requiresDeletePermission.asStateFlow()

    // ========== LIFECYCLE FLOWS ==========

    private val _isScreenActive = MutableStateFlow(true)
    val isScreenActive: StateFlow<Boolean> = _isScreenActive.asStateFlow()

    override fun onCleared() {
        _isScreenActive.value = false
        super.onCleared()
    }

    // ========== PUBLIC METHODS ==========

    fun setScreenActive(active: Boolean) {
        _isScreenActive.value = active
    }

    fun setSortMode(mode: String, order: String) {
        _sortMode.value = mode
        _sortOrder.value = order
        viewModelScope.launch(Dispatchers.IO) {
            try {
                songRepository.setSongSortPreferences(mode, order)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving sort prefs", e)
            }
        }
    }

    fun showSortDialog() { _showSortDialog.value = true }
    fun hideSortDialog() { _showSortDialog.value = false }

    fun selectSong(songId: Long) { _selectedSongId.value = songId }
    fun deselectSong() { _selectedSongId.value = null }

    fun openMenuForSong(songId: Long) {
        _selectedSongId.value = songId
        _showMenu.value = true
    }
    fun closeMenu() { _showMenu.value = false }

    fun showDeleteConfirmation(song: Song) { _songToDelete.value = song }
    fun hideDeleteConfirmation() { _songToDelete.value = null }

    fun showSongInfo(song: Song) { _showInfoForSong.value = song }
    fun hideSongInfo() { _showInfoForSong.value = null }

    fun saveScrollPosition(index: Int) { _scrollPosition.value = index }

    fun clearDeleteError() { _deleteError.value = null }

    fun playQueue(songs: List<Song>, index: Int) {
        viewModelScope.launch {
            playbackRepository.playQueue(songs, index)
        }
    }

    fun shuffleAll() {
        viewModelScope.launch {
            playbackRepository.shuffleAll(songs.value)
        }
    }

    fun playAll() {
        viewModelScope.launch {
            playbackRepository.playAll(songs.value)
        }
    }

    fun toggleLike(songId: Long, isLiked: Boolean) {
        viewModelScope.launch {
            songRepository.toggleLike(songId, isLiked)
        }
    }
}
```

**Step 2: Verify file exists**

Run: `ls -la app/src/main/java/com/sukoon/music/ui/viewmodel/SongsViewModel.kt`

Expected: File exists, ~150 lines

**Step 3: Commit**

```bash
git add app/src/main/java/com/sukoon/music/ui/viewmodel/SongsViewModel.kt
git commit -m "feat: create SongsViewModel with UI state management"
```

Expected: Commit successful

---

### Task 2: Add Delete Logic to SongsViewModel

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/ui/viewmodel/SongsViewModel.kt`

**Step 1: Add Mutex and delete method to SongsViewModel**

Add to `SongsViewModel.kt` before closing brace:

```kotlin
    // ========== CONCURRENCY CONTROL ==========

    private val dataModificationMutex = kotlinx.coroutines.sync.Mutex()

    /**
     * Delete song with atomic operation + Mutex to prevent race with scan (Bug #17).
     * Pauses playback if deleting currently playing song (Bug #14).
     * Only callbacks if screen still active (Bug #2).
     */
    fun confirmDelete(song: Song, callback: (DeleteHelper.DeleteResult) -> Unit) {
        if (_isDeleting.value) return  // Prevent duplicate deletes

        _isDeleting.value = true

        viewModelScope.launch {
            try {
                // Check if deleting currently playing song (Bug #14 fix)
                val isCurrentlyPlaying = playbackState.value.currentSong?.id == song.id
                if (isCurrentlyPlaying) {
                    playbackRepository.pause()
                }

                // Acquire lock - prevents race with scan (Bug #17 fix)
                dataModificationMutex.withLock {
                    val result = deleteHelper.deleteSongs(context, listOf(song))

                    // Only callback if screen still active (Bug #2 fix)
                    if (_isScreenActive.value) {
                        callback(result)

                        when (result) {
                            is DeleteHelper.DeleteResult.Success -> {
                                _songToDelete.value = null
                                _isDeleting.value = false
                                // Trigger refresh across all screens
                                songRepository.scanLocalMusic()
                            }
                            is DeleteHelper.DeleteResult.RequiresPermission -> {
                                _requiresDeletePermission.value = result.intentSender
                                _isDeleting.value = false
                            }
                            is DeleteHelper.DeleteResult.Error -> {
                                _deleteError.value = result.message
                                _songToDelete.value = null
                                _isDeleting.value = false
                            }
                        }
                    } else {
                        _isDeleting.value = false
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.w(TAG, "Delete cancelled mid-operation", e)
                _isDeleting.value = false
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Delete failed", e)
                _deleteError.value = e.message
                _songToDelete.value = null
                _isDeleting.value = false
            }
        }
    }

    fun finalizeDeletion() {
        _songToDelete.value = null
        _isDeleting.value = false
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin -x test`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/sukoon/music/ui/viewmodel/SongsViewModel.kt
git commit -m "feat: add atomic delete operation with Mutex and lifecycle safety"
```

Expected: Commit successful

---

### Task 3: Extract SongsScreen from HomeScreen

**Files:**
- Create: `app/src/main/java/com/sukoon/music/ui/screen/SongsScreen.kt`

**Step 1: Create SongsScreen file**

Create `SongsScreen.kt` with the container/coordinator logic. See design document for full code.

**Step 2-3: Verify and Commit**

---

### Task 4: Copy SongsContent to New File

Extract SongsContent from HomeScreen.kt (lines 838-1184) and add to SongsScreen.kt.

---

### Task 5: Complete SongsContent Implementation

Adapt SongsContent to use callbacks instead of ViewModel calls.

---

### Task 6: Update HomeScreen to Use SongsScreen

Replace SongsContent call in pager with SongsScreen. Remove embedded functions.

---

### Task 7: Add Delete Launcher to HomeScreen

Add consolidated delete launcher (not duplicated per screen).

---

### Task 8: Rename HomeTab to HomeTabScreen

Rename file and update calls for consistency.

---

### Task 9: Run Songs Tab Tests

Build, install, and manually test all songs tab features.

---

### Task 10: Verify HomeScreen Size Reduction (Phase 1)

Check HomeScreen reduced from 1199 to ~400 lines.

---

## PHASE 2: PLAYLISTS SCREEN EXTRACTION

*(Tasks 11-20 follow same pattern as Phase 1 for Playlists)*

### Task 11-20: Extract PlaylistsScreen

Similar tasks to Phase 1, but for playlists tab:
- Create PlaylistsScreenViewModel
- Add delete logic to PlaylistsScreenViewModel
- Create PlaylistsScreen container
- Extract PlaylistsContent
- Complete PlaylistsContent implementation
- Update HomeScreen for PlaylistsScreen
- Add delete launcher for playlists
- Test playlists functionality
- Verify HomeScreen further reduced

---

## PHASE 3: VERIFICATION & CLEANUP

### Task 21: Verify Final HomeScreen Size

Check HomeScreen reduced to ~350 lines (70% reduction from 1199).

---

### Task 22: Full App Integration Test

Test all tabs, rotation, navigation, rapid switching.

---

### Task 23: Final Commit & Cleanup

Run lint, tests, final commit with summary.

---

## Summary

| Phase | Tasks | Time | Output |
|-------|-------|------|--------|
| Phase 1 | 1-10 | 1.5h | SongsScreen created, extracted from HomeScreen |
| Phase 2 | 11-20 | 1.5h | PlaylistsScreen created, extracted from HomeScreen |
| Phase 3 | 21-23 | 1h | Verification, testing, final commit |
| **Total** | **23** | **~4h** | HomeScreen: 1199 → 350 lines, all 23 bugs fixed |

---

## Execution Instructions

1. **Create git worktree** (in terminal):
```bash
cd C:\Users\ksham\Documents\SukoonMusicPlayer
git worktree add -b feature/homescreen-refactoring ../SukoonMusicPlayer-homescreen
cd ../SukoonMusicPlayer-homescreen
```

2. **Open new Claude session**:
```bash
claude
```

3. **In new session, invoke subagent-driven-development**:
```
I'm ready to execute the HomeScreen refactoring plan using superpowers:subagent-driven-development.
The plan is at docs/plans/2026-02-19-homescreen-refactoring-implementation.md with 23 tasks across 3 phases.
Start with Phase 1, Task 1: Create SongsViewModel Skeleton.
```

---
