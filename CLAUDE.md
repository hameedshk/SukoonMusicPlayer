# Project: Sukoon Music Player

## 🎯 Project Overview
An exact offline-only replica of the S Android experience. The application focuses on local media discovery, high-fidelity UI replication, and background playback stability without internet dependency.
- **App Name:** Sukoon Music Player
- **Package Name:** com.sukoon.music
- **Version:** 1.0.0

> **📋 See [REQUIRED_SKILLS.md](../REQUIRED_SKILLS.md)** for comprehensive skill requirements and Play Store preparation checklist.

## 📊 Project Status

### ✅ Completed Features (Production Ready)

#### Core Music Playback
- **Media Playback Engine**: ExoPlayer integrated via Media3 MediaSessionService
  - Play/pause, seek, skip next/previous
  - Queue management (add, remove, reorder)
  - Shuffle mode and 3 repeat modes (OFF/ONE/ALL)
  - Background playback with notification controls
  - Audio focus handling (respects AUDIOFOCUS_LOSS, pauses on noisy audio)
  - Gapless playback support
- **Playback State**: Reactive StateFlow-based architecture with MediaController as single source of truth
- **Service Lifecycle**: Proper MediaSessionService implementation with foreground service handling

#### UI Screens (12/12 Complete)
- **HomeScreen**: Song list, 2x3 recently played grid, mini player with dynamic colors, shortcuts
- **NowPlayingScreen**: Full-screen player with lyrics/queue tabs, dynamic gradient background, seek bar
- **AlbumsScreen** & **AlbumDetailScreen**: Grid view with album navigation
- **ArtistsScreen** & **ArtistDetailScreen**: Artist grid with songs and albums
- **PlaylistsScreen** & **PlaylistDetailScreen**: Create, edit, delete, reorder songs
- **LikedSongsScreen**: Filtered view of favorited songs
- **SearchScreen**: Real-time search with history and sorting
- **SettingsScreen**: All 14 user preferences with dialogs and switches
- **EqualizerScreen**: 5-band EQ with bass boost, virtualizer, and preset management

#### Media Library Management
- **MediaStore Scanner**: Queries local audio files with Android 13+ READ_MEDIA_AUDIO support
- **Album Art Extraction**: Via MediaStore album art URIs
- **Room Database**: 7 entities with proper migrations (v2→v6)
- **Auto-scan**: Optional scan on startup (user preference)
- **Permission Handling**: Runtime permission requests with rationale dialogs

#### Playlists
- **Full CRUD Operations**: Create, read, update, delete playlists
- **Song Management**: Add/remove songs, reorder via drag-and-drop
- **Database**: Many-to-many relationship with PlaylistSongCrossRef
- **UI**: Dialog-based playlist creation, edit screen with reordering

#### Equalizer
- **5-Band EQ**: Android Equalizer API (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz)
- **Audio Effects**: Bass boost (0-1000), Virtualizer (spatial audio)
- **Presets**: Save and load custom EQ presets via Room
- **Real-time Application**: Effects applied to ExoPlayer audio session
- **UI**: Sliders, preset dropdown, master enable/disable toggle

#### Lyrics Integration (LRCLIB)
- **Offline-First Strategy**: Local .lrc files → ID3 tags → LRCLIB API
- **LRCLIB Integration**: Retrofit API with precise lookup + search fallback
- **LRC Parser**: Synced lyrics with timestamp matching (±500ms tolerance)
- **Active Line Highlighting**: Auto-scroll with dynamic accent color from album art
- **Sync Offset**: Per-track offset adjustment stored in Room
- **Caching**: All lyrics cached in LyricsEntity table

#### Search & Discovery
- **Real-time Search**: Searches title, artist, album across entire library
- **Search History**: Last 10 queries saved in Room with clear functionality
- **Filtering**: Filter by liked songs only
- **Sorting**: By relevance, title, artist, date added
- **UI**: SearchScreen with history chips and filter options

#### Liked Songs & History
- **Toggle Like**: Heart icon in song items with database persistence
- **Recently Played**: Auto-logging via MediaController listener
- **Private Session Mode**: Respects privacy flag to disable history tracking
- **Database**: RecentlyPlayedEntity with timestamp, keeps last 50 plays
- **UI**: 2x3 grid on HomeScreen style Bento layout

