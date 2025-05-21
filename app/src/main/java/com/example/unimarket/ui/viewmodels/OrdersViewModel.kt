// OrdersViewModel.kt
package com.example.unimarket.ui.viewmodels

import android.content.Context
import androidx.collection.ArrayMap
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.UniMarketDatabase
import com.example.unimarket.data.UniMarketRepository
import com.example.unimarket.data.entities.OrderEntity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.dataStore by preferencesDataStore("orders_prefs")

enum class OrderTab { HISTORY, BUYING, SELLING }

data class Order(
    val id: String,
    val productId: String,
    val buyerID: String,
    val sellerID: String,
    val productTitle: String,
    val imageUrl: String,
    val orderDate: Timestamp,
    val price: Double,
    val status: String
)

class OrdersViewModel(
    private val context: Context,
    private val repository: UniMarketRepository
) : ViewModel() {

    // ROOM DAO
    private val orderDao = UniMarketDatabase.getInstance(context).orderDao()
    // Firebase
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val gson = Gson()

    // In-memory LRU cache for product info
    private val PRODUCT_CACHE_SIZE = 50
    private val PRODUCT_CACHE_TTL  = 60 * 60_000L
    private val productInfoCache = object : androidx.collection.LruCache<String, Pair<String,String>>(PRODUCT_CACHE_SIZE) {}
    private val productInfoMap   = ArrayMap<String,Pair<String,String>>()

    // UI StateFlows
    private val _orders     = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _currentTab = MutableStateFlow(OrderTab.HISTORY)
    val currentTab: StateFlow<OrderTab> = _currentTab.asStateFlow()

    private val _isLoading  = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error      = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // DataStore for last-refresh + last-tab
    private val prefs = context.dataStore

    // last refresh formatted
    val lastRefresh: StateFlow<String> = prefs.data
        .map { it[longPreferencesKey("last_refresh")] ?: 0L }
        .map { ts ->
            if (ts == 0L) "Never"
            else SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "…")

    init {
        // Restore last-tab (nested coroutine: DataStore IO + update on Main)
        viewModelScope.launch {
            prefs.data
                .map { it[stringPreferencesKey("last_tab")] }
                .filterNotNull()
                .map { OrderTab.valueOf(it) }
                .collect { tab ->
                    _currentTab.value = tab
                }
        }
        loadOrders() // kick off initial load
    }

    /** Expose current user ID (synchronous) */
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /** Switch tab and persist in DataStore */
    fun setTab(tab: OrderTab) {
        _currentTab.value = tab
        viewModelScope.launch {
            prefs.edit { it[stringPreferencesKey("last_tab")] = tab.name }
        }
    }

    /**
     * Load orders from Firestore, cache in Room + JSON file + DataStore timestamp.
     * Demonstrates:
     * 1) coroutine on IO dispatcher
     * 2) nested coroutines (async + withContext)
     * 3) switching back to Main for UI updates
     */
    fun loadOrders() = viewModelScope.launch(Dispatchers.IO) {
        _isLoading.value = true
        _error.value     = null

        val uid = getCurrentUserId()
        if (uid == null) {
            // Back on Main thread automatically for StateFlow updates
            _error.value     = "User not signed in"
            _isLoading.value = false
            return@launch
        }

        val cacheFile = File(context.filesDir, "orders_cache.json")

        try {
            // 1) Fetch list of order docs (IO)
            val snap = db.collection("orders").get().await()
            val tmp  = mutableListOf<Order>()
            snap.documents.mapNotNull { doc -> doc.data?.let { it to doc.id } }
                .forEach { (d,id) ->
                    val buyer  = d["buyerID"] as? String ?: return@forEach
                    val seller = d["sellerID"] as? String ?: return@forEach
                    if (buyer != uid && seller != uid) return@forEach
                    tmp += Order(
                        id           = id,
                        productId    = d["productID"] as? String ?: return@forEach,
                        buyerID      = buyer,
                        sellerID     = seller,
                        productTitle = "",
                        imageUrl     = "",
                        orderDate    = d["orderDate"] as? Timestamp ?: Timestamp.now(),
                        price        = (d["price"] as? Number)?.toDouble() ?: 0.0,
                        status       = d["status"] as? String ?: ""
                    )
                }

            // 2) Nested coroutine: fetch missing product info in parallel
            val toFetch = tmp.map { it.productId }.distinct().filter { productInfoCache[it] == null }
            if (toFetch.isNotEmpty()) {
                // launch async fetch on IO again
                val deferred = toFetch.map { pid ->
                    async {
                        repository.getProductByIdCached(pid, PRODUCT_CACHE_TTL).first()
                    }
                }
                // await all
                deferred.forEach { prod ->
                    val p = prod.await()
                    productInfoCache.put(p.id, p.title to p.imageUrls.firstOrNull().orEmpty())
                }
            }

            // build map for quick lookup
            productInfoMap.clear()
            tmp.map { it.productId }.distinct().forEach { pid ->
                productInfoCache.get(pid)?.let { productInfoMap[pid] = it }
            }

            // 3) Persist each order in Room (IO)
            tmp.forEach { o ->
                orderDao.insert(OrderEntity(
                    orderId  = o.id,
                    buyerId  = o.buyerID,
                    sellerId = o.sellerID,
                    productId= o.productId,
                    date     = o.orderDate.toDate().time,
                    price    = o.price,
                    status   = o.status
                ))
            }

            // 4) Back to Main for final UI update
            withContext(Dispatchers.Main) {
                val finalList = tmp.map { o ->
                    val (t,img) = productInfoMap[o.productId] ?: ("" to "")
                    o.copy(productTitle = t, imageUrl = img)
                }.sortedByDescending { it.orderDate.toDate() }
                _orders.value = finalList

                // write JSON cache (IO—but small file)
                cacheFile.writeText(gson.toJson(finalList))

                // persist last refresh
                prefs.edit { it[longPreferencesKey("last_refresh")] = System.currentTimeMillis() }
            }

        } catch (e: Exception) {
            // on error, fallback to JSON cache
            if (cacheFile.exists()) {
                val cached = gson.fromJson(cacheFile.readText(), Array<Order>::class.java).toList()
                _orders.value = cached
            } else {
                _error.value = e.localizedMessage ?: "Unknown error"
            }
        } finally {
            _isLoading.value = false
        }
    }
}
