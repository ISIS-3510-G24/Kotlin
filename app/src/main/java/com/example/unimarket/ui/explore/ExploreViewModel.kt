package com.example.unimarket.ui.explore

import androidx.lifecycle.ViewModel
import com.example.unimarket.ui.data.FirebaseRealtimeDatabaseSingleton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// This ViewModel loads products from Firebase Realtime Database
class ExploreViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    // State to hold error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        loadProductsFromRealtimeDatabase()
    }

    // This function fetches products from Firebase Realtime Database
    private fun loadProductsFromRealtimeDatabase() {
        val productsRef = FirebaseRealtimeDatabaseSingleton.getReference("Product")
        productsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productList = mutableListOf<Product>()
                // Loop through all children in the snapshot
                for (child in snapshot.children) {
                    val product = child.getValue(Product::class.java)
                    product?.let {
                        productList.add(it.copy(id = child.key ?: ""))
                    }
                }
                _products.value = productList
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
                _errorMessage.value = "Error loading products: ${error.message}"
                // Error logging
                error.toException().printStackTrace()
            }
        })
    }
}
