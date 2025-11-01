package com.jstuart0.personaldiary.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jstuart0.personaldiary.presentation.auth.AuthViewModel
import com.jstuart0.personaldiary.presentation.auth.LoginScreen
import com.jstuart0.personaldiary.presentation.auth.SignupScreen
import com.jstuart0.personaldiary.presentation.auth.TierSelectionScreen
import com.jstuart0.personaldiary.presentation.entry.EntryScreen
import com.jstuart0.personaldiary.presentation.search.SearchScreen
import com.jstuart0.personaldiary.presentation.timeline.TimelineScreen

/**
 * Navigation graph for the app
 */
@Composable
fun NavigationGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (currentUser != null) {
            Screen.Timeline.route
        } else {
            Screen.Login.route
        }
    ) {
        // Authentication flow
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignup = {
                    navController.navigate(Screen.TierSelection.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Timeline.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.TierSelection.route) {
            TierSelectionScreen(
                onTierSelected = { tier ->
                    navController.navigate(Screen.Signup.createRoute(tier.name))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Signup.route,
            arguments = listOf(navArgument("tier") { type = NavType.StringType })
        ) {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate(Screen.Timeline.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Main app flow
        composable(Screen.Timeline.route) {
            TimelineScreen(
                onNavigateToEntry = { entryId ->
                    navController.navigate(Screen.Entry.createRoute(entryId))
                },
                onNavigateToNewEntry = {
                    navController.navigate(Screen.NewEntry.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                }
            )
        }

        composable(Screen.NewEntry.route) {
            EntryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEntrySaved = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Entry.route,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType })
        ) {
            EntryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEntrySaved = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateToEntry = { entryId ->
                    navController.navigate(Screen.Entry.createRoute(entryId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Screen destinations
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object TierSelection : Screen("tier_selection")
    object Signup : Screen("signup/{tier}") {
        fun createRoute(tier: String) = "signup/$tier"
    }
    object Timeline : Screen("timeline")
    object NewEntry : Screen("entry/new")
    object Entry : Screen("entry/{entryId}") {
        fun createRoute(entryId: String) = "entry/$entryId"
    }
    object Search : Screen("search")
}
