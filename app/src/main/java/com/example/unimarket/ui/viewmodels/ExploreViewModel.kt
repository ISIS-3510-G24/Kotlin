package com.example.unimarket.ui.viewmodels

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.UniMarketRepository
import com.example.unimarket.di.IoDispatcher
import com.example.unimarket.ui.models.Product
import com.example.unimarket.ui.models.User
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

// This ViewModel loads products from Firestore and handles errors
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repo: UniMarketRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val analytics: FirebaseAnalytics,
    private val performance: FirebasePerformance,
    private val crashlytics: FirebaseCrashlytics,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : ViewModel() {

    companion object {
        private const val DEFAULT_CACHE_TTL_MS = 3_600_000L
    }

    private var uploadingRemotePath: String? = null
    private var loadTrace: Trace? = null

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String?>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val errorMessage: SharedFlow<String?> = _errorMessage.asSharedFlow()

    // Loading state to indicate if data is being refreshed
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    sealed class UIEvent {
        data class ShowMessage(val message: String) : UIEvent()
        object ProductPublished : UIEvent()
    }

    private val _uiEvent = MutableSharedFlow<UIEvent>(extraBufferCapacity = 1)
    val uiEvent: SharedFlow<UIEvent> = _uiEvent.asSharedFlow()

    // User preferences for filtering products
    private val _userPreferences = MutableStateFlow<List<String>>(emptyList())
    val userPreferences: StateFlow<List<String>> = _userPreferences

    private val _wishlistIds = MutableStateFlow<Set<String>>(emptySet())
    val wishlistIds: StateFlow<Set<String>> = _wishlistIds.asStateFlow()

    private val _uploadState = MutableStateFlow<ImageUploadState>(ImageUploadState.Idle)
    val uploadState: StateFlow<ImageUploadState> = _uploadState.asStateFlow()

    private val handler = CoroutineExceptionHandler { _, throwable ->
        crashlytics.recordException(throwable)
        viewModelScope.launch { _errorMessage.emit(throwable.message ?: "Unknown Error") }
    }

    init {
        observeWishlist()
        loadUserPreferences()
        loadProducts()
        viewModelScope.launch(ioDispatcher) {
            repo.observeImageCacheEntries()
                .filter { entries -> entries.any { it.state != "PENDING" } }
                .collect { entries ->
                    uploadingRemotePath?.let { path ->
                        entries
                            .find { it.remotePath == path && it.state != "PENDING" }
                            ?.let { entry ->
                                when (entry.state) {
                                    "SUCCESS" -> _uploadState.value = ImageUploadState.Success(entry.downloadUrl!!)
                                    "FAILED"  -> _uploadState.value = ImageUploadState.Failed(entry.localUri)
                                }

                                viewModelScope.launch(ioDispatcher) {
                                    repo.clearImageCacheEntry(entry)
                                }

                                uploadingRemotePath = null
                            }
                    }
                }
        }
    }

    private fun observeWishlist() {
        viewModelScope.launch(ioDispatcher + handler) {
            repo.getWishlistIds()
                .catch { e -> crashlytics.recordException(e) }
                .collect { _wishlistIds.value = it }
        }
    }

    fun toggleWishlist(productId: String) {
        viewModelScope.launch(ioDispatcher + handler) {
            auth.currentUser?.uid?.let { uid ->
                repo.toggleWishlist(uid, productId)
                analytics.logEvent(
                    "toggle_wishlist",
                    bundleOf(
                        "product_id" to productId,
                        "added" to (_wishlistIds.value.contains(productId).not())
                    )
                )
            }
        }
    }

    fun onScreenLoadStart() {
        loadTrace = performance
            .newTrace("load_ExploreScreen")
            .apply { start() }
        analytics.logEvent("screen_load_start", bundleOf("screen" to "Explore"))
    }

    fun onScreenLoadEnd(success: Boolean = true) {
        loadTrace?.stop()
        analytics.logEvent(
            "screen_load_end",
            bundleOf("screen" to "Explore", "success" to success)
        )
    }

    //
    fun loadProducts(cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS) {
        viewModelScope.launch(ioDispatcher + handler) {
            val trace = performance.newTrace("load_ExploreScreen").apply { start() }
            analytics.logEvent("screen_load_start", bundleOf("screen" to "Explore"))

            _isLoading.value = true
            repo.getProducts(cacheTtlMs)
                .catch { e ->
                    crashlytics.recordException(e)
                    _errorMessage.emit("Could not load products: ${e.message}")
                    analytics.logEvent(
                        "load_products_failure",
                        bundleOf("error_message" to (e.message ?: ""))
                    )
                    _isLoading.value = false
                    trace.stop()
                }
                .collect { entities ->
                    _products.value = entities.map { ent ->
                        Product(
                            id = ent.id,
                            title = ent.title,
                            description = ent.description,
                            imageUrls = ent.imageUrls,
                            labels = ent.labels,
                            price = ent.price,
                            status = ent.status
                        )
                    }
                    analytics.logEvent(
                        "load_products_success",
                        bundleOf("product_count" to entities.size)
                    )
                    _isLoading.value = false
                    trace.stop()
                }
        }
    }

    private fun loadUserPreferences() {
        viewModelScope.launch(ioDispatcher + handler) {
            try {
                val uid = getCurrentUserId()
                val doc = firestore.collection("User").document(uid).get().await()
                val user = doc.toObject(User::class.java)
                _userPreferences.value = user?.preferences ?: emptyList()
            } catch (e: Exception) {
                crashlytics.recordException(e)
                _errorMessage.emit("Error loading user preferences: ${e.message}")
            }
        }
    }

    fun publishProduct(product: Product) {
        viewModelScope.launch(ioDispatcher + handler) {
            _uiEvent.emit(UIEvent.ShowMessage("Publishing product…"))
            try {
                firestore.collection("Product").add(product).await()
                _uiEvent.emit(UIEvent.ShowMessage("Product published successfully"))
                withContext(Dispatchers.Main) {
                    _uiEvent.emit(UIEvent.ProductPublished)
                    resetUploadState()
                }
            } catch (e: Exception) {
                crashlytics.recordException(e)
                _uiEvent.emit(UIEvent.ShowMessage("Failed to publish product: ${e.message}"))
            }
        }
    }

    // Public function to refresh products (when a shake gesture is detected)
    fun refreshProducts() {
        analytics.logEvent("refresh_products", Bundle()) // Log refresh event
        loadProducts()
    }

    private fun observeImageCache() {
        viewModelScope.launch(ioDispatcher + handler) {
            repo.observeImageCacheEntries()
                .filter { list -> list.any { it.state != "PENDING" } }
                .collect { list ->
                    list.find { it.state != "PENDING" }?.let { entry ->
                        when (entry.state) {
                            "SUCCESS" -> _uploadState.value = ImageUploadState.Success(entry.downloadUrl!!)
                            "FAILED"  -> _uploadState.value = ImageUploadState.Failed(entry.localUri)
                        }
                    }
                }
        }
    }

    fun resetUploadState() {
        _uploadState.value = ImageUploadState.Idle
    }


    fun uploadProductImage(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val path = "product_images/$userId/${System.currentTimeMillis()}.jpg"

        uploadingRemotePath = path

        Log.d("Publish", "uploadProductImage → uri=$uri, path=$path")
        _uploadState.value = ImageUploadState.Pending(uri.toString(), path)
        viewModelScope.launch(ioDispatcher + handler) {
            repo.uploadImage(uri.toString(), path)
        }
    }

    fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "defaultUserId"
    }
}


sealed class ImageUploadState {
    object Idle : ImageUploadState()
    data class Pending(val localUri: String, val remotePath: String) : ImageUploadState()
    data class Success(val remotePath: String) : ImageUploadState()
    data class Failed(val remotePath: String) : ImageUploadState()
}