package com.example.unimarket.ui.views

import android.os.Bundle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.unimarket.ui.models.BottomNavItem
import com.google.firebase.analytics.FirebaseAnalytics

@Composable
fun BottomNavBar(navController: NavController, items: List<BottomNavItem>) {
    // This composable draws a Material 3 NavigationBar with items
    val context = LocalContext.current
    val analytics = FirebaseAnalytics.getInstance(context)
    NavigationBar {
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentDestination?.route == item.route,
                onClick = {
                    // Log the selected item to Firebase Analytics
                    if (item.route == "chat") {
                        val bundle = Bundle().apply {
                            putString("communication_method", "in_app_chat")
                        }
                        analytics.logEvent("communication_method_selected", bundle)
                    }

                    // Navigate to the clicked item
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
