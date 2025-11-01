//
//  KeychainService.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import Foundation
import Security
import LocalAuthentication

/// Keychain service for secure storage of sensitive data
final class KeychainService {
    // MARK: - Singleton

    static let shared = KeychainService()

    private init() {}

    // MARK: - Public Methods

    /// Save data to Keychain
    /// - Parameters:
    ///   - data: Data to save
    ///   - key: Keychain key
    ///   - requireBiometric: Whether to require biometric authentication to access
    /// - Throws: AppError if save fails
    func save(_ data: Data, for key: String, requireBiometric: Bool = false) throws {
        // Delete existing item first
        try? delete(key)

        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Constants.Keychain.service,
            kSecAttrAccount as String: key,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]

        // Add biometric protection if requested
        if requireBiometric {
            let access = SecAccessControlCreateWithFlags(
                nil,
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
                .biometryCurrentSet,
                nil
            )
            query[kSecAttrAccessControl as String] = access
        }

        // Add access group if configured
        if let accessGroup = Constants.Keychain.accessGroup {
            query[kSecAttrAccessGroup as String] = accessGroup
        }

        let status = SecItemAdd(query as CFDictionary, nil)

        guard status == errSecSuccess else {
            throw AppError.databaseError("Keychain save failed with status: \(status)")
        }
    }

    /// Save string to Keychain
    /// - Parameters:
    ///   - string: String to save
    ///   - key: Keychain key
    ///   - requireBiometric: Whether to require biometric authentication to access
    /// - Throws: AppError if save fails
    func save(_ string: String, for key: String, requireBiometric: Bool = false) throws {
        guard let data = string.data(using: .utf8) else {
            throw AppError.encodingFailed("Failed to encode string to data")
        }
        try save(data, for: key, requireBiometric: requireBiometric)
    }

    /// Retrieve data from Keychain
    /// - Parameters:
    ///   - key: Keychain key
    ///   - prompt: Optional biometric authentication prompt
    /// - Returns: Data if found, nil otherwise
    /// - Throws: AppError if retrieval fails
    func retrieve(_ key: String, prompt: String? = nil) throws -> Data? {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Constants.Keychain.service,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        // Add access group if configured
        if let accessGroup = Constants.Keychain.accessGroup {
            query[kSecAttrAccessGroup as String] = accessGroup
        }

        // Add biometric authentication prompt if provided
        if let prompt = prompt {
            let context = LAContext()
            query[kSecUseAuthenticationContext as String] = context
            query[kSecUseOperationPrompt as String] = prompt
        }

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        switch status {
        case errSecSuccess:
            return result as? Data
        case errSecItemNotFound:
            return nil
        case errSecUserCanceled:
            throw AppError.biometricAuthenticationFailed
        case errSecAuthFailed:
            throw AppError.biometricAuthenticationFailed
        default:
            throw AppError.databaseError("Keychain retrieve failed with status: \(status)")
        }
    }

    /// Retrieve string from Keychain
    /// - Parameters:
    ///   - key: Keychain key
    ///   - prompt: Optional biometric authentication prompt
    /// - Returns: String if found, nil otherwise
    /// - Throws: AppError if retrieval fails
    func retrieveString(_ key: String, prompt: String? = nil) throws -> String? {
        guard let data = try retrieve(key, prompt: prompt) else {
            return nil
        }

        guard let string = String(data: data, encoding: .utf8) else {
            throw AppError.decodingFailed("Failed to decode data to string")
        }

        return string
    }

    /// Delete item from Keychain
    /// - Parameter key: Keychain key
    /// - Throws: AppError if deletion fails
    func delete(_ key: String) throws {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Constants.Keychain.service,
            kSecAttrAccount as String: key
        ]

        // Add access group if configured
        if let accessGroup = Constants.Keychain.accessGroup {
            query[kSecAttrAccessGroup as String] = accessGroup
        }

        let status = SecItemDelete(query as CFDictionary)

        // Success or item not found are both acceptable
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw AppError.databaseError("Keychain delete failed with status: \(status)")
        }
    }

    /// Check if key exists in Keychain
    /// - Parameter key: Keychain key
    /// - Returns: True if exists, false otherwise
    func exists(_ key: String) -> Bool {
        do {
            let data = try retrieve(key)
            return data != nil
        } catch {
            return false
        }
    }

    /// Update existing Keychain item
    /// - Parameters:
    ///   - data: New data
    ///   - key: Keychain key
    /// - Throws: AppError if update fails
    func update(_ data: Data, for key: String) throws {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Constants.Keychain.service,
            kSecAttrAccount as String: key
        ]

        if let accessGroup = Constants.Keychain.accessGroup {
            query[kSecAttrAccessGroup as String] = accessGroup
        }

        let attributes: [String: Any] = [
            kSecValueData as String: data
        ]

        let status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)

        if status == errSecItemNotFound {
            // Item doesn't exist, create it
            try save(data, for: key)
        } else if status != errSecSuccess {
            throw AppError.databaseError("Keychain update failed with status: \(status)")
        }
    }

    /// Clear all Keychain items for this app
    /// - Throws: AppError if clear fails
    func clearAll() throws {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Constants.Keychain.service
        ]

        if let accessGroup = Constants.Keychain.accessGroup {
            query[kSecAttrAccessGroup as String] = accessGroup
        }

        let status = SecItemDelete(query as CFDictionary)

        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw AppError.databaseError("Keychain clear failed with status: \(status)")
        }
    }

    // MARK: - Convenience Methods for App-Specific Keys

    /// Save E2E private key
    func saveE2EPrivateKey(_ key: Data) throws {
        try save(key, for: Constants.Keychain.e2ePrivateKeyKey, requireBiometric: true)
    }

    /// Retrieve E2E private key
    func retrieveE2EPrivateKey() throws -> Data? {
        try retrieve(Constants.Keychain.e2ePrivateKeyKey, prompt: Constants.Security.biometricAuthReason)
    }

    /// Save E2E public key
    func saveE2EPublicKey(_ key: Data) throws {
        try save(key, for: Constants.Keychain.e2ePublicKeyKey)
    }

    /// Retrieve E2E public key
    func retrieveE2EPublicKey() throws -> Data? {
        try retrieve(Constants.Keychain.e2ePublicKeyKey)
    }

    /// Save UCE master key
    func saveUCEMasterKey(_ key: Data) throws {
        try save(key, for: Constants.Keychain.uceMasterKeyKey, requireBiometric: true)
    }

    /// Retrieve UCE master key
    func retrieveUCEMasterKey() throws -> Data? {
        try retrieve(Constants.Keychain.uceMasterKeyKey, prompt: Constants.Security.biometricAuthReason)
    }

    /// Save access token
    func saveAccessToken(_ token: String) throws {
        try save(token, for: Constants.Keychain.accessTokenKey)
    }

    /// Retrieve access token
    func retrieveAccessToken() throws -> String? {
        try retrieveString(Constants.Keychain.accessTokenKey)
    }

    /// Save refresh token
    func saveRefreshToken(_ token: String) throws {
        try save(token, for: Constants.Keychain.refreshTokenKey)
    }

    /// Retrieve refresh token
    func retrieveRefreshToken() throws -> String? {
        try retrieveString(Constants.Keychain.refreshTokenKey)
    }

    /// Save user ID
    func saveUserId(_ userId: String) throws {
        try save(userId, for: Constants.Keychain.userIdKey)
    }

    /// Retrieve user ID
    func retrieveUserId() throws -> String? {
        try retrieveString(Constants.Keychain.userIdKey)
    }

    /// Save encryption tier
    func saveEncryptionTier(_ tier: EncryptionTier) throws {
        try save(tier.rawValue, for: Constants.Keychain.encryptionTierKey)
    }

    /// Retrieve encryption tier
    func retrieveEncryptionTier() throws -> EncryptionTier? {
        guard let tierString = try retrieveString(Constants.Keychain.encryptionTierKey) else {
            return nil
        }
        return EncryptionTier(rawValue: tierString)
    }

    /// Clear all authentication data
    func clearAuthenticationData() throws {
        try? delete(Constants.Keychain.accessTokenKey)
        try? delete(Constants.Keychain.refreshTokenKey)
        try? delete(Constants.Keychain.userIdKey)
        try? delete(Constants.Keychain.encryptionTierKey)
        try? delete(Constants.Keychain.e2ePrivateKeyKey)
        try? delete(Constants.Keychain.e2ePublicKeyKey)
        try? delete(Constants.Keychain.uceMasterKeyKey)
    }
}
