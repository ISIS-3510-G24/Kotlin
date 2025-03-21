package com.example.unimarket.ui.explore

import androidx.lifecycle.ViewModel
import com.example.unimarket.ui.data.FirebaseFirestoreSingleton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// This ViewModel loads products from Firestore and handles errors
class ExploreViewModel : ViewModel() {
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
            }
            .addOnFailureListener { exception ->
                FirebaseCrashlytics.getInstance().recordException(exception)
                _errorMessage.value = "Error loading products: ${exception.message}"
                _isLoading.value = false // Error occurred, set loading state to false
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
        loadProductsFromFirestore()
    }

    fun publishProduct(
        product: Product,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        _isLoading.value = true
        FirebaseFirestoreSingleton.getCollection("Product")
            .add(product)
            .addOnSuccessListener {
                loadProductsFromFirestore()
                onSuccess()
            }
            .addOnFailureListener { exception ->
                FirebaseCrashlytics.getInstance().recordException(exception)
                onFailure("Error publishing product: ${exception.message}")
                _isLoading.value = false
            }
    }

    fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "defaultUserId"
    }
}
