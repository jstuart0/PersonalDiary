# Android App Specification - Personal Diary Platform

**Document Version:** 1.0  
**Last Updated:** October 31, 2025  
**Target:** AI Agent Implementation  
**Platform:** Android 9+ (API 28+)  
**Approach:** Native Kotlin + Jetpack Compose

---

## 📋 Overview

This document specifies the Android native app for the Personal Diary Platform. The agent must implement a native Android app that handles client-side encryption, offline storage, and seamless sync with the backend API.

**Key Requirements:**
- Native Kotlin implementation
- Jetpack Compose for UI (recommended)
- Client-side encryption for both E2E and UCE tiers
- Secure key storage in KeyStore
- Offline-first with sync
- Biometric authentication
- Material Design 3

---

## 🎯 Target Specifications

### Platform Requirements
- **Minimum Android Version:** Android 9.0 (API 28)
- **Target Android Version:** Android 14 (API 34)
- **Devices:** Phones, Tablets
- **Languages:** English (MVP), localization-ready

### Technical Requirements
- **Language:** Kotlin 1.9+
- **UI Framework:** Jetpack Compose (recommended)
- **Architecture:** MVVM + Clean Architecture
- **Dependency Management:** Gradle
- **Minimum Deployment:** Google Play Store
- **Testing:** Internal testing track required

---

## 🏗️ Architecture Overview

### Recommended Architecture Pattern

Agent should implement **Clean Architecture + MVVM**:

```
┌─────────────────────────────────────────┐
│         Jetpack Compose UI              │
│  (Screens, Components, Themes)          │
└───────────────┬─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│         Presentation Layer              │
│         (ViewModels + States)           │
└───────────────┬─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│           Domain Layer                  │
│      (Use Cases + Entities)             │
└───────────────┬─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│            Data Layer                   │
│                                         │
│  ┌──────────────┐  ┌─────────────────┐│
│  │ Repositories │  │  Data Sources   ││
│  └──────────────┘  └─────────────────┘│
│                                         │
│  ┌──────────────┐  ┌─────────────────┐│
│  │ Encryption   │  │  API Client     ││
│  │ Service      │  │  (Retrofit)     ││
│  └──────────────┘  └─────────────────┘│
│                                         │
│  ┌──────────────┐  ┌─────────────────┐│
│  │ KeyStore     │  │  Room Database  ││
│  │ Manager      │  │                 ││
│  └──────────────┘  └─────────────────┘│
└─────────────────────────────────────────┘
```

### Module Structure (Recommended)

```
PersonalDiary-Android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/jstuart0/personaldiary/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── PersonalDiaryApplication.kt
│   │   │   │   ├── di/ (Dependency Injection - Hilt)
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── auth/
│   │   │   │   │   ├── timeline/
│   │   │   │   │   ├── entry/
│   │   │   │   │   ├── search/
│   │   │   │   │   ├── settings/
│   │   │   │   │   └── social/
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/
│   │   │   │   │   ├── usecase/
│   │   │   │   │   └── repository/
│   │   │   │   ├── data/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── database/
│   │   │   │   │   ├── remote/
│   │   │   │   │   │   ├── api/
│   │   │   │   │   │   └── dto/
│   │   │   │   │   └── encryption/
│   │   │   │   ├── service/
│   │   │   │   │   ├── SyncService.kt
│   │   │   │   │   └── KeyStoreManager.kt
│   │   │   │   └── util/
│   │   │   ├── res/
│   │   │   │   ├── drawable/
│   │   │   │   ├── values/
│   │   │   │   └── xml/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/ (Unit tests)
│   │   └── androidTest/ (Integration/UI tests)
│   └── build.gradle.kts
└── build.gradle.kts
```

Agent chooses specific structure but must maintain Clean Architecture principles.

---

## 🔐 Encryption Implementation

### KeyStore Manager Requirements

**Purpose:** Securely store cryptographic keys using Android KeyStore

**Must Support:**
- Store E2E private keys
- Store UCE session tokens
- Store user credentials (optional, for biometric)
- Biometric protection (Fingerprint/Face)
- Hardware-backed keys (Strongbox when available)

