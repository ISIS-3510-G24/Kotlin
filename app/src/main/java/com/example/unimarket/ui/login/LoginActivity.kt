package com.example.unimarket.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.unimarket.ui.theme.UniMarketTheme
import com.example.unimarket.MainActivity

// Activity for the Login screen
class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UniMarketTheme {
                // Pass the login success callback to the LoginScreen
                LoginScreen(onLoginSuccess = {
                    // Navigate to MainActivity after successful login
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                })
            }
        }
    }
}
