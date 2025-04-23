package com.example.unimarket.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WishlistItem(
    val productId: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val price: Long,
    val available: Boolean
)

class WishlistViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _wishlistItems = MutableStateFlow<List<WishlistItem>>(emptyList())
    val wishlistItems: StateFlow<List<WishlistItem>> = _wishlistItems.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchWishlist()
    }

    fun fetchWishlist() {
        val userId = auth.currentUser?.uid ?: return
        _loading.value = true
        _error.value = null

        firestore.collection("User")
            .document(userId)
            .collection("wishlist")
            .get()
            .addOnSuccessListener { wishDocs ->
                val productIds = wishDocs.documents.map { it.id }.toSet()
                if (productIds.isEmpty()) {
                    _wishlistItems.value = emptyList()
                    _loading.value = false
                    return@addOnSuccessListener
                }
                firestore.collection("Product")
                    .whereIn(FieldPath.documentId(), productIds.toList())
                    .get()
                    .addOnSuccessListener { prodDocs ->
                        val items = prodDocs.documents.mapNotNull { doc ->
                            val id = doc.id
                            val title = doc.getString("title") ?: return@mapNotNull null
                            val desc = doc.getString("description") ?: ""
                            val price = doc.getDouble("price")?.toLong() ?: 0L
                            val urlList = doc.get("imageUrls") as? List<*>
                            val img = (urlList?.firstOrNull() as? String).orEmpty()
                            val status = doc.getString("status") ?: ""
                            WishlistItem(
                                productId = id,
                                title = title,
                                description = desc,
                                imageUrl = img,
                                price = price,
                                available = status.equals("Available", true)
                            )
                        }
                        _wishlistItems.value = items
                        _loading.value = false
                    }
                    .addOnFailureListener { ex ->
                        _error.value = "Error loading products: ${ex.localizedMessage}"
                        _loading.value = false
                    }
            }
            .addOnFailureListener { ex ->
                _error.value = "Error loading wishlist: ${ex.localizedMessage}"
                _loading.value = false
            }
    }

    fun removeFromWishlist(productId: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("User")
            .document(userId)
            .collection("wishlist")
            .document(productId)
            .delete()
            .addOnSuccessListener { fetchWishlist() }
            .addOnFailureListener { ex -> _error.value = "Failed to remove: ${ex.localizedMessage}" }
    }
}