**KeyStore Configuration:**
- `KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT`
- `setUserAuthenticationRequired(true)` for sensitive keys
- `setInvalidatedByBiometricEnrollment(false)` to survive biometric changes
- `setRandomizedEncryptionRequired(true)`

**Implementation Requirements:**

Agent must implement methods for:
- Generate and store key in KeyStore
- Retrieve key from KeyStore
- Delete key from KeyStore
- Check if key exists
- Encrypt data with KeyStore key
- Decrypt data with KeyStore key

**Security:**
- All keys stored in Android KeyStore
- Hardware-backed when available
- Biometric authentication required for key access
- Handle KeyStore exceptions gracefully

---

### E2E Encryption (Client-Side)

**Cryptographic Library:**
Agent chooses:
- Google Tink (recommended, high-level crypto)
- Or Libsodium for Android
- Or BouncyCastle

**Key Generation:**
- Generate X25519 keypair (or similar)
- Store private key in KeyStore
- Send public key to server during signup

**Encryption Process:**
1. User creates entry
2. App encrypts plaintext content with private key
3. Generate SHA-256 content hash of plaintext
4. Upload encrypted content + hash to server

**Decryption Process:**
1. Fetch encrypted entry from server
2. Retrieve private key from KeyStore
3. Decrypt content
4. Display plaintext to user

**Implementation Requirements:**

Agent must implement:
- Key generation function
- Encryption function (plaintext → ciphertext)
- Decryption function (ciphertext → plaintext)
- Hash generation function (SHA-256)
- Error handling for failed encryption/decryption

---

### UCE Encryption (Client-Side)

**Key Derivation:**
- Server provides encrypted master key at login
- Client derives key from user password (Argon2 or PBKDF2)
- Client decrypts master key
- Store decrypted master key in memory (ephemeral)

**Encryption Process:**

Agent can choose:
- **Option A:** Client encrypts locally (recommended)
  - Encrypt entry before sending to server
  - Server stores encrypted blob
  
- **Option B:** Server encrypts (simpler)
  - Client sends plaintext to server
  - Server encrypts with master key

**Recommendation:** Use Option A for consistency with E2E.

**Implementation Requirements:**

Agent must implement:
- Key derivation function (Argon2/PBKDF2)
- Master key decryption
- Entry encryption/decryption
- Secure memory handling (clear keys when app backgrounds)

---

## 💾 Local Database

### Requirements

**Purpose:** Offline storage of encrypted entries

**Technology:**
- **Room Persistence Library** (recommended, official)
- Or SQLDelight
- Or Realm

**Agent chooses** based on preferences.

**Recommended:** Room (official, well-supported, type-safe)

---

### Database Schema (Local)

Agent must design Room entities to mirror API models:

**User Entity**
- userId (primary key)
- email
- encryptionTier
- publicKey (E2E) or encryptedMasterKey (UCE)

**Entry Entity**
- entryId (primary key)
- userId (foreign key)
- encryptedContent
- contentHash
- source
- createdAt, updatedAt
- syncStatus (enum: SYNCED, PENDING, FAILED)

**Media Entity**
- mediaId (primary key)
- entryId (foreign key)
- encryptedFilePath (local file system)
- mimeType
- fileSize
- syncStatus

**Tag Entity**
- tagId (primary key)
- entryId (foreign key)
- tagName
- autoGenerated

**EntryTagCrossRef** (many-to-many)
- entryId
- tagId

**SyncMetadata Entity**
- id (primary key)
- lastSyncTimestamp
- pendingOperationsCount

---

### Offline Support Requirements

**Agent must implement:**

1. **Create entries offline**
   - Store locally with syncStatus = PENDING
   - Queue for upload when online

2. **View entries offline**
   - All entries available from Room database
   - Media cached locally

3. **Edit entries offline**
   - Save edits locally
   - Mark as pending sync
   - Resolve conflicts on sync

4. **Sync when online**
   - Background sync with WorkManager
   - Periodic sync (every 5 minutes when active)
   - Manual sync button

5. **Conflict resolution**
   - Last write wins (with user notification)
   - Timestamp-based resolution

---

## 🎨 UI/UX Specifications

### Design System

