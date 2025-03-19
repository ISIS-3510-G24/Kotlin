package com.example.unimarket

import android.annotation.SuppressLint
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
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
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

    NavHost(navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.navigate("home") },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    // You can choose to either:
                    // 1) Go back to the login screen:
                    navController.popBackStack()
                    // 2) Go to the home screen:
                    // navController.navigate("home") { popUpTo("login") { inclusive = true } }
                }
            )
        }
        composable("home") {
            HomeScreen()
        }
    }
}
