//
//  EncryptionServiceProtocol.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import Foundation

/// Protocol for encryption services
protocol EncryptionServiceProtocol {
    /// Generate encryption keys
    /// - Returns: Tuple of private key and public key (public key may be empty for UCE)
    func generateKeys() throws -> (privateKey: Data, publicKey: Data)

    /// Encrypt data
    /// - Parameter data: Data to encrypt
    /// - Returns: Encrypted data
    func encrypt(_ data: Data) throws -> Data

    /// Encrypt string
    /// - Parameter string: String to encrypt
    /// - Returns: Base64-encoded encrypted string
    func encrypt(_ string: String) throws -> String

    /// Decrypt data
    /// - Parameter data: Encrypted data
    /// - Returns: Decrypted data
    func decrypt(_ data: Data) throws -> Data

    /// Decrypt string
    /// - Parameter string: Base64-encoded encrypted string
    /// - Returns: Decrypted string
    func decrypt(_ string: String) throws -> String

    /// Generate SHA-256 hash for data
    /// - Parameter data: Data to hash
    /// - Returns: Hex string of hash
    func generateHash(for data: Data) -> String

    /// Generate SHA-256 hash for string
    /// - Parameter string: String to hash
    /// - Returns: Hex string of hash
    func generateHash(for string: String) -> String

    /// Load keys from Keychain
    func loadKeys() throws

    /// Clear keys from memory and Keychain
    func clearKeys() throws
}
