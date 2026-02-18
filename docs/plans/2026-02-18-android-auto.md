# Android Auto Integration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add MediaBrowserService-based Android Auto support with basic playback control and queue/playlist/album/artist browsing, without modifying existing MusicPlaybackService.

**Architecture:** Two independent MediaSessionService instances (MusicPlaybackService for playback, AndroidAutoMediaBrowserService for browsing) sharing a single MediaSession via Hilt singleton. Browse tree populated from Room repositories with graceful error handling.

**Tech Stack:**
- Jetpack Media3 (MediaBrowserService)
- Room (data layer)
- Hilt (dependency injection - singleton MediaSession)
- Kotlin Coroutines (async browse tree queries)
- No new external dependencies

---

## Phase 1: MediaSession Singleton Setup

### Task 1: Create MediaSessionModule with Singleton Provider

**Files:**
- Create: `app/src/main/java/com/sukoon/music/di/MediaSessionModule.kt`
- Modify: `app/src/main/java/com/sukoon/music/di/AppModule.kt` (if ExoPlayer/MediaSession currently created there)

**Step 1: Check current MediaSession creation location**

Run: `grep -r "MediaSession.Builder" app/src/main/java/com/sukoon/music/`

Expected: Find where MediaSession is currently created (likely in MusicPlaybackService or AppModule)

**Step 2: Write new MediaSessionModule**

Create `app/src/main/java/com/sukoon/music/di/MediaSessionModule.kt`:

```kotlin
package com.sukoon.music.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaSessionModule {

    @Singleton
    @Provides
    fun provideAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

    @Singleton
    @Provides
    fun provideExoPlayer(
        context: Context,
        audioAttributes: AudioAttributes
    ): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    @Singleton
    @Provides
    fun provideMediaSession(
        context: Context,
        player: ExoPlayer
    ): MediaSession {
        return MediaSession.Builder(context, player)
            .build()
    }
}
```

**Step 3: Update MusicPlaybackService to inject MediaSession**

Modify `app/src/main/java/com/sukoon/music/data/service/MusicPlaybackService.kt`:

Replace:
```kotlin
private lateinit var player: ExoPlayer
// ... later in onCreate()
player = ExoPlayer.Builder(this).build()
mediaSession = MediaSession.Builder(this, player).build()
```

With:
```kotlin
@Inject
lateinit var player: ExoPlayer

@Inject
lateinit var mediaSession: MediaSession
```

(Add to existing @Inject declarations at top of class)

Remove any ExoPlayer/MediaSession creation from `onCreate()` method.

**Step 4: Verify injection works**

Run: `./gradlew assembleDebug`

Expected: Compiles without errors, no "unresolved reference" for injected fields

**Step 5: Test on emulator**

Run: `./gradlew installDebug`

Install and open app, play a song. If playback works, MediaSession injection successful.

**Step 6: Commit**

```bash
git add app/src/main/java/com/sukoon/music/di/MediaSessionModule.kt
git add app/src/main/java/com/sukoon/music/data/service/MusicPlaybackService.kt
git commit -m "refactor: extract MediaSession to singleton module

Create MediaSessionModule with @Singleton ExoPlayer and MediaSession providers.
Update MusicPlaybackService to inject instead of create these instances.
This enables Android Auto service to share same MediaSession without conflicts.

Verification: App builds, playback works on emulator."
```

---

## Phase 2: Android Auto Service Implementation

### Task 2: Create AndroidAutoMediaBrowserService Stub

**Files:**
- Create: `app/src/main/java/com/sukoon/music/data/service/AndroidAutoMediaBrowserService.kt`

**Step 1: Write AndroidAutoMediaBrowserService class**

Create `app/src/main/java/com/sukoon/music/data/service/AndroidAutoMediaBrowserService.kt`:

```kotlin
package com.sukoon.music.data.service

import android.os.Bundle
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaBrowserService
import androidx.media3.session.LibraryResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.sukoon.music.util.DevLogger

@AndroidEntryPoint
class AndroidAutoMediaBrowserService : MediaBrowserService() {

    @Inject
    lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        try {
            setMediaSession(mediaSession)
            DevLogger.debug("Android Auto MediaSession registered")
        } catch (e: Exception) {
            DevLogger.error("Failed to set MediaSession for Android Auto: ${e.message}")
        }
    }

    override fun onGetLibraryRoot(
        session: MediaSession.ControllerInfo,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<com.google.android.gms.wearable.DataItem>> {
        // Placeholder: will implement in Task 3
        return Futures.immediateFuture(
            LibraryResult.ofError(
                LibraryResult.RESULT_ERROR_NOT_SUPPORTED
            )
        )
    }

    override fun onLoadChildren(
        session: MediaSession.ControllerInfo,
        parentId: String,
        params: LibraryParams?,
        callback: Result<ImmutableList<com.google.android.gms.wearable.DataItem>>
    ) {
        // Placeholder: will implement in Task 4
        callback.sendResult(ImmutableList.of())
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
    }
}
```

