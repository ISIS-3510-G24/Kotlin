package com.example.unimarket.ui.viewmodels

import android.util.LruCache
import androidx.collection.ArrayMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.UniMarketRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class OrderTab { HISTORY, BUYING, SELLING }

data class Order(
    val id:           String,
    val productId:    String,
    val buyerID:      String,
    val sellerID:     String,
    val productTitle: String,
    val imageUrl:     String,
    val orderDate:    Timestamp,
    val price:        Double,
    val status:       String
)

class OrdersViewModel(
    private val repository: UniMarketRepository
) : ViewModel() {
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val PRODUCT_CACHE_SIZE = 50
    private val PRODUCT_CACHE_TTL  = 60

    private val productInfoCache = object : LruCache<String, Pair<String, String>>(PRODUCT_CACHE_SIZE) {}
    private val productInfoMap   = ArrayMap<String, Pair<String, String>>()

    private val _orders    = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>>    = _orders.asStateFlow()

    private val _currentTab = MutableStateFlow(OrderTab.HISTORY)
    val currentTab: StateFlow<OrderTab>   = _currentTab.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean>     = _isLoading.asStateFlow()

    private val _error     = MutableStateFlow<String?>(null)
    val error: StateFlow<String?>         = _error.asStateFlow()

    init {
        loadOrders()
    }

    fun setTab(tab: OrderTab) {
        _currentTab.value = tab
    }

    fun getCurrentUserId(): String? =
        auth.currentUser?.uid

    fun loadOrders() = viewModelScope.launch {
        _isLoading.value = true
        _error.value     = null

        val uid = getCurrentUserId()
        if (uid == null) {
            _error.value     = "User not authenticated"
            _isLoading.value = false
            return@launch
        }

        try {
            val snap = db.collection("orders").get().await()
            val tmp  = mutableListOf<Order>()

            snap.documents.forEach { doc ->
                val d      = doc.data ?: return@forEach
                val buyer  = d["buyerID"]  as? String ?: return@forEach
                val seller = d["sellerID"] as? String ?: return@forEach
                if (buyer != uid && seller != uid) return@forEach

                val pid    = d["productID"] as? String ?: return@forEach
                val date   = d["orderDate"]  as? Timestamp ?: Timestamp.now()
                val price  = (d["price"]      as? Number)?.toDouble() ?: 0.0
                val status = d["status"]     as? String ?: ""

                tmp += Order(
                    id           = doc.id,
                    productId    = pid,
                    buyerID      = buyer,
                    sellerID     = seller,
                    productTitle = "",
                    imageUrl     = "",
                    orderDate    = date,
                    price        = price,
                    status       = status
                )
            }

            val pids    = tmp.map { it.productId }.distinct()
            val toFetch = pids.filter { productInfoCache.get(it) == null }

            if (toFetch.isNotEmpty()) {
                val entities = repository.getProducts(
                    PRODUCT_CACHE_TTL * 60_000L
                ).first()
                entities
                    .filter { it.id in toFetch }
                    .forEach { entity ->
                        val img = entity.imageUrls.firstOrNull().orEmpty()
                        productInfoCache.put(
                            entity.id,
                            entity.title to img
                        )
                    }
            }

            productInfoMap.clear()
            pids.forEach { id ->
                productInfoCache.get(id)?.let { productInfoMap[id] = it }
            }

            _orders.value = tmp.map { o ->
                val (t, img) = productInfoMap[o.productId] ?: ("" to "")
                o.copy(productTitle = t, imageUrl = img)
            }.sortedByDescending { it.orderDate.toDate() }

        } catch (e: Exception) {
            _error.value = e.localizedMessage ?: "Error loading orders"
        } finally {
            _isLoading.value = false
        }
    }
}
