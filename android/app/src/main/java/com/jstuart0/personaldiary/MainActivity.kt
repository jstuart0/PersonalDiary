package com.jstuart0.personaldiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jstuart0.personaldiary.presentation.navigation.PersonalDiaryApp
import com.jstuart0.personaldiary.presentation.theme.PersonalDiaryTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for the Personal Diary app
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (Android 12+)
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            PersonalDiaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PersonalDiaryApp()
                }
            }
        }
    }
}
