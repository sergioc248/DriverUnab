package com.sergiocuadros.dannacarrillo.busunab

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun NavigationApp() {
    val navController = rememberNavController()
    val startDestination: String

    val auth = Firebase.auth
    val currentUser = auth.currentUser

    startDestination = if (currentUser != null) {
        "home"
    } else {
        "login"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(onClickRegister = {
                navController.navigate("register")
            }, onSuccesfulLogin = {
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }
        composable("register") {
            RegisterScreen(onClickBack = {
                navController.popBackStack()
            }, onSuccessfulRegister = {
                navController.navigate("home") {
                    popUpTo(0)
                }
            })
        }
        composable("home") {
            HomeScreen(onClickLogout = {
                navController.navigate("login") {
                    popUpTo(0)
                }
            })
        }
    }
}