package com.jstuart0.personaldiary.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController

/**
 * Root composable for the Personal Diary app
 */
@Composable
fun PersonalDiaryApp() {
    val navController = rememberNavController()
    NavigationGraph(navController = navController)
}
