# Privacy Policy - Sukoon Music Player

**Last Updated:** February 2, 2026
**Effective Date:** Upon App Release
**Version:** 1.0.0

---

## 1. Introduction

Sukoon Music Player ("**the App**") is a local music playback application developed to provide an offline-first music listening experience without requiring cloud connectivity or account creation. This Privacy Policy explains what information we collect, how we use it, and your rights regarding your data.

We take your privacy seriously. This application is designed with privacy-by-default principles:
- **Offline-First**: Music playback and library management happen entirely on your device
- **No Account Required**: You can use the app without creating an account or providing personal information
- **Minimal Data Collection**: We only collect data necessary for core functionality
- **Local Storage**: All personal data is stored on your device, not on our servers

---

## 2. Information We Collect

### 2.1 Information Collected Locally on Your Device

The app collects the following information that is **stored exclusively on your device**:

#### Media Library Data
- **Audio File Metadata**: Artist name, track title, album name, duration, genre, and release date extracted from your local audio files via Android's MediaStore API
- **Album Art**: Album artwork associated with your audio files
- **Audio File Paths**: File system locations of audio files on your device

#### Playback History & Statistics
- **Recently Played Tracks**: Songs you have recently played (stored in a local database, limited to 50 most recent plays)
- **Listening Statistics**: Daily aggregated statistics including:
  - Total listening duration per day
  - Most frequently played artist per day
  - Peak listening time (morning/afternoon/evening)
  - Daily play counts
- **Search History**: Last 10 search queries entered in the app (stored on your device)

#### User Preferences & Settings
- **App Configuration**:
  - Theme preference (Light/Dark/System)
  - Audio quality setting
  - Equalizer settings and presets
  - Playback preferences (gapless playback, crossfade duration, audio focus behavior)
  - Notification settings
- **Premium Status**: Whether the user has purchased premium features
- **Username**: Optional user-provided display name
- **Playlist Data**: User-created playlists and their contents
- **Liked Songs**: Tracks marked as favorites
- **Sync Offset**: Per-track lyrics synchronization adjustments

#### Playback State Recovery
- **Last Playback Position**: Current playback position and queue index (for recovery after app restart)

### 2.2 Information Collected by Third-Party Services

#### Google AdMob (Advertising)
When the app displays advertisements, Google AdMob automatically collects:
- **Device Information**: Device model, operating system, and OS version
- **Advertising Identifier**: Google Advertising ID (AAID)
- **IP Address**: Your device's IP address (used for approximate location and to prevent ad fraud)
- **Approximate Location**: Derived from IP address (city/country level, not precise GPS)
- **App Usage Data**: Which ads are displayed and whether they are clicked
- **Device Settings**: Language, time zone, and device orientation

**Google's Privacy Policy**: https://policies.google.com/privacy
**Google's Advertising Privacy Policies**: https://policies.google.com/technologies/ads

#### Google Gemini AI API (Metadata Correction)
To improve music metadata accuracy, the app optionally uses Google Gemini AI to normalize and correct artist names, track titles, and album information:
- **Metadata Sent**: Artist name, track title, and album name are sent to Google's Gemini API
- **Purpose**: Correcting spelling errors, fixing casing, and standardizing metadata formats
- **Scope**: Metadata correction only (never lyrics generation)
- **API Key**: Stored in local configuration (not transmitted with the app)
- **Can Be Disabled**: Set `ENABLE_GEMINI_METADATA_CORRECTION=false` in build configuration

**Google Privacy Policy**: https://policies.google.com/privacy
**Gemini API Terms**: https://ai.google.dev/terms

#### LRCLIB.net (Synced Lyrics)
To fetch synced lyrics for your music, the app queries the public LRCLIB API:
- **Metadata Sent**: Artist name, track name, album name, and duration
- **Purpose**: Locating and fetching synced lyrics for Now Playing view
- **Caching**: Results are cached locally to minimize future API calls
- **Fallback Strategy**: Offline-first (checks local files and ID3 tags before API call)
- **Open Source**: LRCLIB is a community-maintained, open-source service

