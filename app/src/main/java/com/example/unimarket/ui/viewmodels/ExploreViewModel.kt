package com.example.unimarket.ui.viewmodels

import android.net.Uri
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.UniMarketRepository
import com.example.unimarket.di.IoDispatcher
import com.example.unimarket.ui.models.Product
import com.example.unimarket.ui.models.User
import com.example.unimarket.utils.ConnectivityObserver
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    val isOnline: Flow<Boolean> = connectivityObserver.isOnline

    companion object {
        private const val DEFAULT_CACHE_TTL_MS = 300_000L
    }


    private var uploadingRemotePath: String? = null
    private var loadTrace: Trace? = null

    val products: StateFlow<List<Product>> =
        repo.getProducts(DEFAULT_CACHE_TTL_MS)
            .map { entities ->
                entities.map { ent ->
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
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )


    private val _recommendations = MutableStateFlow<List<String>>(emptyList())
    val recommendations: StateFlow<List<String>> = _recommendations.asStateFlow()

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
        observeRecommendations()
        loadUserPreferences()
        observeImageCache()
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
            val uid = auth.currentUser!!.uid
            val added = !_wishlistIds.value.contains(productId)
            repo.toggleWishlist(uid, productId)
            analytics.logEvent(
                if (added) "add_to_wishlist" else "remove_from_wishlist",
                bundleOf("product_id" to productId)
            )
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

    private fun observeRecommendations() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("recommendations")
            .document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    crashlytics.recordException(err)
                    return@addSnapshotListener
                }
                val list = snap?.get("products") as? List<String> ?: emptyList()
                _recommendations.value = list
            }
    }


    private fun observeImageCache() =
        viewModelScope.launch(ioDispatcher + handler) {
            repo.observeImageCacheEntries()
                .filter { it.any { entry -> entry.state != "PENDING" } }
                .collect { entries ->
                    uploadingRemotePath?.let { path ->
                        entries.firstOrNull { it.remotePath == path && it.state != "PENDING" }
                            ?.also { entry ->
                                _uploadState.value = when (entry.state) {
                                    "SUCCESS" -> ImageUploadState.Success(entry.downloadUrl!!)
                                    "FAILED"  -> ImageUploadState.Failed(entry.localUri)
                                    else      -> _uploadState.value
                                }
                                repo.clearImageCacheEntry(entry)
                                uploadingRemotePath = null
                            }
                    }
                }
        }

    fun resetUploadState() {
        _uploadState.value = ImageUploadState.Idle
    }


    fun uploadProductImage(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val path = "product_images/$uid/${System.currentTimeMillis()}.jpg"
        uploadingRemotePath = path
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