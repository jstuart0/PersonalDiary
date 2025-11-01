# Android App - Mock Authentication Testing Guide

This guide explains how to test the Android app using `MockAuthRepository` for offline development and UI testing without requiring backend connectivity.

---

## Why Mock Testing?

**Use Cases:**
- ✅ UI/UX development and testing
- ✅ Offline development without backend
- ✅ Local database and encryption testing
- ✅ Fast iteration on authentication flows
- ✅ Testing before backend is fully set up

**What Works:**
- Complete signup and login flows
- Local user storage (Room database)
- Encryption service initialization
- Timeline and entry screens
- Navigation between screens
- Biometric authentication (if device supports)

**What Doesn't Work:**
- Backend sync
- Server-side search (UCE tier)
- Social media integration
- Password reset emails
- Multi-device sync

---

## Quick Start

### Option 1: Enable Mock Repository via DI Module

Create a new Hilt module to bind MockAuthRepository:

**File:** `android/app/src/main/java/com/jstuart0/personaldiary/di/RepositoryModule.kt`

```kotlin
package com.jstuart0.personaldiary.di

import com.jstuart0.personaldiary.data.repository.AuthRepository
import com.jstuart0.personaldiary.data.repository.MockAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        mockAuthRepository: MockAuthRepository
    ): AuthRepository
}
```

Then comment out the direct injection in `AuthViewModel`:

```kotlin
// Before:
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository  // Uses real repository
)

// After:
class AuthViewModel @Inject constructor(
    @MockMode private val authRepository: AuthRepository  // Uses bound mock
)
```

### Option 2: Feature Flag for Mock Mode

Add a BuildConfig field:

**File:** `android/app/build.gradle.kts`

```kotlin
buildTypes {
    debug {
        isMinifyEnabled = false
        buildConfigField("String", "API_BASE_URL", "\"http://localhost:3001\"")
        buildConfigField("Boolean", "USE_MOCK_AUTH", "true")  // Add this
    }
    release {
        buildConfigField("Boolean", "USE_MOCK_AUTH", "false")
    }
}
```

Then modify the DI binding:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        realRepository: AuthRepository,
        mockRepository: MockAuthRepository
    ): AuthRepository {
        return if (BuildConfig.USE_MOCK_AUTH) {
            mockRepository
        } else {
            realRepository
        }
    }
}
```

---

## Testing Workflow

### 1. Install and Launch

```bash
# Ensure device is connected
adb devices

# Build and install debug APK
cd android
./gradlew installDebug

# Launch app
adb shell am start -n com.jstuart0.personaldiary/.MainActivity
```

### 2. Test Signup Flow (UCE)

1. App opens to Login screen
2. Tap "Create Account"
3. Select "Balanced (UCE)" tier
4. Enter email: `test@example.com`
5. Enter password: `TestPassword123!`
6. Tap "Sign Up"
7. **Mock behavior:**
   - Simulates 1-second delay
   - Creates local user with UUID
   - Stores in Room database
   - Navigates to Timeline

### 3. Test Login Flow

1. Force stop app: `adb shell am force-stop com.jstuart0.personaldiary`
2. Launch again
3. Enter same credentials
4. Tap "Login"
5. **Mock behavior:**
   - Simulates 500ms delay
   - Looks up user by email in local database
   - If not found, creates new user
   - Navigates to Timeline

### 4. Test Entry Creation

1. From Timeline, tap "+" FAB
2. Enter diary content
3. Tap "Save"
4. **Mock behavior:**
   - Encrypts content locally
   - Saves to Room database
   - Syncs to backend (mock - no actual network call)
   - Returns to Timeline with new entry

### 5. Verify Local Database

```bash
# Pull database from device
adb pull /data/data/com.jstuart0.personaldiary/databases/personal_diary.db ./

# Inspect with SQLite
sqlite3 personal_diary.db
.tables
SELECT * FROM users;
SELECT * FROM entries;
.quit
```

---

## Mock Repository Behavior

### MockAuthRepository Methods

#### `signup(email, password, encryptionTier)`
```kotlin
// Simulates network delay
delay(1000)

// Creates user locally
val userId = UUID.randomUUID().toString()
val user = User(
    userId = userId,
    email = email,
    encryptionTier = encryptionTier,
    publicKey = if (encryptionTier == EncryptionTier.E2E) "mock-public-key" else null,
    encryptedMasterKey = if (encryptionTier == EncryptionTier.UCE) "mock-encrypted-master-key" else null,
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)

// Saves to Room database
userDao.insert(UserEntity.fromDomain(user))

return Result.success(user)
```

#### `login(email, password)`
```kotlin
// Simulates network delay
delay(500)

// Tries to find user by email
val userEntity = userDao.getUserByEmail(email)
if (userEntity != null) {
    return Result.success(userEntity.toDomain())
} else {
    // Auto-creates user if not found (for demo purposes)
    return signup(email, password, EncryptionTier.UCE)
}
```

#### `logout()`
```kotlin
// Mock implementation - doesn't clear database
// Could be enhanced to clear data if needed
```

### What Gets Stored

**Room Database Tables:**
- `users` - User account information
- `entries` - Diary entries (encrypted)
- `entries_fts` - Full-text search index (E2E only)
- `media` - Media attachments
- `tags` - Entry tags
- `social_accounts` - Connected social accounts

**Encrypted Shared Preferences:**
- User session data
- Encryption keys (Android Keystore)
- App settings

---

## Advanced Mock Testing

### Test Encryption Flows

```kotlin
// In MockAuthRepository, you can customize encryption testing:

