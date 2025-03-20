package com.example.unimarket.ui.register

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.unimarket.ui.data.FirebaseFirestoreSingleton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
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

    // Profile picture URL state
    // Default value: a default profile picture URL
    val profilePictureUrl = mutableStateOf("https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Default_pfp.svg/2048px-Default_pfp.svg.png")

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    // Uploads profile picture to Firebase Storage and updates profilePictureUrl
    fun uploadProfilePicture(imageUri: Uri, onComplete: (Boolean) -> Unit) {
        // Create a unique reference for the image (e.g., using user ID and timestamp)
        val userId = auth.currentUser?.uid ?: "unknown_user"
        val ref: StorageReference = storage.reference.child("profile_pictures/${userId}_${System.currentTimeMillis()}.jpg")
        ref.putFile(imageUri)
            .addOnSuccessListener {
                // Get the download URL
                ref.downloadUrl.addOnSuccessListener { uri ->
                    profilePictureUrl.value = uri.toString()
                    onComplete(true)
                }.addOnFailureListener {
                    onComplete(false)
                }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun registerUser() {
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
                } else {
                    registerSuccess.value = false
                    errorMessage.value = task.exception?.localizedMessage
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
