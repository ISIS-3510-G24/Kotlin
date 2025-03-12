package com.example.unimarket.ui.login

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.ui.data.FirebaseRepository
import kotlinx.coroutines.launch

// ViewModel for the Login screen
class LoginViewModel : ViewModel() {
    var email = mutableStateOf("")
    var password = mutableStateOf("")
    var loginError = mutableStateOf<String?>(null)
    var isLoggedIn = mutableStateOf(false)

    // Updates the email state
    fun onEmailChange(newEmail: String) {
        email.value = newEmail
    }

    // Updates the password state
    fun onPasswordChange(newPassword: String) {
        password.value = newPassword
    }

    // Initiates the login process using FirebaseRepository
    fun login() {
        viewModelScope.launch {
            FirebaseRepository.signIn(email.value, password.value) { success, errorMsg ->
                if (success) {
                    isLoggedIn.value = true
                } else {
                    loginError.value = errorMsg
                }
            }
        }
    }
}
