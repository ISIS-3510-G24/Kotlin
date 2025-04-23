package com.example.unimarket.ui.views

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.ui.viewmodels.Order
import com.example.unimarket.ui.viewmodels.OrderTab
import com.example.unimarket.ui.viewmodels.OrdersViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onNavigateToOrder: (String) -> Unit,
    viewModel: OrdersViewModel = viewModel()
) {
    val orders by viewModel.orders.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val userId = viewModel.getCurrentUserId()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Orders") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = currentTab.ordinal) {
                OrderTab.values().forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = { Text(tab.name.capitalize()) }
                    )
                }
            }

            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                error != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    val filtered = orders.filter { order ->
                        when (currentTab) {
                            OrderTab.HISTORY -> true
                            OrderTab.BUYING -> order.buyerID == userId
                            OrderTab.SELLING -> order.sellerID == userId
                        }
                    }.sortedByDescending { it.orderDate.toDate() }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filtered) { order ->
                            OrderItem(
                                order = order,
                                onCardClick = { onNavigateToOrder(order.id) },
                                onChatClick = { onNavigateToOrder(order.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderItem(
    order: Order,
    onCardClick: () -> Unit,
    onChatClick: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onCardClick)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(order.imageUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = order.productTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(4.dp))
                Text("Price: \$${order.price.toInt()}", fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text("Date: ${dateFmt.format(order.orderDate.toDate())}", fontSize = 12.sp)
                Spacer(Modifier.height(2.dp))
                Text("Status: ${order.status}", fontSize = 14.sp)
            }
            IconButton(onClick = onChatClick) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Chat"
                )
            }
        }
    }
}