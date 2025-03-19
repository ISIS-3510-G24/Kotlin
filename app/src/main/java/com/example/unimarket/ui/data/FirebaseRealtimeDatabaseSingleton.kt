package com.example.unimarket.ui.data

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseRealtimeDatabaseSingleton {
    // Create a singleton of the Firebase Realtime Database
    val databaseInstance: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    // Get a reference to the root of the Firebase Realtime Database
    val rootReference: DatabaseReference by lazy {
        databaseInstance.reference
    }

    // Get a reference to a specific child path in the Firebase Realtime Database
    fun getReference(childPath: String): DatabaseReference {
        return rootReference.child(childPath)
    }
}