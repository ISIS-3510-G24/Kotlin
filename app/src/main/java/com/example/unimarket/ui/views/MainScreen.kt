package com.example.unimarket.ui.views

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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.unimarket.ui.models.BottomNavItem

//import com.example.unimarket.ui.profile.ProfileScreen

@Composable
fun MainScreen(
    rootNavController: NavController
) {
    // Use a NavController for bottom navigation
    val navController = rememberNavController()

    // List of items for the bottom bar
    val bottomNavItems = listOf(
        BottomNavItem("Orders", "orders", Icons.Default.Widgets),
        BottomNavItem("Find & Offer", "find_offer", Icons.Default.Search),
        BottomNavItem("Explore", "explore", Icons.Default.Explore),
        BottomNavItem("Chat", "chat", Icons.Default.ChatBubble),
        BottomNavItem("Profile", "profile", Icons.Default.Person)
    )

    // Scaffold with bottomBar
    Scaffold(
        bottomBar = {
            BottomNavBar(navController = navController, items = bottomNavItems)
        }
    ) { innerPadding ->
        // NavHost for each route
        NavHost(
            navController = navController,
            startDestination = "explore",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("find_offer") {
                FindOfferScreen(onNavigateToProductDetail = { productId ->
                }
                )
            }
            composable("orders") {
                OrdersScreen(onNavigateToOrder = { orderId ->
                }
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
                ExploreScreen(
                    navController = rootNavController,
                    bottomNavController = navController
                )
            }
            composable("profile") {
                ProfileScreen(
                    navController = navController,
                    rootNavController = rootNavController,
                    bottomItems = bottomNavItems
                )
            }
            composable("publish") { PublishProductScreen(navController) }
            composable("wishlist") {
                WishlistScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable("edit_profile") {
                EditProfileScreen(navController = navController)
            }
        }
    }
}
