package com.example.unimarket.ui.views

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.unimarket.ui.models.BottomNavItem
import com.example.unimarket.ui.viewmodels.ProductDetailViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(
    rootNavController: NavController,
) {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem("Orders", "orders", Icons.Default.Widgets),
        BottomNavItem("Find & Offer", "find_offer", Icons.Default.Search),
        BottomNavItem("Explore", "explore", Icons.Default.Explore),
        BottomNavItem("Chat", "chat", Icons.Default.ChatBubble),
        BottomNavItem("Profile", "profile", Icons.Default.Person)
    )


    Scaffold(
        topBar = { TopOfflineBar() },
        bottomBar = {
            BottomNavBar(navController = navController, items = bottomNavItems)
        },
        //contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "explore",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("find_offer") {
                FindOfferScreen(
                    navController = rootNavController,
                    bottomNavController = navController
                )
            }
            composable("orders") {
                OrdersScreen(
                    navController = rootNavController,
                    bottomNavController = navController
                )
            }
            composable("chat") {
                ChatScreen(
                    navController = navController,
                    onNavigateToChat = { chatId ->
                        navController.navigate("chatDetail/$chatId")
                    }
                )
            }
            composable("explore") {
                ExploreScreen(navController)
            }

            composable("profile") {
                ProfileScreen(
                    navController = navController,
                    rootNavController = rootNavController,
                    bottomItems = bottomNavItems
                )
            }

            composable(
                route = "productDetail/{productId}",
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val detailVm: ProductDetailViewModel = hiltViewModel(backStackEntry)
                ProductDetailScreen(
                    navController = rootNavController,
                    viewModel = detailVm
                )
            }


            composable("publishProduct") {
                PublishProductScreen(navController)
            }
            composable("publishFind") {
                PublishFindScreen(navController)
            }
            composable("wishlist") {
                WishlistScreen(onBack = { navController.popBackStack() })
            }
            composable("edit_profile") {
                EditProfileScreen(navController = navController)
            }
            composable("validate_seller") {
                ValidateDeliveryScreen(navController = navController)
            }

            composable(
                "writeUserReview/{orderId}/{targetId}",
                arguments = listOf(
                    navArgument("orderId") { type = NavType.StringType },
                    navArgument("targetId") { type = NavType.StringType }
                )
            ) { back ->
                val orderId = back.arguments!!.getString("orderId")!!
                val target = back.arguments!!.getString("targetId")!!
                WriteUserReviewScreen(
                    orderId = orderId,
                    targetId = target,
                    navController = navController
                )
            }

            composable("myUserReviews") {
                MyUserReviewsScreen(navController = navController)
            }
        }
    }
}

