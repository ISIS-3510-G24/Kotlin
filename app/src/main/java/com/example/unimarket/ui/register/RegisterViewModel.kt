package com.example.unimarket.ui.register

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.unimarket.ui.data.FirebaseRealtimeDatabaseSingleton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference

class RegisterViewModel : ViewModel() {

    // UI State
    val registerSuccess = mutableStateOf<Boolean?>(null)
    val errorMessage = mutableStateOf<String?>(null)

    // Extra fields for validation
    val name = mutableStateOf("")
    val email = mutableStateOf("")
    val password = mutableStateOf("")
    val confirmPassword = mutableStateOf("")
    val acceptTerms = mutableStateOf(false)

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    fun registerUser() {
        // Passwords validation
        if (password.value != confirmPassword.value) {
            errorMessage.value = "Passwords do not match"
            return
        }
        // Checkbox validation
        if (!acceptTerms.value) {
            errorMessage.value = "You must accept the Terms and Conditions"
            return
        }

        // Create user in Firebase Auth
        auth.createUserWithEmailAndPassword(email.value, password.value)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Save name
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        saveUserNameToDatabase(userId, name.value)
                    }
                    registerSuccess.value = true
                } else {
                    registerSuccess.value = false
                    errorMessage.value = task.exception?.localizedMessage
                }
            }
    }

    private fun saveUserNameToDatabase(userId: String, userName: String) {

        val userRef: DatabaseReference =
            FirebaseRealtimeDatabaseSingleton.getReference("User").child(userId)

        val userData = mapOf(
            "name" to userName,
            "email" to email.value
        )

        userRef.setValue(userData).addOnCompleteListener { dbTask ->
            if (!dbTask.isSuccessful) {
                // Error handling is we can't save name
                errorMessage.value = dbTask.exception?.localizedMessage
            }
        }
    }
}
