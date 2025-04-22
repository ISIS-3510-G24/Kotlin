package com.tuapp.ui.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// --- DATA CLASS ---
data class OrderWithProduct(
    val orderId: String,
    val productTitle: String,
    val hashConfirm: String
)

// --- VIEWMODEL ---
class ValidateDeliveryViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _orders = MutableStateFlow<List<OrderWithProduct>>(emptyList())
    val orders: StateFlow<List<OrderWithProduct>> = _orders.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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
                val orders = orderDocs.documents
                if (orders.isEmpty()) {
                    _orders.value = emptyList()
                    _loading.value = false
                    return@addOnSuccessListener
                }

                val productIds = orders.mapNotNull { it.getString("productID") }.toSet()

                firestore.collection("Product")
                    .whereIn(FieldPath.documentId(), productIds.toList())
                    .get()
                    .addOnSuccessListener { productDocs ->
                        val productMap = productDocs.documents.associateBy(
                            { it.id },
                            { it.getString("title") ?: "Product without title" }
                        )

                        val orderList = orders.mapNotNull { doc ->
                            val productId = doc.getString("productID") ?: return@mapNotNull null
                            val title = productMap[productId] ?: "Product without title"
                            val hash = doc.getString("hashConfirm") ?: return@mapNotNull null
                            OrderWithProduct(doc.id, title, hash)
                        }

                        _orders.value = orderList
                        _loading.value = false
                    }
                    .addOnFailureListener { ex ->
                        _error.value = "Error on obtaining product: ${ex.localizedMessage}"
                        _loading.value = false
                    }
            }
            .addOnFailureListener { ex ->
                _error.value = "Error on obtaining orders: ${ex.localizedMessage}"
                _loading.value = false
            }
    }
}

// --- MAIN SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidateDeliveryScreen(
    navController: NavController,
    viewModel: ValidateDeliveryViewModel = viewModel()
) {
    val orders by viewModel.orders.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    var selectedOrder by remember { mutableStateOf<OrderWithProduct?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Validate Delivery (Seller)") })
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
            when {
                loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.fetchOrders() }) {
                            Text("Retry")
                        }
                    }
                }
                selectedOrder != null -> {
                    val order = selectedOrder!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Order: ${order.productTitle}", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("QR code to confirm delivery", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(24.dp))
                        generateQrBitmap(order.hashConfirm)?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(250.dp)
                                    .background(Color.White),
                                contentScale = ContentScale.Crop
                            )
                        } ?: Text("Error on generating QR", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { selectedOrder = null }) {
                            Text("Close")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(orders) { order ->
                            Button(
                                onClick = { selectedOrder = order },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Order: ${order.productTitle}")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun generateQrBitmap(data: String): Bitmap? {
    return try {
        val size = 512
        val matrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size)
        Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also { bmp ->
            for (x in 0 until size) for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
    } catch (e: Exception) {
        Log.e("GenerateQR", "Error on en generating QR", e)
        null
    }
}