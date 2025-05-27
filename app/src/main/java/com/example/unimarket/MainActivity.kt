package com.example.unimarket

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.unimarket.data.PreferencesManager
import com.example.unimarket.ui.theme.UniMarketTheme
import com.example.unimarket.ui.viewmodels.FindDetailViewModel
import com.example.unimarket.ui.viewmodels.OfferDetailViewModel
import com.example.unimarket.ui.viewmodels.ProductDetailViewModel
import com.example.unimarket.ui.viewmodels.SellerReviewsViewModel
import com.example.unimarket.ui.views.FindDetailScreen
import com.example.unimarket.ui.views.LoginScreen
import com.example.unimarket.ui.views.MainScreen
import com.example.unimarket.ui.views.OfferDetailScreen
import com.example.unimarket.ui.views.OnboardingScreen
import com.example.unimarket.ui.views.PersonalizationScreen
import com.example.unimarket.ui.views.ProductDetailScreen
import com.example.unimarket.ui.views.PublishProductScreen
import com.example.unimarket.ui.views.RegisterScreen
import com.example.unimarket.ui.views.SellerReviewsScreen
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UniMarketTheme {
                AppNavigation()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    // Este es el NavController raíz de toda la app
    val navController = rememberNavController()
    val context = LocalContext.current

    // 1) Observamos si completó onboarding
    val onboardingCompletedFlow = PreferencesManager.isOnboardingCompleted(context)
    val onboardingCompleted by onboardingCompletedFlow.collectAsState(initial = false)

    // 2) Chequeamos si está autenticado
    val currentUser = FirebaseAuth.getInstance().currentUser

    // 3) Definimos pantalla inicial
    val startDestination = when {
        !onboardingCompleted -> "onboarding"
        currentUser == null    -> "login"
        else                   -> "main"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        // Onboarding
        composable("onboarding") {
            OnboardingScreen(
                onFinishOnboarding = {
                    navController.navigate("personalization") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate("login") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        // Personalization
        composable("personalization") {
            PersonalizationScreen(
                onFinishPersonalization = {
                    navController.navigate("login") {
                        popUpTo("personalization") { inclusive = true }
                    }
                }
            )
        }

        // Login
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        // Register
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.popBackStack()
                }
            )
        }

        // Main (pantalla principal con Bottom Nav)
        composable("main") {
            // Aquí pasamos el NavController raíz para que, desde MainScreen,
            // cuando naveguen a ProductDetailScreen o SellerReviewsScreen,
            // usen exactamente este mismo navController,
            // donde ya están registradas las rutas.
            MainScreen(rootNavController = navController)
        }

        // ProductDetail: recibe productId como argumento
        composable(
            route = "productDetail/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            // IMPORTANTE: obtenemos el ViewModel con hiltViewModel(backStackEntry),
            // para que Hilt inyecte correctamente UniMarketRepository, SavedStateHandle, etc.
            val detailVm: ProductDetailViewModel =
                hiltViewModel(backStackEntry)
            ProductDetailScreen(
                navController = navController,
                viewModel = detailVm
            )
        }

        // SellerReviews: recibe sellerId como argumento
        composable(
            route = "sellerReviews/{sellerId}",
            arguments = listOf(navArgument("sellerId") { type = NavType.StringType })
        ) { backStackEntry ->
            // Aquí también usamos hiltViewModel(backStackEntry) en lugar de viewModel()
            // para que Hilt inyecte UniMarketRepository y SavedStateHandle.
            val sellerReviewsVm: SellerReviewsViewModel =
                hiltViewModel(backStackEntry)
            SellerReviewsScreen(
                navController = navController,
                viewModel = sellerReviewsVm
            )
        }

        // FindDetail
        composable(
            route = "findDetail/{findId}",
            arguments = listOf(navArgument("findId") { type = NavType.StringType })
        ) { backStackEntry ->
            val findDetailVm: FindDetailViewModel =
                hiltViewModel(backStackEntry)
            FindDetailScreen(
                navController = navController,
                viewModel = findDetailVm
            )
        }

        // OfferDetail
        composable(
            route = "offerDetail/{findId}",
            arguments = listOf(navArgument("findId") { type = NavType.StringType })
        ) { backStackEntry ->
            val offerDetailVm: OfferDetailViewModel =
                hiltViewModel(backStackEntry)
            OfferDetailScreen(
                navController = navController,
                viewModel = offerDetailVm
            )
        }

        // PublishProduct
        composable("publishProduct") {
            PublishProductScreen(navController)
        }
    }
}
