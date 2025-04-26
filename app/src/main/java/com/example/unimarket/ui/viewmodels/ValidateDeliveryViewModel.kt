package com.example.unimarket.ui.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OrderWithProduct(
    val orderId: String,
    val productTitle: String,
    val hashConfirm: String
)

class ValidateDeliveryViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _orders = MutableStateFlow<List<OrderWithProduct>>(emptyList())
    val orders: StateFlow<List<OrderWithProduct>> = _orders.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val qrCache = mutableMapOf<String, Bitmap>()
    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap.asStateFlow()

    init {
        fetchOrders()
    }

    fun fetchOrders() {
        val sellerId = auth.currentUser?.uid ?: return
        _loading.value = true
        _error.value = null

        firestore.collection("orders")
            .whereEqualTo("sellerID", sellerId)
            .get()
            .addOnSuccessListener { orderDocs ->
                val docs = orderDocs.documents
                if (docs.isEmpty()) {
                    _orders.value = emptyList()
                    _loading.value = false
                    return@addOnSuccessListener
                }

                val productIds = docs.mapNotNull { it.getString("productID") }.toSet()
                firestore.collection("Product")
                    .whereIn(FieldPath.documentId(), productIds.toList())
                    .get()
                    .addOnSuccessListener { productDocs ->
                        val titleMap = productDocs.documents.associateBy(
                            { it.id },
                            { it.getString("title") ?: "" }
                        )

                        val result = docs.mapNotNull { doc ->
                            val pid = doc.getString("productID") ?: return@mapNotNull null
                            val title = titleMap[pid] ?: ""
                            val hash = doc.getString("hashConfirm") ?: return@mapNotNull null
                            OrderWithProduct(doc.id, title, hash)
                        }
                        _orders.value = result
                        _loading.value = false
                    }
                    .addOnFailureListener { ex ->
                        _error.value = "Error loading products: ${ex.localizedMessage}"
                        _loading.value = false
                    }
            }
            .addOnFailureListener { ex ->
                _error.value = "Error loading orders: ${ex.localizedMessage}"
                _loading.value = false
            }
    }


    fun loadQrBitmap(order: OrderWithProduct) {
        viewModelScope.launch {
            val key = order.hashConfirm
            qrCache[key]?.let { cached ->
                _qrBitmap.value = cached
                return@launch
            }
            val generated = withContext(Dispatchers.Default) {
                generateQrBitmap(order.hashConfirm)
            }
            qrCache[key] = generated
            _qrBitmap.value = generated
        }
    }

    private fun generateQrBitmap(data: String): Bitmap {
        val size = 512
        val writer = QRCodeWriter()
        val matrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size)
        return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also { bmp ->
            for (x in 0 until size) for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
    }
}