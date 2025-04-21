package com.example.unimarket.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.unimarket.ui.data.FirebaseFirestoreSingleton
import com.example.unimarket.ui.models.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProductDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

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

    private fun fetchProduct() {
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
            }
            .addOnFailureListener { ex ->
                _error.value = "Error: ${ex.message}"
                _isLoading.value = false
            }
    }
}