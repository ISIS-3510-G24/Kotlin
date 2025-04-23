package com.example.unimarket.ui.viewmodels

import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.unimarket.data.FirebaseFirestoreSingleton
import com.example.unimarket.ui.models.Product
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProductDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val performance = FirebasePerformance.getInstance()
    private val analytics: FirebaseAnalytics = Firebase.analytics
    private var trace: Trace? = null

    private val productId: String = checkNotNull(savedStateHandle["productId"])

    private val _isInWishlist = MutableStateFlow(false)
    val isInWishlist: StateFlow<Boolean> = _isInWishlist

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        fetchProduct()
        loadWishlistState()
    }

    private fun loadWishlistState() {
        val userId = getCurrentUserId()
        FirebaseFirestoreSingleton
            .getCollection("User")
            .document(userId)
            .collection("wishlist")
            .document(productId)
            .get()
            .addOnSuccessListener { doc ->
                _isInWishlist.value = doc.exists()
            }
    }

    fun toggleWishlist() {
        val userId = getCurrentUserId()
        val docRef = FirebaseFirestoreSingleton
            .getCollection("User")
            .document(userId)
            .collection("wishlist")
            .document(productId)

        if (_isInWishlist.value) {
            docRef.delete()
                .addOnSuccessListener { _isInWishlist.value = false }
        } else {
            docRef.set(mapOf("addedAt" to FieldValue.serverTimestamp()))
                .addOnSuccessListener { _isInWishlist.value = true }
        }
    }

    fun onScreenLoadStart() {
        trace = performance.newTrace("load_ProductDetailScreen").apply { start() }
        analytics.logEvent("screen_load_start", bundleOf("screen" to "ProductDetail"))
    }
    fun onScreenLoadEnd(success: Boolean = true) {
        trace?.stop()
        analytics.logEvent(
            "screen_load_end",
            bundleOf("screen" to "ProductDetail", "success" to success)
        )
    }

    private fun fetchProduct() {
        onScreenLoadStart()
        _isLoading.value = true
        FirebaseFirestoreSingleton
            .getCollection("Product")
            .document(productId)
            .get()
            .addOnSuccessListener { doc ->
                val prod = doc.toObject(Product::class.java)
                if (prod != null) {
                    _product.value = prod.copy(id = doc.id)
                } else {
                    _error.value = "Product not found."
                }
                _isLoading.value = false
                onScreenLoadEnd(true)
            }
            .addOnFailureListener { ex ->
                _error.value = "Error: ${ex.message}"
                _isLoading.value = false
                onScreenLoadEnd(false)
            }
    }

    private fun getCurrentUserId(): String =
        FirebaseAuth.getInstance().currentUser?.uid ?: throw IllegalStateException("No user")
}