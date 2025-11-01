package com.jstuart0.personaldiary.data.encryption

import com.jstuart0.personaldiary.domain.model.EncryptionTier

/**
 * Abstraction for encryption services
 * Strategy pattern for E2E vs UCE encryption
 */
interface EncryptionService {
    /**
     * Encrypt plaintext content
     */
    suspend fun encrypt(plaintext: String): Result<String>

    /**
     * Decrypt ciphertext content
     */
    suspend fun decrypt(ciphertext: String): Result<String>

    /**
     * Generate SHA-256 hash of content
     */
    fun generateContentHash(content: String): String

    /**
     * Get encryption tier
     */
    fun getEncryptionTier(): EncryptionTier
}