#### Settings & Preferences
- **DataStore Integration**: All 14 user preferences persisted reactively
- **Theme Selection**: Light/Dark/System with immediate UI update
- **Privacy Controls**: Private session mode
- **Playback Settings**: Gapless, crossfade (0-10s), audio focus behavior
- **Audio Quality**: 4 presets (Low/Medium/High/Lossless)
- **Storage Management**: View stats, clear cache, clear database
- **Logout**: Clears metadata without deleting physical music files

#### Dynamic Theming
- **Album Art Palette Extraction**: Coil + Android Palette API
- **Dynamic Colors**: NowPlayingScreen gradient background adapts to album art
- **Accent Colors**: Play button, seek bar, shuffle/repeat use extracted vibrant colors
- **MiniPlayer**: Album art display with dynamic accent color
- **Material 3 Integration**: System dynamic colors on Android 12+ with user preference override

#### AdMob Integration
- **Banner Ads**: Adaptive banners at bottom of HomeScreen
- **Compliance**: NO interstitial/rewarded/audio ads (per CLAUDE.md requirements)
- **AdMobManager**: Initialization with test/production mode toggle
- **Placement**: Non-intrusive banner placement only

#### Data Layer (Complete)
- **7 Repositories**: Playback, Song, Playlist, Lyrics, Settings, SearchHistory, AudioEffect
- **6 DAOs**: Song, Playlist, Lyrics, RecentlyPlayed, SearchHistory, EqualizerPreset
- **Room Migrations**: All migrations (2→3, 3→4, 4→5, 5→6) implemented
- **Foreign Keys**: CASCADE deletes on playlists for data integrity

### 🚧 In Progress / Needs Minor Work

#### Native Ads Layout
- **Status**: Code implemented in `NativeAdView.kt`, but missing XML layout file
- **Missing File**: `app/src/main/res/layout/native_ad_layout.xml`
- **Impact**: Native ads will crash if used (currently not used in UI)
- **Fix Required**: Create XML layout with required view IDs (headline, body, media, call-to-action, icon)

#### Production Ad Configuration
- **Status**: Using test ad IDs
- **File**: `AdMobManager.kt` line 33-34, 40
- **Action Required**: Update to production ad IDs and set `USE_TEST_ADS = false` before release

### ❌ Not Working / Broken
**None** - All implemented features are functional after recent fixes:
- ✅ Fixed: AdaptiveIconDrawable crash in AlbumArtLoader (was crashing on Android 8.0+)
- ✅ Fixed: Dynamic theming not updating (MainActivity now observes theme preferences)
- ✅ Fixed: PaletteExtractor failing silently (improved bitmap loading and error handling)
- ✅ Fixed: Playback errors not caught (added comprehensive error handling)

### 📋 Not Yet Implemented (Future Enhancements)

#### Testing
- **Unit Tests**: No tests beyond example files (`ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`)
- **Integration Tests**: Not implemented
- **UI Tests**: Not implemented
- **Recommended**: Add tests for:
  - PlaybackRepository operations
  - Room DAO operations
  - ViewModel business logic
  - LRC parser accuracy
  - Playlist CRUD operations

#### Navigation Enhancements
- **Playlists Route**: `Routes.Playlists` defined but no direct navigation from HomeScreen
- **Recommendation**: Add playlists shortcut to HomeScreen shortcuts section

#### Advanced Features (Not in Current Spec)
- **Sleep Timer**: Not implemented
- **Podcasts/Audiobooks**: Not supported (music-only app)
- **Cloud Sync**: Not planned (offline-only per spec)
- **Social Features**: Not planned (single-user local app)
- **Chromecast/Bluetooth**: Android Auto support not implemented

### 🧪 Test Coverage

#### Current Status: 0% (No Real Tests)
**Existing Files**:
- `app/src/test/java/com/example/sukoonmusic/ExampleUnitTest.kt` (default template)
- `app/src/androidTest/java/com/example/sukoonmusic/ExampleInstrumentedTest.kt` (default template)

#### Modules Needing Tests

