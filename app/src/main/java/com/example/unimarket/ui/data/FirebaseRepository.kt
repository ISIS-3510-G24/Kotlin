package com.example.unimarket.ui.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Singleton to handle Firebase interactions
object FirebaseRepository {
    // Lazy initialization of Firebase Auth instance
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // Lazy initialization of Firestore instance
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    // Function to sign in a user using Firebase Auth
    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    // Function to get a reference to the 'Product' collection in Firestore
    fun getProductCollection() = firestore.collection("Product")

    fun getUsersCollection() = firestore.collection("User")
}
