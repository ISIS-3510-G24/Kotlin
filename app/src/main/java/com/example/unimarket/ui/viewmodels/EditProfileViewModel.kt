package com.example.unimarket.ui.viewmodels

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import com.example.unimarket.data.FirebaseFirestoreSingleton
import com.example.unimarket.ui.models.EditProfileUiState
import com.example.unimarket.ui.models.Major
import com.example.unimarket.ui.models.User
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class EditProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val usersRef = FirebaseFirestoreSingleton.getCollection("User")
    private val majorsRef = FirebaseFirestoreSingleton.getCollection("majors")

    private val performance = FirebasePerformance.getInstance()
    private val analytics: FirebaseAnalytics = Firebase.analytics
    private var trace: Trace? = null

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    fun onScreenLoadStart() {
        trace = performance.newTrace("load_EditProfileScreen").apply { start() }
        analytics.logEvent("screen_load_start", bundleOf("screen" to "EditProfile"))
    }
    fun onScreenLoadEnd(success: Boolean = true) {
        trace?.stop()
        analytics.logEvent(
            "screen_load_end",
            bundleOf("screen" to "EditProfile", "success" to success)
        )
    }

    init {
        onScreenLoadStart()
        loadMajors()
        loadUser()
    }

    /** Load the list of majors from Firestore */
    private fun loadMajors() {
        majorsRef.get()
            .addOnSuccessListener { snaps ->
                val list = snaps.documents.mapNotNull { it.toObject(Major::class.java)?.copy(id = it.id) }
                _uiState.value = _uiState.value.copy(majorList = list)
            }
    }

    /** Load current user data and pre-select their major */
    private fun loadUser() {
        val uid = auth.currentUser?.uid ?: return
        _uiState.value = _uiState.value.copy(isLoading = true)
        usersRef.document(uid).get()
            .addOnSuccessListener { snap ->
                val user = snap.toObject(User::class.java)
                if (user != null) {
                    // Find the Major object matching the stored major ID (if any)
                    val selected = _uiState.value.majorList.find { it.id == user.major }
                    _uiState.value = EditProfileUiState(
                        isLoading     = false,
                        displayName   = user.displayName,
                        bio           = user.bio,
                        majorList     = _uiState.value.majorList,
                        selectedMajor = selected,
                        profilePicUrl = user.profilePicture
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

    // Handlers for fields
    fun onDisplayNameChange(value: String) {
        _uiState.value = _uiState.value.copy(displayName = value)
    }
    fun onBioChange(value: String) {
        _uiState.value = _uiState.value.copy(bio = value)
    }
    fun onMajorSelected(major: Major) {
        _uiState.value = _uiState.value.copy(selectedMajor = major)
    }
    fun onProfilePicUrlChange(value: String) {
        _uiState.value = _uiState.value.copy(profilePicUrl = value)
    }

    /** Save all changes, including major ID */
    fun saveChanges(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true)

        val updatedUser = User(
            displayName    = state.displayName.trim(),
            bio            = state.bio.trim(),
            major          = state.selectedMajor?.id.orEmpty(),  // persist the major ID
            profilePicture = state.profilePicUrl.trim(),
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