**LRCLIB Privacy**: https://lrclib.net
**Data Retention**: LRCLIB retains lyrics data for public access; refer to their privacy practices

#### Google Play Billing (In-App Purchases)
If you purchase premium features, payment is processed via Google Play Billing:
- **Payment Data**: Handled entirely by Google Play Services
- **Personal Information**: Your Google account email is not directly accessible to the app
- **Transaction Records**: Purchase history is managed by Google Play Console

**Google Play Billing Privacy**: https://support.google.com/googleplay/answer/2851743

---

## 3. Permissions and Their Use

### 3.1 Required Permissions

| Permission | Purpose | Data Accessed |
|-----------|---------|---------------|
| `READ_MEDIA_AUDIO` | To scan and access your local music library | Audio file metadata and album artwork |
| `INTERNET` | To fetch synced lyrics (LRCLIB), display ads (AdMob), and correct metadata (Gemini) | Music metadata for API requests |
| `ACCESS_NETWORK_STATE` | To check internet connectivity before making API calls | Network availability only |
| `FOREGROUND_SERVICE` | To keep music playback running while the app is in the background | No personal data accessed |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | To display playback notification and controls in the system notification panel | Playback state only |
| `WAKE_LOCK` | To prevent the device from sleeping during audio playback | No personal data accessed |
| `POST_NOTIFICATIONS` | To send playback notifications on Android 13+ | Playback state and current track info |

### 3.2 Optional Permissions

| Permission | Purpose | Use Case |
|-----------|---------|----------|
| `READ_EXTERNAL_STORAGE` (Android 12 and below) | Legacy permission for accessing music files on older Android versions | Scanned automatically when app starts (with user consent) |

---

## 4. How We Use Your Information

### 4.1 Core Playback & Discovery
- **Music Playback**: Playing audio files, managing queues, and displaying metadata
- **Search**: Indexing your library and enabling search across your music collection
- **Recently Played**: Displaying your 2×3 Recently Played grid on the Home screen
- **Recommendations**: Using your listening patterns to potentially suggest related artists (all computed locally)

### 4.2 User Experience
- **Preferences**: Saving your theme, audio quality, and playback settings
- **Equalizer Settings**: Storing custom audio effect presets
- **Playback State Recovery**: Resuming playback at the last position after app restart

### 4.3 Lyrics Display
- **Synced Lyrics Fetching**: Querying LRCLIB to display lyrics synchronized with playback
- **Lyrics Caching**: Storing fetched lyrics locally to reduce future API calls
- **Metadata Correction**: Using Gemini to improve accuracy of artist/title/album fields for better lyrics matching

### 4.4 Advertising
- **Ad Serving**: Google AdMob uses your approximate location, device info, and advertising ID to select relevant ads
- **Ad Performance**: Tracking which ads are displayed and clicked to measure campaign effectiveness

### 4.5 Analytics
**Important**: This app does **NOT** use traditional analytics services like Google Analytics, Firebase Analytics, or Crashlytics. We do not track:
- Session duration
- Feature usage patterns
- User demographics
- Crash reports with personal data
- User flows or conversion funnels

---

## 5. Data Retention & Deletion

### 5.1 Automatic Retention Policies
- **Recently Played**: Limited to last 50 plays (oldest entries automatically removed)
- **Search History**: Limited to last 10 queries (oldest entries automatically removed)
- **Listening Statistics**: Last 7 days of daily statistics retained
- **Lyrics Cache**: Indefinitely (until manually cleared)

### 5.2 User-Initiated Deletion

Users can delete personal data via the app's Settings:
- **Clear Database**: Removes all local metadata, playlists, and history
- **Clear Cache**: Removes cached lyrics and temporary files
- **Clear Search History**: Removes saved search queries
- **Logout**: Clears app metadata and preferences (audio files remain untouched)

**Important Note**: Deleting data through the app settings only removes data from the app's local database. It does not delete the actual audio files on your device.

### 5.3 Third-Party Data Retention

