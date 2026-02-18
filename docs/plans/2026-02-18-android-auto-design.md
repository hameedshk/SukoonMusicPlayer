# Android Auto Integration Design Document

**Date:** 2026-02-18
**Project:** Sukoon Music Player v1.0.0
**Status:** Approved Design
**Author:** Claude Code

---

## 1. Overview

### Objective
Enable offline-only Android Auto support for Sukoon Music Player, allowing users to control music playback safely while driving via car head unit display. Integration must not break any existing functionality.

### Scope
- **What's Included:** Basic playback control (play/pause/next/previous) + queue management (browse queue, view playlists, albums, artists)
- **What's NOT Included:** Lyrics display, dynamic theming, equalizer controls, search (simplified for driver safety)
- **Target:** Android Auto only (not Android Automotive OS)
- **Architecture:** MediaBrowserService (separate from existing MusicPlaybackService)

### Key Design Principle
**Zero changes to existing `MusicPlaybackService`** — new Android Auto service operates independently while sharing the same MediaSession.

---

## 2. Architecture

### 2.1 System Diagram

```
┌─────────────────────────────────────────────────────┐
│           Android Auto Head Unit                     │
│  (displays media browser + uses MediaSession)        │
└────────────────┬────────────────────────────────────┘
                 │
         MediaBrowser + MediaSession
                 │
    ┌────────────┴────────────────┐
    │                             │
┌───▼──────────────────┐  ┌──────▼──────────────────┐
│ AndroidAutoMedia     │  │ MusicPlaybackService    │
│ BrowserService (NEW) │  │ (EXISTING - UNCHANGED)  │
│                      │  │                         │
│ - onGetRoot()        │  │ - ExoPlayer             │
│ - onLoadChildren()   │  │ - MediaSession (shared) │
│ - BrowseTree        ├──┤ - Playback control      │
│   - Queue            │  │ - Notification          │
│   - Playlists        │  │ - Audio focus           │
│   - Albums           │  │ - Crossfade/EQ         │
│   - Artists          │  │                         │
│   - Recently Played  │  │                         │
│     (if not private) │  │                         │
└──────────────────────┘  └──────────────────────────┘
    Browse Protocol           MediaSession/ExoPlayer
    (Android Auto)            (Single Source of Truth)
```

### 2.2 Service Architecture

**Two independent MediaSessionService instances:**

1. **MusicPlaybackService** (existing)
   - Owns ExoPlayer instance
   - Manages MediaSession
   - Handles playback, notifications, audio focus, effects
   - **NOT MODIFIED**

2. **AndroidAutoMediaBrowserService** (new)
   - Extends `MediaBrowserService`
   - Shares same MediaSession (via Hilt singleton)
   - Implements browse tree for Android Auto
   - Routes all commands back through MediaController

**Critical:** Both services use the **same MediaSession instance** (provided by Hilt singleton), avoiding conflicts and ensuring state coherency.

---

## 3. Component Design

### 3.1 New Files to Create

| File | Purpose | Responsibility |
|------|---------|-----------------|
| `data/service/AndroidAutoMediaBrowserService.kt` | Main MediaBrowserService impl | Handle Android Auto connection, delegate to BrowseTree |
| `data/browsertree/BrowseTree.kt` | Browse hierarchy logic | Build menu structure (Queue, Playlists, Albums, Artists) |
| `data/browsertree/BrowseNode.kt` | Data model for browse items | MediaItem metadata wrapper |
| `data/browsertree/MediaItemConverter.kt` | Conversion utility | Song/Album/Playlist → MediaItem |

### 3.2 Modified Files

| File | Change | Reason |
|------|--------|--------|
| `AndroidManifest.xml` | Add `<service>` declaration for AndroidAutoMediaBrowserService | Register service with Android Auto intent-filter |
| `di/AppModule.kt` or new `di/MediaSessionModule.kt` | Ensure MediaSession is @Singleton @ApplicationScope | Both services inject same instance |

