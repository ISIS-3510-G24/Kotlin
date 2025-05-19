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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.disk.DiskCache
import coil.memory.MemoryCache
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
    bottomNavController: NavController
) {
    val context = LocalContext.current
    val db      = UniMarketDatabase.getInstance(context)

    val repo = remember {
        UniMarketRepository(
            appContext    = context,
            productDao    = db.productDao(),
            wishlistDao   = db.wishlistDao(),
            orderDao      = db.orderDao(),
            imageCacheDao = db.imageCacheDao(),
            pendingOpDao  = db.pendingOpDao(),
            ioDispatcher  = Dispatchers.IO
        )
    }

    val factory = remember {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(OrdersViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return OrdersViewModel(repo) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: \$modelClass")
            }
        }
    }

    val viewModel: OrdersViewModel = viewModel(factory = factory)

    val orders     by viewModel.orders.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val isLoading  by viewModel.isLoading.collectAsState()
    val error      by viewModel.error.collectAsState()
    val userId     = viewModel.getCurrentUserId()

    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache { MemoryCache.Builder(context).maxSizePercent(0.25).build() }
            .diskCache { DiskCache.Builder().directory(context.cacheDir.resolve("image_cache")).maxSizeBytes(50L * 1024 * 1024).build() }
            .build()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Orders") }) }
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
                        onClick  = { viewModel.setTab(tab) },
                        text     = { Text(tab.name.capitalize(Locale.ROOT)) }
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
                                OrderTab.BUYING  -> o.buyerID  == userId
                                OrderTab.SELLING -> o.sellerID == userId
                            }
                        }

                    LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered) { order ->
                            OrderItem(
                                order       = order,
                                imageLoader = imageLoader,
                                onCardClick = {
                                    bottomNavController
                                        .navigate("productDetail/${order.productId}")
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
    order: com.example.unimarket.ui.viewmodels.Order,
    imageLoader: ImageLoader,
    onCardClick: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        modifier  = Modifier
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
                painter            = rememberAsyncImagePainter(
                    model       = order.imageUrl,
                    imageLoader = imageLoader
                ),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(order.productTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text("Price: $${order.price.toInt()}", fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text("Date: ${fmt.format(order.orderDate.toDate())}", fontSize = 12.sp)
                Spacer(Modifier.height(2.dp))
                Text("Status: ${order.status}", fontSize = 14.sp)
            }
        }
    }
}