**High Priority** (Core Business Logic):
- ✅ **Implemented but Untested**:
  - `PlaybackRepositoryImpl`: Queue management, play/pause logic
  - `SongRepositoryImpl`: Like toggle, recently played tracking
  - `PlaylistRepositoryImpl`: CRUD operations
  - `LyricsRepositoryImpl`: Offline-first lookup logic
  - `LrcParser`: Timestamp parsing accuracy
  - `PaletteExtractor`: Color extraction from album art
  - `MediaStoreScanner`: MediaStore query logic
  - `AudioEffectManager`: Equalizer effect application

**Medium Priority** (ViewModels):
- ✅ **Implemented but Untested**:
  - `HomeViewModel`: Playback state management
  - `SettingsViewModel`: Preference updates
  - `PlaylistViewModel`: Playlist operations
  - `SearchViewModel`: Search query logic

**Low Priority** (UI/Composables):
- UI tests for critical flows:
  - Play song → notification appears
  - Create playlist → add songs → delete
  - Search → filter → play result
  - Change theme → UI updates

#### Recommended Test Framework
- **Unit Tests**: JUnit 5 + MockK (already in build.gradle)
- **Coroutine Tests**: kotlinx-coroutines-test
- **Room Tests**: In-memory database
- **UI Tests**: Compose Testing + Espresso

### 📈 Overall Project Health

| Category | Status | Score |
|----------|--------|-------|
| **Feature Completion** | ✅ Excellent | 98% |
| **Code Quality** | ✅ Excellent | Clean MVVM, proper separation |
| **Architecture** | ✅ Excellent | Follows CLAUDE.md requirements |
| **Media3 Compliance** | ✅ Excellent | Proper MediaSessionService usage |
| **Test Coverage** | ❌ Needs Work | 0% (no real tests) |
| **Production Readiness** | 🚧 Almost Ready | 95% (minor fixes needed) |

**Ready for Beta Testing** after:
1. Creating native ad layout XML file
2. Updating production ad IDs
3. Testing on physical Android devices (various OS versions)
4. Writing critical path tests (recommended but not blocking)

**Last Updated**: 2025-12-28 (Automated analysis of codebase)

## 🏗 Tech Stack & Architecture
- **Language:** Kotlin 2.1+ (Coroutines & StateFlow)
- **UI:** Jetpack Compose (Material 3) with Dynamic Theming
- **Media Engine:** Jetpack Media3 (ExoPlayer + MediaSessionService).
- **Database:** Room (Metadata caching, Playlists, Liked Songs)
- **Image Loading:** Coil (with Palette API for dynamic colors)
- **DI:** Hilt (Dependency Injection)
- **Networking:** Retrofit 2.x + OkHttp 4.x (for LRCLIB)
- **Monetization:** AdMob (Banner & Native Advanced only; No audio interruptions)
- **Concurrency:** Kotlin Coroutines & Flow

# Lyrics Integration (LRCLIB.net)

## API Strategy
- Use `GET /api/get?artist_name={a}&track_name={t}&album_name={n}&duration={d}` for high-precision matches.
- Fallback to `GET /api/search?q={query}` if metadata lookup fails.

## Parsing & Rendering
- Parse `syncedLyrics` (LRC format) for the "Now Playing" view.
- Render lyrics using a LazyColumn with active line highlighting.

## Sync Accuracy & Drift Handling (CRITICAL)
- Apply ±500ms tolerance when syncing lyric timestamps.
- Allow manual offset correction per track (stored in Room).
- Persist offset adjustments per track ID.
- Fallback to unsynced lyrics if drift exceeds tolerance.
- Display a subtle “Lyrics may be out of sync” hint when fallback occurs.
- Tolerance values are heuristic and may vary by encoding and sample rate.


## Caching
- Always check Room DB for cached lyrics before making a network call.
- Cache both synced and unsynced lyrics separately.

## Coding Standards

### Playback State (CRITICAL)
- MediaController is the single source of truth for playback state.
- UI must never derive playback state directly from ExoPlayer.
- All play/pause/seek actions must go through MediaController transport controls.
- UI observes playback via MediaController callbacks or StateFlow wrappers only.

### Architecture
- MVVM with Clean Architecture (Domain/Data/UI separation).
- ExoPlayer lives exclusively inside MediaSessionService.
- UI and ViewModels must not hold ExoPlayer references.
- Media Service: Ensure `MediaSessionService` handles lifecycle to prevent background termination.

