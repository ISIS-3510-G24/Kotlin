package com.example.unimarket.ui.viewmodels

import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.UniMarketRepository
import com.example.unimarket.ui.models.Product
import com.example.unimarket.utils.ConnectivityObserver
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: UniMarketRepository,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val crashlytics: FirebaseCrashlytics,
    private val analytics: FirebaseAnalytics,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    companion object {
        private const val TAG = "ProductDetailVM"
    }

    private val productId: String = checkNotNull(savedStateHandle["productId"])

    private val _isInWishlist = MutableStateFlow(false)
    val isInWishlist: StateFlow<Boolean> = _isInWishlist.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product.asStateFlow()

    private val _sellerName = MutableStateFlow<String?>(null)
    val sellerName: StateFlow<String?> = _sellerName.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                _isOnline.value = online
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.getWishlistIds()
                .collect { ids ->
                    _isInWishlist.value = productId in ids
                }
        }

        fetchProductEnDirecto()
    }

    fun toggleWishlist() {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = auth.currentUser?.uid ?: return@launch
            repository.toggleWishlist(uid, productId)
            analytics.logEvent(
                "toggle_wishlist",
                bundleOf(
                    "productId" to productId,
                    "added" to (_isInWishlist.value.not())
                )
            )
        }
    }

    private fun fetchProductEnDirecto() {
        viewModelScope.launch {
            _isLoading.value = true
            analytics.logEvent("screen_load_start", bundleOf("screen" to "ProductDetail"))

            try {
                val docProduct = firestore
                    .collection("Product")
                    .document(productId)
                    .get()
                    .await()

                if (docProduct.exists()) {
                    val id        = docProduct.id
                    val title     = docProduct.getString("title") ?: "Sin título"
                    val desc      = docProduct.getString("description") ?: ""
                    val price     = docProduct.getLong("price") ?: 0L
                    val status    = docProduct.getString("status") ?: ""
                    val majorID   = docProduct.getString("majorID") ?: ""
                    val classId   = docProduct.getString("classId") ?: ""
                    val sellerID  = docProduct.getString("sellerID") ?: ""
                    val labels    = docProduct.get("labels") as? List<String> ?: emptyList()
                    val imageUrls = docProduct.get("imageUrls") as? List<String> ?: emptyList()

                    val uiModel = Product(
                        id          = id,
                        title       = title,
                        description = desc,
                        status      = status,
                        price       = price.toDouble(),
                        majorID     = majorID,
                        classId     = classId,
                        sellerID    = sellerID,
                        labels      = labels,
                        imageUrls   = imageUrls
                    )

                    _product.value = uiModel
                    Log.d(TAG, "fetchProductLive: Obtained Product = $uiModel")

                    fetchSellerName(sellerID)
                } else {
                    _error.value = "Product not found"
                }

                analytics.logEvent(
                    "screen_load_end",
                    bundleOf("screen" to "ProductDetail", "success" to true)
                )

            } catch (e: Exception) {
                crashlytics.recordException(e)
                _error.value = "Error loading product: ${e.localizedMessage}"
                analytics.logEvent(
                    "screen_load_end",
                    bundleOf("screen" to "ProductDetail", "success" to false)
                )
                Log.e(TAG, "fetchProductLive — error reading Product/$productId", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchSellerName(sellerId: String) {
        if (sellerId.isBlank()) {
            _sellerName.value = "Undefined Seller"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val docUser = firestore
                    .collection("User")
                    .document(sellerId)
                    .get()
                    .await()

                Log.d(TAG, "fetchSellerName: docUser.exists = ${docUser.exists()}")
                Log.d(TAG, "fetchSellerName: docUser.data = ${docUser.data}")

                if (docUser.exists()) {
                    val nombre = docUser.getString("displayName")
                    if (!nombre.isNullOrBlank()) {
                        _sellerName.value = nombre
                        Log.d(TAG, "fetchSellerName: displayName='$nombre'")
                    } else {
                        _sellerName.value = "No available name"
                    }
                } else {
                    _sellerName.value = "Seller not found"
                }
            } catch (e: Exception) {
                crashlytics.recordException(e)
                _sellerName.value = "Error on loading seller"
                Log.e(TAG, "fetchSellerName — exception reading User/$sellerId", e)
            }
        }
    }
}
