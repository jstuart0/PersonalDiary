//
//  AppError.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import Foundation

/// Custom error types for the app
enum AppError: LocalizedError {
    // MARK: - Authentication Errors

    case invalidCredentials
    case userAlreadyExists
    case userNotFound
    case invalidToken
    case tokenExpired
    case biometricAuthenticationFailed
    case biometricNotAvailable
    case biometricNotEnrolled

    // MARK: - Encryption Errors

    case encryptionFailed(String)
    case decryptionFailed(String)
    case keyGenerationFailed
    case keyNotFound
    case invalidKey
    case hashingFailed

    // MARK: - Network Errors

    case networkUnavailable
    case serverError(Int, String?)
    case invalidResponse
    case decodingFailed(String)
    case encodingFailed(String)
    case timeout
    case unauthorized
    case forbidden

    // MARK: - Database Errors

    case databaseError(String)
    case entityNotFound(String)
    case saveFailed(String)
    case deleteFailed(String)
    case queryFailed(String)

    // MARK: - Sync Errors

    case syncFailed(String)
    case conflictResolutionFailed
    case uploadFailed(String)
    case downloadFailed(String)

    // MARK: - Media Errors

    case mediaAccessDenied
    case mediaNotFound
    case mediaEncryptionFailed
    case mediaDecryptionFailed
    case mediaUploadFailed(String)
    case mediaDownloadFailed(String)
    case invalidMediaFormat
    case mediaTooLarge(Int64, Int64)  // actual size, max size

    // MARK: - Facebook Integration Errors

    case facebookAuthFailed
    case facebookAPIError(String)
    case facebookTokenExpired
    case facebookPermissionDenied

    // MARK: - Validation Errors

    case invalidEmail
    case passwordTooWeak
    case contentTooLarge
    case invalidInput(String)

    // MARK: - Storage Errors

    case storageLimitExceeded
    case insufficientStorage
    case fileSystemError(String)

    // MARK: - General Errors

    case unknown(String)
    case notImplemented

    // MARK: - LocalizedError Protocol

    var errorDescription: String? {
        switch self {
        // Authentication Errors
        case .invalidCredentials:
            return "Invalid email or password. Please try again."
        case .userAlreadyExists:
            return "An account with this email already exists."
        case .userNotFound:
            return "User not found. Please check your credentials."
        case .invalidToken:
            return "Invalid authentication token. Please log in again."
        case .tokenExpired:
            return "Your session has expired. Please log in again."
        case .biometricAuthenticationFailed:
            return "Biometric authentication failed. Please try again."
        case .biometricNotAvailable:
            return "Biometric authentication is not available on this device."
        case .biometricNotEnrolled:
            return "No biometric data is enrolled. Please set up Face ID or Touch ID in Settings."

        // Encryption Errors
        case .encryptionFailed(let reason):
            return "Failed to encrypt data: \(reason)"
        case .decryptionFailed(let reason):
            return "Failed to decrypt data: \(reason)"
        case .keyGenerationFailed:
            return "Failed to generate encryption key."
        case .keyNotFound:
            return "Encryption key not found. Please log in again."
        case .invalidKey:
            return "Invalid encryption key."
        case .hashingFailed:
            return "Failed to generate content hash."

        // Network Errors
        case .networkUnavailable:
            return "No internet connection. Please check your network settings."
        case .serverError(let code, let message):
            return "Server error (\(code)): \(message ?? "Unknown error")"
        case .invalidResponse:
            return "Invalid response from server."
        case .decodingFailed(let detail):
            return "Failed to decode response: \(detail)"
        case .encodingFailed(let detail):
            return "Failed to encode request: \(detail)"
        case .timeout:
            return "Request timed out. Please try again."
        case .unauthorized:
            return "Unauthorized. Please log in again."
        case .forbidden:
            return "Access forbidden. You don't have permission to access this resource."

        // Database Errors
        case .databaseError(let detail):
            return "Database error: \(detail)"
        case .entityNotFound(let entity):
            return "\(entity) not found."
        case .saveFailed(let detail):
            return "Failed to save: \(detail)"
        case .deleteFailed(let detail):
            return "Failed to delete: \(detail)"
        case .queryFailed(let detail):
            return "Query failed: \(detail)"

        // Sync Errors
        case .syncFailed(let reason):
            return "Sync failed: \(reason)"
        case .conflictResolutionFailed:
            return "Failed to resolve sync conflict. Please try manual sync."
        case .uploadFailed(let reason):
            return "Upload failed: \(reason)"
        case .downloadFailed(let reason):
            return "Download failed: \(reason)"

        // Media Errors
        case .mediaAccessDenied:
            return "Access to photos or camera denied. Please enable in Settings."
        case .mediaNotFound:
            return "Media file not found."
        case .mediaEncryptionFailed:
            return "Failed to encrypt media file."
        case .mediaDecryptionFailed:
            return "Failed to decrypt media file."
        case .mediaUploadFailed(let reason):
            return "Media upload failed: \(reason)"
        case .mediaDownloadFailed(let reason):
            return "Media download failed: \(reason)"
        case .invalidMediaFormat:
            return "Invalid media format. Please use JPEG, PNG, or MP4."
        case .mediaTooLarge(let actual, let max):
            let actualMB = Double(actual) / 1_048_576
            let maxMB = Double(max) / 1_048_576
            return String(format: "Media file too large (%.1f MB). Maximum size is %.1f MB.", actualMB, maxMB)

        // Facebook Integration Errors
        case .facebookAuthFailed:
            return "Facebook authentication failed. Please try again."
        case .facebookAPIError(let detail):
            return "Facebook API error: \(detail)"
        case .facebookTokenExpired:
            return "Facebook token expired. Please reconnect your account."
        case .facebookPermissionDenied:
            return "Facebook permission denied. Please grant required permissions."

        // Validation Errors
        case .invalidEmail:
            return "Invalid email address. Please enter a valid email."
        case .passwordTooWeak:
            return "Password is too weak. Use at least 12 characters with mixed case, numbers, and symbols."
        case .contentTooLarge:
            return "Content is too large. Please reduce the size."
        case .invalidInput(let field):
            return "Invalid input for \(field)."

        // Storage Errors
        case .storageLimitExceeded:
            return "Storage limit exceeded. Please upgrade your plan or delete some entries."
        case .insufficientStorage:
            return "Insufficient device storage. Please free up space."
        case .fileSystemError(let detail):
            return "File system error: \(detail)"

        // General Errors
        case .unknown(let detail):
            return "An unknown error occurred: \(detail)"
        case .notImplemented:
            return "This feature is not yet implemented."
        }
    }

    var recoverySuggestion: String? {
        switch self {
        case .networkUnavailable:
            return "Check your internet connection and try again."
        case .tokenExpired, .invalidToken:
            return "Please log in again to continue."
        case .storageLimitExceeded:
            return "Upgrade to a paid plan for more storage, or delete some entries to free up space."
        case .mediaAccessDenied:
            return "Go to Settings > Privacy > Photos to enable access."
        case .biometricNotEnrolled:
            return "Set up Face ID or Touch ID in your device Settings."
        case .passwordTooWeak:
            return "Use a combination of uppercase, lowercase, numbers, and special characters."
        default:
            return nil
        }
    }
}
