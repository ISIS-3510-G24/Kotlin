package com.example.unimarket.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Define the light color scheme using the colors we declared
private val LightColorScheme = lightColorScheme(
    primary = Blue500,
    primaryContainer = Blue600,
    secondary = Blue700,
    background = GrayLight500,
    surface = White,
    onPrimary = White,
    onBackground = GrayDark100
    // Add more color assignments if needed
)

// Define a dark color scheme (optional)
private val DarkColorScheme = darkColorScheme(
    primary = Blue500,
    primaryContainer = Blue600,
    secondary = Blue700,
    background = GrayDark100,
    surface = GrayDark200,
    onPrimary = White,
    onBackground = White
    // Add more color assignments if needed
)

@Composable
fun UniMarketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Choose between light and dark color schemes based on the system setting
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Reference the custom Typography defined in Type.kt
        shapes = Shapes,         // Reference the custom Shapes defined in Shapes.kt
        content = content
    )
}
