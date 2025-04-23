package com.example.unimarket.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OrderWithProduct(
    val orderId: String,
    val productTitle: String,
    val hashConfirm: String
)

class ValidateDeliveryViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _orders = MutableStateFlow<List<OrderWithProduct>>(emptyList())
    val orders: StateFlow<List<OrderWithProduct>> = _orders.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchOrders()
    }

    fun fetchOrders() {
        val sellerId = auth.currentUser?.uid ?: return
        _loading.value = true
        _error.value = null

        firestore.collection("orders")
            .whereEqualTo("sellerID", sellerId)
            .get()
            .addOnSuccessListener { orderDocs ->
                val docs = orderDocs.documents
                if (docs.isEmpty()) {
                    _orders.value = emptyList()
                    _loading.value = false
                    return@addOnSuccessListener
                }

                val productIds = docs.mapNotNull { it.getString("productID") }.toSet()
                firestore.collection("Product")
                    .whereIn(FieldPath.documentId(), productIds.toList())
                    .get()
                    .addOnSuccessListener { productDocs ->
                        val titleMap = productDocs.documents.associateBy(
                            { it.id },
                            { it.getString("title") ?: "" }
                        )

                        val result = docs.mapNotNull { doc ->
                            val pid = doc.getString("productID") ?: return@mapNotNull null
                            val title = titleMap[pid] ?: ""
                            val hash = doc.getString("hashConfirm") ?: return@mapNotNull null
                            OrderWithProduct(doc.id, title, hash)
                        }
                        _orders.value = result
                        _loading.value = false
                    }
                    .addOnFailureListener { ex ->
                        _error.value = "Error loading products: ${ex.localizedMessage}"
                        _loading.value = false
                    }
            }
            .addOnFailureListener { ex ->
                _error.value = "Error loading orders: ${ex.localizedMessage}"
                _loading.value = false
            }
    }
}