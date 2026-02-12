# Sukoon Music Player

Offline-only Android music player. Package: `com.sukoon.music`, v1.0.0

## Tech Stack
Kotlin 2.1+ | Jetpack Compose (Material 3) | Media3 ExoPlayer + MediaSessionService | Room DB (v6, 7 entities) | Coil + Palette API | Hilt DI | Retrofit/OkHttp (LRCLIB only) | AdMob (banners only) | DataStore Preferences

## Architecture
- MVVM + Clean Architecture (domain/data/ui layers)
- MediaController = single source of truth for playback state
- ExoPlayer lives exclusively inside MediaSessionService; UI/ViewModels must never hold ExoPlayer refs
- StateFlow + `collectAsStateWithLifecycle` for reactive UI
- All play/pause/seek goes through MediaController transport controls

## Critical Rules
1. **Offline-only**: Network allowed only for LRCLIB lyrics + AdMob ads. Audio playback/discovery = 100% local
2. **Ads**: Banner/Native only. NO interstitial/rewarded/audio-interrupting ads
3. **MediaSessionService**: Don't modify without full plan (background notification crash risk)
4. **Permissions**: Handle Android 13+ `READ_MEDIA_AUDIO` gracefully
5. **Manifest**: Declare `foregroundServiceType="mediaPlayback"` (Android 14/15)
6. **ExoPlayer lifecycle**: Lives in service, released only on service stop, UI never owns it
7. **Performance**: LazyColumn with keys for lists/lyrics
8. **LRCLIB ethics**: Include descriptive `User-Agent` header in Retrofit
9. **Audio focus**: Respect LOSS/LOSS_TRANSIENT, duck on CAN_DUCK, resume only on user action
10. **Noisy audio**: Pause on ACTION_AUDIO_BECOMING_NOISY, never auto-resume
11. **MediaStore**: Never scan in background; foreground/explicit user action only
12. **Strings**: No hardcoded UI strings; use `strings.xml`
13. **Scoped Storage**: `READ_MEDIA_AUDIO` for API 33+

## Lyrics (LRCLIB.net)
- Lookup: `GET /api/get?artist_name=&track_name=&album_name=&duration=` then fallback to `GET /api/search?q=`
- Parse `syncedLyrics` (LRC format), render in LazyColumn with active line highlight
- ±500ms sync tolerance, per-track manual offset stored in Room
- Fallback to unsynced if drift exceeds tolerance (show "may be out of sync" hint)
- Cache in Room before any network call

## Visual Standards
- Dark theme base palette, Material 3 dynamic colors with contrast guarantees
- Sans-serif typography, headers 24sp bold
- HomeScreen: 2x3 "Recently Played" bento grid at top
- NowPlayingScreen: Large rounded album art, green transport controls, dynamic gradient from album art
- MiniPlayer: Album art + dynamic accent color

## Session & Privacy
- DataStore for user flags (login, private session)
- Logout: Clear DB metadata, never delete physical .mp3 files
- Private session: Don't log history or update Recently Played grid

## Known TODOs
- Missing: `app/src/main/res/layout/native_ad_layout.xml` (native ads will crash without it; not currently used)
- `AdMobManager.kt:33-34,40`: Switch to production ad IDs + set `USE_TEST_ADS = false` before release
- Test coverage: 0% (framework ready: JUnit 5 + MockK + coroutines-test)

## Build Commands
```
./gradlew assembleDebug    # Build
./gradlew installDebug     # Install
./gradlew lint             # Lint (run after logic changes)
./gradlew test             # Unit tests
./gradlew connectedAndroidTest  # UI tests
./gradlew clean            # Clean
```
