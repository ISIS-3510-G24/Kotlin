package com.example.unimarket.ui.orders

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

enum class OrderTab { HISTORY, BUYING, SELLING }

data class Order(
    val id: String,
    val buyer: String,
    val seller: String,
    val productTitle: String,
    val imageUrl: String,
    val createdAt: Timestamp,
    val price: Double,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(onChatClick: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser

    var allOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(OrderTab.HISTORY) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (user == null) {
            error = "Usuario no autenticado"
            isLoading = false
            return@LaunchedEffect
        }

        try {
            val ordersSnapshot = db.collection("orders").get().await()
            val ordersList = mutableListOf<Order>()

            for (doc in ordersSnapshot.documents) {
                val data = doc.data ?: continue
                val productId = data["productID"] as? String ?: continue
                val buyer = data["buyer"] as? String ?: ""
                val seller = data["seller"] as? String ?: ""
                val createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now()
                val status = data["status"] as? String ?: "Unknown"

                val productDoc = db.collection("Product").document(productId).get().await()
                val productData = productDoc.data ?: continue
                val title = productData["title"] as? String ?: "No title"
                val price = (productData["price"] as? Number)?.toDouble() ?: 0.0
                val imageUrl = (productData["imageUrls"] as? List<*>)?.firstOrNull() as? String ?: ""

                ordersList.add(
                    Order(
                        id = doc.id,
                        buyer = buyer,
                        seller = seller,
                        productTitle = title,
                        imageUrl = imageUrl,
                        createdAt = createdAt,
                        price = price,
                        status = status
                    )
                )
            }

            allOrders = ordersList
        } catch (e: Exception) {
            Log.e("OrdersScreen", "Error: ${e.message}", e)
            error = "Error al cargar las órdenes"
        } finally {
            isLoading = false
        }
    }

    val filteredOrders = allOrders.filter { order ->
        val uid = user?.uid ?: return@filter false

        val matchesTab = when (selectedTab) {
            OrderTab.HISTORY -> true
            OrderTab.BUYING -> true
            OrderTab.SELLING -> true
        }

        val matchesSearch = order.productTitle.contains(searchQuery, ignoreCase = true)
        matchesTab && (!isSearchActive || matchesSearch)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Buscar...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        } else {
                            Text("Orders")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = !isSearchActive }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab.ordinal) {
                    OrderTab.values().forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab.ordinal == index,
                            onClick = { selectedTab = OrderTab.values()[index] },
                            text = { Text(tab.name) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                filteredOrders.isEmpty() -> {
                    Text(
                        text = "No orders found in ${selectedTab.name} tab.",
                        modifier = Modifier.padding(16.dp)
                    )
                }

                else -> {
                    LazyColumn {
                        items(filteredOrders) { order ->
                            OrderItem(order = order, onChatClick = onChatClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderItem(order: Order, onChatClick: (String) -> Unit) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val priceFormat = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter(order.imageUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.medium)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(order.productTitle, style = MaterialTheme.typography.titleMedium)
                Text("Date: ${dateFormat.format(order.createdAt.toDate())}")
                Text(priceFormat.format(order.price))
                Text("Status: ${order.status}")
            }

            IconButton(onClick = { onChatClick(order.id) }) {
                Icon(Icons.Default.Chat, contentDescription = "Chat")
            }
        }
    }
}
