package com.saferoute.ai.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.saferoute.ai.data.local.SessionDataStore
import com.saferoute.ai.service.LocationStateHolder
import com.saferoute.ai.ui.screens.auth.LoginScreen
import com.saferoute.ai.ui.screens.auth.SignupScreen
import com.saferoute.ai.ui.screens.auth.SplashScreen
import com.saferoute.ai.ui.screens.map.MapScreen
import com.saferoute.ai.ui.screens.map.MapViewModel
import com.saferoute.ai.ui.screens.notifications.NotificationsScreen
import com.saferoute.ai.ui.screens.profile.ProfileScreen
import com.saferoute.ai.ui.screens.reports.ReportsScreen
import com.saferoute.ai.ui.screens.route.RoutePlannerScreen
import com.saferoute.ai.ui.screens.route.RouteViewModel
import com.saferoute.ai.ui.theme.ErrorRed
import com.saferoute.ai.util.ConnectivityObserver
import kotlinx.coroutines.flow.first

@Composable
fun SafeRouteNavHost(
    sessionDataStore: SessionDataStore,
    connectivityObserver: ConnectivityObserver
) {
    val navController = rememberNavController()
    var authDestination by remember { mutableStateOf<String?>(null) }
    val isOnline by connectivityObserver.isOnline.collectAsState(initial = true)
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val bottomRoutes = setOf(Screen.Map.route, Screen.Reports.route, Screen.Profile.route)

    LaunchedEffect(Unit) {
        val userId = sessionDataStore.userId.first()
        authDestination = if (userId != null) Screen.Map.route else Screen.Login.route
    }

    if (authDestination == null) {
        SplashScreen(onFinished = {})
        return
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Screen.Splash.route) {
            composable(Screen.Splash.route) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1500)
                    navController.navigate(authDestination!!) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
                SplashScreen(onFinished = {})
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateSignup = { navController.navigate(Screen.Signup.route) },
                    onLoginSuccess = {
                        navController.navigate(Screen.Map.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Signup.route) {
                SignupScreen(
                    onNavigateLogin = { navController.popBackStack() },
                    onSignupSuccess = {
                        navController.navigate(Screen.Map.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Map.route) {
                val mapViewModel: MapViewModel = hiltViewModel()
                LaunchedEffect(Unit) {
                    LocationStateHolder.location.collect { loc ->
                        loc?.let { mapViewModel.updateLocation(it) }
                    }
                }
                LaunchedEffect(Unit) {
                    LocationStateHolder.speedKmh.collect { mapViewModel.setSpeed(it) }
                }
                MainScaffold(
                    currentRoute = Screen.Map.route,
                    navController = navController,
                    showBottomBar = true
                ) {
                    MapScreen(
                        onNavigateRoute = { navController.navigate(Screen.RoutePlanner.route) },
                        onNavigateNotifications = { navController.navigate(Screen.Notifications.route) }
                    )
                }
            }
            composable(Screen.Reports.route) {
                MainScaffold(
                    currentRoute = Screen.Reports.route,
                    navController = navController,
                    showBottomBar = true
                ) {
                    ReportsScreen(onIncidentClick = { _, _ ->
                        navController.navigate(Screen.Map.route)
                    })
                }
            }
            composable(Screen.Profile.route) {
                MainScaffold(
                    currentRoute = Screen.Profile.route,
                    navController = navController,
                    showBottomBar = true
                ) {
                    ProfileScreen(
                        onLoggedOut = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
            composable(Screen.RoutePlanner.route) {
                val mapBackStackEntry = remember(navController) {
                    navController.getBackStackEntry(Screen.Map.route)
                }
                val mapViewModel: MapViewModel = hiltViewModel(mapBackStackEntry)
                val routeViewModel: RouteViewModel = hiltViewModel()
                val mapState by mapViewModel.uiState.collectAsState()
                val routeState by routeViewModel.uiState.collectAsState()
                RoutePlannerScreen(
                    sourceLat = mapState.userLat,
                    sourceLng = mapState.userLng,
                    sourceLabel = mapState.userLocationName,
                    onRoutePlanned = { primary, alternatives ->
                        mapViewModel.setActiveRoute(primary, alternatives)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    onNavigateToMap = { _, _ -> navController.navigate(Screen.Map.route) }
                )
            }
        }

        if (!isOnline) {
            Text(
                "You are offline",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(ErrorRed)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun MainScaffold(
    currentRoute: String,
    navController: androidx.navigation.NavHostController,
    showBottomBar: Boolean,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Map.route,
                        onClick = { navController.navigate(Screen.Map.route) },
                        icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                        label = { Text("Map") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Reports.route,
                        onClick = { navController.navigate(Screen.Reports.route) },
                        icon = { Icon(Icons.Default.List, contentDescription = "Reports") },
                        label = { Text("Reports") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Profile.route,
                        onClick = { navController.navigate(Screen.Profile.route) },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) { content() }
    }
}
