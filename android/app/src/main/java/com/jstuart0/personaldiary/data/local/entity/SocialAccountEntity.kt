package com.jstuart0.personaldiary.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jstuart0.personaldiary.domain.model.SocialAccount
import com.jstuart0.personaldiary.domain.model.SocialPlatform

/**
 * Room entity for connected social media accounts
 */
@Entity(
    tableName = "social_accounts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("platform")]
)
data class SocialAccountEntity(
    @PrimaryKey
    val accountId: String,
    val userId: String,
    val platform: String, // FACEBOOK, INSTAGRAM, TWITTER
    val platformUserId: String,
    val platformUsername: String,
    val isActive: Boolean,
    val lastSyncAt: Long?,
    val createdAt: Long
) {
    fun toDomain(): SocialAccount {
        return SocialAccount(
            accountId = accountId,
            platform = SocialPlatform.valueOf(platform),
            platformUserId = platformUserId,
            platformUsername = platformUsername,
            isActive = isActive,
            lastSyncAt = lastSyncAt,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(account: SocialAccount, userId: String): SocialAccountEntity {
            return SocialAccountEntity(
                accountId = account.accountId,
                userId = userId,
                platform = account.platform.name,
                platformUserId = account.platformUserId,
                platformUsername = account.platformUsername,
                isActive = account.isActive,
                lastSyncAt = account.lastSyncAt,
                createdAt = account.createdAt
            )
        }
    }
}
