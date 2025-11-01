package com.jstuart0.personaldiary.service

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for biometric authentication
 */
@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Check if biometric authentication is available
     */
    fun isBiometricAvailable(): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)

        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                BiometricAvailability.Available

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                BiometricAvailability.NoHardware

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                BiometricAvailability.HardwareUnavailable

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                BiometricAvailability.NoneEnrolled

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                BiometricAvailability.SecurityUpdateRequired

            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
                BiometricAvailability.Unsupported

            BiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
                BiometricAvailability.Unknown

            else -> BiometricAvailability.Unknown
        }
    }

    /**
     * Show biometric prompt
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Authenticate",
        subtitle: String = "Verify your identity to continue",
        description: String? = null,
        negativeButtonText: String = "Cancel",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_CANCELED -> {
                            // User cancelled, don't show error
                        }
                        else -> onError(errString.toString())
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .apply {
                if (description != null) {
                    setDescription(description)
                }
            }
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Get biometric types available
     */
    fun getBiometricTypes(): List<BiometricType> {
        val types = mutableListOf<BiometricType>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val biometricManager = BiometricManager.from(context)

            // Check for fingerprint
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
                types.add(BiometricType.FINGERPRINT)
            }

            // Check for face
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                    types.add(BiometricType.FACE)
                }
            }

            // Check for iris (rare, but possible)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                types.add(BiometricType.IRIS)
            }
        }

        return types
    }
}

/**
 * Biometric availability status
 */
sealed class BiometricAvailability {
    object Available : BiometricAvailability()
    object NoHardware : BiometricAvailability()
    object HardwareUnavailable : BiometricAvailability()
    object NoneEnrolled : BiometricAvailability()
    object SecurityUpdateRequired : BiometricAvailability()
    object Unsupported : BiometricAvailability()
    object Unknown : BiometricAvailability()

    fun isAvailable() = this is Available

    fun getMessage(): String = when (this) {
        is Available -> "Biometric authentication is available"
        is NoHardware -> "No biometric hardware detected"
        is HardwareUnavailable -> "Biometric hardware is currently unavailable"
        is NoneEnrolled -> "No biometrics enrolled. Please add fingerprint or face in device settings"
        is SecurityUpdateRequired -> "Security update required"
        is Unsupported -> "Biometric authentication is not supported"
        is Unknown -> "Unknown biometric status"
    }
}

/**
 * Types of biometric authentication
 */
enum class BiometricType {
    FINGERPRINT,
    FACE,
    IRIS
}
