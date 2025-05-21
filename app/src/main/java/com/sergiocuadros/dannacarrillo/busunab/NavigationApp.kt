package com.sergiocuadros.dannacarrillo.busunab

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.sergiocuadros.dannacarrillo.busunab.models.UserRole
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.AuthViewModel
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.CurrentUserState
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.BusViewModel

@Composable
fun NavigationApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val currentUserState by authViewModel.currentUserData.collectAsState()

    when (val state = currentUserState) {
        is CurrentUserState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is CurrentUserState.Error -> {
            LoginScreen(
                onClickRegister = {
                    authViewModel.signOut()
                },
                onSuccesfulLogin = { authViewModel.refreshUserData() }
            )
        }
        is CurrentUserState.NotAuthenticated -> {
            NavHost(navController = navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(
                        onClickRegister = {
                            navController.navigate("register")
                        },
                        onSuccesfulLogin = {
                            authViewModel.refreshUserData()
                        }
                    )
                }
                composable("register") {
                    RegisterScreen(
                        onClickBack = {
                            navController.popBackStack()
                        },
                        onSuccessfulRegister = { role ->
                            authViewModel.refreshUserData()
                            val destination = if (role == UserRole.ADMIN) "admin_stats" else "bus_view"
                            navController.navigate(destination) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
        is CurrentUserState.Authenticated -> {
            val user = state.user
            val startDestination = if (user.role == UserRole.ADMIN) "admin_stats" else "bus_view"

            NavHost(navController = navController, startDestination = startDestination) {
                // Admin routes (directly inlined)
                composable("admin_stats") {
                    AdminStatsScreen(
                        onNavigateToBusManagement = {
                            navController.navigate("admin_bus")
                        },
                        onLogout = {
                            authViewModel.signOut()
                        }
                    )
                }
                composable("admin_bus") {
                    BusManagementScreen( 
                        onNavigateToStats = {
                            navController.navigate("admin_stats")
                        },
                        onLogout = {
                            authViewModel.signOut()
                        }
                    )
                }

                composable("bus_view") {
                    BusViewScreen(
                        onBusClick = { plate ->
                            val driverId = (currentUserState as? CurrentUserState.Authenticated)?.user?.id ?: "unknown_driver"
                            navController.navigate("bus_seats/$plate/$driverId")
                        },
                        onLogout = {
                            authViewModel.signOut()
                        }
                    )
                }
                composable(
                    route = "scan/{plate}/{seatNumber}",
                    arguments = listOf(
                        navArgument("plate") { type = NavType.StringType },
                        navArgument("seatNumber") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val plate = backStackEntry.arguments?.getString("plate") ?: "unknown_bus_plate"
                    val seatNumber = backStackEntry.arguments?.getInt("seatNumber") ?: -1
                    val authenticatedUser = (currentUserState as? CurrentUserState.Authenticated)?.user
                    val busViewModel: BusViewModel = viewModel()
                    ScanScreen(
                        driverDisplayName = authenticatedUser?.name ?: "Conductor",
                        driverId = authenticatedUser?.id ?: "unknown_driver",
                        busId = plate,
                        seatNumberToOccupy = seatNumber,
                        busViewModel = busViewModel,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
                 composable(
                        route = "bus_seats/{plate}/{driverId}",
                        arguments = listOf(
                            navArgument("plate") { type = NavType.StringType },
                            navArgument("driverId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val plate = backStackEntry.arguments?.getString("plate") ?: ""
                        val driverId = backStackEntry.arguments?.getString("driverId") ?: "unknown_driver"
                        BusSeatsScreen(
                            busPlate = plate,
                            driverId = driverId,
                            onBusView = { 
                                navController.navigate("bus_view") {
                                    popUpTo("bus_view") {inclusive = true} 
                                }
                            },
                            onNavigateToScan = { seatNum ->
                                navController.navigate("scan/$plate/$seatNum")
                            },
                        )
                    }
            }
        }
    }
}