**Agent must follow:**
- Material Design 3 guidelines
- Material You dynamic colors
- Material icons
- Native Android components
- Accessibility (TalkBack, Large Text, High Contrast)

**Color System:**
- Use Material Theme Builder or dynamic colors
- Primary color: Brand color or Material Blue
- E2E Indicator: Orange (lock icon)
- UCE Indicator: Green (shield icon)
- Shared: Purple (share icon)

**Typography:**
- Roboto font family (system default)
- Material Type Scale (Display, Headline, Title, Body, Label)

---

### Screen Specifications

#### 1. Splash Screen

**Implementation:**
- Use Android 12+ Splash Screen API
- Simple logo or app icon
- Check authentication status
- Navigate to Login or Timeline

---

#### 2. Signup Screen

**Components:**
- Email text field (Material3 OutlinedTextField)
- Password text field (secure, visibility toggle)
- Password strength indicator (LinearProgressIndicator)
- "Sign Up" button

**Flow:**
1. Validate email format
2. Validate password strength
3. Navigate to Encryption Tier Selection

**Validation:**
- Email must be valid format
- Password minimum 12 characters
- Show Snackbar for errors

---

#### 3. Encryption Tier Selection Screen

**Components:**
- Title: "Choose Your Security Model"
- Two cards: E2E and UCE
- Feature comparison
- Warning chip: "Permanent choice"
- "Continue" buttons

**E2E Card (Material3 Card):**
- 🔒 Icon
- Title: "Maximum Privacy"
- Features (Checkboxes):
  - ✅ Keys never on server
  - ✅ True end-to-end encryption
  - ❌ Limited search
  - ❌ Manual device setup
- Description

**UCE Card:**
- 🛡️ Icon
- Title: "Smart Features"
- Features:
  - ✅ Full-text search
  - ✅ AI auto-tagging
  - ✅ Easy recovery
  - ✅ Instant sync
- Description

**Selection:**
- Card elevation on tap
- Confirmation dialog
- If E2E: Navigate to Recovery Codes
- If UCE: Complete signup

---

#### 4. Recovery Codes Screen (E2E Only)

**Components:**
- Top app bar with warning icon
- Title: "Save Your Recovery Codes"
- Warning card (Material3 Card, error color)
- Lazy column of 10 codes
- Action buttons:
  - "Download" (save as text file)
  - "Share" (Android share sheet)
  - "Copy All"
- Checkbox: "I have saved my codes"
- "Continue" FAB (disabled until checkbox)

**Code Display:**
- 2 columns grid
- Monospace font
- Copy on long press
- Material3 OutlinedCard per code

**Behavior:**
- Force confirmation
- Show multiple dialogs about importance
- Only proceed when checkbox is checked

---

#### 5. Login Screen

**Components:**
- Email text field
- Password text field
- "Log In" button
- "Forgot Password?" text button
- Biometric prompt (if enabled)

**Flow:**
1. User enters credentials
2. Authenticate with API
3. Store tokens in Encrypted SharedPreferences
4. Navigate to Timeline

**Biometric Authentication:**
- BiometricPrompt API
- Show fingerprint/face dialog
- On success, auto-login

---

#### 6. Timeline Screen (Main)

**Navigation:**
- Bottom Navigation Bar: Timeline, Search, Create, Settings

**Components:**
- Top app bar:
  - Title: "My Diary"
  - Encryption tier badge
  - Sync status icon
  - Menu (filter, sort)
- Pull-to-refresh (SwipeRefresh)
- Lazy column of entries
- FAB: "+" (create entry)

**Entry Item (Material3 Card):**
- Title or content preview
- Date and time
- Tags (Chip group)
- Media thumbnail (AsyncImage)
- Share indicator icon
- Source badge
- Tap to open detail

**Empty State:**
- Illustration
- "Start your first entry"
- "Create Entry" button

---

#### 7. Entry Detail Screen

**Components:**
- Top app bar:
  - Back navigation
  - Edit icon
  - Share icon (to Facebook)
  - More menu (delete, history)
- Scrollable content:
  - Title (if exists)
  - Full decrypted content
  - Media pager (photos/videos)
  - Tags (Flow row of chips)
  - Metadata card:
    - Created date/time
    - Modified date/time
    - Source
  - External post link (if shared)

