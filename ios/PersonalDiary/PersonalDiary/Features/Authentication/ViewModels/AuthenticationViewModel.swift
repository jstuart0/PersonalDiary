//
//  AuthenticationViewModel.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import Foundation
import Combine

@MainActor
final class AuthenticationViewModel: ObservableObject {
    // MARK: - Published Properties

    @Published var isAuthenticated = false
    @Published var isLoading = false
    @Published var currentUser: User?
    @Published var errorMessage: String?
    @Published var recoveryCodes: [String] = []

    // MARK: - Properties

    private let apiClient = APIClient.shared
    private let databaseService = DatabaseService.shared
    private let keychainService = KeychainService()
    private let syncService = SyncService.shared

    // MARK: - Initialization

    init() {}

    // MARK: - Authentication Status

    func checkAuthenticationStatus() async {
        isLoading = true

        do {
            // Check if we have valid tokens
            guard let accessToken = try keychainService.retrieveAccessToken() else {
                isAuthenticated = false
                isLoading = false
                return
            }

            // Validate token by fetching user
            if let user = try databaseService.fetchCurrentUser() {
                currentUser = user
                isAuthenticated = true

                // Trigger background sync
                Task {
                    await syncService.syncIncremental()
                }
            } else {
                isAuthenticated = false
            }
        } catch {
            isAuthenticated = false
            errorMessage = error.localizedDescription
        }

        isLoading = false
    }

    // MARK: - Registration

    func register(
        email: String,
        password: String,
        name: String?,
        encryptionTier: EncryptionTier
    ) async throws {
        isLoading = true
        errorMessage = nil

        do {
            // Validate input
            try validateEmail(email)
            try validatePassword(password)

            // Prepare encryption keys based on tier
            var publicKey: String?
            var encryptedMasterKey: String?

            switch encryptionTier {
            case .e2e:
                // Generate E2E keys
                let e2eService = E2EEncryptionService()
                let keyPair = try e2eService.generateKeyPair()

                // Save private key to keychain
                try keychainService.saveE2EPrivateKey(keyPair.privateKey)

                // Convert public key to base64 for server
                publicKey = keyPair.publicKey.withUnsafeBytes { Data($0).base64EncodedString() }

            case .uce:
                // Generate UCE master key
                let uceService = UCEEncryptionService()
                let masterKey = try uceService.generateMasterKey()

                // Derive encryption key from password
                let derivedKey = try uceService.deriveKey(from: password)

                // Encrypt master key with derived key
                let encryptedKey = try uceService.encryptMasterKey(masterKey, with: derivedKey)

                // Save master key to keychain
                try keychainService.saveUCEMasterKey(masterKey)

                // Send encrypted master key to server
                encryptedMasterKey = encryptedKey.base64EncodedString()
            }

            // Register with API
            let request = RegisterRequest(
                email: email,
                password: password,
                name: name,
                encryptionTier: encryptionTier.rawValue,
                publicKey: publicKey,
                encryptedMasterKey: encryptedMasterKey
            )

            let response: RegisterResponse = try await apiClient.post(.register, body: request)

            // Save tokens
            try keychainService.saveAccessToken(response.accessToken)
            try keychainService.saveRefreshToken(response.refreshToken)

            // Save user info
            let user = User(
                id: response.user.id,
                email: response.user.email,
                name: response.user.name,
                encryptionTier: encryptionTier,
                createdAt: response.user.createdAt,
                lastLoginAt: Date()
            )

            try databaseService.saveUser(user)
            try keychainService.saveUserID(user.id.uuidString)
            try keychainService.saveUserTier(encryptionTier.rawValue)

            // Store recovery codes if E2E
            if encryptionTier == .e2e, let codes = response.recoveryCodes {
                recoveryCodes = codes

                // Hash and store recovery codes
                let e2eService = E2EEncryptionService()
                for code in codes {
                    try e2eService.saveRecoveryCodeHash(code)
                }
            }

            currentUser = user
            isAuthenticated = true

            // Trigger initial sync
            Task {
                await syncService.syncFull()
            }

            isLoading = false
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            throw error
        }
    }

    // MARK: - Login

