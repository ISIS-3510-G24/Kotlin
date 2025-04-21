package com.example.unimarket.ui.viewmodels

import android.net.Uri
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import com.example.unimarket.ui.data.FirebaseFirestoreSingleton
import com.example.unimarket.ui.models.ProfileUiState
import com.example.unimarket.ui.models.User
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val usersRef = FirebaseFirestoreSingleton.getCollection("User")
    private val storageRef = FirebaseStorage.getInstance().reference

    private val performance = FirebasePerformance.getInstance()
    private val analytics: FirebaseAnalytics = Firebase.analytics
    private var trace: Trace? = null

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { loadUser() }

    fun onScreenLoadStart() {
        trace = performance.newTrace("load_ProfileScreen").apply { start() }
        analytics.logEvent("screen_load_start", bundleOf("screen" to "Profile"))
    }
    fun onScreenLoadEnd(success: Boolean = true) {
        trace?.stop()
        analytics.logEvent(
            "screen_load_end",
            bundleOf("screen" to "Profile", "success" to success)
        )
    }

    private fun loadUser() {
        onScreenLoadStart()
        val uid = auth.currentUser?.uid ?: return
        _uiState.value = _uiState.value.copy(isLoading = true)
        usersRef.document(uid).get()
            .addOnSuccessListener { snap ->
                val u = snap.toObject(User::class.java)
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    user      = u,
                    errorMessage = if (u == null) "User not found" else null
                )
                onScreenLoadEnd(true)
            }
            .addOnFailureListener { ex ->
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    user = null,
                    errorMessage = ex.localizedMessage
                )
                onScreenLoadEnd(false)
            }
    }

    /** Upload the selected image URI to Storage and update Firestore. */
    fun uploadProfilePicture(
        uri: Uri,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return
        // Reference like "profilePictures/{uid}.jpg"
        val picRef = storageRef.child("profilePictures/$uid.jpg")

        // 1) Upload the file
        picRef.putFile(uri)
            .addOnSuccessListener {
                // 2) Get download URL
                picRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        // 3) Update Firestore user doc
                        usersRef.document(uid)
                            .update("profilePicture", downloadUri.toString())
                            .addOnSuccessListener {
                                // 4) Refresh local UI state
                                loadUser()
                                onSuccess()
                            }
                            .addOnFailureListener { ex ->
                                onError(ex.localizedMessage ?: "Failed to update profile")
                            }
                    }
                    .addOnFailureListener { ex ->
                        onError(ex.localizedMessage ?: "Failed to retrieve download URL")
                    }
            }
            .addOnFailureListener { ex ->
                onError(ex.localizedMessage ?: "Upload failed")
            }
    }
}
