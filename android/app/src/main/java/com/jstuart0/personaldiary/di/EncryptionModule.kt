package com.jstuart0.personaldiary.di

import com.jstuart0.personaldiary.data.encryption.EncryptionService
import com.jstuart0.personaldiary.data.encryption.UCEEncryptionService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for encryption services
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EncryptionModule {

    /**
     * Provides EncryptionService implementation
     * For now, default to UCE (User-Controlled Encryption)
     * TODO: Implement dynamic selection based on user encryption tier preference
     */
    @Binds
    @Singleton
    abstract fun bindEncryptionService(
        uceEncryptionService: UCEEncryptionService
    ): EncryptionService
}