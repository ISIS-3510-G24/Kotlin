package com.example.unimarket.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.unimarket.ui.data.FirebaseFirestoreSingleton
import com.example.unimarket.ui.models.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val errorMessage: String? = null
)

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val usersRef = FirebaseFirestoreSingleton.getCollection("users")

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        val uid = auth.currentUser?.uid ?: return
        _uiState.value = _uiState.value.copy(isLoading = true)
        usersRef.document(uid).get()
            .addOnSuccessListener { snap ->
                val u = snap.toObject(User::class.java)
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    user = u,
                    errorMessage = if (u == null) "User not found" else null
                )
            }
            .addOnFailureListener { ex ->
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    user = null,
                    errorMessage = ex.localizedMessage
                )
            }
    }
}