**Actions:**
- Edit entry
- Delete entry (confirmation dialog)
- Share to Facebook
- View history (bottom sheet)

---

#### 8. Create/Edit Entry Screen

**Components:**
- Top app bar:
  - Close icon
  - Save icon
  - Title
- Content:
  - Title text field (optional)
  - Content text field (expanded)
  - Media picker row (horizontal scroll)
  - Location chip (optional)
  - Mood selector (dropdown)
  - Tag input (Chip input)

**Media Picker:**
- Bottom sheet with options:
  - Take photo
  - Choose from gallery
  - Record video
- Show thumbnails below content
- Remove icon on thumbnails

**Behavior:**
- Auto-save draft locally
- Encrypt on save
- Upload to server (with progress)
- Show success Snackbar

---

#### 9. Search Screen

**For UCE Users:**
- Search bar (Material3 SearchBar)
- Search suggestions (LazyColumn)
- Recent searches (Chip group)
- Results list (same as Timeline)
- Filter FAB (tags, date, source)

**For E2E Users:**
- Search bar
- Info card: "Client-side search only"
- Results (from local Room database)
- Filters: tags, date range, source
- Note: "Cross-device search not available"

---

#### 10. Settings Screen

**Sections (Lazy Column):**

**Account:**
- Email (display only)
- Encryption tier badge
- Change password
- Logout

**Sync:**
- Last sync time
- Manual sync button
- Sync frequency (dropdown)

**Storage:**
- Storage used (LinearProgressIndicator)
- Storage limit
- Upgrade button (if free)

**Social Media:**
- Connected accounts
  - Facebook card (connect/disconnect)
  - Instagram (coming soon, disabled)
- Sync settings

**Security:**
- Biometric authentication (Switch)
- Recovery codes (E2E only) - view

**Preferences:**
- Theme (Light/Dark/System)
- Notifications (Switch)
- Default privacy

**About:**
- Version number
- Privacy policy
- Terms of service
- Support

---

#### 11. Facebook Integration Screens

**Connect Facebook:**
- "Connect to Facebook" button
- Chrome Custom Tabs for OAuth
- Success/error Snackbar

**Share to Facebook (Bottom Sheet):**
- Entry preview (decrypted)
- Edit before sharing (TextField)
- Privacy selector (Radio buttons)
- "Post" button
- Cancel button

**Import from Facebook:**
- "Import" button
- Date range picker (Material3 DateRangePicker)
- Progress indicator
- Success Snackbar with count
- Navigate to Timeline

---

## 🔄 Sync Implementation

### Sync Strategy

**Agent must implement using WorkManager:**

1. **Background Sync**
   - OneTimeWorkRequest on app open
   - PeriodicWorkRequest every 15 minutes
   - ExistingPeriodicWorkPolicy.KEEP

2. **Manual Sync**
   - OneTimeWorkRequest on user action
   - Show progress in UI

3. **Upload Queue**
   - Queue local changes in Room
   - WorkManager processes queue
   - Retry failed uploads (exponential backoff)

4. **Conflict Resolution**
   - Download server changes
   - Compare timestamps
   - Last write wins
   - Notify user (Notification)

---

### Sync Worker Requirements

**Agent must create SyncWorker (Worker):**

```
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params)
```

**Methods:**
- `doWork()` - Main sync logic
- `syncAll()` - Full sync
- `syncIncremental()` - Since last sync
- `uploadPending()` - Upload local changes
- `downloadNew()` - Fetch new from server
- `resolveConflicts()` - Handle conflicts

**Constraints:**
- Requires internet connectivity
- Battery not low (optional)

---

## 📸 Media Handling

### Requirements

**Agent must implement:**

1. **Media Capture**
   - Camera X for camera access
   - Media picker for gallery
   - Video recording
   - Multiple selection

2. **Media Encryption**
   - Encrypt media files before upload
   - Store encrypted in app-specific storage
   - Generate SHA-256 hash

3. **Media Upload**
   - Upload encrypted media to server
   - Show progress (WorkManager + Progress)
   - Handle large files (chunking if needed)

4. **Media Display**
   - Decrypt media for display
   - Coil or Glide for image loading
   - ExoPlayer for video playback

