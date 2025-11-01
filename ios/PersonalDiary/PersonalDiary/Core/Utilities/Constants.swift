//
//  Constants.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import Foundation

/// App-wide constants
enum Constants {
    // MARK: - API Configuration

    enum API {
        #if DEBUG
        static let baseURL = "http://localhost:8000/api/v1"
        #else
        static let baseURL = "https://api.personaldiary.com/api/v1"
        #endif

        static let timeout: TimeInterval = 30
        static let maxRetries = 3
    }

    // MARK: - Encryption Configuration

    enum Encryption {
        static let algorithm = "AES-256-GCM"
        static let keySize = 256  // bits
        static let saltSize = 32  // bytes
        static let nonceSize = 12  // bytes for AES-GCM
        static let tagSize = 16    // bytes for AES-GCM authentication tag

        // Password hashing
        static let argon2Iterations = 3
        static let argon2Memory = 65536  // 64 MB
        static let argon2Parallelism = 4
    }

    // MARK: - Keychain Configuration

    enum Keychain {
        static let service = "com.jstuart0.personaldiary"
        static let accessGroup: String? = nil  // Set if using app groups

        // Keys
        static let e2ePrivateKeyKey = "e2e_private_key"
        static let e2ePublicKeyKey = "e2e_public_key"
        static let uceMasterKeyKey = "uce_master_key"
        static let accessTokenKey = "access_token"
        static let refreshTokenKey = "refresh_token"
        static let userIdKey = "user_id"
        static let encryptionTierKey = "encryption_tier"
    }

    // MARK: - Storage Configuration

    enum Storage {
        static let freeTierLimit: Int64 = 1_073_741_824  // 1 GB
        static let paidTierLimit: Int64 = 53_687_091_200  // 50 GB

        // Media limits
        static let maxImageSize: Int64 = 10_485_760  // 10 MB
        static let maxVideoSize: Int64 = 104_857_600  // 100 MB
        static let maxMediaPerEntry = 10

        // Content limits
        static let maxEntryContentLength = 100_000  // characters
        static let maxTagsPerEntry = 20
    }

    // MARK: - Sync Configuration

    enum Sync {
        static let backgroundSyncInterval: TimeInterval = 300  // 5 minutes
        static let maxConcurrentUploads = 3
        static let maxConcurrentDownloads = 5
        static let chunkSize = 1_048_576  // 1 MB for chunked uploads
    }

    // MARK: - Database Configuration

    enum Database {
        static let name = "PersonalDiary"
        static let version = 1
        static let maxCacheSize = 100  // entries to keep in memory
    }

    // MARK: - UI Configuration

    enum UI {
        // Debounce delays
        static let searchDebounceDelay: TimeInterval = 0.5
        static let typingDebounceDelay: TimeInterval = 0.3

        // Animation durations
        static let shortAnimationDuration: TimeInterval = 0.2
        static let mediumAnimationDuration: TimeInterval = 0.3
        static let longAnimationDuration: TimeInterval = 0.5

        // Pagination
        static let entriesPerPage = 20
        static let searchResultsPerPage = 20

        // Thumbnail sizes
        static let thumbnailSize: CGFloat = 80
        static let previewImageSize: CGFloat = 300
    }

    // MARK: - Security Configuration

    enum Security {
        // Biometric authentication
        static let biometricAuthReason = "Authenticate to access your diary"
        static let maxBiometricAttempts = 3

        // Session management
        static let sessionTimeout: TimeInterval = 3600  // 1 hour
        static let refreshTokenBeforeExpiry: TimeInterval = 300  // 5 minutes

        // Recovery codes (E2E)
        static let recoveryCodeCount = 10
        static let recoveryCodeLength = 16
    }

    // MARK: - Facebook Configuration

    enum Facebook {
        static let appId = "YOUR_FACEBOOK_APP_ID"  // Replace with actual ID
        static let redirectURI = "personaldiary://facebook-callback"
        static let permissions = ["email", "user_posts", "publish_actions"]
    }

    // MARK: - Validation

    enum Validation {
        static let minPasswordLength = 12
        static let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"

        // Password strength requirements
        static let requireUppercase = true
        static let requireLowercase = true
        static let requireNumbers = true
        static let requireSpecialChars = true
    }

    // MARK: - Error Messages

    enum ErrorMessages {
        static let genericError = "An error occurred. Please try again."
        static let networkError = "No internet connection. Please check your network settings."
        static let serverError = "Server error. Please try again later."
    }

    // MARK: - Notifications

    enum NotificationNames {
        static let userDidLogout = Notification.Name("userDidLogout")
        static let syncDidComplete = Notification.Name("syncDidComplete")
        static let syncDidFail = Notification.Name("syncDidFail")
        static let encryptionKeyDidChange = Notification.Name("encryptionKeyDidChange")
        static let storageDidExceedLimit = Notification.Name("storageDidExceedLimit")
    }

    // MARK: - UserDefaults Keys

    enum UserDefaultsKeys {
        static let lastSyncTimestamp = "last_sync_timestamp"
        static let biometricAuthEnabled = "biometric_auth_enabled"
        static let darkModeEnabled = "dark_mode_enabled"
        static let syncFrequency = "sync_frequency"
        static let notificationsEnabled = "notifications_enabled"
        static let hasCompletedOnboarding = "has_completed_onboarding"
        static let lastSelectedEncryptionTier = "last_selected_encryption_tier"
    }

    // MARK: - Date Formatting

    enum DateFormat {
        static let iso8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        static let displayDate = "MMM d, yyyy"
        static let displayDateTime = "MMM d, yyyy 'at' h:mm a"
        static let displayTime = "h:mm a"
    }
}
