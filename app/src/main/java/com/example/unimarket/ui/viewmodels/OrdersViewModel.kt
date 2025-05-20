package com.example.unimarket.ui.viewmodels

import android.content.Context
import android.util.LruCache
import androidx.collection.ArrayMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.UniMarketDatabase
import com.example.unimarket.data.UniMarketRepository
import com.example.unimarket.data.entities.OrderEntity
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
    context: Context,
    private val repository: UniMarketRepository
) : ViewModel() {
    private val orderDao = UniMarketDatabase.getInstance(context).orderDao()
    private val db       = FirebaseFirestore.getInstance()
    private val auth     = FirebaseAuth.getInstance()

    private val PRODUCT_CACHE_SIZE = 50
    private val PRODUCT_CACHE_TTL  = 60 * 60_000L

    private val productInfoCache = object : LruCache<String, Pair<String, String>>(PRODUCT_CACHE_SIZE) {}
    private val productInfoMap   = ArrayMap<String, Pair<String, String>>()

    private val _orders    = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>>    = _orders.asStateFlow()

    private val _currentTab = MutableStateFlow(OrderTab.HISTORY)
    val currentTab: StateFlow<OrderTab>   = _currentTab.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
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

            snap.documents
                .mapNotNull { doc ->
                    doc.data?.let { d -> d to doc.id }
                }
                .forEach { (d, id) ->
                    val buyer  = d["buyerID"]  as? String ?: return@forEach
                    val seller = d["sellerID"] as? String ?: return@forEach
                    if (buyer != uid && seller != uid) return@forEach

                    val pid    = d["productID"] as? String ?: return@forEach
                    val date   = d["orderDate"]  as? Timestamp ?: Timestamp.now()
                    val price  = (d["price"]      as? Number)?.toDouble() ?: 0.0
                    val status = d["status"]     as? String ?: ""

                    tmp += Order(
                        id           = id,
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
                val entities = repository.getProducts(PRODUCT_CACHE_TTL).first()
                entities.filter { it.id in toFetch }
                    .forEach { e ->
                        val img = e.imageUrls.firstOrNull().orEmpty()
                        productInfoCache.put(e.id, e.title to img)
                    }
            }

            productInfoMap.clear()
            pids.forEach { id ->
                productInfoCache.get(id)?.let { productInfoMap[id] = it }
            }

            tmp.forEach { order ->
                val entity = OrderEntity(
                    orderId = order.id,
                    buyerId = order.buyerID,
                    sellerId = order.sellerID,
                    productId = order.productId,
                    date     = order.orderDate.toDate().time,
                    price    = order.price,
                    status   = order.status
                )
                orderDao.insert(entity)
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
