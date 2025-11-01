//
//  E2EEncryptionService.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import Foundation
import CryptoKit

/// E2E (End-to-End) Encryption Service
/// Uses X25519 key agreement and AES-256-GCM for encryption
final class E2EEncryptionService: EncryptionServiceProtocol {
    // MARK: - Properties

    private let keychainService = KeychainService.shared
    private var privateKey: Curve25519.KeyAgreement.PrivateKey?
    private var publicKey: Curve25519.KeyAgreement.PublicKey?

    // MARK: - Initialization

    init() {
        // Try to load existing keys from Keychain
        try? loadKeys()
    }

    // MARK: - EncryptionServiceProtocol

    func generateKeys() throws -> (privateKey: Data, publicKey: Data) {
        // Generate new X25519 keypair
        let privateKey = Curve25519.KeyAgreement.PrivateKey()
        let publicKey = privateKey.publicKey

        // Store keys
        self.privateKey = privateKey
        self.publicKey = publicKey

        // Save to Keychain
        try keychainService.saveE2EPrivateKey(privateKey.rawRepresentation)
        try keychainService.saveE2EPublicKey(publicKey.rawRepresentation)

        return (
            privateKey: privateKey.rawRepresentation,
            publicKey: publicKey.rawRepresentation
        )
    }

    func encrypt(_ data: Data) throws -> Data {
        guard let privateKey = privateKey else {
            throw AppError.keyNotFound
        }

        // Generate ephemeral key for this encryption
        let ephemeralKey = Curve25519.KeyAgreement.PrivateKey()

        // Derive symmetric key using key agreement
        let sharedSecret = try privateKey.sharedSecretFromKeyAgreement(with: ephemeralKey.publicKey)
        let symmetricKey = sharedSecret.hkdfDerivedSymmetricKey(
            using: SHA256.self,
            salt: Data(),
            sharedInfo: Data(),
            outputByteCount: 32
        )

        // Encrypt data using AES-GCM
        let sealedBox = try AES.GCM.seal(data, using: symmetricKey)

        guard let combined = sealedBox.combined else {
            throw AppError.encryptionFailed("Failed to create combined ciphertext")
        }

        // Prepend ephemeral public key to ciphertext
        var encryptedData = ephemeralKey.publicKey.rawRepresentation
        encryptedData.append(combined)

        return encryptedData
    }

    func encrypt(_ string: String) throws -> String {
        guard let data = string.data(using: .utf8) else {
            throw AppError.encodingFailed("Failed to encode string to data")
        }

        let encryptedData = try encrypt(data)
        return encryptedData.base64EncodedString()
    }

    func decrypt(_ data: Data) throws -> Data {
        guard let privateKey = privateKey else {
            throw AppError.keyNotFound
        }

        // Extract ephemeral public key (first 32 bytes)
        let publicKeySize = 32
        guard data.count > publicKeySize else {
            throw AppError.decryptionFailed("Invalid encrypted data length")
        }

        let ephemeralPublicKeyData = data.prefix(publicKeySize)
        let ciphertext = data.suffix(from: publicKeySize)

        // Reconstruct ephemeral public key
        let ephemeralPublicKey = try Curve25519.KeyAgreement.PublicKey(rawRepresentation: ephemeralPublicKeyData)

        // Derive symmetric key
        let sharedSecret = try privateKey.sharedSecretFromKeyAgreement(with: ephemeralPublicKey)
        let symmetricKey = sharedSecret.hkdfDerivedSymmetricKey(
            using: SHA256.self,
            salt: Data(),
            sharedInfo: Data(),
            outputByteCount: 32
        )

        // Decrypt using AES-GCM
        let sealedBox = try AES.GCM.SealedBox(combined: ciphertext)
        let decryptedData = try AES.GCM.open(sealedBox, using: symmetricKey)

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
        guard let privateKeyData = try keychainService.retrieveE2EPrivateKey(),
              let publicKeyData = try keychainService.retrieveE2EPublicKey() else {
            return  // Keys not found, that's okay
        }

        self.privateKey = try Curve25519.KeyAgreement.PrivateKey(rawRepresentation: privateKeyData)
        self.publicKey = try Curve25519.KeyAgreement.PublicKey(rawRepresentation: publicKeyData)
    }

    func clearKeys() throws {
        try keychainService.delete(Constants.Keychain.e2ePrivateKeyKey)
        try keychainService.delete(Constants.Keychain.e2ePublicKeyKey)
        self.privateKey = nil
        self.publicKey = nil
    }

    // MARK: - E2E Specific Methods

    /// Generate recovery codes for E2E encryption
    /// - Returns: Array of 10 recovery codes
    func generateRecoveryCodes() -> [String] {
        var codes = [String]()

        for _ in 0..<Constants.Security.recoveryCodeCount {
            let code = generateRecoveryCode()
            codes.append(code)
        }

        return codes
    }

    /// Hash recovery codes for server storage
    /// - Parameter codes: Recovery codes to hash
    /// - Returns: Array of hashed recovery codes
    func hashRecoveryCodes(_ codes: [String]) -> [String] {
        codes.map { $0.sha256Hash }
    }

    /// Verify recovery code against hash
    /// - Parameters:
    ///   - code: Recovery code to verify
    ///   - hash: Expected hash
    /// - Returns: True if code matches hash
    func verifyRecoveryCode(_ code: String, againstHash hash: String) -> Bool {
        code.sha256Hash == hash
    }

    // MARK: - Private Helper Methods

    private func generateRecoveryCode() -> String {
        let characters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"  // Excluding confusing chars
        let length = Constants.Security.recoveryCodeLength

        var code = ""
        for _ in 0..<length {
            let randomIndex = Int.random(in: 0..<characters.count)
            let index = characters.index(characters.startIndex, offsetBy: randomIndex)
            code.append(characters[index])

            // Add hyphen every 4 characters for readability
            if code.count % 5 == 4 && code.count < length {
                code.append("-")
            }
        }

        return code
    }

    /// Get public key as Data
    func getPublicKey() -> Data? {
        publicKey?.rawRepresentation
    }

    /// Get public key as base64 string
    func getPublicKeyString() -> String? {
        guard let publicKeyData = publicKey?.rawRepresentation else {
            return nil
        }
        return publicKeyData.base64EncodedString()
    }

    /// Check if keys are loaded
    var hasKeys: Bool {
        privateKey != nil && publicKey != nil
    }
}