### 3.3 Browse Tree Structure

```
Root (mediaId: "root")
├── Now Playing (mediaId: "now_playing")
│   └── [Current queue items]
├── Queue (mediaId: "queue")
│   └── [All upcoming songs]
├── Playlists (mediaId: "playlists")
│   ├── Playlist A (mediaId: "playlist_123")
│   │   └── [Songs in playlist]
│   └── Playlist B (mediaId: "playlist_456")
│       └── [Songs in playlist]
├── Albums (mediaId: "albums")
│   ├── Album X (mediaId: "album_789")
│   │   └── [Songs in album]
│   └── Album Y (mediaId: "album_101")
│       └── [Songs in album]
├── Artists (mediaId: "artists")
│   ├── Artist A (mediaId: "artist_202")
│   │   ├── Albums by Artist A
│   │   └── All songs by Artist A
│   └── Artist B (mediaId: "artist_303")
│       └── [Albums & songs]
└── Recently Played (mediaId: "recently_played")
    └── [Last 20 played songs - ONLY if NOT private session]
```

---

## 4. Critical Edge Cases & Mitigation

### 4.1 MediaSession Sharing (HIGHEST RISK)

**Problem:** Two services cannot access the same MediaSession instance without careful coordination.

**Solution:**
```kotlin
// MediaSessionModule.kt
@Module
@InstallIn(SingletonComponent::class)
object MediaSessionModule {
    @Singleton
    @Provides
    fun provideMediaSession(
        context: Context,
        player: ExoPlayer
    ): MediaSession {
        return MediaSession.Builder(context, player)
            .setCallback(MediaSessionCallback(player))
            .build()
    }
}
```

- Both services inject `@Singleton` MediaSession
- Hilt ensures single instance across app lifetime
- Thread-safe (Hilt handles synchronization)
- Android Auto receives SessionToken automatically

**Verification:** MediaSession initialization order doesn't matter; both services will use the same instance.

---

### 4.2 Service Lifecycle & Independence (HIGH RISK)

**Problem:** If MusicPlaybackService crashes, AndroidAutoMediaBrowserService becomes orphaned (no MediaSession).

**Solution:**
- Both services are **independent** — one dying doesn't kill the other
- AndroidAutoMediaBrowserService gracefully handles null MediaSession
- If MediaSession is unavailable, browse queries return empty lists

```kotlin
class AndroidAutoMediaBrowserService : MediaBrowserService() {
    @Inject lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        try {
            setMediaSession(mediaSession)  // Register with Android Auto
        } catch (e: Exception) {
            DevLogger.error("Failed to set MediaSession: ${e.message}")
            // Android Auto will show error, but service doesn't crash
        }
    }
}
```

**Verification:**
- If MusicPlaybackService dies: Android Auto loses audio, but browse service stays alive
- If AndroidAutoMediaBrowserService dies: Phone app continues working
- No cross-service dependencies

---

### 4.3 Thread Safety of Shared MediaSession (HIGH RISK)

**Problem:** MusicPlaybackService and AndroidAutoMediaBrowserService may access MediaSession simultaneously.

**Solution:**
- All MediaSession access goes through **MediaController** (thread-safe by design)
- BrowseTree queries are **read-only** from Room (thread-safe)
- Playback commands serialized through ExoPlayer's event loop

```kotlin
// Safe: BrowseTree only reads from database
override fun onLoadChildren(parentId: String, result: Result<List<MediaItem>>) {
    scope.launch {
        try {
            val items = when (parentId) {
                PLAYLISTS_ID -> playlistRepository.getAllPlaylists()
                    .first()
                    .map { it.toMediaItem() }
                QUEUE_ID -> playbackRepository.getCurrentQueue()
                    .first()
                    .items
                    .map { it.toMediaItem() }
                else -> emptyList()
            }
            result.sendResult(items)
        } catch (e: Exception) {
            result.sendResult(emptyList())  // Graceful failure
        }
    }
}

// Safe: All playback through MediaController (serialized)
mediaController.play()           // Goes through ExoPlayer event loop
mediaController.skipToNext()     // Queued safely
```

