package com.example.unimarket.ui.viewmodels

import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

class LoginViewModel : ViewModel() {
    val loginSuccess = mutableStateOf<Boolean?>(null)
    val errorMessage = mutableStateOf<String?>(null)

    private val analytics: FirebaseAnalytics = Firebase.analytics

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    fun loginUser(email: String, password: String) {
        val loginTrace: Trace = FirebasePerformance.getInstance().newTrace("login_trace")
        loginTrace.start()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                // Stop the trace when the task is complete
                loginTrace.stop()
                if (task.isSuccessful) {
                    loginSuccess.value = true

                    // Log successful login event
                    val bundle = Bundle().apply {
                        putString("user_email", email)
                    }
                    analytics.logEvent("login_success", bundle)
                } else {
                    loginSuccess.value = false
                    errorMessage.value = task.exception?.localizedMessage

                    val bundle = Bundle().apply {
                        putString(
                            "error_message",
                            task.exception?.localizedMessage ?: "Unknown error"
                        )
                    }
                    analytics.logEvent("login_failure", bundle)
                }
            }
    }

    fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val bundle = Bundle().apply {
                        putString("user_email", email)
                    }
                    analytics.logEvent("reset_password_success", bundle)
                } else {
                    errorMessage.value = task.exception?.localizedMessage
                    val bundle = Bundle().apply {
                        putString(
                            "error_message",
                            task.exception?.localizedMessage ?: "Unknown error"
                        )
                    }
                    analytics.logEvent("reset_password_failure", bundle)
                }
            }
    }
}