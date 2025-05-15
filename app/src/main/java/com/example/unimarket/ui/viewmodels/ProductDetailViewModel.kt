package com.example.unimarket.ui.viewmodels

import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.UniMarketRepository
import com.example.unimarket.di.IoDispatcher
import com.example.unimarket.ui.models.Product
import com.example.unimarket.utils.ConnectivityObserver
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: UniMarketRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val performance: FirebasePerformance,
    private val analytics: FirebaseAnalytics,
    private val crashlytics: FirebaseCrashlytics,
    private val auth: FirebaseAuth,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    companion object {
        private const val DEFAULT_CACHE_TTL_MS = 3_600_000L // 1 hour
    }

    private val productId: String = checkNotNull(savedStateHandle["productId"])

    // Exposed states
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isInWishlist = MutableStateFlow(false)
    val isInWishlist: StateFlow<Boolean> = _isInWishlist

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var trace: Trace? = null

    private val handler = CoroutineExceptionHandler { _, exception ->
        crashlytics.recordException(exception)
        viewModelScope.launch(Dispatchers.Main) {
            _error.value = exception.message
        }
    }

    init {
        // Observe connectivity changes
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { _isOnline.value = it }
        }

        // Load local wishlist state
        viewModelScope.launch(ioDispatcher + handler) {
            repository.getWishlistIds()
                .collect { ids -> _isInWishlist.value = productId in ids}
        }

        // Load product details
        fetchProduct()
    }


    fun toggleWishlist() {
        viewModelScope.launch(ioDispatcher + handler) {
            val uid = auth.currentUser?.uid ?: return@launch
            repository.toggleWishlist(uid, productId)
            analytics.logEvent("toggle_wishlist", bundleOf(
                "productId" to productId,
                "added" to (_isInWishlist.value.not())
                ))
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

    private fun fetchProduct(cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS) {
        viewModelScope.launch(ioDispatcher + handler) {
            onScreenLoadStart()
            withContext(Dispatchers.Main) {
                _isLoading.value = true
                trace = performance.newTrace("load_ProductDetailScreen").apply { start() }
                analytics.logEvent("screen_load_start",
                    bundleOf("screen" to "ProductDetail")
                )
            }

            try {
                // IO: Fetch product details from the repository
                val entity = repository.getProductByIdCached(productId, cacheTtlMs).first()

                // Default: map entity to UI model
                val uiModel = withContext(Dispatchers.Default) {
                    Product(
                        id = entity.id,
                        title = entity.title,
                        description = entity.description,
                        price = entity.price,
                        imageUrls = entity.imageUrls,
                        labels = entity.labels,
                        status = entity.status,
                        majorID = entity.majorID,
                        classId = entity.classId,
                        sellerID = entity.sellerID,
                    )
                }

                // Main: Update UI state
                withContext(Dispatchers.Main) {
                    _product.value = uiModel
                    _error.value = null
                    trace?.stop()
                    analytics.logEvent(
                        "screen_load_end",
                        bundleOf("screen" to "ProductDetail", "success" to true)
                    )
                }
                onScreenLoadEnd(success = true)
            } catch (e: Exception) {
                crashlytics.recordException(e)
                withContext(Dispatchers.Main) {
                    _error.value = e.message ?: "Unknown error at fetching product"
                    trace?.stop()
                    analytics.logEvent(
                        "screen_load_end",
                        bundleOf("screen" to "ProductDetail", "success" to false)
                    )
                    onScreenLoadEnd(success = false)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }
}