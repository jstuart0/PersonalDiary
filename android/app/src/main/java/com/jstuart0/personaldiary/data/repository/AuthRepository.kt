package com.jstuart0.personaldiary.data.repository

import android.util.Base64
import com.jstuart0.personaldiary.data.encryption.E2EEncryptionService
import com.jstuart0.personaldiary.data.encryption.UCEEncryptionService
import com.jstuart0.personaldiary.data.local.dao.UserDao
import com.jstuart0.personaldiary.data.local.entity.UserEntity
import com.jstuart0.personaldiary.data.remote.TokenManager
import com.jstuart0.personaldiary.data.remote.api.PersonalDiaryApi
import com.jstuart0.personaldiary.data.remote.model.*
import com.jstuart0.personaldiary.domain.model.EncryptionTier
import com.jstuart0.personaldiary.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for authentication and user management
 * Handles both local and remote data sources
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: PersonalDiaryApi,
    private val userDao: UserDao,
    private val tokenManager: TokenManager,
    private val e2eEncryptionService: E2EEncryptionService,
    private val uceEncryptionService: UCEEncryptionService
) {

    /**
     * Get current user from local database
     */
    fun getCurrentUserFlow(): Flow<User?> {
        return userDao.getCurrentUserFlow().map { it?.toDomain() }
    }

    suspend fun getCurrentUser(): User? {
        return userDao.getCurrentUser()?.toDomain()
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return tokenManager.hasValidTokens()
    }

    /**
     * Sign up new user
     */
    suspend fun signup(
        email: String,
        password: String,
        encryptionTier: EncryptionTier
    ): Result<User> {
        return try {
            // Prepare signup request based on encryption tier
            val request = when (encryptionTier) {
                EncryptionTier.E2E -> {
                    // Initialize E2E encryption and generate keypair
                    val publicKey = e2eEncryptionService.exportPublicKey()
                    SignupRequest(
                        email = email,
                        password = password,
                        encryptionTier = "E2E",
                        publicKey = publicKey
                    )
                }
                EncryptionTier.UCE -> {
                    // Derive encryption key from password
                    val encryptedMasterKey = uceEncryptionService.deriveAndEncryptMasterKey(password)
                    SignupRequest(
                        email = email,
                        password = password,
                        encryptionTier = "UCE",
                        encryptedMasterKey = encryptedMasterKey
                    )
                }
            }

            // Call API
            val response = api.signup(request)
            if (response.isSuccessful && response.body() != null) {
                val signupResponse = response.body()!!

                // Save tokens
                tokenManager.saveTokens(
                    signupResponse.accessToken,
                    signupResponse.refreshToken
                )

                // Save user locally
                val user = User(
                    userId = signupResponse.userId,
                    email = signupResponse.email,
                    encryptionTier = encryptionTier,
                    publicKey = request.publicKey,
                    encryptedMasterKey = request.encryptedMasterKey,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                userDao.insert(UserEntity.fromDomain(user))

                // Initialize encryption service
                when (encryptionTier) {
                    EncryptionTier.E2E -> e2eEncryptionService.initialize(user.userId)
                    EncryptionTier.UCE -> {
                        // For UCE, we need the encrypted master key from the request
                        val encryptedMasterKey = request.encryptedMasterKey ?: ""
                        // Extract salt from the encrypted master key (it's the first part)
                        val decodedData = String(Base64.decode(encryptedMasterKey, Base64.NO_WRAP), Charsets.UTF_8)
                        val parts = decodedData.split(":")
                        val salt = if (parts.isNotEmpty()) parts[0] else ""
                        uceEncryptionService.initialize(password, encryptedMasterKey, salt)
                    }
                }

                Result.success(user)
            } else {
                Result.failure(Exception("Signup failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Login user
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val request = LoginRequest(email, password)
            val response = api.login(request)

            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!

                // Save tokens
                tokenManager.saveTokens(
                    loginResponse.accessToken,
                    loginResponse.refreshToken
                )

                // Save user locally
                val encryptionTier = EncryptionTier.valueOf(loginResponse.encryptionTier)
                val user = User(
                    userId = loginResponse.userId,
                    email = loginResponse.email,
                    encryptionTier = encryptionTier,
                    publicKey = loginResponse.publicKey,
                    encryptedMasterKey = loginResponse.encryptedMasterKey,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                userDao.insert(UserEntity.fromDomain(user))

                // Initialize encryption service
                when (encryptionTier) {
                    EncryptionTier.E2E -> e2eEncryptionService.initialize(user.userId)
                    EncryptionTier.UCE -> {
                        // For UCE, we need the encrypted master key from the response
                        val encryptedMasterKey = loginResponse.encryptedMasterKey ?: ""
                        // Extract salt from the encrypted master key (it's the first part)
                        val decodedData = String(Base64.decode(encryptedMasterKey, Base64.NO_WRAP), Charsets.UTF_8)
                        val parts = decodedData.split(":")
                        val salt = if (parts.isNotEmpty()) parts[0] else ""
                        uceEncryptionService.initialize(password, encryptedMasterKey, salt)
                    }
                }

                Result.success(user)
            } else {
                Result.failure(Exception("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout user
     */
    suspend fun logout() {
        try {
            api.logout()
        } catch (e: Exception) {
            // Continue with local logout even if API call fails
        } finally {
            // Clear tokens
            tokenManager.clearTokens()

            // Clear encryption keys
            e2eEncryptionService.clear()
            uceEncryptionService.clear()

            // Clear local database
            userDao.deleteAll()
        }
    }

    /**
     * Request password reset
     */
    suspend fun requestPasswordReset(email: String): Result<Unit> {
        return try {
            val response = api.requestPasswordReset(PasswordResetRequest(email))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Password reset failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verify recovery code (E2E tier only)
     */
    suspend fun verifyRecoveryCode(email: String, recoveryCode: String): Result<User> {
        return try {
            val request = RecoveryCodeVerifyRequest(email, recoveryCode)
            val response = api.verifyRecoveryCode(request)

            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!

                // Save tokens
                tokenManager.saveTokens(
                    loginResponse.accessToken,
                    loginResponse.refreshToken
                )

                // Save user locally
                val user = User(
                    userId = loginResponse.userId,
                    email = loginResponse.email,
                    encryptionTier = EncryptionTier.valueOf(loginResponse.encryptionTier),
                    publicKey = loginResponse.publicKey,
                    encryptedMasterKey = loginResponse.encryptedMasterKey,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                userDao.insert(UserEntity.fromDomain(user))

                Result.success(user)
            } else {
                Result.failure(Exception("Recovery code verification failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
