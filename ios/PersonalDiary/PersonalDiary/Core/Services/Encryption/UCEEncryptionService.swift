//
//  UCEEncryptionService.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import Foundation
import CryptoKit

/// UCE (User-Controlled Encryption) Service
/// Uses password-derived keys and AES-256-GCM for encryption
final class UCEEncryptionService: EncryptionServiceProtocol {
    // MARK: - Properties

    private let keychainService = KeychainService.shared
    private var masterKey: SymmetricKey?

    // MARK: - Initialization

    init() {
        // Try to load master key from Keychain
        try? loadKeys()
    }

    // MARK: - EncryptionServiceProtocol

    func generateKeys() throws -> (privateKey: Data, publicKey: Data) {
        // For UCE, we generate a master symmetric key
        // The "private key" is the master key, "public key" is empty for UCE
        let masterKey = SymmetricKey(size: .bits256)
        self.masterKey = masterKey

        let masterKeyData = masterKey.withUnsafeBytes { Data($0) }

        // Save to Keychain
        try keychainService.saveUCEMasterKey(masterKeyData)

        return (
            privateKey: masterKeyData,
            publicKey: Data()  // No public key for UCE
        )
    }

    func encrypt(_ data: Data) throws -> Data {
        guard let masterKey = masterKey else {
            throw AppError.keyNotFound
        }

        // Encrypt using AES-GCM
        let sealedBox = try AES.GCM.seal(data, using: masterKey)

        guard let combined = sealedBox.combined else {
            throw AppError.encryptionFailed("Failed to create combined ciphertext")
        }

        return combined
    }

    func encrypt(_ string: String) throws -> String {
        guard let data = string.data(using: .utf8) else {
            throw AppError.encodingFailed("Failed to encode string to data")
        }

        let encryptedData = try encrypt(data)
        return encryptedData.base64EncodedString()
    }

    func decrypt(_ data: Data) throws -> Data {
        guard let masterKey = masterKey else {
            throw AppError.keyNotFound
        }

        // Decrypt using AES-GCM
        let sealedBox = try AES.GCM.SealedBox(combined: data)
        let decryptedData = try AES.GCM.open(sealedBox, using: masterKey)

        return decryptedData
    }

    func decrypt(_ string: String) throws -> String {
        guard let data = Data(base64Encoded: string) else {
            throw AppError.decodingFailed("Invalid base64 string")
        }

        let decryptedData = try decrypt(data)

        guard let decryptedString = String(data: decryptedData, encoding: .utf8) else {
            throw AppError.decodingFailed("Failed to decode decrypted data to string")
        }

        return decryptedString
    }

    func generateHash(for data: Data) -> String {
        data.sha256Hash
    }

    func generateHash(for string: String) -> String {
        string.sha256Hash
    }

    func loadKeys() throws {
        guard let masterKeyData = try keychainService.retrieveUCEMasterKey() else {
            return  // Key not found, that's okay
        }

        self.masterKey = SymmetricKey(data: masterKeyData)
    }

    func clearKeys() throws {
        try keychainService.delete(Constants.Keychain.uceMasterKeyKey)
        self.masterKey = nil
    }

    // MARK: - UCE Specific Methods

    /// Derive key from password using Argon2id (simulated with PBKDF2 for now)
    /// Note: iOS doesn't have native Argon2id, so we use PBKDF2 as fallback
    /// In production, consider using a third-party library for Argon2id
    /// - Parameters:
    ///   - password: User password
    ///   - salt: Salt for key derivation
    /// - Returns: Derived key
    func deriveKeyFromPassword(_ password: String, salt: Data) throws -> Data {
        guard let passwordData = password.data(using: .utf8) else {
            throw AppError.encodingFailed("Failed to encode password")
        }

        // Use PBKDF2 with SHA-256
        // Note: In production, use Argon2id for better security
        let iterations = 100_000  // PBKDF2 iterations
        let keyLength = 32  // 256 bits

        var derivedKeyData = Data(count: keyLength)
        let result = derivedKeyData.withUnsafeMutableBytes { derivedKeyBytes in
            passwordData.withUnsafeBytes { passwordBytes in
                salt.withUnsafeBytes { saltBytes in
                    CCKeyDerivationPBKDF(
                        CCPBKDFAlgorithm(kCCPBKDF2),
                        passwordBytes.baseAddress?.assumingMemoryBound(to: Int8.self),
                        passwordData.count,
                        saltBytes.baseAddress?.assumingMemoryBound(to: UInt8.self),
                        salt.count,
                        CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256),
                        UInt32(iterations),
                        derivedKeyBytes.baseAddress?.assumingMemoryBound(to: UInt8.self),
                        keyLength
                    )
                }
            }
        }