**Verification:**
- Room DAOs are thread-safe (SQLite handles locking)
- MediaController operations are serialized
- No data races possible

---

### 4.4 Private Session Mode State Sync (MEDIUM-HIGH RISK)

**Problem:** If private session is enabled on phone, should Android Auto still log history and show Recently Played?

**Solution:**
- Respect private session flag in browse tree
- Don't show Recently Played section if private session active
- Don't log Android Auto playback to history if private session active

```kotlin
// BrowseTree.kt
suspend fun getChildren(parentId: String): List<MediaItem> {
    val isPrivate = preferencesManager.isPrivateSession()

    return when (parentId) {
        ROOT_ID -> {
            val children = mutableListOf(
                queueItem, playlistsItem, albumsItem, artistsItem
            )
            // Only add Recently Played if NOT in private session
            if (!isPrivate) {
                children.add(recentlyPlayedItem)
            }
            children
        }
        // ... other cases
    }
}

// MediaSessionCallback.kt
override fun onPlaybackStateChanged() {
    if (!preferencesManager.isPrivateSession()) {
        recentlyPlayedRepository.addToHistory(currentSong)
    }
}
```

**Verification:**
- Private session flag checked on every browse
- History logging respects flag
- Consistent behavior between phone UI and Android Auto

---

### 4.5 Empty Queue & Deleted Playlists (MEDIUM RISK)

**Problem:** Playlist deleted on phone while Android Auto displays it, or queue is empty when browsing.

**Solution:**
- Graceful handling of null/empty data
- Try/catch blocks return empty lists on errors
- No crashes propagated to Android Auto

```kotlin
suspend fun getChildren(parentId: String): List<MediaItem> {
    return try {
        when {
            parentId.startsWith("playlist_") -> {
                val playlistId = parentId.substringAfter("playlist_").toLong()
                playlistRepository.getPlaylistSongs(playlistId)
                    .firstOrNull()?.songs  // Null if playlist deleted
                    ?.map { it.toMediaItem() }
                    ?: emptyList()  // Return empty list gracefully
            }
            parentId == QUEUE_ID -> {
                playbackRepository.getCurrentQueue()
                    .firstOrNull()?.items
                    ?.map { it.toMediaItem() }
                    ?: emptyList()
            }
            else -> emptyList()
        }
    } catch (e: Exception) {
        DevLogger.error("Browse error for parentId=$parentId: ${e.message}")
        emptyList()  // Fail silently to Android Auto
    }
}
```

**Verification:**
- All database queries wrapped in try/catch
- Empty lists returned on any error
- No exceptions propagate to Android Auto

---

## 5. Implementation Outline

### Phase 1: MediaSession Singleton Setup
1. Create `di/MediaSessionModule.kt` with @Singleton MediaSession provider
2. Verify MusicPlaybackService still creates/owns ExoPlayer (unchanged)
3. Update MusicPlaybackService to inject MediaSession instead of creating it

### Phase 2: Android Auto Service
1. Create `AndroidAutoMediaBrowserService.kt`
2. Implement `onGetRoot()` and `onLoadChildren()`
3. Add to AndroidManifest.xml with intent-filter

### Phase 3: Browse Tree
1. Create `BrowseTree.kt` with logic for all 5 browse categories
2. Create `MediaItemConverter.kt` for Song/Album/Playlist → MediaItem
3. Implement private session flag check
4. Add error handling (try/catch, graceful empty lists)

### Phase 4: Integration
1. Verify both services start correctly
2. Test MediaSession is shared (single instance)
3. Manual Android Auto testing (Android Automotive emulator or car head unit)

