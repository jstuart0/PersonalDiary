# iOS App Specification - Personal Diary Platform

**Document Version:** 1.0  
**Last Updated:** October 31, 2025  
**Target:** AI Agent Implementation  
**Platform:** iOS 16+  
**Approach:** Native Swift + SwiftUI

---

## 📋 Overview

This document specifies the iOS native app for the Personal Diary Platform. The agent must implement a native iOS app that handles client-side encryption, offline storage, and seamless sync with the backend API.

**Key Requirements:**
- Native Swift implementation
- SwiftUI for UI (recommended)
- Client-side encryption for both E2E and UCE tiers
- Secure key storage in Keychain
- Offline-first with sync
- Biometric authentication
- Universal app (iPhone + iPad)

---

## 🎯 Target Specifications

### Platform Requirements
- **Minimum iOS Version:** iOS 16.0
- **Target iOS Version:** iOS 17.0+
- **Devices:** iPhone, iPad (Universal)
- **Orientations:** Portrait (primary), Landscape (iPad)
- **Languages:** English (MVP), localization-ready

### Technical Requirements
- **Language:** Swift 5.9+
- **UI Framework:** SwiftUI (recommended) or UIKit
- **Architecture:** MVVM or Clean Architecture
- **Dependency Management:** Swift Package Manager
- **Minimum Deployment:** App Store
- **TestFlight:** Beta testing required

---

## 🏗️ Architecture Overview

### Recommended Architecture Pattern

Agent should implement **MVVM (Model-View-ViewModel)** or **Clean Architecture**:

```
┌─────────────────────────────────────────┐
│              SwiftUI Views              │
│  (Signup, Timeline, Entry, Settings)   │
└───────────────┬─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│            ViewModels                   │
│  (Business logic, State management)    │
└───────────────┬─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│            Services Layer               │
│                                         │
│  ┌──────────────┐  ┌─────────────────┐│
│  │ Encryption   │  │  API Client     ││
│  │ Service      │  │  Service        ││
│  └──────────────┘  └─────────────────┘│
│                                         │
│  ┌──────────────┐  ┌─────────────────┐│
│  │ Keychain     │  │  Local Database ││
│  │ Service      │  │  Service        ││
│  └──────────────┘  └─────────────────┘│
│                                         │
│  ┌──────────────┐  ┌─────────────────┐│
│  │ Sync         │  │  Media          ││
│  │ Service      │  │  Service        ││
│  └──────────────┘  └─────────────────┘│
└─────────────────────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│         Data Persistence                │
│                                         │
│  ┌──────────────┐  ┌─────────────────┐│
│  │ Core Data/   │  │  Keychain       ││
│  │ SwiftData    │  │                 ││
│  └──────────────┘  └─────────────────┘│
└─────────────────────────────────────────┘
```

### Directory Structure (Recommended)

```
PersonalDiary-iOS/
├── App/
│   ├── PersonalDiaryApp.swift
│   └── AppDelegate.swift
├── Core/
│   ├── Models/
│   │   ├── User.swift
│   │   ├── Entry.swift
│   │   ├── Media.swift
│   │   └── Tag.swift
│   ├── Services/
│   │   ├── Encryption/
│   │   │   ├── EncryptionService.swift
│   │   │   ├── E2EEncryption.swift
│   │   │   └── UCEEncryption.swift
│   │   ├── KeychainService.swift
│   │   ├── APIClient.swift
│   │   ├── DatabaseService.swift
│   │   ├── SyncService.swift
│   │   └── MediaService.swift
│   ├── Utilities/
│   │   ├── Constants.swift
│   │   ├── Extensions/
│   │   └── Helpers/
│   └── Errors/
│       └── AppError.swift
├── Features/
│   ├── Authentication/
│   │   ├── Views/
│   │   ├── ViewModels/
│   │   └── Models/
│   ├── Onboarding/
│   ├── Timeline/
│   ├── Entry/
│   ├── Search/
│   ├── Settings/
│   └── Social/
├── Resources/
│   ├── Assets.xcassets
│   ├── Localizable.strings
│   └── Info.plist
└── Tests/
    ├── UnitTests/
    ├── IntegrationTests/
    └── UITests/
```

Agent chooses specific structure but must maintain separation of concerns.

---

## 🔐 Encryption Implementation

### Keychain Service Requirements

**Purpose:** Securely store cryptographic keys

**Must Support:**
- Store E2E private keys
- Store UCE session tokens
- Store user credentials (optional, for biometric)
- Biometric protection (Face ID/Touch ID)
- Secure enclave usage when possible

