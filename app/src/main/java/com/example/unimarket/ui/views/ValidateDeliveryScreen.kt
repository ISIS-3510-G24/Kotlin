package com.example.unimarket.ui.views

import android.graphics.Bitmap
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
import androidx.compose.runtime.LaunchedEffect
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

    val qrBitmapState = viewModel.qrBitmap.collectAsState(initial = null)
    val qrBitmap: Bitmap? by qrBitmapState

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
                    qrBitmap = qrBitmap,
                    onLoadQr = viewModel::loadQrBitmap,
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
    qrBitmap: Bitmap?,
    onLoadQr: (OrderWithProduct) -> Unit,
    onClose: () -> Unit
) {
    // Disparamos la carga o reutilización del QR cuando cambie el pedido
    LaunchedEffect(key1 = order.hashConfirm) {
        onLoadQr(order)
    }

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

        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "QR para orden ${order.orderId}",
                modifier = Modifier
                    .size(250.dp)
                    .background(Color.White),
                contentScale = ContentScale.Crop
            )
        } else {
            CircularProgressIndicator()
        }

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
