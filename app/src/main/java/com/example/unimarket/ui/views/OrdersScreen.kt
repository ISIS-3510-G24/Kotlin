package com.example.unimarket.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.ui.viewmodels.Order
import com.example.unimarket.ui.viewmodels.OrderTab
import com.example.unimarket.ui.viewmodels.OrdersViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    navController: NavController,          // para detalle de producto
    bottomNavController: NavController,    // para chat
    viewModel: OrdersViewModel = viewModel(),
) {
    val orders by viewModel.orders.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val userId = viewModel.getCurrentUserId()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Orders") })
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = currentTab.ordinal) {
                OrderTab.values().forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = { Text(tab.name.capitalize(Locale.ROOT)) }
                    )
                }
            }

            when {
                isLoading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                error != null -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text(error!!, color = MaterialTheme.colorScheme.error) }

                else -> {
                    val filtered = orders
                        .filter { o ->
                            when (currentTab) {
                                OrderTab.HISTORY -> true
                                OrderTab.BUYING -> o.buyerID == userId
                                OrderTab.SELLING -> o.sellerID == userId
                            }
                        }
                        .sortedByDescending { it.orderDate.toDate() }

                    LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered) { order ->
                            OrderItem(
                                order = order,
                                currentUserId = userId,
                                onCardClick = {
                                    bottomNavController.navigate("productDetail/${order.productId}")
                                },
                                onReviewClick = { targetId ->
                                    navController.navigate("writeUserReview/$targetId")
                                }
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
    currentUserId: String?,
    onCardClick: () -> Unit,
    onReviewClick: (String) -> Unit,
) {
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
    ) {
        Row(
            Modifier
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
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(order.productTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text("Price: \$${order.price.toInt()}", fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text("Date: ${fmt.format(order.orderDate.toDate())}", fontSize = 12.sp)
                Spacer(Modifier.height(2.dp))
                Text("Status: ${order.status}", fontSize = 14.sp)
            }
        }
        Row(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                val targetId = if (order.buyerID == currentUserId)
                    order.sellerID else order.buyerID
                onReviewClick(targetId)
            }) {
                Text("Write Review")
            }
        }
    }
}
