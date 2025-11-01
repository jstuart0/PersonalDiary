package com.jstuart0.personaldiary.data.encryption

import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.jstuart0.personaldiary.domain.model.EncryptionTier
import de.mkammerer.argon2.Argon2Factory
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

/**
 * User-Controlled Encryption Service
 *
 * Uses password-derived keys with Argon2
 * Master key decrypted from server using password
 * Server can decrypt content with user password
 */
class UCEEncryptionService @Inject constructor() : EncryptionService {

    private var masterKey: SecretKey? = null
    private var aead: Aead? = null

    companion object {
        private const val ARGON2_ITERATIONS = 3
        private const val ARGON2_MEMORY = 65536 // 64 MB
        private const val ARGON2_PARALLELISM = 2
        private const val KEY_LENGTH = 32 // 256 bits
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    /**
     * Initialize with password and encrypted master key from server
     *
     * @param password User's password
     * @param encryptedMasterKey Encrypted master key from server (Base64)
     * @param salt Salt used for key derivation (Base64)
     */
    fun initialize(
        password: String,
        encryptedMasterKey: String,
        salt: String
    ): Boolean {
        return try {
            // Derive key from password using Argon2
            val derivedKey = deriveKeyFromPassword(password, salt)

            // Decrypt master key using derived key
            masterKey = decryptMasterKey(encryptedMasterKey, derivedKey)

            // Initialize Tink with master key
            val keysetHandle = KeysetHandle.generateNew(AeadKeyTemplates.AES256_GCM)
            aead = keysetHandle.getPrimitive(Aead::class.java)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Derive encryption key from password using Argon2
     */
    private fun deriveKeyFromPassword(password: String, saltBase64: String): ByteArray {
        val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)

        return try {
            val hash = argon2.hash(
                ARGON2_ITERATIONS,
                ARGON2_MEMORY,
                ARGON2_PARALLELISM,
                password.toCharArray(),
                salt
            )

            // Convert hash to raw bytes
            hash.toByteArray(Charsets.UTF_8).copyOf(KEY_LENGTH)
        } finally {
            argon2.wipeArray(password.toCharArray())
        }
    }

    /**
     * Decrypt master key using password-derived key
     */
    private fun decryptMasterKey(
        encryptedMasterKeyBase64: String,
        derivedKey: ByteArray
    ): SecretKey {
        val encryptedData = Base64.decode(encryptedMasterKeyBase64, Base64.NO_WRAP)

        // Extract IV and ciphertext
        val ivSize = 12 // GCM standard
        val iv = encryptedData.copyOfRange(0, ivSize)
        val ciphertext = encryptedData.copyOfRange(ivSize, encryptedData.size)

        // Decrypt master key
        val key = SecretKeySpec(derivedKey, "AES")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val masterKeyBytes = cipher.doFinal(ciphertext)
        return SecretKeySpec(masterKeyBytes, "AES")
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
        return EncryptionTier.UCE
    }

    /**
     * Generate salt for key derivation
     */
    fun generateSalt(): String {
        val salt = ByteArray(16)
        java.security.SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    /**
     * Clear encryption keys from memory
     */
    fun clear() {
        masterKey = null
        aead = null
    }
}
