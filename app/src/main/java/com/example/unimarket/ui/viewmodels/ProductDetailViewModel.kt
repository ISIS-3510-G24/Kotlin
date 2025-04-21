package com.example.unimarket.ui.viewmodels

import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.unimarket.ui.data.FirebaseFirestoreSingleton
import com.example.unimarket.ui.models.Product
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
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

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        fetchProduct()
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
}