5. **Media Caching**
   - Cache decrypted media temporarily
   - Clear cache on app close
   - Manage cache size (LRU)

---

### Media Picker Integration

**Use:**
- Photo Picker (Android 11+, recommended)
- Or Intent.ACTION_GET_CONTENT
- Camera X for camera

**Permissions:**
- Request camera permission
- Request storage permission (if needed)
- Handle permission denial (rationale dialog)

**Manifest:**
```xml
<uses-permission android:name="android.permission.CAMERA"/>
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO"/>
```

---

## 🔔 Notifications

**Agent must implement:**

- Push notifications (Firebase Cloud Messaging)
- Local notifications for sync completion
- Notification channels (required Android 8+)
- Deep linking from notifications

**Notification Channels:**
- Sync notifications (low importance)
- Social notifications (default)
- Important updates (high)

**Requirements:**
- Request notification permission (Android 13+)
- Handle notification taps (PendingIntent)
- Update notification when sync progress changes

---

## 🧪 Testing Requirements

### Unit Tests

**Agent must create tests using JUnit + MockK:**

**Test:**
- Encryption/decryption functions
- Key generation
- Hash generation
- Repository methods
- Use cases
- ViewModel logic

**Target Coverage:** 80%+

---

### Instrumentation Tests

**Agent must create tests using AndroidX Test:**

**Test:**
- Room database operations
- KeyStore operations
- Encryption with real KeyStore
- WorkManager sync
- Navigation flow

---

### UI Tests

**Agent must create UI tests using Compose Testing:**

**Test:**
- Signup flow (both tiers)
- Create entry
- View entry
- Edit entry
- Share to Facebook (mocked)
- Search entries

**Use:** `createAndroidComposeRule<MainActivity>()`

---

## 🚀 Deployment

### Google Play Requirements

**Agent must prepare:**
- App icon (adaptive icon, all densities)
- Splash screen (Android 12+)
- Screenshots (phone and tablet, different locales)
- Feature graphic
- App description
- Privacy policy URL
- Support email

**Google Play Console:**
- Package name: `com.jstuart0.personaldiary`
- Version code: Auto-increment
- Version name: 1.0.0

---

### Internal Testing Track

**Agent must:**
- Configure internal testing
- Add tester emails
- Write release notes
- Collect feedback via Play Console

---

## 📱 Platform-Specific Features

### Android-Specific Features to Implement

1. **Widgets (Optional)**
   - Quick entry widget (Glance)
   - Recent entries widget

2. **App Shortcuts**
   - Static: "New Entry"
   - Dynamic: Recent entries

3. **Share Target**
   - Share content from other apps to diary
   - Share photos, text, URLs

**Manifest:**
```xml
<activity android:name=".ShareActivity">
    <intent-filter>
        <action android:name="android.intent.action.SEND"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <data android:mimeType="text/plain"/>
        <data android:mimeType="image/*"/>
    </intent-filter>
</activity>
```

4. **Backup (Optional)**
   - Auto-backup encrypted entries
   - Restore on new device
   - Use Auto Backup for Apps

---

## ⚡ Performance Requirements

**Agent must ensure:**
- App launch time < 2 seconds (cold start)
- Entry list scrolling: 60 FPS
- Encryption/decryption < 100ms per entry
- Image decryption < 500ms per image
- Memory usage < 150MB typical
- Battery efficient (Doze mode compatible)

**Optimization:**
- Use Lazy columns for lists
- Image loading with Coil (caching)
- Proper coroutine scoping
- Room query optimization
- Background work with WorkManager (not foreground services)

---

## ♿ Accessibility Requirements

**Agent must implement:**
- TalkBack support (contentDescription for all elements)
- Large text support (scalable text)
- High contrast mode support
- Touch target size minimum 48dp
- Keyboard navigation (Tab support)

**Compose Accessibility:**
- Use `Modifier.semantics` for custom components
- Provide `contentDescription` for icons
- Use `Role` for semantic meaning

---

## 🌍 Localization (Future)

**Agent must prepare for:**
- English (MVP)
- Strings.xml for all user-facing text
- Date/time formatting with user's locale
- Right-to-left language support (RTL)
- Plurals support