**Keychain Attributes:**
- `kSecAttrAccessible`: `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`
- `kSecAttrSynchronizable`: `false` (keys should NOT sync via iCloud)
- `kSecAttrAccessControl`: Biometric authentication required

**Implementation Requirements:**

Agent must implement methods for:
- Save key to Keychain
- Retrieve key from Keychain
- Delete key from Keychain
- Check if key exists
- Update key in Keychain

**Security:**
- All keys stored with device-only accessibility
- No iCloud Keychain sync for encryption keys
- Biometric authentication required for key access
- Handle keychain errors gracefully

---

### E2E Encryption (Client-Side)

**Cryptographic Library:**
Agent chooses:
- CryptoKit (Apple's framework, recommended)
- Or third-party: Sodium, OpenSSL wrappers

**Key Generation:**
- Generate X25519 keypair (or similar)
- Store private key in Keychain
- Send public key to server during signup

**Encryption Process:**
1. User creates entry
2. App encrypts plaintext content with private key
3. Generate SHA-256 content hash of plaintext
4. Upload encrypted content + hash to server

**Decryption Process:**
1. Fetch encrypted entry from server
2. Retrieve private key from Keychain
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
- Client derives key from user password (Argon2id or PBKDF2)
- Client decrypts master key
- Store decrypted master key in memory (ephemeral)

**Encryption Process:**

Agent can choose:
- **Option A:** Client encrypts locally (recommended for privacy)
  - Encrypt entry before sending to server
  - Server stores encrypted blob
  
- **Option B:** Server encrypts (simpler)
  - Client sends plaintext to server
  - Server encrypts with master key

**Recommendation:** Use Option A for consistency with E2E and better privacy.

**Implementation Requirements:**

Agent must implement:
- Key derivation function
- Master key decryption
- Entry encryption/decryption
- Secure memory handling (clear keys when app backgrounds)

---

## 💾 Local Database

### Requirements

**Purpose:** Offline storage of encrypted entries

**Technology Options:**
- Core Data (traditional, well-supported)
- SwiftData (modern, Swift-native, iOS 17+)
- SQLite.swift (lightweight)
- Realm (third-party)

**Agent chooses** based on target iOS version and preferences.

**Recommended:** SwiftData for iOS 17+, Core Data for iOS 16 support.

---

### Database Schema (Local)

Agent must design local schema to mirror API models:

**User (Local)**
- user_id
- email
- encryption_tier
- public_key (E2E) or encrypted_master_key (UCE)

**Entry (Local)**
- entry_id
- encrypted_content
- content_hash
- source
- created_at, updated_at
- sync_status (enum: synced, pending, failed)
- tags (relationship)
- media (relationship)

**Media (Local)**
- media_id
- encrypted_file_path (local file system)
- mime_type
- file_size
- sync_status

**Tag (Local)**
- tag_id
- entry_id
- tag_name
- auto_generated

**Sync Metadata (Local)**
- last_sync_timestamp
- pending_operations (queue)

---

### Offline Support Requirements

**Agent must implement:**

1. **Create entries offline**
   - Store locally with sync_status = pending
   - Queue for upload when online

2. **View entries offline**
   - All entries available from local database
   - Media cached locally

3. **Edit entries offline**
   - Save edits locally
   - Mark as pending sync
   - Resolve conflicts on sync

4. **Sync when online**
   - Background sync on app open
   - Periodic sync (every 5 minutes when active)
   - Manual sync button

5. **Conflict resolution**
   - Last write wins (with user notification)
   - Timestamp-based resolution

---

## 🎨 UI/UX Specifications

### Design System

**Agent must follow:**
- iOS Human Interface Guidelines
- SF Symbols for icons
- Native iOS components
- Dynamic Type support
- Dark mode support
- Accessibility (VoiceOver, Dynamic Type, High Contrast)

**Color Palette (Recommended):**
- Primary: System Blue or custom brand color
- E2E Indicator: System Orange (lock icon)
- UCE Indicator: System Green (shield icon)
- Shared: System Purple (share icon)
- Background: System background colors

**Typography:**
- System font (San Francisco)
- Dynamic Type sizes
- Text styles: Title, Headline, Body, Caption

---

### Screen Specifications

#### 1. Splash / Launch Screen

**Purpose:** App launch

**Requirements:**
- Simple logo or app name
- Check authentication status
- Navigate to Login or Timeline

---

#### 2. Signup Screen

**Components:**
- Email text field
- Password text field (secure entry)
- Password strength indicator
- "Sign Up" button

**Flow:**
1. Validate email format
2. Validate password strength
3. Navigate to Encryption Tier Selection

**Validation:**
- Email must be valid format
- Password minimum 12 characters
- Show inline error messages

---

#### 3. Encryption Tier Selection Screen

**Components:**
- Two cards: E2E and UCE
- Feature comparison checklist
- Warning: "This choice is permanent"
- "Continue with E2E" button
- "Continue with UCE" button

**E2E Card:**
- 🔒 Icon
- Title: "Maximum Privacy"
- Features:
  - ✅ Keys never on server
  - ✅ True end-to-end encryption
  - ❌ Limited search
  - ❌ Manual device setup
- Description: "For privacy enthusiasts"

**UCE Card:**
- 🛡️ Icon
- Title: "Smart Features"
- Features:
  - ✅ Full-text search
  - ✅ AI auto-tagging
  - ✅ Easy recovery
  - ✅ Instant sync
- Description: "For most users"

**User Selection:**
- Tap card to select
- Show confirmation alert
- If E2E: Navigate to Recovery Codes
- If UCE: Complete signup

---

#### 4. Recovery Codes Screen (E2E Only)

**Components:**
- Title: "Save Your Recovery Codes"
- Warning: "These are the ONLY way to recover your account"
- Grid of 10 recovery codes
- "Download" button (save as text file)
- "Copy All" button
- Checkbox: "I have saved my codes"
- "Continue" button (disabled until checkbox)

**Layout:**
- 2 columns of 5 codes each
- Monospace font for codes
- Highlight on tap to copy individual code

**Behavior:**
- Force user to confirm they saved codes
- Show multiple warnings about importance
- Only proceed when checkbox is checked

---

#### 5. Login Screen

**Components:**
- Email text field
- Password text field
- "Log In" button
- "Forgot Password?" link
- Biometric prompt (if enabled)

**Flow:**
1. User enters credentials
2. Authenticate with API
3. Store tokens in Keychain
4. Navigate to Timeline

**Biometric Authentication:**
- If enabled and credentials saved
- Show Face ID / Touch ID prompt
- On success, auto-login

---

#### 6. Timeline Screen (Main)

**Navigation:**
- Tab bar: Timeline, Search, Create, Settings

**Components:**
- Navigation bar:
  - Title: "My Diary"
  - Encryption tier badge (🔒 or 🛡️)
  - Sync status indicator
- Pull-to-refresh
- Entry list:
  - Date section headers
  - Entry cards
- Floating Action Button: "+"

**Entry Card:**
- Encrypted title (if exists) or preview of content
- Date and time
- Tags (chips)
- Media thumbnail (if has media)
- Share indicator (if shared to Facebook)
- Source badge (if imported from Facebook)
- Tap to open entry detail

**Empty State:**
- "Start your first entry"
- Large "+" button

---

#### 7. Entry Detail Screen

**Components:**
- Navigation bar:
  - Back button
  - Edit button
  - Share button (to Facebook)
  - More menu (delete, view history)
- Decrypted content (full text)
- Media gallery (photos/videos)
- Tags (chips)
- Metadata:
  - Created date/time
  - Modified date/time
  - Source (diary / facebook)
- External post link (if shared)

**Actions:**
- Edit entry
- Delete entry (confirmation alert)
- Share to Facebook
- View history (show entry events)

---

#### 8. Create/Edit Entry Screen

**Components:**
- Navigation bar:
  - Cancel button
  - Save button
- Title text field (optional)
- Content text view (main diary text)
- Media picker button
- Location button (optional)
- Mood selector (optional)
- Tag input field

**Media Picker:**
- Access photo library
- Take photo/video
- Multiple selection
- Show thumbnails below content

**Behavior:**
- Auto-save draft locally
- Encrypt on save
- Upload to server
- Show success/error feedback

---

#### 9. Search Screen

**For UCE Users:**
- Search bar
- Search suggestions
- Recent searches
- Results list (same as Timeline)
- Filters: tags, date range, source

**For E2E Users:**
- Search bar
- "Client-side search only" notice
- Search results (from local database)
- Filters: tags, date range, source
- Note: "Search across all devices not available"

---

#### 10. Settings Screen

**Sections:**

**Account:**
- Email (display only)
- Encryption tier badge
- Change password
- Logout

**Sync:**
- Last sync time
- Manual sync button
- Sync settings (frequency)

**Storage:**
- Used storage (progress bar)
- Storage limit
- Upgrade button (if free tier)

**Social Media:**
- Connected accounts
  - Facebook (connect/disconnect)
  - Instagram (coming soon)
- Sync settings

**Security:**
- Biometric authentication toggle
- Recovery codes (E2E only) - view/regenerate

**Preferences:**
- Theme (light/dark/system)
- Notifications
- Default privacy for new entries

**About:**
- Version number
- Privacy policy
- Terms of service
- Support/feedback

---

#### 11. Facebook Integration Screens

**Connect Facebook:**
- "Connect to Facebook" button
- OAuth webview/browser flow
- Success/error feedback

**Share to Facebook:**
- Modal sheet
- Entry preview (decrypted)
- Edit before sharing (text area)
- Privacy selector (public/friends/only_me)
- "Post" button
- Cancel button

**Import from Facebook:**
- "Import" button
- Date range selector (optional)
- Progress indicator
- Success message with count
- View imported entries

---

## 🔄 Sync Implementation

### Sync Strategy

**Agent must implement:**

1. **Background Sync**
   - On app open
   - On app return from background
   - Every 5 minutes when active

2. **Manual Sync**
   - Pull-to-refresh on Timeline
   - "Sync Now" button in Settings

3. **Upload Queue**
   - Queue local changes
   - Upload when online
   - Retry failed uploads (exponential backoff)

4. **Conflict Resolution**
   - Download server changes
   - Compare timestamps
   - Last write wins
   - Notify user of conflicts

---

### Sync Service Requirements

**Agent must implement methods for:**

- `syncAll()` - Full sync (download all entries)
- `syncIncremental()` - Sync since last sync
- `uploadPendingEntries()` - Upload local changes
- `downloadNewEntries()` - Fetch new from server
- `resolveConflicts()` - Handle conflicting edits
- `cancelSync()` - Cancel ongoing sync

**Sync Status Tracking:**
- Store last_sync_timestamp
- Track sync_status per entry
- Show sync indicator in UI

---

## 📸 Media Handling

### Requirements

**Agent must implement:**

1. **Media Capture**
   - Access camera
   - Access photo library
   - Video recording
   - Multiple selection

2. **Media Encryption**
   - Encrypt media files before upload
   - Store encrypted locally
   - Generate SHA-256 hash

3. **Media Upload**
   - Upload encrypted media to server
   - Progress indicator
   - Handle large files (chunking if needed)

4. **Media Display**
   - Decrypt media for display
   - Thumbnail generation
   - Full-screen viewer
   - Video playback

5. **Media Caching**
   - Cache decrypted media temporarily
   - Clear cache on app close
   - Manage cache size

---

### Photo/Video Picker Integration

**Use:**
- PHPicker (iOS 14+) for photo library access
- UIImagePickerController for camera
- Or third-party library for better UX

**Permissions:**
- Request photo library access
- Request camera access
- Handle permission denial gracefully

---

## 🔔 Notifications (Optional for MVP)

**If implemented, agent must support:**

- Push notifications for sync completion
- Push notifications for Facebook mentions
- Local notifications for sync errors
- Notification settings in Settings screen

**Requirements:**
- Request notification permission
- Handle notification taps
- Deep linking to specific entry

---

## 🧪 Testing Requirements

### Unit Tests

**Agent must create tests for:**
- Encryption/decryption functions
- Key generation
- Hash generation
- API client methods
- Database CRUD operations
- Sync logic
- Conflict resolution

**Target Coverage:** 80%+

---

### Integration Tests

**Agent must create tests for:**
- Complete signup flow
- Login and authentication
- Entry creation and sync
- Media upload and download
- Facebook OAuth flow (mocked)

---

### UI Tests

**Agent must create UI tests for:**
- Signup flow (both tiers)
- Create entry
- View entry
- Edit entry
- Share to Facebook (mocked)
- Search entries

**Use:** XCTest UI Testing

---

## 🚀 Deployment

### App Store Requirements

**Agent must prepare:**
- App icon (all required sizes)
- Launch screen
- Screenshots (iPhone and iPad)
- App description
- Privacy policy URL
- Support URL

**App Store Connect:**
- Bundle identifier: `com.jstuart0.personaldiary`
- Version: 1.0.0
- Build number: Auto-increment

---

### TestFlight Beta

**Agent must:**
- Configure TestFlight build
- Add beta tester groups
- Write beta testing notes
- Collect feedback

---

## 📱 Platform-Specific Features

### iOS-Specific Features to Implement

1. **Widgets (Optional)**
   - Quick entry widget
   - Recent entries widget

2. **Shortcuts Integration (Optional)**
   - Siri shortcuts for quick entry
   - "Add to diary" shortcut

3. **Share Extension**
   - Share content from other apps to diary
   - Share photos, text, URLs

4. **Handoff (Optional)**
   - Continue entry creation on Mac
   - Requires Mac app

5. **iCloud Backup (Optional)**
   - Backup encrypted entries to iCloud
   - Restore on new device

---

## ⚡ Performance Requirements

**Agent must ensure:**
- App launch time < 2 seconds
- Entry list scrolling: 60 FPS
- Encryption/decryption < 100ms per entry
- Image decryption < 500ms per image
- Memory usage < 150MB typical
- Battery efficient (no excessive background activity)

---

## ♿ Accessibility Requirements

**Agent must implement:**
- VoiceOver support for all UI elements
- Dynamic Type support
- High Contrast mode support
- Reduce Motion support
- Keyboard navigation (iPad)
- Accessibility labels for all buttons/images

---

## 🌍 Localization (Future)

**Agent must prepare for:**
- English (MVP)
- Localization-ready code
- NSLocalizedString for all user-facing text
- Date/time formatting with user's locale
- Right-to-left language support (future)

---

## 📦 Dependencies (Recommended)

Agent chooses specific libraries, but consider:

**Networking:**
- Native URLSession (built-in)
- Or Alamofire (popular third-party)

**Encryption:**
- CryptoKit (built-in, recommended)
- Or Sodium (third-party)

**Database:**
- SwiftData (built-in, iOS 17+)
- Or Core Data (built-in, iOS 16+)
- Or Realm (third-party)

**Image Caching:**
- Kingfisher (popular)
- Or native caching

**Keychain:**
- Native Security framework (built-in)
- Or KeychainAccess (wrapper)

---

## 🔧 Configuration

**Agent must create:**

- Debug configuration (development API)
- Release configuration (production API)
- Environment variables:
  - API_BASE_URL
  - FACEBOOK_APP_ID
  - SENTRY_DSN (error tracking, optional)

**Info.plist entries:**
- NSCameraUsageDescription
- NSPhotoLibraryUsageDescription
- NSFaceIDUsageDescription
- CFBundleURLTypes (for OAuth callback)

---

## 📝 Implementation Checklist

**Phase 1: Setup**
- [ ] Create Xcode project
- [ ] Set up project structure
- [ ] Configure SPM dependencies
- [ ] Set up Core Data/SwiftData models
- [ ] Create color/typography system

**Phase 2: Authentication**
- [ ] Implement Keychain service
- [ ] Create signup screens
- [ ] Create login screen
- [ ] Implement encryption tier selection
- [ ] Generate E2E keys
- [ ] Handle UCE key derivation

**Phase 3: Core Features**
- [ ] Implement timeline screen
- [ ] Implement entry detail screen
- [ ] Implement create/edit entry
- [ ] Implement local database
- [ ] Implement encryption service
- [ ] Implement API client

**Phase 4: Sync**
- [ ] Implement sync service
- [ ] Handle offline mode
- [ ] Implement conflict resolution
- [ ] Background sync

**Phase 5: Media**
- [ ] Implement photo picker
- [ ] Implement media encryption
- [ ] Implement media upload
- [ ] Implement media display

**Phase 6: Social**
- [ ] Implement Facebook OAuth
- [ ] Implement share to Facebook
- [ ] Implement import from Facebook

**Phase 7: Search**
- [ ] Implement search UI
- [ ] Implement E2E client-side search
- [ ] Implement UCE server search

**Phase 8: Polish**
- [ ] Implement settings screen
- [ ] Add animations
- [ ] Accessibility
- [ ] Error handling
- [ ] Unit tests
- [ ] UI tests

---

## 🎯 Success Criteria

**Agent must achieve:**
- [ ] App runs on iOS 16+
- [ ] Signup flow works for both tiers
- [ ] Entry creation and encryption works
- [ ] Sync works reliably
- [ ] Facebook integration works
- [ ] Search works (tier-appropriate)
- [ ] Offline mode works
- [ ] 80%+ test coverage
- [ ] Passes App Store review

---

**End of iOS App Specification**

Agent should use this specification to implement a production-ready iOS app that adheres to all requirements while making appropriate technical decisions.
