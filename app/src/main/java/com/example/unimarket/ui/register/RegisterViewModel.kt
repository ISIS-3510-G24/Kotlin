package com.example.unimarket.ui.register

import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.unimarket.ui.data.FirebaseFirestoreSingleton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

// This ViewModel handles user registration, image upload, and saving additional user data to Firestore
class RegisterViewModel : ViewModel() {

    // UI State for registration status and error messages
    val registerSuccess = mutableStateOf<Boolean?>(null)
    val errorMessage = mutableStateOf<String?>(null)

    // Registration fields
    val displayName = mutableStateOf("")
    val bio = mutableStateOf("")
    val email = mutableStateOf("")
    val password = mutableStateOf("")
    val confirmPassword = mutableStateOf("")
    val major = mutableStateOf("")
    val preferences = mutableStateOf<List<String>>(emptyList())
    val acceptTerms = mutableStateOf(false)

    // Profile picture URL state; default profile picture if none is uploaded
    val profilePictureUrl =
        mutableStateOf("https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Default_pfp.svg/2048px-Default_pfp.svg.png")

    private val analytics: FirebaseAnalytics = Firebase.analytics
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    // Uploads profile picture to Firebase Storage and updates profilePictureUrl
    fun uploadProfilePicture(imageUri: Uri, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: "unknown_user"
        val ref: StorageReference =
            storage.reference.child("profile_pictures/${userId}_${System.currentTimeMillis()}.jpg")
        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    profilePictureUrl.value = uri.toString()
                    val bundle = Bundle().apply {
                        putString("user_id", userId)
                    }
                    analytics.logEvent("profile_picture_upload_success", bundle)
                    onComplete(true)
                }.addOnFailureListener {
                    FirebaseCrashlytics.getInstance().recordException(it)
                    val bundle = Bundle().apply {
                        putString("error_message", it.message ?: "Unknown error")
                    }
                    analytics.logEvent("profile_picture_upload_failure", bundle)
                    onComplete(false)
                }
            }
            .addOnFailureListener {
                FirebaseCrashlytics.getInstance().recordException(it)
                val bundle = Bundle().apply {
                    putString("error_message", it.message ?: "Unknown error")
                }
                analytics.logEvent("profile_picture_upload_failure", bundle)
                onComplete(false)
            }
    }

    fun registerUser() {
        // Validate that all required fields are filled
        if (displayName.value.isEmpty() ||
            email.value.isEmpty() ||
            password.value.isEmpty() ||
            confirmPassword.value.isEmpty() ||
            major.value.isEmpty() ||
            preferences.value.isEmpty()
        ) {
            errorMessage.value = "Please fill out all required fields"
            return
        }

        // Validate that passwords match
        if (password.value != confirmPassword.value) {
            errorMessage.value = "Passwords do not match"
            return
        }
        // Validate that Terms and Conditions are accepted
        if (!acceptTerms.value) {
            errorMessage.value = "You must accept the Terms and Conditions"
            return
        }
        // Create user with Firebase Auth
        auth.createUserWithEmailAndPassword(email.value, password.value)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        saveUserDataToFirestore(userId)
                    }
                    registerSuccess.value = true
                    val bundle = Bundle().apply {
                        putString("user_email", email.value)
                    }
                    analytics.logEvent("registration_success", bundle)
                } else {
                    registerSuccess.value = false
                    errorMessage.value = task.exception?.localizedMessage
                    val bundle = Bundle().apply {
                        putString("error_message", task.exception?.localizedMessage ?: "Unknown error")
                    }
                    analytics.logEvent("registration_failure", bundle)
                }
            }
    }

    // Saves additional user data to Firestore in the "User" collection
    private fun saveUserDataToFirestore(userId: String) {
        val userData = mapOf(
            "displayName" to displayName.value,
            "bio" to bio.value,
            "email" to email.value,
            "major" to major.value,
            "preferences" to preferences.value,
            "profilePicture" to profilePictureUrl.value,
            "ratingAverage" to 0,
            "reviewsCount" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        FirebaseFirestoreSingleton.getCollection("User")
            .document(userId)
            .set(userData, SetOptions.merge())
            .addOnCompleteListener { dbTask ->
                if (!dbTask.isSuccessful) {
                    errorMessage.value = dbTask.exception?.localizedMessage
                }
            }
    }
}
