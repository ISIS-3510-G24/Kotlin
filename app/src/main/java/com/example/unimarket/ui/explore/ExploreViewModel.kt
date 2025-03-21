package com.example.unimarket.ui.explore

import android.os.Bundle
import androidx.lifecycle.ViewModel
import com.example.unimarket.ui.data.FirebaseFirestoreSingleton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// This ViewModel loads products from Firestore and handles errors
class ExploreViewModel : ViewModel() {
    // Firebase Analytics instance
    private val analytics: FirebaseAnalytics = Firebase.analytics

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Loading state to indicate if data is being refreshed
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // User preferences for filtering products
    private val _userPreferences = MutableStateFlow<List<String>>(emptyList())
    val userPreferences: StateFlow<List<String>> = _userPreferences

    init {
        loadProductsFromFirestore()
        loadUserPreferences()
    }

    // This function fetches products from Firestore
    private fun loadProductsFromFirestore() {
        _isLoading.value = true
        FirebaseFirestoreSingleton.getCollection("Product")
            .get()
            .addOnSuccessListener { result ->
                val productList = result.documents.mapNotNull { doc ->
                    // Convert document to Product object and assign document id
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }
                _products.value = productList
                _isLoading.value = false // Data loaded, set loading state to false
                // Log successful product load event
                val bundle = Bundle().apply {
                    putLong("product_count", productList.size.toLong())
                }
                analytics.logEvent("load_products_success", bundle)
            }
            .addOnFailureListener { exception ->
                FirebaseCrashlytics.getInstance().recordException(exception)
                _errorMessage.value = "Error loading products: ${exception.message}"
                _isLoading.value = false // Error occurred, set loading state to false

                // Log failed product load event with error message
                val bundle = Bundle().apply {
                    putString("error_message", exception.message ?: "Unknown error")
                }
                analytics.logEvent("load_products_failure", bundle)
            }
    }

    private fun loadUserPreferences() {
        // Get user preferences from Firestore
        val currentUserId = getCurrentUserId()
        FirebaseFirestoreSingleton.getCollection("User")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                _userPreferences.value = user?.preferences ?: emptyList()
            }
            .addOnFailureListener { exception ->
                FirebaseCrashlytics.getInstance().recordException(exception)
                _errorMessage.value = "Error loading user preferences: ${exception.message}"
            }
    }

    // Public function to refresh products (when a shake gesture is detected)
    fun refreshProducts() {
        analytics.logEvent("refresh_products", Bundle()) // Log refresh event
        loadProductsFromFirestore()
    }

    fun publishProduct(
        product: Product,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        _isLoading.value = true

        // Log product publish event with product title
        val attemptBundle = Bundle().apply {
            putString("product_title", product.title)
        }
        analytics.logEvent("publish_product_attempt", attemptBundle)

        FirebaseFirestoreSingleton.getCollection("Product")
            .add(product)
            .addOnSuccessListener {
                loadProductsFromFirestore()

                // Log successful product publish event with product title
                val successBundle = Bundle().apply {
                    putString("product_title", product.title)
                }
                analytics.logEvent("publish_product_success", successBundle)

                onSuccess()
            }
            .addOnFailureListener { exception ->
                FirebaseCrashlytics.getInstance().recordException(exception)
                onFailure("Error publishing product: ${exception.message}")
                _isLoading.value = false

                // Log failed product publish event with error message
                val failureBundle = Bundle().apply {
                    putString("error_message", exception.message ?: "Unknown error")
                }
                analytics.logEvent("publish_product_failure", failureBundle)
            }
    }

    fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "defaultUserId"
    }
}
