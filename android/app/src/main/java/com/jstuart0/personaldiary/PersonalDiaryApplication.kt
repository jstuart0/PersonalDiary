package com.jstuart0.personaldiary

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for Personal Diary
 * Initializes Hilt dependency injection and Google Tink encryption
 */
@HiltAndroidApp
class PersonalDiaryApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Initialize Google Tink for encryption
        try {
            AeadConfig.register()
        } catch (e: Exception) {
            // Log error in production
            e.printStackTrace()
        }
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}
