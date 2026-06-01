package com.saferoute.ai.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("auth/login")
    data object Signup : Screen("auth/signup")
    data object Map : Screen("map")
    data object Reports : Screen("reports")
    data object Profile : Screen("profile")
    data object RoutePlanner : Screen("route")
    data object Notifications : Screen("notifications")
    data object MyReports : Screen("reports/mine")
    data object AllReports : Screen("reports/all")
}
