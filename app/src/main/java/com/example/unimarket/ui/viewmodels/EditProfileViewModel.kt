package com.example.unimarket.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.unimarket.ui.data.FirebaseFirestoreSingleton
import com.example.unimarket.ui.models.EditProfileUiState
import com.example.unimarket.ui.models.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val usersRef = FirebaseFirestoreSingleton.getCollection("users")

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    /** Load current user data from Firestore and populate the UI state */
    private fun loadUser() {
        val uid = auth.currentUser?.uid ?: return
        _uiState.value = _uiState.value.copy(isLoading = true)
        usersRef.document(uid).get()
            .addOnSuccessListener { snap ->
                val user = snap.toObject(User::class.java)
                if (user != null) {
                    _uiState.value = EditProfileUiState(
                        isLoading      = false,
                        displayName    = user.displayName,
                        bio            = user.bio,
                        major          = user.major,
                        profilePicUrl  = user.profilePicture
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading    = false,
                        errorMessage = "User not found"
                    )
                }
            }
            .addOnFailureListener { ex ->
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    errorMessage = ex.localizedMessage
                )
            }
    }

    /** Handlers for each editable field */
    fun onDisplayNameChange(value: String) {
        _uiState.value = _uiState.value.copy(displayName = value)
    }
    fun onBioChange(value: String) {
        _uiState.value = _uiState.value.copy(bio = value)
    }
    fun onMajorChange(value: String) {
        _uiState.value = _uiState.value.copy(major = value)
    }
    fun onProfilePicUrlChange(value: String) {
        _uiState.value = _uiState.value.copy(profilePicUrl = value)
    }

    /**
     * Persist the changes back to Firestore.
     * Calls onSuccess() if save succeeds, onError(msg) if it fails.
     */
    fun saveChanges(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true)

        // Build updated User object, keep other fields default or untouched
        val updatedUser = User(
            displayName    = state.displayName.trim(),
            bio            = state.bio.trim(),
            major          = state.major.trim(),
            profilePicture = state.profilePicUrl.trim(),
            // These fields are required in the data class but we leave them as-is or default
            email          = auth.currentUser?.email ?: "",
            createdAt      = null,
            preferences    = emptyList(),
            ratingAverage  = 0.0,
            reviewsCount   = 0,
            updatedAt      = null
        )

        usersRef.document(uid)
            .set(updatedUser)
            .addOnSuccessListener {
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess()
            }
            .addOnFailureListener { ex ->
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    errorMessage = ex.localizedMessage
                )
                onError(ex.localizedMessage ?: "Unknown error")
            }
    }
}
