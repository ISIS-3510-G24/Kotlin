package com.example.unimarket

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.unimarket.ui.data.PreferencesManager
import com.example.unimarket.ui.login.LoginScreen
import com.example.unimarket.ui.main.MainScreen
import com.example.unimarket.ui.onboarding.OnboardingScreen
import com.example.unimarket.ui.onboarding.PersonalizationScreen
import com.example.unimarket.ui.register.RegisterScreen
import com.example.unimarket.ui.theme.UniMarketTheme
import com.google.firebase.auth.FirebaseAuth

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
    val context = LocalContext.current

    // Observe onboarding completion from DataStore
    val onboardingCompletedFlow = PreferencesManager.isOnboardingCompleted(context)
    val onboardingCompleted by onboardingCompletedFlow.collectAsState(initial = false)

    // Check if user is authenticated
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Set startDestination based on whether onboarding and authentication status
    val startDestination = if (!onboardingCompleted) {
        "onboarding"
    } else {
        if (currentUser != null) "main" else "login"
    }

    NavHost(navController = navController, startDestination = startDestination)
    {
        composable("onboarding") {
            OnboardingScreen(
                onFinishOnboarding = {
                    // Navigate to personalization after finishing onboarding
                    navController.navigate("personalization") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onSkip = {
                    // Skip onboarding and navigate to login
                    navController.navigate("login") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("personalization") {
            PersonalizationScreen(
                onFinishPersonalization = {
                    // After personalization, navigate to login
                    navController.navigate("login") {
                        popUpTo("personalization") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
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
        composable("main") {
            MainScreen()
        }
    }
}
