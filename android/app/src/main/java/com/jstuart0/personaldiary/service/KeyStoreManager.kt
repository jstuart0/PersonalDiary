package com.jstuart0.personaldiary.service

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages secure key storage using Android KeyStore
 *
 * Key features:
 * - Hardware-backed encryption keys
 * - Biometric authentication protection
 * - Strongbox support when available
 * - Secure key generation and retrieval
 */
@Singleton
class KeyStoreManager @Inject constructor() {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val E2E_PRIVATE_KEY_ALIAS = "e2e_private_key"
        private const val UCE_SESSION_KEY_ALIAS = "uce_session_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    /**
     * Generate and store a new AES key in KeyStore
     *
     * @param alias Key alias for retrieval
     * @param requireBiometric Whether biometric authentication is required
     * @return True if key was generated successfully
     */
    fun generateKey(
        alias: String,
        requireBiometric: Boolean = true
    ): Boolean {
        return try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val builder = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(requireBiometric)

            // Use hardware-backed security (Strongbox) if available
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                builder.setIsStrongBoxBacked(true)
            }

            // Don't invalidate key if biometric enrollment changes
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(false)
            }

            // Require user authentication within 30 seconds
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(
                    30,
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
                )
            }

            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Check if a key exists in KeyStore
     */
    fun keyExists(alias: String): Boolean {
        return try {
            keyStore.containsAlias(alias)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get key from KeyStore
     *
     * @param alias Key alias
     * @return SecretKey or null if not found
     */
    fun getKey(alias: String): SecretKey? {
        return try {
            val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Delete key from KeyStore
     */
    fun deleteKey(alias: String): Boolean {
        return try {
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Encrypt data using KeyStore key
     *
     * @param alias Key alias
     * @param plaintext Data to encrypt
     * @return Encrypted data (IV + ciphertext) or null on error
     */
    fun encrypt(alias: String, plaintext: ByteArray): ByteArray? {
        return try {
            val key = getKey(alias) ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext)

            // Combine IV and ciphertext (IV is needed for decryption)
            iv + ciphertext
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decrypt data using KeyStore key
     *
     * @param alias Key alias
     * @param encryptedData Data to decrypt (IV + ciphertext)
     * @return Decrypted data or null on error
     */
    fun decrypt(alias: String, encryptedData: ByteArray): ByteArray? {
        return try {
            val key = getKey(alias) ?: return null

            // Extract IV and ciphertext
            val ivSize = 12 // GCM standard IV size
            val iv = encryptedData.copyOfRange(0, ivSize)
            val ciphertext = encryptedData.copyOfRange(ivSize, encryptedData.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate E2E private key alias (user-specific)
     */
    fun getE2EKeyAlias(userId: String): String {
        return "${E2E_PRIVATE_KEY_ALIAS}_$userId"
    }

    /**
     * Generate UCE session key alias (user-specific)
     */
    fun getUCEKeyAlias(userId: String): String {
        return "${UCE_SESSION_KEY_ALIAS}_$userId"
    }

    /**
     * Check if device supports biometric authentication
     */
    fun isBiometricSupported(activity: FragmentActivity): Boolean {
        val biometricManager = BiometricManager.from(activity)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    /**
     * Create biometric prompt for authentication
     */
    fun createBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (Int, String) -> Unit
    ): BiometricPrompt {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError(-1, "Authentication failed")
            }
        }

        return BiometricPrompt(activity, executor, callback)
    }

    /**
     * Clear all keys for a user
     */
    fun clearUserKeys(userId: String) {
        deleteKey(getE2EKeyAlias(userId))
        deleteKey(getUCEKeyAlias(userId))
    }
}
