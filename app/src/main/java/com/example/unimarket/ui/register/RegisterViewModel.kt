package com.example.unimarket.ui.register

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class RegisterViewModel : ViewModel() {
    val registerSuccess = mutableStateOf<Boolean?>(null)
    val errorMessage = mutableStateOf<String?>(null)

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    registerSuccess.value = true
                } else {
                    registerSuccess.value = false
                    errorMessage.value = task.exception?.localizedMessage
                }
            }
    }
}
