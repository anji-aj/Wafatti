package com.aj.wafatti.core.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")
    data object Home : Screen("home")
    data object Chats : Screen("chats")
    data object Groups : Screen("groups")
    data object Calls : Screen("calls")
    data object Profile : Screen("profile")
    data object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    data object Contacts : Screen("contacts")
    data object Settings : Screen("settings")
}
