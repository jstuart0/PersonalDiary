package com.jstuart0.personaldiary.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jstuart0.personaldiary.domain.model.EncryptionTier
import com.jstuart0.personaldiary.domain.model.User

/**
 * Room entity for User
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val email: String,
    val encryptionTier: String, // E2E or UCE
    val publicKey: String? = null, // E2E only
    val encryptedMasterKey: String? = null, // UCE only
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): User {
        return User(
            userId = userId,
            email = email,
            encryptionTier = EncryptionTier.valueOf(encryptionTier),
            publicKey = publicKey,
            encryptedMasterKey = encryptedMasterKey,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(user: User): UserEntity {
            return UserEntity(
                userId = user.userId,
                email = user.email,
                encryptionTier = user.encryptionTier.name,
                publicKey = user.publicKey,
                encryptedMasterKey = user.encryptedMasterKey,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt
            )
        }
    }
}
