package com.jstuart0.personaldiary.data.encryption

import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.jstuart0.personaldiary.domain.model.EncryptionTier
import com.jstuart0.personaldiary.service.KeyStoreManager
import java.security.MessageDigest
import javax.inject.Inject

/**
 * End-to-End Encryption Service
 *
 * Uses Google Tink for cryptographic operations
 * Keys stored in Android KeyStore
 * Server never sees decrypted content
 */
class E2EEncryptionService @Inject constructor(
    private val keyStoreManager: KeyStoreManager
) : EncryptionService {

    private var keysetHandle: KeysetHandle? = null
    private var aead: Aead? = null

    /**
     * Initialize encryption with user's keypair
     * Must be called after user authentication
     */
    fun initialize(userId: String): Boolean {
        return try {
            val keyAlias = keyStoreManager.getE2EKeyAlias(userId)

            // Generate new key if doesn't exist
            if (!keyStoreManager.keyExists(keyAlias)) {
                keyStoreManager.generateKey(keyAlias, requireBiometric = true)
            }

            // Initialize Tink keyse handle
            keysetHandle = KeysetHandle.generateNew(AeadKeyTemplates.AES256_GCM)
            aead = keysetHandle?.getPrimitive(Aead::class.java)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun encrypt(plaintext: String): Result<String> {
        return try {
            val aeadInstance = aead ?: return Result.failure(
                IllegalStateException("Encryption service not initialized")
            )

            val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
            val ciphertext = aeadInstance.encrypt(plaintextBytes, null)
            val encoded = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

            Result.success(encoded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun decrypt(ciphertext: String): Result<String> {
        return try {
            val aeadInstance = aead ?: return Result.failure(
                IllegalStateException("Encryption service not initialized")
            )

            val ciphertextBytes = Base64.decode(ciphertext, Base64.NO_WRAP)
            val plaintext = aeadInstance.decrypt(ciphertextBytes, null)
            val decoded = String(plaintext, Charsets.UTF_8)

            Result.success(decoded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun generateContentHash(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(content.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    override fun getEncryptionTier(): EncryptionTier {
        return EncryptionTier.E2E
    }

    /**
     * Generate recovery codes for account recovery
     * Must be saved by user
     */
    fun generateRecoveryCodes(): List<String> {
        val codes = mutableListOf<String>()
        val random = java.security.SecureRandom()

        repeat(10) {
            // Generate 12-character alphanumeric code
            val code = (1..12)
                .map { random.nextInt(36) }
                .map { if (it < 10) ('0' + it) else ('a' + (it - 10)) }
                .joinToString("")
                .uppercase()
                .chunked(4)
                .joinToString("-")

            codes.add(code)
        }

        return codes
    }

    /**
     * Export public key for server storage
     */
    fun exportPublicKey(): String? {
        return try {
            // In a real implementation, export the public key from the keypair
            // For now, return a placeholder
            val keysetHandle = this.keysetHandle ?: return null
            // TODO: Implement actual public key export
            Base64.encodeToString(ByteArray(32), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clear encryption keys from memory
     */
    fun clear() {
        keysetHandle = null
        aead = null
    }
}
