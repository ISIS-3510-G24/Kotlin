package com.example.unimarket.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.R
import com.example.unimarket.ui.models.Product
import com.example.unimarket.ui.viewmodels.ExploreViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ExploreScreen(
    navController: NavController,
    exploreViewModel: ExploreViewModel = hiltViewModel(),
) {
    val isOnline by exploreViewModel.isOnline.collectAsState()
    val products by exploreViewModel.products.collectAsState()
    val wishlistIds by exploreViewModel.wishlistIds.collectAsState()
    val recIds by exploreViewModel.recommendations.collectAsState()
    val isLoading by exploreViewModel.isLoading.collectAsState()
    val uploadState by exploreViewModel.uploadState.collectAsState()

    val analytics = FirebaseAnalytics.getInstance(LocalContext.current)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var tipShown by remember { mutableStateOf(false) }

    // State for scrolling
    val listState = rememberLazyListState()



//    LaunchedEffect(tipShown) {
//        if (!tipShown) {
//            delay(10_000)
//            snackbarHostState.showSnackbar("Tip: Shake your phone to refresh products.")
//            tipShown = true
//        }
//    }

    val recommendedProducts = recIds.mapNotNull { recId ->
        products.find { it.id == recId }
    }
    val available = products.filter { it.status == "Available" && it.id !in recIds }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("publishProduct") }) {
                Icon(Icons.Default.Add, contentDescription = "Publish Product")
            }
        }
    ) { padding ->
        Column(Modifier
            .fillMaxSize()
            .padding(padding)) {}

        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (recommendedProducts.isNotEmpty()) {
                    item {
                        Text(
                            "Recommended for you",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                        )
                    }
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recommendedProducts) { product ->
                            ProductCard(
                                product = product,
                                isFavorite = product.id in wishlistIds,
                                onFavoriteClick = { exploreViewModel.toggleWishlist(product.id) },
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(280.dp),
                                onClick = {
                                    navController.navigate("productDetail/${product.id}")
                                }
                            )
                        }
                    }
                }
                item {
                    Text(
                        "All Products",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                    )
                }
                items(available.chunked(2)) { rowItems ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { product ->
                            ProductCard(
                                product = product,
                                isFavorite = product.id in wishlistIds,
                                onFavoriteClick = { exploreViewModel.toggleWishlist(product.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(280.dp),
                                onClick = {
                                    analytics.logEvent(
                                        "view_product_detail",
                                        bundleOf("product_id" to product.id)
                                    )
                                    navController.navigate("productDetail/${product.id}")
                                }
                            )
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Loader overlay
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            if (listState.firstVisibleItemIndex > 0) {
                FloatingActionButton(
                    onClick = {
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 100.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Scroll to top"
                    )
                }
            }
        }
    }
}


@Composable
fun ProductCard(
    product: Product,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val painter = product.imageUrls.firstOrNull()
                    ?.let { rememberAsyncImagePainter(it) }
                    ?: rememberAsyncImagePainter(R.drawable.default_product)

                Image(
                    painter = painter,
                    contentDescription = product.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                        .format(product.price),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Heart icon with background and tint for contrast
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from wishlist" else "Add to wishlist",
                    tint = if (isFavorite)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}