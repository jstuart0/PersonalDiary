package com.jstuart0.personaldiary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jstuart0.personaldiary.data.local.dao.*
import com.jstuart0.personaldiary.data.local.entity.*

/**
 * Room Database for Personal Diary
 *
 * Entities:
 * - UserEntity: User profile and encryption settings
 * - EntryEntity: Encrypted diary entries
 * - EntryFtsEntity: Full-text search index (E2E tier only)
 * - MediaEntity: Encrypted media files
 * - EntryTagEntity: Many-to-many relationship between entries and tags
 * - SocialAccountEntity: Connected social media accounts
 */
@Database(
    entities = [
        UserEntity::class,
        EntryEntity::class,
        EntryFtsEntity::class,
        MediaEntity::class,
        EntryTagEntity::class,
        SocialAccountEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun entryDao(): EntryDao
    abstract fun entryFtsDao(): EntryFtsDao
    abstract fun mediaDao(): MediaDao
    abstract fun socialAccountDao(): SocialAccountDao

    companion object {
        private const val DATABASE_NAME = "personal_diary.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(
                        // Add migrations here as database evolves
                        // MIGRATION_1_2,
                        // MIGRATION_2_3,
                    )
                    .fallbackToDestructiveMigration() // Remove in production after v1
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Clear database instance (used for testing or logout)
         */
        fun clearInstance() {
            INSTANCE = null
        }

        /**
         * Example migration (for future use)
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example: Adding a new column
                // database.execSQL("ALTER TABLE entries ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
