package com.example.unimarket.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.unimarket.ui.theme.UniMarketTheme
import com.example.unimarket.ui.login.LoginActivity // Add import statement
import kotlin.jvm.java

// Activity for the Onboarding screen
class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UniMarketTheme {
                // Pass the navigation callback to the OnboardingScreen
                OnboardingScreen(onNavigateToLogin = {
                    // Navigate to LoginActivity
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish() // Close OnboardingActivity so user cannot return
                })
            }
        }
    }
}
