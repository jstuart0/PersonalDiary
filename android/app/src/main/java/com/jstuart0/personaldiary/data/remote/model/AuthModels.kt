package com.jstuart0.personaldiary.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * API Request/Response models for authentication
 */

data class SignupRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("encryption_tier")
    val encryptionTier: String, // "E2E" or "UCE"
    @SerializedName("public_key")
    val publicKey: String? = null, // Required for E2E
    @SerializedName("encrypted_master_key")
    val encryptedMasterKey: String? = null // Required for UCE
)

data class SignupResponse(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("encryption_tier")
    val encryptionTier: String,
    @SerializedName("recovery_codes")
    val recoveryCodes: List<String>? = null, // Only for E2E tier
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class LoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

data class LoginResponse(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("encryption_tier")
    val encryptionTier: String,
    @SerializedName("public_key")
    val publicKey: String? = null,
    @SerializedName("encrypted_master_key")
    val encryptedMasterKey: String? = null,
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class RefreshTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class PasswordResetRequest(
    @SerializedName("email")
    val email: String
)

data class PasswordResetConfirmRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("reset_code")
    val resetCode: String,
    @SerializedName("new_password")
    val newPassword: String
)

data class RecoveryCodeVerifyRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("recovery_code")
    val recoveryCode: String
)
