# Required Skills - Sukoon Music Player
*Offline-only S replica with advanced audio features*

## 🎯 Critical Core Skills (MANDATORY)

### Advanced Media Engineering
- **Media3 Architecture** (MediaSessionService lifecycle, MediaController state management)
- **ExoPlayer Mastery** (gapless playback, queue management, audio focus handling)
- **Audio Effects API** (5-band Equalizer, BassBoost, Virtualizer with real-time control)
- **Foreground Services** (Android 14/15 compliance with `foregroundServiceType="mediaPlayback"`)
- **Audio Focus** (AUDIOFOCUS_LOSS handling, ducking, noisy audio events)
- **Media Notifications** (custom controls, background-to-foreground transition stability)

### Modern Android Architecture
- **MVVM + Clean Architecture** (Domain/Data/UI separation, Repository pattern)
- **Kotlin Coroutines & Flow** (StateFlow, SharedFlow, reactive data streams)
- **Hilt/Dagger Dependency Injection** (scoped dependencies, service injection)
- **Jetpack Compose** (Material 3, state management, LazyColumn optimization)
- **Room Database** (complex schemas, migrations, foreign keys, many-to-many relationships)

### Advanced UI/UX
- **Dynamic Theming** (Palette API color extraction from album art)
- **Material 3 Theming** (dynamic colors, contrast guarantees, gradient backgrounds)
- **Compose Navigation** (nested routes, backstack handling)
- **Custom Animations** (seek bar interactions, lyrics auto-scroll)
- **Accessibility** (TalkBack support, content descriptions)

## 🔧 Technical Implementation Skills

### Data Management
- **Room Advanced Patterns**
  - 7 entities with relationships (PlaylistSongCrossRef, PlaylistWithSongs)
  - Database migrations (v2→v6 migration paths)
  - CASCADE deletes for data integrity
  - DAO with complex queries (filters, sorting, search)
- **DataStore Preferences** (reactive user settings, theme persistence)
- **MediaStore API** (Android 13+ READ_MEDIA_AUDIO, album art URIs, metadata extraction)

### Networking & APIs
- **Retrofit 2.x** (RESTful API integration with LRCLIB)
- **OkHttp 4.x** (logging interceptors, error handling)
- **LRC Format Parsing** (timestamp extraction, ±500ms sync tolerance, offset correction)
- **Offline-First Strategy** (local cache → ID3 tags → API fallback)

### Performance & Optimization
- **LazyColumn Optimization** (keys, item reuse, large lists)
- **Memory Management** (bitmap loading, album art caching)
- **Coroutine Scoping** (lifecycle-aware, proper cancellation)
- **R8/ProGuard** (code shrinking, obfuscation, keep rules for Media3/Hilt)

### Monetization
- **AdMob Integration** (banner/native ads only, NO audio-interrupting ads)
- **Ad Lifecycle** (proper initialization, test vs production IDs)
- **Ad Placement Ethics** (non-intrusive banner at bottom only)

## 📱 Google Play Store Preparation (CRITICAL)

### Pre-Launch Requirements
1. **Privacy Policy**
   - MANDATORY for apps with INTERNET permission + ads
   - Must disclose: ad data collection, LRCLIB API usage, MediaStore access
   - Host on publicly accessible URL (GitHub Pages, your domain)

2. **Data Safety Form**
   - Declare: Device/app IDs (AdMob), user interactions (search history)
   - Encryption in transit: YES (HTTPS for LRCLIB)
   - Data deletion: Explain logout = clear metadata only

3. **App Signing**
   - Generate upload keystore: `keytool -genkey -v -keystore sukoon-upload.jks`
   - Enroll in Play App Signing (Google manages production key)
   - Store keystore credentials securely (never commit to Git)

4. **Content Rating**
   - Complete IARC questionnaire (likely rated E for Everyone)
   - Music apps: disclose if lyrics contain explicit content

### Store Listing Assets
- **App Icon**: 512x512 PNG (hi-res)
- **Feature Graphic**: 1024x500 PNG (Play Store hero banner)
- **Screenshots**: 4-8 images (phone + tablet if supporting)
  - Recommended: Home, NowPlaying, Playlists, Equalizer, Search
- **Video** (optional): 30s demo showcasing UI

### Release Build Configuration
- **Enable R8**: `isMinifyEnabled = true` in `build.gradle.kts`
- **ProGuard Rules**: Add keep rules for:
  ```
  -keep class androidx.media3.** { *; }
  -keep class com.google.android.exoplayer2.** { *; }
  -keep class com.sukoon.music.data.remote.dto.** { *; }
  ```
- **Update Ad IDs**: Replace test IDs with production (AdMobManager.kt:33-40)
- **Version Management**: Use semantic versioning (1.0.0 → 1.0.1)

### Compliance Checklist
- ✅ `targetSdk = 36` (latest API level)
- ✅ 64-bit architecture support (ARM64, x86_64)
- ✅ `foregroundServiceType` declared in manifest
- ✅ Runtime permission handling with rationale
- ✅ Background location NOT used (Play Store policy)
- ✅ Scoped storage compliance (no WRITE_EXTERNAL_STORAGE)
- ✅ No hardcoded secrets (API keys in BuildConfig or secure storage)

## 🧪 Testing Skills (Recommended Before Launch)

### Manual Testing
- **Device Matrix**: Test on Android 10, 11, 12, 13, 14, 15
- **Background Scenarios**: Lock screen → unlock, switch apps, low memory
- **Audio Scenarios**: Headphone plug/unplug, Bluetooth connect/disconnect, calls
- **Edge Cases**: Empty library, corrupted audio files, no lyrics found

### Automated Testing (Currently 0% Coverage)
- **Unit Tests**: JUnit 5 + MockK (repository logic, parsers)
- **Instrumentation Tests**: Espresso + Compose Testing (UI flows)
- **Critical Paths**:
  - Play song → notification appears → controls work
  - Create playlist → add songs → delete → verify cascade
  - Search → filter → sort → play result
  - Change theme → UI updates immediately

## 🚀 Pre-Release Production Fixes

### Immediate Actions Required
1. **Native Ad Layout**: Create `app/src/main/res/layout/native_ad_layout.xml`
   - Required view IDs: headline, body, media, call-to-action, icon
2. **Production Ad IDs**: Update AdMobManager.kt with real ad unit IDs
3. **Remove Test Code**: Set `USE_TEST_ADS = false` in AdMobManager

### Post-Launch Monitoring
- **Firebase Crashlytics** (crash rate < 1.09% for Good Standing)
- **Play Console Metrics** (ANR rate, battery drain, wake locks)
- **User Reviews** (respond within 7 days for engagement)

## 📚 Reference Documentation
- [Media3 Developer Guide](https://developer.android.com/media/media3)
- [Play Store Policy Center](https://play.google.com/console/about/guides/)
- [AdMob Policy](https://support.google.com/admob/answer/6128543)
- [LRCLIB API Docs](https://lrclib.net/docs)

---

**Estimated Play Store Approval Time**: 1-7 days after submission
**Production Readiness**: 95% (pending ad layout + production IDs)
