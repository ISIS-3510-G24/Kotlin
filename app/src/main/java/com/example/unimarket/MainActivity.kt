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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.unimarket.data.PreferencesManager
import com.example.unimarket.ui.theme.UniMarketTheme
import com.example.unimarket.ui.viewmodels.FindDetailViewModel
import com.example.unimarket.ui.views.FindDetailScreen
import com.example.unimarket.ui.views.LoginScreen
import com.example.unimarket.ui.views.MainScreen
import com.example.unimarket.ui.views.OnboardingScreen
import com.example.unimarket.ui.views.PersonalizationScreen
import com.example.unimarket.ui.views.RegisterScreen
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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
    val startDestination = when {
        !onboardingCompleted -> "onboarding"
        currentUser == null -> "login"
        else -> "main"
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
                    navController.popBackStack()
                }
            )
        }

        composable("main") {
            MainScreen(rootNavController = navController)

        }

        composable(
            route = "findDetail/{findId}",
            arguments = listOf(navArgument("findId") { type = NavType.StringType })
        ) { backStackEntry ->
            val findDetailVm: FindDetailViewModel =
                viewModel (viewModelStoreOwner = backStackEntry)
            FindDetailScreen(
                navController = navController,
                viewModel = findDetailVm
            )
        }
    }
}