        guard result == kCCSuccess else {
            throw AppError.keyGenerationFailed
        }

        return derivedKeyData
    }

    /// Set master key from server (after decrypting with password-derived key)
    /// - Parameter encryptedMasterKey: Encrypted master key from server
    /// - Parameter password: User password
    /// - Parameter salt: Salt used for key derivation
    func setMasterKeyFromServer(encryptedMasterKey: String, password: String, salt: String) throws {
        guard let saltData = Data(base64Encoded: salt) else {
            throw AppError.decodingFailed("Invalid salt")
        }

        guard let encryptedData = Data(base64Encoded: encryptedMasterKey) else {
            throw AppError.decodingFailed("Invalid encrypted master key")
        }

        // Derive key from password
        let derivedKey = try deriveKeyFromPassword(password, salt: saltData)
        let symmetricKey = SymmetricKey(data: derivedKey)

        // Decrypt master key
        let sealedBox = try AES.GCM.SealedBox(combined: encryptedData)
        let decryptedMasterKey = try AES.GCM.open(sealedBox, using: symmetricKey)

        // Store master key
        self.masterKey = SymmetricKey(data: decryptedMasterKey)
        try keychainService.saveUCEMasterKey(decryptedMasterKey)
    }

    /// Encrypt master key with password-derived key (for server storage)
    /// - Parameter password: User password
    /// - Returns: Tuple of (encrypted master key, salt)
    func encryptMasterKeyForServer(password: String) throws -> (encryptedKey: String, salt: String) {
        guard let masterKey = masterKey else {
            throw AppError.keyNotFound
        }

        // Generate random salt
        var saltData = Data(count: Constants.Encryption.saltSize)
        let result = saltData.withUnsafeMutableBytes { bytes in
            SecRandomCopyBytes(kSecRandomDefault, Constants.Encryption.saltSize, bytes.baseAddress!)
        }

        guard result == errSecSuccess else {
            throw AppError.keyGenerationFailed
        }

        // Derive key from password
        let derivedKey = try deriveKeyFromPassword(password, salt: saltData)
        let symmetricKey = SymmetricKey(data: derivedKey)

        // Encrypt master key
        let masterKeyData = masterKey.withUnsafeBytes { Data($0) }
        let sealedBox = try AES.GCM.seal(masterKeyData, using: symmetricKey)

        guard let combined = sealedBox.combined else {
            throw AppError.encryptionFailed("Failed to encrypt master key")
        }

        return (
            encryptedKey: combined.base64EncodedString(),
            salt: saltData.base64EncodedString()
        )
    }

    /// Check if master key is loaded
    var hasMasterKey: Bool {
        masterKey != nil
    }

    /// Get master key as Data (for backup purposes only)
    func getMasterKey() -> Data? {
        masterKey?.withUnsafeBytes { Data($0) }
    }
}

// MARK: - CommonCrypto Bridge

import CommonCrypto

// Helper function for CCKeyDerivationPBKDF
private func CCKeyDerivationPBKDF(
    _ algorithm: CCPBKDFAlgorithm,
    _ password: UnsafePointer<Int8>?,
    _ passwordLen: Int,
    _ salt: UnsafePointer<UInt8>?,
    _ saltLen: Int,
    _ prf: CCPseudoRandomAlgorithm,
    _ rounds: UInt32,
    _ derivedKey: UnsafeMutablePointer<UInt8>?,
    _ derivedKeyLen: Int
) -> Int32 {
    return CCKeyDerivationPBKDF(
        algorithm,
        password,
        passwordLen,
        salt,
        saltLen,
        prf,
        rounds,
        derivedKey,
        derivedKeyLen
    )
}
