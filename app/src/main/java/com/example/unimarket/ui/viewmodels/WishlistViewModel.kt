package com.example.unimarket.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.UniMarketRepository
import com.example.unimarket.di.IoDispatcher
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WishlistItem(
    val productId: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val price: Long,
    val available: Boolean,
)

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val repo: UniMarketRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
    private val auth: FirebaseAuth,
) : ViewModel() {
    private val _items = MutableStateFlow<List<WishlistItem>>(emptyList())
    val wishListItems: StateFlow<List<WishlistItem>> = _items.asStateFlow()

    companion object {
        private const val DEFAULT_CACHE_TTL_MS = 300_000L
    }

    init {
        viewModelScope.launch(io) {
            repo.getWishlistProducts(DEFAULT_CACHE_TTL_MS)
                .map { entities ->
                    entities.map { e->
                        WishlistItem(
                            productId = e.id,
                            title = e.title,
                            description = e.description,
                            imageUrl = e.imageUrls.firstOrNull().orEmpty(),
                            price = e.price.toLong(),
                            available = e.status.equals("Available", true)
                        )
                    }
                }
                .collect { _items.value = it }
        }
    }



    fun removeFromWishlist(productId: String) {
        viewModelScope.launch(io) {
            repo.toggleWishlist(auth.currentUser!!.uid, productId)
        }
    }
}