- **Google AdMob**: Retains advertising data per Google's retention policy (typically 90 days for anonymous IDs)
- **LRCLIB**: Lyrics are retained indefinitely as public data
- **Google Gemini**: API request logs retained per Google's terms (typically 90 days)

---

## 6. Private Session Mode

The app includes a **Private Session** feature that enhances privacy:
- **History Disabled**: When enabled, the app will NOT log:
  - Recently played songs
  - Search queries
  - Listening statistics
- **Database Unaffected**: Your playlists and liked songs remain accessible
- **Manual Control**: User can toggle this setting at any time

When Private Session is active, no listening history is recorded, providing a privacy-focused experience.

---

## 7. Data Security

### 7.1 On-Device Security
- **Local Storage**: All personal data is stored in Android's protected app-specific directories with standard file permissions
- **Encrypted Communications**: All API requests (LRCLIB, Gemini, AdMob) use HTTPS
- **No Backup Transmission**: App data backup is handled by Android's automatic backup system (controlled by user device settings)

### 7.2 Third-Party Security
- **Google Services**: AdMob, Gemini, and Play Billing are protected by Google's enterprise-grade security infrastructure
- **LRCLIB**: Uses standard HTTPS for all API requests

### 7.3 Data Breach Notification
In the unlikely event of a data breach involving third-party services (AdMob, Gemini, LRCLIB), users will be notified through:
- In-app notifications
- Email (if an account-linked email address is available)
- Public announcement on the official project repository

---

## 8. Children's Privacy (COPPA Compliance)

**This app is not designed for children under 13 years old.**

- **No Directed Marketing to Children**: The app does not display targeted ads designed specifically for children
- **Parental Controls**: Parents using family-shared devices should use Android's built-in parental controls to restrict access
- **No Social Features**: The app does not include social networking, in-app messaging, or user-to-user communication
- **No In-App Chat**: There are no direct messaging or live interaction features

**EU Children's Privacy (GDPR-K)**: For users in the EU under 16 years old:
- Parental consent is required for app installation and use
- Data collection is limited to functional necessity only
- Parents can request data access or deletion via the contact information below

---

## 9. GDPR & Regional Privacy Laws

### 9.1 Data Subject Rights (GDPR, CCPA, and Similar Laws)

Users have the right to:

#### **Right to Access**
Request all personal data the app has collected about you.

#### **Right to Deletion** ("Right to be Forgotten")
Request deletion of all personal data stored locally on your device by:
1. Using the in-app **"Clear Database"** or **"Logout"** functions
2. Uninstalling the app (which removes all app-specific data)
3. Requesting permanent deletion via email (see contact section)

#### **Right to Portability**
Request your personal data in a portable format:
- All local data can be exported via Android's standard backup and restore functions
- Search history, playlists, and preferences are stored in standard formats accessible to other apps

#### **Right to Rectification**
Correct inaccurate metadata:
- Edit track metadata directly in the app (stored locally)
- Use the Metadata Correction feature to normalize artist/title/album via Gemini

#### **Right to Restrict Processing**
- Disable Metadata Correction by setting `ENABLE_GEMINI_METADATA_CORRECTION=false`
- Disable Lyrics Fetching by disabling the Now Playing Lyrics view
- Disable Advertising tracking via Android system-wide advertising preferences

#### **Right to Object**
- Opt-out of metadata correction and lyrics fetching
- Disable ad personalization via Google's Ad Settings: https://adssettings.google.com

### 9.2 Legal Basis for Data Processing

| Data | Legal Basis |
|------|-------------|
| Playback history & statistics | Legitimate interest (improving user experience) |
| Search history | Legitimate interest (enabling search functionality) |
| User preferences | Legitimate interest & contract (providing configurable service) |
| AdMob data | Consent (opt-in via Terms of Service at app installation) |
| Gemini metadata | Legitimate interest (improving metadata accuracy) |
| LRCLIB lyrics | Legitimate interest (providing lyrics display) |

### 9.3 Data Protection Officer

For privacy inquiries and Data Subject Access Requests, contact us at:
📧 **support@sukoonmusicplayer.com** (to be configured)

