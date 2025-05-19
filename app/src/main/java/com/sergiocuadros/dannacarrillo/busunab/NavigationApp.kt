package com.sergiocuadros.dannacarrillo.busunab

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun NavigationApp() {
    val navController = rememberNavController()
    val startDestination: String

    val auth = Firebase.auth
    val currentUser = auth.currentUser

    // TODO: Replace this with actual user role check from Firestore
    val isAdmin = currentUser?.email?.contains("admin") == true

    startDestination = if (currentUser != null) {
        if (isAdmin) "admin_stats" else "bus_view"
    } else {
        "login"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Authentication routes
        composable("login") {
            LoginScreen(
                onClickRegister = {
                    navController.navigate("register")
                },
                onSuccesfulLogin = {
                    if (isAdmin) {
                        navController.navigate("admin_stats") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("bus_view") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("register") {
            RegisterScreen(
                onClickBack = {
                    navController.popBackStack()
                },
                onSuccessfulRegister = {
                    if (isAdmin) {
                        navController.navigate("admin_stats") {
                            popUpTo(0)
                        }
                    } else {
                        navController.navigate("bus_view") {
                            popUpTo(0)
                        }
                    }
                }
            )
        }

        // Admin routes
        composable("admin_stats") {
            AdminStatsScreen(
                onNavigateToBusManagement = {
                    navController.navigate("admin_bus")
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }
        composable("admin_bus") {
            BusManagementScreen(
                onNavigateToStats = {
                    navController.navigate("admin_stats")
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }

        // Driver routes
        composable("bus_view") {
            BusViewScreen(
                onBusClick = { plate ->
                    navController.navigate("bus_management/$plate")
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }
        composable(
            route = "bus_management/{plate}",
            arguments = listOf(
                navArgument("plate") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val plate = backStackEntry.arguments?.getString("plate") ?: ""
            BusManagementScreen(
                busPlate = plate,
                onNavigateToScan = {
                    navController.navigate("scan/$plate")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = "scan/{plate}",
            arguments = listOf(
                navArgument("plate") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val plate = backStackEntry.arguments?.getString("plate") ?: ""
            ScanScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}