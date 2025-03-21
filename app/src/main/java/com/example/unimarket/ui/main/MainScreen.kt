package com.example.unimarket.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.unimarket.ui.explore.ExploreScreen
import com.example.unimarket.ui.explore.PublishProductScreen
import com.example.unimarket.ui.findOffer.FindOfferScreen
import com.example.unimarket.ui.navigation.BottomNavBar
import com.example.unimarket.ui.navigation.BottomNavItem

//import com.example.unimarket.ui.orders.OrdersScreen
//import com.example.unimarket.ui.chat.ChatScreen
//import com.example.unimarket.ui.profile.ProfileScreen

@Composable
fun MainScreen() {
    // Use a NavController for bottom navigation
    val navController = rememberNavController()

    // List of items for the bottom bar
    val bottomNavItems = listOf(
        BottomNavItem("Orders", "orders", Icons.Default.ShoppingCart),
        BottomNavItem("Find & Offer", "find_offer", Icons.Default.Search),
        BottomNavItem("Explore", "explore", Icons.Default.Home),
        BottomNavItem("Chat", "chat", Icons.Default.MailOutline),
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
            //composable("orders") { OrdersScreen() }
                composable("find_offer") {
                    FindOfferScreen(onNavigateToProductDetail = { productId ->
                        }
                    )
                }
            composable("explore") { ExploreScreen(navController) }
            //composable("chat") { ChatScreen() }
            //composable("profile") { ProfileScreen() }
            composable("publish") { PublishProductScreen(navController) }
        }
    }
}
