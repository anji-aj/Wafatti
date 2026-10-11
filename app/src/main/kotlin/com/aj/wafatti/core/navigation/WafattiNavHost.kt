package com.aj.wafatti.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aj.wafatti.feature.auth.ForgotPasswordScreen
import com.aj.wafatti.feature.auth.LoginScreen
import com.aj.wafatti.feature.auth.RegisterScreen
import com.aj.wafatti.feature.chat.ChatScreen
import com.aj.wafatti.feature.contacts.ContactsScreen
import com.aj.wafatti.feature.home.HomeScreen
import com.aj.wafatti.feature.profile.ProfileScreen
import com.aj.wafatti.feature.settings.SettingsScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun WafattiNavHost(
    navController: NavHostController,
    startDestination: String = if (FirebaseAuth.getInstance().currentUser != null) Screen.Home.route else Screen.Login.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToChat = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                },
                onNavigateToContacts = { navController.navigate(Screen.Contacts.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            ChatScreen(
                conversationId = conversationId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Contacts.route) {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            ContactsScreen(
                onBackClick = { navController.popBackStack() },
                onUserSelected = { targetUserId ->
                    val conversationId = if (targetUserId.contains("_")) {
                        targetUserId
                    } else if (currentUid.isNotBlank()) {
                        if (currentUid < targetUserId) "${currentUid}_${targetUserId}" else "${targetUserId}_${currentUid}"
                    } else {
                        targetUserId
                    }
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
