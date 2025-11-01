package com.jstuart0.personaldiary.data.repository

import com.jstuart0.personaldiary.data.local.dao.UserDao
import com.jstuart0.personaldiary.data.local.entity.UserEntity
import com.jstuart0.personaldiary.domain.model.EncryptionTier
import com.jstuart0.personaldiary.domain.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock AuthRepository for offline/testing mode
 * Simulates successful authentication without network calls
 */
@Singleton
class MockAuthRepository @Inject constructor(
    private val userDao: UserDao
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
     * Check if user is logged in (mock implementation)
     */
    fun isLoggedIn(): Boolean {
        // For mock mode, check if we have a user in local database
        return true // Simplified for demo
    }

    /**
     * Mock signup - creates user locally without network call
     */
    suspend fun signup(
        email: String,
        password: String,
        encryptionTier: EncryptionTier
    ): Result<User> {
        return try {
            // Simulate some delay
            delay(1000)

            // Create user locally
            val userId = UUID.randomUUID().toString()
            val currentTime = System.currentTimeMillis()
            val user = User(
                userId = userId,
                email = email,
                encryptionTier = encryptionTier,
                publicKey = if (encryptionTier == EncryptionTier.E2E) "mock-public-key" else null,
                encryptedMasterKey = if (encryptionTier == EncryptionTier.UCE) "mock-encrypted-master-key" else null,
                createdAt = currentTime,
                updatedAt = currentTime
            )

            // Save to local database
            val userEntity = UserEntity.fromDomain(user)
            userDao.insert(userEntity)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mock login - finds user in local database
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            // Simulate some delay
            delay(500)

            // Try to find user by email
            val userEntity = userDao.getUserByEmail(email)
            if (userEntity != null) {
                Result.success(userEntity.toDomain())
            } else {
                // Create a mock user if not found
                signup(email, password, EncryptionTier.UCE) // Default to UCE for mock
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mock logout
     */
    suspend fun logout() {
        // In real implementation, this would clear tokens
        // For mock, we could clear local user data if needed
    }

    /**
     * Mock password reset
     */
    suspend fun requestPasswordReset(email: String): Result<Unit> {
        delay(500)
        return Result.success(Unit)
    }
}