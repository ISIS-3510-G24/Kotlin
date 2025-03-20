package com.example.unimarket.ui.data

import com.google.firebase.firestore.FirebaseFirestore

object FirebaseFirestoreSingleton {
    // Create a singleton for Firestore
    val firestoreInstance: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    // Get a reference to a specific collection in Firestore
    fun getCollection(collectionName: String) = firestoreInstance.collection(collectionName)
}