### Phase 5: Regression Testing
1. Existing phone UI still works (HomeScreen, NowPlayingScreen, playlists, etc.)
2. Playback continues uninterrupted when Android Auto connects/disconnects
3. Settings changes (private session, preferences) reflected in Android Auto
4. No new crashes in logcat

---

## 6. Manifest Changes

Add to `<application>` block:

```xml
<!-- Android Auto MediaBrowserService -->
<service
    android:name=".data.service.AndroidAutoMediaBrowserService"
    android:exported="true"
    android:label="@string/app_name">
    <intent-filter>
        <action android:name="android.media.browse.MediaBrowserService" />
    </intent-filter>
</service>
```

**No other manifest changes needed.**
- Foreground service type already declared in MusicPlaybackService
- Permissions already sufficient
- No new hardware features required

---

## 7. Dependencies

**No new dependencies required.**
- `androidx.media3:media3-session` already in build.gradle
- Room, Hilt, Coroutines already present
- All necessary APIs available in Media3

---

## 8. Testing Strategy (Without Hardware)

### 8.1 Unit Tests
- BrowseTree logic (mocked repositories)
- MediaItem conversion
- Private session flag behavior

### 8.2 Integration Tests
- AndroidAutoMediaBrowserService with real MediaSession
- Browse tree queries with real Room database
- Error handling (deleted playlists, empty queue)

### 8.3 Manual Testing (With Emulator or Car Head Unit)

**Android Automotive Emulator:**
1. Create Android Automotive emulator image (API 30+)
2. Build and run app
3. Connect emulator to Android Auto app
4. Browse queue, playlists, albums, artists
5. Play songs via Android Auto
6. Verify phone UI continues working

**Physical Car Head Unit:**
1. Build debug APK, install on device
2. Connect car head unit via USB or WiFi
3. Browse and play music
4. Disconnect and reconnect
5. Switch between phone UI and car display

---

## 9. Risk Assessment & Mitigation

| Risk | Likelihood | Impact | Mitigation | Status |
|------|------------|--------|-----------|--------|
| MediaSession conflicts | Medium | High | Singleton injection | ✅ Mitigated |
| Playback interruption | Low | Critical | Independent services | ✅ Mitigated |
| Thread race conditions | Low | High | MediaController serialization | ✅ Mitigated |
| History logging confusion | Medium | Medium | Private session check | ✅ Mitigated |
| Browse crashes | Low | Medium | Try/catch + graceful empty lists | ✅ Mitigated |
| Missing metadata | Low | Low | MediaItem conversion utility | ✅ Mitigated |

**Overall Risk:** **LOW** — Architecture is sound, edge cases addressed, existing code unchanged.

---

## 10. Success Criteria

- ✅ Android Auto displays media browser with queue, playlists, albums, artists
- ✅ Play/pause/next/previous work from Android Auto
- ✅ Queue browsing functional
- ✅ Recently Played respects private session flag
- ✅ No changes to MusicPlaybackService
- ✅ No new crashes in existing phone UI
- ✅ Playback uninterrupted when Android Auto connects/disconnects
- ✅ All 12 existing screens still work correctly
- ✅ Equalizer, lyrics, dynamic theming unaffected

---

## 11. Rollout Plan

1. **Branch:** Create feature branch `feature/android-auto`
2. **Develop:** Implement in 5 phases (see Section 5)
3. **Test:** Unit + integration tests pass, manual testing successful
4. **Review:** Code review for MediaSession architecture, error handling
5. **Merge:** Merge to main after regression testing
6. **Release:** Include in next app version with "Android Auto Support" in release notes

---

## 12. Future Enhancements (Out of Scope)

- Custom Android Auto actions (not in v1.0)
- Search in Android Auto (driver safety limitation)
- Lyrics display (Android Auto screen size limitation)
- Playlist editing from Android Auto (too complex for driving)
- Voice commands (requires additional integration)

---

**Document approved by:** User
**Date approved:** 2026-02-18
**Next step:** Implementation plan (writing-plans skill)
