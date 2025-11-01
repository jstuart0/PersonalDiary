package com.jstuart0.personaldiary.data.local.dao

import androidx.room.*
import com.jstuart0.personaldiary.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for User operations
 */
@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUser(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUserFlow(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Delete
    suspend fun delete(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}