---

## 📦 Dependencies (Recommended)

**Build.gradle.kts:**

Agent chooses specific versions, but consider:

**Core:**
- androidx.core:core-ktx
- androidx.lifecycle:lifecycle-runtime-ktx
- androidx.activity:activity-compose

**Compose:**
- androidx.compose.ui:ui
- androidx.compose.material3:material3
- androidx.compose.ui:ui-tooling-preview
- androidx.navigation:navigation-compose

**Room:**
- androidx.room:room-runtime
- androidx.room:room-ktx
- kapt androidx.room:room-compiler

**Networking:**
- com.squareup.retrofit2:retrofit
- com.squareup.retrofit2:converter-gson
- com.squareup.okhttp3:logging-interceptor

**Encryption:**
- com.google.crypto.tink:tink-android (recommended)
- Or org.libsodium:libsodium-jni

**Image Loading:**
- io.coil-kt:coil-compose

**Dependency Injection:**
- com.google.dagger:hilt-android
- kapt com.google.dagger:hilt-compiler
- androidx.hilt:hilt-navigation-compose

**WorkManager:**
- androidx.work:work-runtime-ktx

**Coroutines:**
- org.jetbrains.kotlinx:kotlinx-coroutines-android

**Testing:**
- junit:junit
- androidx.test.ext:junit
- androidx.test.espresso:espresso-core
- io.mockk:mockk
- androidx.compose.ui:ui-test-junit4

---

## 🔧 Configuration

**Agent must create:**

**Build Variants:**
- Debug (development API)
- Release (production API)

**BuildConfig fields:**
```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://api.example.com\"")
buildConfigField("String", "FACEBOOK_APP_ID", "\"123456789\"")
```

**AndroidManifest.xml:**
- Permissions (Camera, Internet, etc.)
- OAuth redirect activity
- Backup rules
- Network security config

**Proguard rules:**
- Keep encryption classes
- Keep Retrofit interfaces
- Keep Room entities

---

## 📝 Implementation Checklist

**Phase 1: Setup**
- [ ] Create Android Studio project
- [ ] Set up module structure
- [ ] Configure Gradle dependencies
- [ ] Set up Room database
- [ ] Create Material3 theme

**Phase 2: Authentication**
- [ ] Implement KeyStore manager
- [ ] Create signup screens (Compose)
- [ ] Create login screen
- [ ] Implement encryption tier selection
- [ ] Generate E2E keys
- [ ] Handle UCE key derivation

**Phase 3: Core Features**
- [ ] Implement timeline screen
- [ ] Implement entry detail screen
- [ ] Implement create/edit entry
- [ ] Implement Room database
- [ ] Implement encryption service
- [ ] Implement Retrofit API client

**Phase 4: Sync**
- [ ] Implement SyncWorker
- [ ] Handle offline mode
- [ ] Implement conflict resolution
- [ ] Background sync with WorkManager

**Phase 5: Media**
- [ ] Implement photo picker
- [ ] Implement media encryption
- [ ] Implement media upload
- [ ] Implement media display (Coil)

**Phase 6: Social**
- [ ] Implement Facebook OAuth (Chrome Custom Tabs)
- [ ] Implement share to Facebook
- [ ] Implement import from Facebook

**Phase 7: Search**
- [ ] Implement search UI
- [ ] Implement E2E client-side search (Room FTS)
- [ ] Implement UCE server search

**Phase 8: Polish**
- [ ] Implement settings screen
- [ ] Add animations
- [ ] Accessibility
- [ ] Error handling
- [ ] Unit tests
- [ ] Instrumentation tests
- [ ] UI tests (Compose)

---

## 🎯 Success Criteria

**Agent must achieve:**
- [ ] App runs on Android 9+
- [ ] Signup flow works for both tiers
- [ ] Entry creation and encryption works
- [ ] Sync works reliably
- [ ] Facebook integration works
- [ ] Search works (tier-appropriate)
- [ ] Offline mode works
- [ ] 80%+ test coverage
- [ ] Passes Google Play review

---

**End of Android App Specification**

Agent should use this specification to implement a production-ready Android app that adheres to Material Design 3 and all requirements while making appropriate technical decisions.
