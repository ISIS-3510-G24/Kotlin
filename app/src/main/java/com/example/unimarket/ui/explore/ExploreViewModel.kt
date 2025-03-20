package com.example.unimarket.ui.explore

import androidx.lifecycle.ViewModel
import com.example.unimarket.ui.data.FirebaseFirestoreSingleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// This ViewModel loads products from Firestore and handles errors
class ExploreViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        loadProductsFromFirestore()
    }

    // This function fetches products from Firestore
    private fun loadProductsFromFirestore() {
        FirebaseFirestoreSingleton.getCollection("Product")
            .get()
            .addOnSuccessListener { result ->
                val productList = result.documents.mapNotNull { doc ->
                    // Convert document to Product object and assign document id
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }
                _products.value = productList
            }
            .addOnFailureListener { exception ->
                _errorMessage.value = "Error loading products: ${exception.message}"
            }
    }
}