    func login(email: String, password: String) async throws {
        isLoading = true
        errorMessage = nil

        do {
            // Validate input
            try validateEmail(email)

            // Login with API
            let request = LoginRequest(email: email, password: password)
            let response: LoginResponse = try await apiClient.post(.login, body: request)

            // Save tokens
            try keychainService.saveAccessToken(response.accessToken)
            try keychainService.saveRefreshToken(response.refreshToken)

            // Determine encryption tier
            let tier = EncryptionTier(rawValue: response.user.encryptionTier) ?? .e2e

            // For UCE tier, derive key from password and decrypt master key
            if tier == .uce, let encryptedMasterKey = response.user.encryptedMasterKey {
                let uceService = UCEEncryptionService()

                // Derive key from password
                let derivedKey = try uceService.deriveKey(from: password)

                // Decrypt master key
                guard let encryptedData = Data(base64Encoded: encryptedMasterKey) else {
                    throw AppError.encryption(.decryptionFailed)
                }

                let masterKey = try uceService.decryptMasterKey(encryptedData, with: derivedKey)

                // Save master key to keychain
                try keychainService.saveUCEMasterKey(masterKey)
            }

            // Save user info
            let user = User(
                id: response.user.id,
                email: response.user.email,
                name: response.user.name,
                encryptionTier: tier,
                createdAt: response.user.createdAt,
                lastLoginAt: Date()
            )

            try databaseService.saveUser(user)
            try keychainService.saveUserID(user.id.uuidString)
            try keychainService.saveUserTier(tier.rawValue)

            currentUser = user
            isAuthenticated = true

            // Trigger full sync
            Task {
                await syncService.syncFull()
            }

            isLoading = false
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            throw error
        }
    }

    // MARK: - Logout

    func logout() async {
        isLoading = true

        // Call logout API
        try? await apiClient.perform(.logout, method: "POST")

        // Clear local data
        try? keychainService.clearAuthenticationData()
        try? databaseService.clearAllData()

        isAuthenticated = false
        currentUser = nil
        isLoading = false
    }

    // MARK: - Account Recovery (E2E)

    func recoverAccount(email: String, recoveryCode: String) async throws {
        isLoading = true
        errorMessage = nil

        do {
            // Validate recovery code format
            guard recoveryCode.count == 24 else {
                throw AppError.validation(.invalidInput(field: "Recovery Code"))
            }

            // TODO: Implement account recovery flow with backend
            // This would involve:
            // 1. Verify recovery code with server
            // 2. Generate new key pair
            // 3. Re-encrypt data with new keys
            // 4. Update server with new public key

            throw AppError.validation(.notImplemented)
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            throw error
        }
    }

    // MARK: - Password Reset (UCE)

    func requestPasswordReset(email: String) async throws {
        isLoading = true
        errorMessage = nil

        do {
            try validateEmail(email)

            // Request password reset from API
            // TODO: Implement with backend endpoint

            isLoading = false
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            throw error
        }
    }

    // MARK: - Biometric Authentication

    func enableBiometricAuthentication() async throws {
        guard var user = currentUser else {
            throw AppError.authentication(.notAuthenticated)
        }

        // Enable biometric access for keychain items
        try keychainService.enableBiometricAccess()

        // Update user preference
        user.isBiometricEnabled = true
        try databaseService.saveUser(user)
        currentUser = user
    }

    func disableBiometricAuthentication() throws {
        guard var user = currentUser else {
            throw AppError.authentication(.notAuthenticated)
        }

        // Update user preference
        user.isBiometricEnabled = false
        try databaseService.saveUser(user)
        currentUser = user
    }

    // MARK: - Validation

    private func validateEmail(_ email: String) throws {
        guard email.isValidEmail else {
            throw AppError.validation(.invalidEmail)
        }
    }

    private func validatePassword(_ password: String) throws {
        let strength = password.passwordStrength()

        guard strength >= 3 else {
            throw AppError.validation(.weakPassword)
        }
    }

    // MARK: - Recovery Codes

    func clearRecoveryCodes() {
        recoveryCodes = []
    }

    func saveRecoveryCodesAcknowledged() async throws {
        // Mark recovery codes as saved
        // TODO: Implement with backend to ensure user acknowledged
        recoveryCodes = []
    }
}

// MARK: - Supporting Extensions

extension User {
    var isBiometricEnabled: Bool {
        get {
            // Stored in UserEntity
            false // Placeholder
        }
        set {
            // Update UserEntity
        }
    }
}

extension KeychainService {
    func enableBiometricAccess() throws {
        // Re-save keys with biometric access flags
        // Implementation would update access control flags
    }

    func saveUserTier(_ tier: String) throws {
        let data = tier.data(using: .utf8)!
        try save(data, for: Constants.Keychain.userTierKey)
    }
}
