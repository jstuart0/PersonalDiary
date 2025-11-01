package com.jstuart0.personaldiary.domain.model

/**
 * User domain model
 */
data class User(
    val userId: String,
    val email: String,
    val encryptionTier: EncryptionTier,
    val publicKey: String? = null, // E2E only
    val encryptedMasterKey: String? = null, // UCE only
    val createdAt: Long,
    val updatedAt: Long
)
