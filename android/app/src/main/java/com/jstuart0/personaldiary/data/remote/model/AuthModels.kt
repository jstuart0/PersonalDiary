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

data class UserDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("encryptionTier")
    val encryptionTier: String,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String,
    @SerializedName("publicKey")
    val publicKey: String? = null,
    @SerializedName("encryptedMasterKey")
    val encryptedMasterKey: String? = null,
    @SerializedName("keyDerivationSalt")
    val keyDerivationSalt: String? = null
)

data class TokensDto(
    @SerializedName("accessToken")
    val accessToken: String,
    @SerializedName("refreshToken")
    val refreshToken: String,
    @SerializedName("expiresIn")
    val expiresIn: Int
)

data class SignupResponse(
    @SerializedName("user")
    val user: UserDto,
    @SerializedName("tokens")
    val tokens: TokensDto,
    @SerializedName("recoveryCodes")
    val recoveryCodes: List<String>? = null // Only for E2E tier
)

data class LoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

data class LoginResponse(
    @SerializedName("user")
    val user: UserDto,
    @SerializedName("tokens")
    val tokens: TokensDto
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class RefreshTokenResponse(
    @SerializedName("tokens")
    val tokens: TokensDto
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