---

## 10. California Privacy Rights (CCPA)

Under the California Consumer Privacy Act, residents have specific rights:

### 10.1 Right to Know
You have the right to request:
- What personal information is collected
- The sources of that information
- The business purpose for collection

### 10.2 Right to Delete
You can request deletion of personal information collected (except where legally required to retain)

### 10.3 Right to Opt-Out
- Opt-out of data sales (this app does NOT sell personal information)
- Opt-out of targeted advertising via AdMob preferences

### 10.4 Right to Non-Discrimination
You will not be discriminated against for exercising your CCPA rights

**California Residents Can Submit Requests To:**
📧 **support@sukoonmusicplayer.com**
(Response within 45 days)

---

## 11. International Data Transfers

- **EU/UK/EEA Users**: Metadata sent to Google Gemini and LRCLIB may be processed in the United States or other jurisdictions. By using the app, you consent to such transfers. Google maintains Standard Contractual Clauses (SCCs) for data protection.
- **UK Users**: LRCLIB operations comply with UK GDPR standards. For concerns, contact the Information Commissioner's Office (ICO).

---

## 12. Third-Party Services & Links

The app integrates with the following third-party services. Review their privacy policies:

| Service | Purpose | Privacy Policy |
|---------|---------|-----------------|
| Google AdMob | Ad serving | https://policies.google.com/privacy |
| Google Gemini AI | Metadata correction | https://ai.google.dev/terms |
| LRCLIB | Synced lyrics | https://lrclib.net |
| Google Play Billing | In-app purchases | https://support.google.com/googleplay |

**We are not responsible for these third parties' privacy practices. Review their policies independently.**

---

## 13. Policy Changes & Updates

We may update this Privacy Policy periodically to reflect:
- Changes in data collection practices
- New features or third-party integrations
- Legal or regulatory requirements
- Security improvements

**Users will be notified of material changes via:**
- In-app notifications
- Email (if applicable)
- Updated policy version number (reflected in app settings)

**Material changes take effect 30 days after notification.**

---

## 14. Contact & Privacy Inquiries

### 14.1 Privacy Questions

For privacy-related questions, data access requests, or concerns:

📧 **Email**: support@sukoonmusicplayer.com
📍 **Project Repository**: [GitHub Link - To Be Updated]
⏱️ **Response Time**: Within 30 days of inquiry

### 14.2 Data Subject Access Requests (DSARs)

To request access to your personal data or exercise your privacy rights:

1. **Send a request email to**: support@sukoonmusicplayer.com
2. **Include**:
   - Your request type (Access / Deletion / Portability / Rectification)
   - Device information (if applicable)
   - Specific data you're requesting
3. **Provide verification**: We may request proof of identity for security purposes

---

## 15. Dispute Resolution

If you have a dispute regarding this Privacy Policy or our data practices:

1. **First**: Contact us at support@sukoonmusicplayer.com to resolve the issue
2. **Second**: If unresolved after 30 days, escalate to relevant authorities:
   - **EU/EEA**: Your local Data Protection Authority
   - **UK**: Information Commissioner's Office (ICO)
   - **California**: California Attorney General or CCPA Enforcement Agencies
   - **Other US States**: Your state's Attorney General

---

## 16. Additional Information for App Store Compliance

### 16.1 Google Play Store Certification

This app certifies compliance with:
- ✅ **Google Play Targeting**: No collection of sensitive data from children
- ✅ **Advertising Standards**: Only AdMob banner ads; no interstitial or rewarded ads that disrupt UX
- ✅ **Permissions Justified**: All requested permissions are necessary and explained
- ✅ **Data Minimization**: Only data required for core functionality is collected
- ✅ **Transparency**: Privacy practices fully disclosed

### 16.2 Advertising Practices

- **Ad Types**: Banner and native ads only
- **Placement**: Non-intrusive banner at bottom of Home screen
- **Data Sharing**: Advertising ID and approximate location shared with AdMob only
- **User Control**: Users can reset advertising ID via Android settings

---

## 17. Frequently Asked Questions (FAQ)

