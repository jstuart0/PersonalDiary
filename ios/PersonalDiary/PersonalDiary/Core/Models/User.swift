//
//  User.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import Foundation

/// Encryption tier enum
enum EncryptionTier: String, Codable {
    case e2e = "E2E"  // End-to-End Encryption
    case uce = "UCE"  // User-Controlled Encryption

    var displayName: String {
        switch self {
        case .e2e:
            return "Maximum Privacy"
        case .uce:
            return "Smart Features"
        }
    }

    var icon: String {
        switch self {
        case .e2e:
            return "lock.fill"
        case .uce:
            return "shield.fill"
        }
    }

    var description: String {
        switch self {
        case .e2e:
            return "Keys never leave your device. True end-to-end encryption."
        case .uce:
            return "Full-text search, AI features, and easy recovery."
        }
    }
}

/// User model
struct User: Codable, Identifiable {
    let id: String
    let email: String
    let encryptionTier: EncryptionTier
    let createdAt: Date
    let updatedAt: Date

    // E2E specific fields
    let publicKey: String?

    // UCE specific fields
    let encryptedMasterKey: String?
    let keyDerivationSalt: String?

    enum CodingKeys: String, CodingKey {
        case id
        case email
        case encryptionTier = "encryption_tier"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
        case publicKey = "public_key"
        case encryptedMasterKey = "encrypted_master_key"
        case keyDerivationSalt = "key_derivation_salt"
    }
}

/// User credentials for authentication
struct UserCredentials {
    let email: String
    let password: String
}

/// JWT tokens
struct AuthTokens: Codable {
    let accessToken: String
    let refreshToken: String
    let expiresIn: Int

    enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case refreshToken = "refresh_token"
        case expiresIn = "expires_in"
    }
}

/// Authentication response
struct AuthenticationResponse: Codable {
    let user: User
    let tokens: AuthTokens
}

/// Storage tier information
struct StorageTier: Codable {
    let name: String
    let storageLimit: Int64  // bytes
    let storageUsed: Int64   // bytes
    let price: Double        // monthly price in USD

    var storageUsedMB: Double {
        Double(storageUsed) / 1_048_576
    }

    var storageLimitMB: Double {
        Double(storageLimit) / 1_048_576
    }

    var storageUsedGB: Double {
        Double(storageUsed) / 1_073_741_824
    }

    var storageLimitGB: Double {
        Double(storageLimit) / 1_073_741_824
    }

    var percentageUsed: Double {
        guard storageLimit > 0 else { return 0 }
        return (Double(storageUsed) / Double(storageLimit)) * 100
    }

    enum CodingKeys: String, CodingKey {
        case name
        case storageLimit = "storage_limit"
        case storageUsed = "storage_used"
        case price
    }
}
