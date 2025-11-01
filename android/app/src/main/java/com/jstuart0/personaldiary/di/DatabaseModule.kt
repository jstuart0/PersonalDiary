package com.jstuart0.personaldiary.di

import android.content.Context
import com.jstuart0.personaldiary.data.local.AppDatabase
import com.jstuart0.personaldiary.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideEntryDao(database: AppDatabase): EntryDao {
        return database.entryDao()
    }

    @Provides
    @Singleton
    fun provideEntryFtsDao(database: AppDatabase): EntryFtsDao {
        return database.entryFtsDao()
    }

    @Provides
    @Singleton
    fun provideMediaDao(database: AppDatabase): MediaDao {
        return database.mediaDao()
    }

    @Provides
    @Singleton
    fun provideSocialAccountDao(database: AppDatabase): SocialAccountDao {
        return database.socialAccountDao()
    }
}
