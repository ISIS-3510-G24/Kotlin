package com.example.unimarket.ui.models

import androidx.compose.ui.graphics.vector.ImageVector

// This data class represents each item in the bottom bar
data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)