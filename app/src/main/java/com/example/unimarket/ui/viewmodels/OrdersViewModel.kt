package com.example.unimarket.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class OrderTab { HISTORY, BUYING, SELLING }

data class Order(
    val id: String,
    val buyerID: String,
    val sellerID: String,
    val productTitle: String,
    val imageUrl: String,
    val orderDate: Timestamp,
    val price: Double,
    val status: String
)

class OrdersViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentTab = MutableStateFlow(OrderTab.HISTORY)
    val currentTab: StateFlow<OrderTab> = _currentTab.asStateFlow()

    fun setTab(tab: OrderTab) {
        _currentTab.value = tab
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            val userId = getCurrentUserId()
            if (userId == null) {
                _error.value = "User not authenticated"
                _loading.value = false
                return@launch
            }

            try {
                val snapshot = db.collection("orders").get().await()
                val ordersList = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val buyerID = data["buyerID"] as? String ?: return@mapNotNull null
                    val sellerID = data["sellerID"] as? String ?: return@mapNotNull null
                    val productID = data["productID"] as? String ?: return@mapNotNull null
                    val orderDate = data["orderDate"] as? Timestamp ?: Timestamp.now()
                    val price = (data["price"] as? Number)?.toDouble() ?: 0.0
                    val status = data["status"] as? String ?: ""

                    Triple(
                        Order(
                            id = doc.id,
                            buyerID = buyerID,
                            sellerID = sellerID,
                            productTitle = "", // Placeholder
                            imageUrl = "", // Placeholder
                            orderDate = orderDate,
                            price = price,
                            status = status
                        ), productID, doc.id
                    )
                }

                val productIDs = ordersList.map { it.second }.distinct()
                val productDocs = if (productIDs.isNotEmpty()) {
                    db.collection("Product").whereIn(FieldPath.documentId(), productIDs).get().await().documents
                } else emptyList()

                val productMap = productDocs.associateBy({ it.id }, {
                    val title = it.getString("title") ?: ""
                    val images = it.get("imageUrls") as? List<*> ?: emptyList<Any>()
                    val firstImage = images.firstOrNull() as? String ?: ""
                    Pair(title, firstImage)
                })

                _orders.value = ordersList.mapNotNull { (order, pid, _) ->
                    val product = productMap[pid] ?: return@mapNotNull null
                    order.copy(productTitle = product.first, imageUrl = product.second)
                }
                _loading.value = false
            } catch (e: Exception) {
                _error.value = e.localizedMessage
                _loading.value = false
            }
        }
    }
}