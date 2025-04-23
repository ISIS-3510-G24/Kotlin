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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.R
import com.example.unimarket.ui.models.Product
import com.example.unimarket.ui.viewmodels.ExploreViewModel
import com.example.unimarket.ui.viewmodels.ShakeDetector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ExploreScreen(
    navController: NavController,
    bottomNavController: NavController,
    exploreViewModel: ExploreViewModel = viewModel()
) {
    val allProducts by exploreViewModel.products.collectAsState()
    val isLoading by exploreViewModel.isLoading.collectAsState()
    val userPreferences by exploreViewModel.userPreferences.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for scrolling
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Tip after 10s
    var tipShown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        exploreViewModel.loadProductsFromFirestore()
    }

    LaunchedEffect(Unit) {
        delay(10_000)
        if (!tipShown) {
            snackbarHostState.showSnackbar("Tip: Shake your phone to refresh products.")
            tipShown = true
        }
    }

    ShakeDetector {
        exploreViewModel.refreshProducts()
        scope.launch {
            snackbarHostState.showSnackbar("Products refreshed!")
        }
    }

    val recommended = allProducts.filter { p ->
        p.labels.any { it in userPreferences }
    }
    val available = allProducts.filter { it.status == "Available" }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { bottomNavController.navigate("publish") }) {
                Icon(Icons.Default.Add, contentDescription = "Publish Product")
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        "Recommended for you",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recommended) { product ->
                            ProductCard(
                                product = product,
                                isFavorite = product.id in exploreViewModel.wishlistIds.collectAsState().value,
                                onFavoriteClick = { exploreViewModel.toggleWishlist(product) },
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
                                isFavorite = product.id in exploreViewModel.wishlistIds.collectAsState().value,
                                onFavoriteClick = { exploreViewModel.toggleWishlist(product) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(280.dp),
                                onClick = {
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
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
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
    onClick: () -> Unit = {}
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