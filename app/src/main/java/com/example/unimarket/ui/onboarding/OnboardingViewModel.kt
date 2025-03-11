package com.example.unimarket.ui.onboarding

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// ViewModel for the Onboarding screen
class OnboardingViewModel : ViewModel() {
    // State to trigger navigation to the Login screen
    var navigateToLogin by mutableStateOf(false)
        private set

    // Called when the "Get Started" button is clicked
    fun onGetStartedClicked() {
        navigateToLogin = true
    }
}
