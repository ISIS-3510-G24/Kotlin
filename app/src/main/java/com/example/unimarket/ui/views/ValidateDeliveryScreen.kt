package com.example.unimarket.ui.views

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimarket.ui.viewmodels.OrderWithProduct
import com.example.unimarket.ui.viewmodels.ValidateDeliveryViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

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
        topBar = { TopAppBar(title = { Text("Validate Delivery (Seller)") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                error != null -> ErrorContent(
                    error = error!!,
                    onRetry = { viewModel.fetchOrders() },
                    modifier = Modifier.align(Alignment.Center)
                )
                selectedOrder != null -> SelectedOrderContent(
                    order = selectedOrder!!,
                    onClose = { selectedOrder = null }
                )
                else -> OrdersList(
                    orders = orders,
                    onSelect = { selectedOrder = it }
                )
            }
        }
    }
}

@Composable
private fun OrdersList(
    orders: List<OrderWithProduct>,
    onSelect: (OrderWithProduct) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(orders) { order ->
            Button(
                onClick = { onSelect(order) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Order: ${order.productTitle}")
            }
        }
    }
}

@Composable
private fun SelectedOrderContent(
    order: OrderWithProduct,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // CORRECTED INTERPOLATION
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
        } ?: Text("Error generating QR", color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onClose) { Text("Close") }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = error, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
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
        Log.e("GenerateQR", "Error generating QR", e)
        null
    }
}
