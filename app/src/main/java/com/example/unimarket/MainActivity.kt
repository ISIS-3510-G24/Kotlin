package com.example.unimarket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.unimarket.ui.home.HomeScreen
import com.example.unimarket.ui.login.LoginScreen
import com.example.unimarket.ui.register.RegisterScreen
import com.example.unimarket.ui.theme.UniMarketTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UniMarketTheme {
                Scaffold {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "login"  // First screen is login
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // If login successful, go to Home
                    navController.navigate("home") {
                        // Avoid user to go back
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    // Navigate register screen
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    // After registration, go back to login screen
                    navController.popBackStack()
                }
            )
        }

        composable("home") {
            HomeScreen()
        }
    }
}