### Q: Does Sukoon Music Player collect my location data?
**A**: No. The app does not request GPS location permissions. Approximate location (city/country level) is derived from your IP address by AdMob for ad targeting only. This is standard for advertising platforms.

### Q: Does the app require an account or login?
**A**: No. Sukoon Music Player is completely offline-first. No account creation is required. Optional username is for local display only.

### Q: Can the developer access my music or listening data?
**A**: No. All data is stored locally on your device. The developer has no server-side access to your music, playlists, or listening history.

### Q: Does the app share data with other apps?
**A**: No. The app uses encrypted local storage. Data is not shared with third parties except as described in this policy (AdMob, Gemini, LRCLIB—and only metadata).

### Q: What happens if I uninstall the app?
**A**: All app-specific data is removed from your device. Your actual audio files remain untouched.

### Q: Can I delete my data without uninstalling the app?
**A**: Yes. Use **Settings → Storage Management → Clear Database** or **Logout** to delete all app data while keeping the app installed.

### Q: Is my data backed up to the cloud?
**A**: Only if you have Android Cloud Backup enabled in your device settings. The app does not explicitly upload data to cloud services.

### Q: Does the app support "Do Not Track" requests?
**A**: Yes. Users can disable:
- Metadata correction
- Ad personalization (via Android Settings > Google Ads)
- History tracking (via Private Session mode)

### Q: How do I opt out of personalized ads?
**A**: Visit https://adssettings.google.com and adjust your ad personalization preferences.

---

## 18. Summary: Key Privacy Points

| Feature | Privacy Status |
|---------|----------------|
| **Music Playback** | Local, no tracking |
| **Playlists** | Local only |
| **Search** | Local history only |
| **Lyrics** | Cached locally after fetching |
| **User Preferences** | Local only |
| **Account Required** | No |
| **Cloud Sync** | No |
| **Analytics** | None (no Firebase, Mixpanel, etc.) |
| **Crash Reporting** | None with personal data |
| **Social Features** | None |
| **In-App Messaging** | None |
| **Third-Party Ad Networks** | Google AdMob only |
| **Data Sold** | No |

---

## 19. Acknowledgments

This Privacy Policy was created to comply with:
- European Union General Data Protection Regulation (GDPR)
- California Consumer Privacy Act (CCPA)
- Google Play Store Privacy Policy Requirements
- Apple App Store Privacy Policy Guidelines
- Children's Online Privacy Protection Act (COPPA)

---

## 20. Appendix: Technical Details

### 20.1 Local Data Storage Locations (Android)

```
/data/data/com.sukoon.music/
├── databases/
│   ├── sukoon_music.db          (Room database: songs, playlists, history)
│   └── sukoon_preferences_*.pb   (DataStore: user preferences)
├── files/
│   ├── coil/                     (Cached album art)
│   └── lyrics_cache/             (Cached lyrics)
└── cache/
    └── temp_files/               (Temporary files)
```

### 20.2 Data Flow Diagram

```
User Device
├── Local Music Files (unchanged)
├── Room Database (playlists, history, preferences)
│
├─→ LRCLIB API [artist, title, album, duration] → Cached Lyrics
├─→ AdMob SDK [device ID, approximate location] → Ads displayed
└─→ Gemini API [artist, title, album] → Corrected metadata
```

### 20.3 Network Requests

Only the following network requests are made:

| Endpoint | Trigger | Data Sent | Purpose |
|----------|---------|-----------|---------|
| `https://lrclib.net/api/get` | User opens Now Playing | Artist, title, album, duration | Fetch synced lyrics |
| `https://generativelanguage.googleapis.com/` | Metadata correction enabled | Artist, title, album | Normalize metadata |
| Google AdMob SDK | App launch, ad impressions | Device ID, approx. location, IP | Display ads |

---

**End of Privacy Policy**

---

**Version History:**
- **v1.0.0** (Feb 2, 2026): Initial Privacy Policy for Sukoon Music Player v1.0.0

📋 **For the most current version, always refer to the app's Settings → About → Privacy Policy**