**Step 2: Fix imports and compile**

Run: `./gradlew assembleDebug`

Expected: May have import errors. Correct them by reviewing Media3 API docs for MediaBrowserService correct return types.

**Step 3: Update AndroidManifest.xml**

Add to `app/src/main/AndroidManifest.xml` inside `<application>` block:

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

**Step 4: Verify compilation**

Run: `./gradlew assembleDebug`

Expected: Compiles successfully

**Step 5: Test app still runs**

Run: `./gradlew installDebug`

Install and verify phone playback still works (AndroidAutoMediaBrowserService doesn't interfere).

**Step 6: Commit**

```bash
git add app/src/main/java/com/sukoon/music/data/service/AndroidAutoMediaBrowserService.kt
git add app/src/main/AndroidManifest.xml
git commit -m "feat: add AndroidAutoMediaBrowserService stub

Create new MediaBrowserService that injects shared MediaSession.
Register in AndroidManifest with media browse intent-filter.
Stub implementations of onGetLibraryRoot and onLoadChildren (to be filled in).

Verification: App builds and playback still works."
```

---

## Phase 3: Browse Tree Implementation

### Task 3: Create BrowseNode Data Model

**Files:**
- Create: `app/src/main/java/com/sukoon/music/data/browsertree/BrowseNode.kt`

**Step 1: Define BrowseNode class**

Create `app/src/main/java/com/sukoon/music/data/browsertree/BrowseNode.kt`:

```kotlin
package com.sukoon.music.data.browsertree

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class BrowseNode(
    val mediaId: String,
    val title: String,
    val subtitle: String? = null,
    val mediaUri: String? = null,
    val artworkUri: String? = null,
    val isPlayable: Boolean = false,
    val isBrowsable: Boolean = true
) {
    fun toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setArtworkUri(artworkUri?.let { android.net.Uri.parse(it) })
            .build()

        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(mediaUri?.let { android.net.Uri.parse(it) })
            .setMediaMetadata(metadata)
            .setIsPlayable(isPlayable)
            .setIsPlaylistOrFolder(!isPlayable)
            .build()
    }
}
```

**Step 2: Compile**

Run: `./gradlew compileDebugKotlin`

Expected: No errors

**Step 3: Commit**

```bash
git add app/src/main/java/com/sukoon/music/data/browsertree/BrowseNode.kt
git commit -m "feat: add BrowseNode data model

Define BrowseNode to wrap playlist items, albums, songs for Android Auto.
Includes toMediaItem() conversion for MediaBrowserService compatibility.

Verification: Compiles."
```

---

### Task 4: Create MediaItemConverter Utility

**Files:**
- Create: `app/src/main/java/com/sukoon/music/data/browsertree/MediaItemConverter.kt`

**Step 1: Define converter extension functions**

Create `app/src/main/java/com/sukoon/music/data/browsertree/MediaItemConverter.kt`:

```kotlin
package com.sukoon.music.data.browsertree

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.domain.model.Playlist

fun Song.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(albumArtUri?.let { Uri.parse(it) })
        .build()

    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(Uri.parse(path))
        .setMediaMetadata(metadata)
        .setIsPlayable(true)
        .build()
}

fun Album.toMediaItem(withUri: Boolean = false): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setArtist(artist)
        .setArtworkUri(albumArtUri?.let { Uri.parse(it) })
        .build()

    return MediaItem.Builder()
        .setMediaId("album_$id")
        .apply { if (withUri) setUri(Uri.parse(albumArtUri ?: "")) }
        .setMediaMetadata(metadata)
        .setIsPlayable(false)
        .setIsPlaylistOrFolder(true)
        .build()
}

fun Artist.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setArtworkUri(artistArtUri?.let { Uri.parse(it) })
        .build()

    return MediaItem.Builder()
        .setMediaId("artist_$id")
        .setMediaMetadata(metadata)
        .setIsPlayable(false)
        .setIsPlaylistOrFolder(true)
        .build()
}

fun Playlist.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .build()

    return MediaItem.Builder()
        .setMediaId("playlist_$id")
        .setMediaMetadata(metadata)
        .setIsPlayable(false)
        .setIsPlaylistOrFolder(true)
        .build()
}
```

**Step 2: Compile**

Run: `./gradlew compileDebugKotlin`

Expected: No errors (verify domain models exist)

**Step 3: Commit**

```bash
git add app/src/main/java/com/sukoon/music/data/browsertree/MediaItemConverter.kt
git commit -m "feat: add MediaItemConverter extensions

Add extension functions to convert Song, Album, Artist, Playlist to MediaItem
for Android Auto browsing. Includes proper metadata (title, artist, artwork).

Verification: Compiles."
```

---

### Task 5: Create BrowseTree with Root & Category Items

**Files:**
- Create: `app/src/main/java/com/sukoon/music/data/browsertree/BrowseTree.kt`

**Step 1: Write BrowseTree class with constants**

Create `app/src/main/java/com/sukoon/music/data/browsertree/BrowseTree.kt`:

```kotlin
package com.sukoon.music.data.browsertree

import androidx.media3.common.MediaItem
import com.sukoon.music.domain.repository.PlaylistRepository
import com.sukoon.music.domain.repository.SongRepository
import com.sukoon.music.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import com.sukoon.music.util.DevLogger

class BrowseTree @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        const val ROOT_ID = "root"
        const val QUEUE_ID = "queue"
        const val NOW_PLAYING_ID = "now_playing"
        const val PLAYLISTS_ID = "playlists"
        const val ALBUMS_ID = "albums"
        const val ARTISTS_ID = "artists"
        const val RECENTLY_PLAYED_ID = "recently_played"
    }

    suspend fun getChildren(parentId: String): List<MediaItem> {
        return try {
            when (parentId) {
                ROOT_ID -> getRootChildren()
                QUEUE_ID -> getQueueChildren()
                PLAYLISTS_ID -> getPlaylistsChildren()
                ALBUMS_ID -> getAlbumsChildren()
                ARTISTS_ID -> getArtistsChildren()
                RECENTLY_PLAYED_ID -> getRecentlyPlayedChildren()
                else -> {
                    if (parentId.startsWith("playlist_")) {
                        getPlaylistSongsChildren(parentId)
                    } else if (parentId.startsWith("album_")) {
                        getAlbumSongsChildren(parentId)
                    } else if (parentId.startsWith("artist_")) {
                        getArtistSongsChildren(parentId)
                    } else {
                        emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            DevLogger.error("Browse error for parentId=$parentId: ${e.message}")
            emptyList()
        }
    }

    private suspend fun getRootChildren(): List<MediaItem> {
        val children = mutableListOf<MediaItem>()

        // Add browsable categories
        children.add(
            MediaItem.Builder()
                .setMediaId(QUEUE_ID)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle("Queue")
                        .build()
                )
                .setIsPlaylistOrFolder(true)
                .build()
        )

        children.add(
            MediaItem.Builder()
                .setMediaId(PLAYLISTS_ID)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle("Playlists")
                        .build()
                )
                .setIsPlaylistOrFolder(true)
                .build()
        )

        children.add(
            MediaItem.Builder()
                .setMediaId(ALBUMS_ID)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle("Albums")
                        .build()
                )
                .setIsPlaylistOrFolder(true)
                .build()
        )

        children.add(
            MediaItem.Builder()
                .setMediaId(ARTISTS_ID)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle("Artists")
                        .build()
                )
                .setIsPlaylistOrFolder(true)
                .build()
        )

        // Only show Recently Played if NOT in private session
        if (!preferencesManager.isPrivateSession()) {
            children.add(
                MediaItem.Builder()
                    .setMediaId(RECENTLY_PLAYED_ID)
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle("Recently Played")
                            .build()
                    )
                    .setIsPlaylistOrFolder(true)
                    .build()
            )
        }

        return children
    }

    private suspend fun getQueueChildren(): List<MediaItem> {
        // TODO: Implement in Task 6 (needs PlaybackRepository)
        return emptyList()
    }

    private suspend fun getPlaylistsChildren(): List<MediaItem> {
        // TODO: Implement in Task 7
        return emptyList()
    }

    private suspend fun getAlbumsChildren(): List<MediaItem> {
        // TODO: Implement in Task 8
        return emptyList()
    }

    private suspend fun getArtistsChildren(): List<MediaItem> {
        // TODO: Implement in Task 9
        return emptyList()
    }

    private suspend fun getRecentlyPlayedChildren(): List<MediaItem> {
        // TODO: Implement in Task 10
        return emptyList()
    }

    private suspend fun getPlaylistSongsChildren(playlistId: String): List<MediaItem> {
        // TODO: Implement in Task 11
        return emptyList()
    }

    private suspend fun getAlbumSongsChildren(albumId: String): List<MediaItem> {
        // TODO: Implement in Task 12
        return emptyList()
    }

    private suspend fun getArtistSongsChildren(artistId: String): List<MediaItem> {
        // TODO: Implement in Task 13
        return emptyList()
    }
}
```

**Step 2: Compile**

Run: `./gradlew compileDebugKotlin`

Expected: No errors

**Step 3: Commit**

```bash
git add app/src/main/java/com/sukoon/music/data/browsertree/BrowseTree.kt
git commit -m "feat: add BrowseTree skeleton with root categories

Implement getRootChildren() with Queue, Playlists, Albums, Artists, Recently Played.
Respect private session flag to hide Recently Played when active.
Stub implementations for all child category methods (to be filled in next tasks).

Verification: Compiles."
```

---

### Task 6: Implement Queue Browse Children

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/data/browsertree/BrowseTree.kt`

**Step 1: Inject PlaybackRepository**

In BrowseTree constructor, add:

```kotlin
private val playbackRepository: PlaybackRepository,
```

(Verify PlaybackRepository exists in domain layer; if not, create minimal interface)

**Step 2: Implement getQueueChildren()**

Replace TODO in BrowseTree:

```kotlin
private suspend fun getQueueChildren(): List<MediaItem> {
    return try {
        val queue = playbackRepository.getCurrentQueue().first()
        queue.items.map { it.song.toMediaItem() }
    } catch (e: Exception) {
        DevLogger.error("Failed to load queue: ${e.message}")
        emptyList()
    }
}
```

**Step 3: Compile**

Run: `./gradlew compileDebugKotlin`

Expected: No errors

**Step 4: Commit**

```bash
git add app/src/main/java/com/sukoon/music/data/browsertree/BrowseTree.kt
git commit -m "feat: implement queue browsing in BrowseTree

Load current queue from PlaybackRepository and convert songs to MediaItems.
Handle empty queue gracefully with empty list return.

Verification: Compiles."
```

---

### Task 7: Implement Playlists Browse Children

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/data/browsertree/BrowseTree.kt`

**Step 1: Implement getPlaylistsChildren()**

```kotlin
private suspend fun getPlaylistsChildren(): List<MediaItem> {
    return try {
        playlistRepository.getAllPlaylists()
            .first()
            .map { it.toMediaItem() }
    } catch (e: Exception) {
        DevLogger.error("Failed to load playlists: ${e.message}")
        emptyList()
    }
}
```

**Step 2: Compile & commit**

Run: `./gradlew compileDebugKotlin`

```bash
git commit -m "feat: implement playlists browsing in BrowseTree

Load all playlists from PlaylistRepository and convert to MediaItems.

Verification: Compiles."
```

---

### Task 8-10: Implement Albums, Artists, Recently Played (Similar Pattern)

Repeat Task 7 pattern for:

**Task 8:** `getAlbumsChildren()` using `songRepository.getAllAlbums()`

**Task 9:** `getArtistsChildren()` using `songRepository.getAllArtists()`

**Task 10:** `getRecentlyPlayedChildren()` using `recentlyPlayedRepository.getRecentlyPlayedSongs(limit = 20)`

Each gets its own commit.

---

### Task 11-13: Implement Nested Browse (Playlist/Album/Artist Songs)

**Task 11:** `getPlaylistSongsChildren()`

Extract playlistId from mediaId, fetch songs via `playlistRepository.getPlaylistSongs(id)`

**Task 12:** `getAlbumSongsChildren()`

Extract albumId, fetch songs via `songRepository.getSongsByAlbum(id)`

**Task 13:** `getArtistSongsChildren()`

Extract artistId, fetch songs via `songRepository.getSongsByArtist(id)`

Each with try/catch returning empty list on error.

---

## Phase 4: Integrate BrowseTree into AndroidAutoMediaBrowserService

### Task 14: Wire BrowseTree into AndroidAutoMediaBrowserService

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/data/service/AndroidAutoMediaBrowserService.kt`

**Step 1: Inject BrowseTree and update service**

```kotlin
@Inject
lateinit var browseTree: BrowseTree

@Inject
@ApplicationScope
lateinit var scope: CoroutineScope

override fun onGetLibraryRoot(
    session: MediaSession.ControllerInfo,
    params: MediaBrowserService.LibraryParams?
): ListenableFuture<MediaBrowserService.LibraryResult<MediaItem>> {
    return Futures.immediateFuture(
        MediaBrowserService.LibraryResult.ofItem(
            MediaItem.Builder()
                .setMediaId(BrowseTree.ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Sukoon Music")
                        .build()
                )
                .setIsPlaylistOrFolder(true)
                .build(),
            params
        )
    )
}

override fun onLoadChildren(
    session: MediaSession.ControllerInfo,
    parentId: String,
    params: MediaBrowserService.LibraryParams?,
    callback: MediaBrowserService.Result<ImmutableList<MediaItem>>
) {
    scope.launch {
        val children = browseTree.getChildren(parentId)
        callback.sendResult(ImmutableList.copyOf(children))
    }
}
```

**Step 2: Update imports**

Add necessary Media3 imports.

**Step 3: Compile & test**

Run: `./gradlew assembleDebug`

Run: `./gradlew installDebug`

Open app, play a song (verify playback still works).

**Step 4: Commit**

```bash
git add app/src/main/java/com/sukoon/music/data/service/AndroidAutoMediaBrowserService.kt
git commit -m "feat: integrate BrowseTree into AndroidAutoMediaBrowserService

Inject BrowseTree and implement onGetLibraryRoot() and onLoadChildren().
Service now provides complete browse hierarchy to Android Auto.

Verification: App builds, playback works."
```

---

## Phase 5: Testing & Verification

### Task 15: Write Unit Tests for BrowseTree (TDD)

**Files:**
- Create: `app/src/test/java/com/sukoon/music/data/browsertree/BrowseTreeTest.kt`

**Step 1: Write failing tests**

```kotlin
package com.sukoon.music.data.browsertree

import com.sukoon.music.domain.repository.PlaylistRepository
import com.sukoon.music.domain.repository.SongRepository
import com.sukoon.music.data.preferences.PreferencesManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class BrowseTreeTest {

    private val playlistRepository = mockk<PlaylistRepository>()
    private val songRepository = mockk<SongRepository>()
    private val preferencesManager = mockk<PreferencesManager>()

    private val browseTree = BrowseTree(
        playlistRepository,
        songRepository,
        preferencesManager
    )

    @Test
    fun `getRootChildren includes Queue, Playlists, Albums, Artists`() = runTest {
        coEvery { preferencesManager.isPrivateSession() } returns false

        val children = browseTree.getChildren(BrowseTree.ROOT_ID)

        assertEquals(5, children.size)  // Queue, Playlists, Albums, Artists, Recently Played
        assertEquals("Queue", children[0].mediaMetadata?.title)
        assertEquals("Playlists", children[1].mediaMetadata?.title)
    }

    @Test
    fun `getRootChildren excludes Recently Played in private session`() = runTest {
        coEvery { preferencesManager.isPrivateSession() } returns true

        val children = browseTree.getChildren(BrowseTree.ROOT_ID)

        assertEquals(4, children.size)  // Queue, Playlists, Albums, Artists (NO Recently Played)
    }

    @Test
    fun `getQueueChildren returns empty list on error`() = runTest {
        coEvery { playbackRepository.getCurrentQueue() } throws Exception("DB error")

        val children = browseTree.getChildren(BrowseTree.QUEUE_ID)

        assertEquals(0, children.size)
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew test`

Expected: FAIL (methods don't exist yet in our test setup)

**Step 3: Run implementation**

(Already done in previous tasks - BrowseTree is fully implemented)

**Step 4: Run tests again**

Run: `./gradlew test`

Expected: PASS

**Step 5: Commit**

```bash
git add app/src/test/java/com/sukoon/music/data/browsertree/BrowseTreeTest.kt
git commit -m "test: add unit tests for BrowseTree

Test getRootChildren() with/without private session, error handling.
Tests verify browse tree returns correct categories and gracefully
handles database errors.

Verification: All tests pass."
```

---

### Task 16: Manual Integration Testing (Android Auto Emulator)

**Files:** None (testing only)

**Steps:**

1. **Create Android Automotive Emulator**
   - Open Android Studio → AVD Manager
   - Create new emulator with image "Android Automotive" (API 30+)

2. **Build and Install Debug APK**
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug -s  # serial for emulator
   ```

3. **Open Android Auto App on Emulator**
   - Install "Android Auto" app on emulator (usually pre-installed)
   - Allow media browser permissions

4. **Test Browse Tree**
   - Tap "Browse" in Android Auto UI
   - Verify you see: Queue, Playlists, Albums, Artists, Recently Played
   - Tap "Playlists" → see all playlists
   - Tap a playlist → see songs in playlist
   - Tap a song → playback starts

5. **Test Playback Controls**
   - Play/pause button works
   - Next/previous skip songs
   - Seek bar moves
   - Verify phone UI still works simultaneously

6. **Test Private Session**
   - Go to phone Settings → toggle "Private Session"
   - Return to Android Auto browser
   - Verify "Recently Played" disappears
   - Verify "Recently Played" re-appears when private session disabled

7. **Verify No Regressions**
   - All 12 existing phone screens still work
   - Playlists, lyrics, equalizer still functional
   - No new crashes in logcat

**Commit if successful:**

```bash
git commit --allow-empty -m "test: manual Android Auto integration testing

Tested on Android Automotive emulator:
- Browse tree shows all categories (Queue, Playlists, Albums, Artists, Recently Played)
- Playback controls (play/pause, next, skip, seek) work
- Private session flag properly hides Recently Played
- All existing phone UI screens still functional
- No new crashes

Verification: PASSED"
```

---

## Phase 6: Final Cleanup & Documentation

### Task 17: Add Android Auto Intent-Filter to AndroidManifest.xml (if missing)

Verify manifest has:

```xml
<service
    android:name=".data.service.AndroidAutoMediaBrowserService"
    android:exported="true"
    android:label="@string/app_name">
    <intent-filter>
        <action android:name="android.media.browse.MediaBrowserService" />
    </intent-filter>
</service>
```

(Should already be there from Task 2, but verify.)

---

### Task 18: Final Regression Testing on Real Device

**Steps:**

1. Build release APK: `./gradlew assembleRelease`
2. Install on real device with Android Auto head unit capability
3. Connect via USB or WiFi
4. Test all 5 browse categories
5. Test playback controls
6. Test private session toggle
7. Verify existing features (playlists, search, settings, etc.) still work
8. Check logcat for any errors

**Commit:**

```bash
git commit --allow-empty -m "test: regression testing on real device

Tested Android Auto on real car head unit:
- All browse categories functional
- Playback controls responsive
- Private session properly respected
- Existing phone app features intact
- No crashes or errors in logcat

Verification: PASSED"
```

---

## Summary of New Files

```
app/src/main/java/com/sukoon/music/
├── di/
│   └── MediaSessionModule.kt (NEW)
├── data/
│   ├── service/
│   │   └── AndroidAutoMediaBrowserService.kt (NEW)
│   └── browsertree/
│       ├── BrowseTree.kt (NEW)
│       ├── BrowseNode.kt (NEW)
│       └── MediaItemConverter.kt (NEW)

app/src/test/java/com/sukoon/music/
└── data/browsertree/
    └── BrowseTreeTest.kt (NEW)
```

## Summary of Modified Files

```
app/src/main/java/com/sukoon/music/
├── data/service/
│   └── MusicPlaybackService.kt (MODIFIED - remove ExoPlayer/MediaSession creation, inject)
└── AndroidManifest.xml (MODIFIED - add service declaration)
```

## Build Commands Reference

- **Build:** `./gradlew assembleDebug`
- **Install:** `./gradlew installDebug`
- **Lint:** `./gradlew lint`
- **Tests:** `./gradlew test`
- **Emulator Tests:** `./gradlew connectedAndroidTest`

---

## Execution Strategy

**This plan has 18 tasks:**
- Tasks 1-2: Setup (MediaSession singleton, service stub)
- Tasks 3-13: Implementation (BrowseTree components)
- Task 14: Integration (wire into service)
- Tasks 15-18: Testing & verification

**Estimated Time:** 4-6 hours for experienced Android developer familiar with Media3

**Checkpoints:**
- After Task 2: App still builds and playback works
- After Task 6: BrowseTree stub compiles
- After Task 14: Android Auto browse tree functional
- After Task 15: Unit tests pass
- After Task 16: Manual integration testing on emulator complete
- After Task 18: Final regression testing on real device complete

---

**Plan Status:** READY FOR IMPLEMENTATION
**Last Updated:** 2026-02-18
**Next Step:** Choose execution approach below