// E2E Tier Testing
if (encryptionTier == EncryptionTier.E2E) {
    // Generate real keypair using E2EEncryptionService
    val publicKey = e2eEncryptionService.exportPublicKey()
    // Test local encryption/decryption
}

// UCE Tier Testing
if (encryptionTier == EncryptionTier.UCE) {
    // Derive real key from password using UCEEncryptionService
    val encryptedKey = uceEncryptionService.deriveAndEncryptMasterKey(password)
    // Test encryption/decryption with derived key
}
```

### Test Error Scenarios

Modify `MockAuthRepository` to simulate errors:

```kotlin
suspend fun signup(email: String, password: String, encryptionTier: EncryptionTier): Result<User> {
    delay(1000)

    // Simulate network error
    if (email.contains("error")) {
        return Result.failure(Exception("Network error"))
    }

    // Simulate validation error
    if (password.length < 12) {
        return Result.failure(Exception("Password too short"))
    }

    // Normal flow...
}
```

### Test Multi-User Scenarios

```kotlin
// Create multiple test users
signup("user1@example.com", "Password123!", EncryptionTier.UCE)
signup("user2@example.com", "Password456!", EncryptionTier.E2E)

// Test switching between users
logout()
login("user1@example.com", "Password123!")
```

---

## Debugging Mock Mode

### Enable Logging

Add logging to `MockAuthRepository`:

```kotlin
suspend fun signup(...): Result<User> {
    Log.d("MockAuth", "Signup started for email: $email")
    delay(1000)
    Log.d("MockAuth", "Creating user with tier: $encryptionTier")
    val user = User(...)
    Log.d("MockAuth", "User created with ID: ${user.userId}")
    userDao.insert(UserEntity.fromDomain(user))
    Log.d("MockAuth", "User saved to database")
    return Result.success(user)
}
```

### Monitor Logs

```bash
# Filter by tag
adb logcat -s MockAuth:V

# Filter by app
adb logcat | grep com.jstuart0.personaldiary

# Monitor Room database operations
adb logcat -s RoomDatabase:V
```

### Inspect Database

```bash
# Pull and inspect database
adb pull /data/data/com.jstuart0.personaldiary/databases/personal_diary.db
sqlite3 personal_diary.db "SELECT * FROM users;"
```

---

## Switching Back to Real Backend

### Option 1: Change BuildConfig Flag

```kotlin
buildTypes {
    debug {
        buildConfigField("Boolean", "USE_MOCK_AUTH", "false")  // Change to false
    }
}
```

Then rebuild:
```bash
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Option 2: Comment Out DI Binding

Remove or comment out the `RepositoryModule`:

```kotlin
// @Module
// @InstallIn(SingletonComponent::class)
// abstract class RepositoryModule {
//     @Binds
//     @Singleton
//     abstract fun bindAuthRepository(
//         mockAuthRepository: MockAuthRepository
//     ): AuthRepository
// }
```

This will fall back to constructor injection of the real `AuthRepository`.

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Android UI Tests

on: [push, pull_request]

jobs:
  ui-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Build debug APK
        run: |
          cd android
          ./gradlew assembleDebug -PUSE_MOCK_AUTH=true

      - name: Run instrumentation tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 33
          arch: x86_64
          script: ./gradlew connectedDebugAndroidTest
```

---

## Best Practices

### Do's ✅
- Use mock mode for UI development
- Test encryption flows locally
- Verify database operations
- Test navigation and error states
- Clear app data between test runs
- Log important state changes

### Don'ts ❌
- Don't use mock mode for production builds
- Don't skip integration testing with real backend
- Don't commit sensitive test data
- Don't assume mock behavior matches backend exactly
- Don't forget to test network error scenarios

---

## Troubleshooting

### Issue: App crashes on startup
**Solution:** Check if Room database schema matches entity definitions. Clear app data and reinstall.

```bash
adb shell pm clear com.jstuart0.personaldiary
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Issue: Login always creates new user
**Solution:** This is expected behavior in mock mode. User lookup by email works, but password validation is skipped for simplicity.

### Issue: Encryption errors
**Solution:** Ensure encryption services are properly initialized:

```kotlin
// Check logs for encryption initialization
adb logcat -s E2EEncryption:V UCEEncryption:V
```

### Issue: Database not persisting
**Solution:** Verify Room database is configured correctly:

```kotlin
// In AppDatabase.kt
@Database(
    entities = [UserEntity::class, EntryEntity::class, ...],
    version = 1,
    exportSchema = true
)
```

---

## Conclusion

Mock testing allows rapid development and testing of the Android app without backend dependencies. Use it for:

- **UI/UX iteration**
- **Local feature development**
- **Encryption testing**
- **Database operations**
- **Offline functionality**

For full integration testing, switch to the real `AuthRepository` once the backend is available.

---

**Last Updated:** 2025-11-01
**Android App Version:** 1.0.0
**Mock Repository Location:** `android/app/src/main/java/com/jstuart0/personaldiary/data/repository/MockAuthRepository.kt`