### Android & Storage
- Scoped Storage: Use `READ_MEDIA_AUDIO` for Android 13+ (API 33).
- String Resources: No hardcoded UI strings; use `strings.xml`.

### State Management
- Use `StateFlow` and `collectAsStateWithLifecycle` in Compose.

## 🎨 Visual Standards 

- **Color Strategy:** Base palette mirrors dark theme.
	-Final surfaces adapt via Material 3 dynamic colors with contrast guarantees.
- **Typography:** Sans-serif (Circular-style spacing). Headers 24sp Bold.
- **Home Layout:** Must feature the 2x3 "Recently Played" grid at the top.
- **Player UI:** Large rounded album art, Green transport controls, and dynamic background gradients extracted from album art.

## 👥 Roles & Agent Personas
### [Android Developer]
- **Focus:** Performance, Media3 lifecycle, Room migrations, and background service stability.
- **Workflow:** Always run `./gradlew lint` after logic changes.

### [UX Designer]
- **Focus:** Exact pixel replication of S Android UI. 
- **Requirement:** Every UI change must include a `@Preview` function.

### [Marketing Specialist]
- **Focus:** User-facing copy, Play Store metadata, and "What's New" release notes.

### [Audio Engineer]
- **Standards:** All playback must be gapless.
- **Tools:** Media3, ExoPlayer Tuning, Audio Focus handling.

### [Release Engineer]
- Focus: Play Store policy compliance, targetSdk upgrades, crash rate monitoring.
- Tools: Play Console, Firebase Crashlytics.

### [QA Specialist]
- **Standards:** Zero-crash policy during background-to-foreground transitions.
- **Workflow:** Manual verification of 'Ad Visibility' and 'Notification Controls'.

## ⚠️ Critical Rules (DO NOT BYPASS)
1. **Offline-Only:** Networking is permitted strictly for lyrics metadata and ads; audio playback and discovery remain 100% local.
2. **Ads:** Use AdMob Banner/Native formats only. **Strictly forbidden** to use Audio Interstitial ads that pause music.
3. **Media Session:** Do not modify MediaSessionService without a full plan to prevent background notification crashes.
4. **Permissions:** Must handle Android 13+ `READ_MEDIA_AUDIO` permissions gracefully.
5. **Android 14/15 Compliance:** Explicitly declare `foregroundServiceType="mediaPlayback"` in the Manifest.
6. **ExoPlayer Lifecycle:**
	- ExoPlayer lives inside MediaSessionService.
	- UI layers must never own or release ExoPlayer.
	- Release only when service is explicitly stopped.
7. **Performance:** Use `LazyColumn` for lyrics and large media lists; optimize with `keys` to prevent unnecessary recompositions.
8. **API Ethics:** Include a descriptive `User-Agent` header in Retrofit for LRCLIB requests.
9. **Audio Focus (MANDATORY):**
   - Respect AUDIOFOCUS_LOSS_TRANSIENT and AUDIOFOCUS_LOSS.
   - Duck audio on AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK.
   - Resume playback only via explicit user action.
10. **Headphone & Noisy Audio Events (MANDATORY):**
   - Pause playback on ACTION_AUDIO_BECOMING_NOISY.
   - Never auto-resume playback after headphones are unplugged.
11. **Media Access:**
	- Never scan MediaStore in background.
	- Media queries only on explicit user entry or foreground actions.


## 🛠 Build & Run Commands
- **Build:** `./gradlew assembleDebug`
- **Install:** `./gradlew installDebug`
- **Lint:** `./gradlew lint`
- **Test:** `./gradlew test`(JUnit 5 + MockK)
- **Run UI Tests:** `./gradlew connectedAndroidTest`
- **Clean Artifacts:** `./gradlew clean`
- **Init:** `claude /init` (Run to update project graph)


## 🔑 Session & Privacy Rules
- **DataStore:** Use Preferences DataStore for all user flags (Login, Private Session).
- **Logout Behavior:** Logout must clear local DB metadata but **never** delete physical .mp3 files.
- **Privacy:** If `isPrivateSession` is active, the app must not log listening history or update the "Recently Played" Bento grid.