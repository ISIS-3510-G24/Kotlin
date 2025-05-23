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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import com.example.unimarket.data.UniMarketDatabase
import com.example.unimarket.data.UniMarketRepository
import com.example.unimarket.ui.viewmodels.OrderTab
import com.example.unimarket.ui.viewmodels.OrdersViewModel
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    navController: NavController,
    bottomNavController: NavController,
) {
    val context = LocalContext.current
    val db = UniMarketDatabase.getInstance(context)
    val repo = remember {
        UniMarketRepository(
            appContext = context,
            productDao = db.productDao(),
            wishlistDao = db.wishlistDao(),
            findDao = db.findDao(),
            orderDao = db.orderDao(),
            imageCacheDao = db.imageCacheDao(),
            pendingOpDao = db.pendingOpDao(),
            userReviewDao = db.userReviewDao(),
            ioDispatcher = Dispatchers.IO
        )
    }

    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(OrdersViewModel::class.java)) {
                    return OrdersViewModel(context, repo) as T
                }
                throw IllegalArgumentException("Unknown ViewModel")
            }
        }
    }

    val vm: OrdersViewModel = viewModel(factory = factory)

    // collect UI state
    val orders by vm.orders.collectAsState()
    val currentTab by vm.currentTab.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    val lastRef by vm.lastRefresh.collectAsState()
    val userId = vm.getCurrentUserId()!!
    val myReviews by repo.observeUserReviewsFor(userId).collectAsState(initial = emptyList())
    val reviewedSet = remember(myReviews) {
        myReviews.map { it.targetUserId }.toSet()
    }

    // Coil ImageLoader with caching = Strategy 1
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Orders")
                        Text("Last refresh: $lastRef", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.loadOrders() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = currentTab.ordinal) {
                OrderTab.entries.forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { vm.setTab(tab) },
                        text = { Text(tab.name) }
                    )
                }
            }

            when {
                isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }

                error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders.filter {
                        when (currentTab) {
                            OrderTab.HISTORY -> true
                            OrderTab.BUYING -> it.buyerID == userId
                            OrderTab.SELLING -> it.sellerID == userId
                        }
                        // Strategy 2
                    }) { order ->
                        OrderItem(
                            order = order,
                            imageLoader = imageLoader,
                            currentUserId = userId,
                            onCardClick = {
                                // Navigate to product detail screen
                                bottomNavController
                                    .navigate("productDetail/${order.productId}")
                            },
                            onReviewClick = { targetId ->
                                bottomNavController
                                    .navigate("writeUserReview/$targetId")
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun OrderItem(
    order: com.example.unimarket.ui.viewmodels.Order,
    imageLoader: ImageLoader,
    currentUserId: String?,
    onCardClick: () -> Unit,
    onReviewClick: (String) -> Unit,
) {
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(order.imageUrl, imageLoader),
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(order.productTitle,
                    style = MaterialTheme.typography.titleMedium)
                Text("Price: \$${order.price.toInt()}")
                Text("Date: ${order.orderDate.toDate()}")
                Text("Status: ${order.status}")
            }
        }

        Row(
            Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                val target = if (order.buyerID == currentUserId)
                    order.sellerID else order.buyerID
                onReviewClick(target)
            }) {
                Text("Write Review")
            }
        }
    }
}
