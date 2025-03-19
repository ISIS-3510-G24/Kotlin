package com.example.unimarket.ui.login

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class LoginViewModel: ViewModel() {
    val loginSucess = mutableStateOf<Boolean?>(null)
    val errorMessage = mutableStateOf<String?>(null)

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    loginSucess.value = true
                } else {
                    loginSucess.value = false
                    errorMessage.value = task.exception?.localizedMessage
                }
            }
    }

    fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    errorMessage.value = task.exception?.localizedMessage
                }
            }
    }
}