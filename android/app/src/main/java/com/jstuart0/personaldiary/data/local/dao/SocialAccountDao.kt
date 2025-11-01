package com.jstuart0.personaldiary.data.local.dao

import androidx.room.*
import com.jstuart0.personaldiary.data.local.entity.SocialAccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for SocialAccount operations
 */
@Dao
interface SocialAccountDao {

    @Query("SELECT * FROM social_accounts WHERE userId = :userId")
    fun getAccountsForUser(userId: String): Flow<List<SocialAccountEntity>>

    @Query("SELECT * FROM social_accounts WHERE userId = :userId AND isActive = 1")
    fun getActiveAccountsForUser(userId: String): Flow<List<SocialAccountEntity>>

    @Query("SELECT * FROM social_accounts WHERE accountId = :accountId")
    suspend fun getAccount(accountId: String): SocialAccountEntity?

    @Query("SELECT * FROM social_accounts WHERE accountId = :accountId")
    suspend fun getById(accountId: String): SocialAccountEntity?

    @Query("SELECT * FROM social_accounts WHERE userId = :userId AND platform = :platform")
    suspend fun getAccountByPlatform(userId: String, platform: String): SocialAccountEntity?

    @Query("SELECT * FROM social_accounts WHERE platformUserId = :platformUserId")
    suspend fun getAccountByPlatformUserId(platformUserId: String): SocialAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: SocialAccountEntity)

    @Update
    suspend fun update(account: SocialAccountEntity)

    @Delete
    suspend fun delete(account: SocialAccountEntity)

    @Query("UPDATE social_accounts SET isActive = :isActive WHERE accountId = :accountId")
    suspend fun updateActiveStatus(accountId: String, isActive: Boolean)

    @Query("UPDATE social_accounts SET lastSyncAt = :timestamp WHERE accountId = :accountId")
    suspend fun updateLastSync(accountId: String, timestamp: Long)

    @Query("DELETE FROM social_accounts WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
