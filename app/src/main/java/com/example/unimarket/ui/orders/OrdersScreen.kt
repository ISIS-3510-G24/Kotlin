package com.example.unimarket.ui.orders

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

enum class OrderTab { HISTORY, BUYING, SELLING }

data class OrderItem(
    val id: String,
    val title: String,
    val imageUrl: String,
    val orderDate: String,
    val price: Double,
    val status: String,
    val type: OrderTab
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(onChatClick: (orderId: String) -> Unit) {
    val db   = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser

    var orders      by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(OrderTab.HISTORY) }

    LaunchedEffect(Unit) {
        if (user == null) {
            isLoading = false
            return@LaunchedEffect
        }
        try {
            val snap = db.collection("orders").get().await()
            orders = snap.documents.mapNotNull { doc ->
                val data   = doc.data ?: return@mapNotNull null
                val pid    = data["productID"]  as? String ?: return@mapNotNull null
                val bId    = data["buyerID"]    as? String
                val sId    = data["sellerID"]   as? String

                val pdoc   = db.collection("Product").document(pid).get().await()
                val pd     = pdoc.data ?: return@mapNotNull null

                val urls     = pd["imageUrls"] as? List<*>
                val imageUrl = (urls?.firstOrNull() as? String).orEmpty()
                val date     = data["orderDate"] as? String ?: ""
                val price    = (data["price"] as? Number)?.toDouble() ?: 0.0
                val status   = data["status"] as? String ?: ""
                val type     = when {
                    bId == user.uid -> OrderTab.BUYING
                    sId == user.uid -> OrderTab.SELLING
                    else            -> OrderTab.HISTORY
                }

                OrderItem(
                    id        = doc.id,
                    title     = pd["title"] as? String ?: "",
                    imageUrl  = imageUrl,
                    orderDate = date,
                    price     = price,
                    status    = status,
                    type      = type
                )
            }
        } catch (e: Exception) {
            Log.e("OrdersScreen", "Error loading orders", e)
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orders") },
                actions = {
                    IconButton(onClick = { /* TODO: open search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
                return@Box
            }

            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1F2024))
                        .padding(4.dp)
                ) {
                    listOf(
                        "History" to OrderTab.HISTORY,
                        "Buying"  to OrderTab.BUYING,
                        "Selling" to OrderTab.SELLING
                    ).forEach { (label, tab) ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (selectedTab == tab)
                                        MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { selectedTab = tab },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (selectedTab == tab) Color.White else Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                val filtered = orders.filter { it.type == selectedTab }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filtered) { order ->
                        OrderItemRow(order, onChatClick)
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderItemRow(order: OrderItem, onChatClick: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { /* TODO: navigate to detail */ }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter           = rememberAsyncImagePainter(order.imageUrl),
            contentDescription= order.title,
            contentScale      = ContentScale.Crop,
            modifier          = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                order.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Order Date: ${order.orderDate}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                "${"%.3f".format(order.price)} \$",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                order.status,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        IconButton(onClick = { onChatClick(order.id) }) {
            Icon(
                Icons.Default.ChatBubble,
                contentDescription = "Chat",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}