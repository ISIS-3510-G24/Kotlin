package com.example.unimarket.ui.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(
    navController: NavController,
    exploreViewModel: ExploreViewModel = hiltViewModel()
) {
    val products by exploreViewModel.products.collectAsState()
    val wishlistIds by exploreViewModel.wishlistIds.collectAsState()
    val recIds by exploreViewModel.recommendations.collectAsState()
    val isLoading by exploreViewModel.isLoading.collectAsState()
    val recentlyIds by exploreViewModel.recentlyViewed.collectAsState()
    val analytics = FirebaseAnalytics.getInstance(LocalContext.current)

    val recently: List<Product> = recentlyIds
        .mapNotNull { id -> products.find { it.id == id } }
        .take(10)

    val recommended: List<Product> = recIds.mapNotNull { id -> products.find { it.id == id } }

    val available: List<Product> = products.filter { it.status == "Available" }

    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { navController.navigate("publishProduct") },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Publish Product")
                }
                if (gridState.firstVisibleItemIndex > 0) {
                    FloatingActionButton(
                        onClick = { scope.launch { gridState.animateScrollToItem(0) } },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(y = (-72).dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Scroll to top")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (recently.isNotEmpty()) {
                    item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                        Text(
                            text = "Recently viewed",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }
                    item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                        LazyRow(
                            state = rememberLazyListState(),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            items(
                                items = recently,
                                key = { it.id }
                            ) { product ->
                                RecentProductCard(
                                    product = product,
                                    isFavorite = product.id in wishlistIds,
                                    onFavoriteClick = { exploreViewModel.toggleWishlist(product.id) },
                                    onClick = {
                                        exploreViewModel.recordView(product.id)
                                        analytics.logEvent(
                                            "view_product_detail",
                                            bundleOf("product_id" to product.id)
                                        )
                                        navController.navigate("productDetail/${product.id}")
                                    }
                                )
                            }
                        }
                    }
                }

                if (recommended.isNotEmpty()) {
                    item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                        Text(
                            text = "Recommended for you",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = if (recently.isNotEmpty()) 8.dp else 0.dp,
                                    bottom = 8.dp
                                )
                        )
                    }
                    items(
                        items = recommended,
                        key = { it.id }
                    ) { product ->
                        ProductCard(
                            product = product,
                            isFavorite = product.id in wishlistIds,
                            onFavoriteClick = { exploreViewModel.toggleWishlist(product.id) },
                            onClick = {
                                exploreViewModel.recordView(product.id)
                                analytics.logEvent(
                                    "view_product_detail",
                                    bundleOf("product_id" to product.id)
                                )
                                navController.navigate("productDetail/${product.id}")
                            }
                        )
                    }
                }

                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    Text(
                        text = "All Products",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = if (recently.isNotEmpty() || recommended.isNotEmpty()) 8.dp else 0.dp,
                                bottom = 8.dp
                            )
                    )
                }
                items(
                    items = available,
                    key = { it.id }
                ) { product ->
                    ProductCard(
                        product = product,
                        isFavorite = product.id in wishlistIds,
                        onFavoriteClick = { exploreViewModel.toggleWishlist(product.id) },
                        onClick = {
                            exploreViewModel.recordView(product.id)
                            analytics.logEvent(
                                "view_product_detail",
                                bundleOf("product_id" to product.id)
                            )
                            navController.navigate("productDetail/${product.id}")
                        }
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun RecentProductCard(
    product: Product,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(width = 160.dp, height = 180.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Image(
                    painter = product.imageUrls.firstOrNull()
                        ?.let { rememberAsyncImagePainter(it) }
                        ?: rememberAsyncImagePainter(R.drawable.default_product),
                    contentDescription = product.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = NumberFormat
                        .getCurrencyInstance(Locale("es", "CO"))
                        .format(product.price),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            IconButton(
                onClick = onFavoriteClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme
                        .colorScheme
                        .surface
                        .copy(alpha = 0.7f)
                ),
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    modifier: Modifier = Modifier,
    product: Product,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Image(
                    painter = product.imageUrls.firstOrNull()
                        ?.let { rememberAsyncImagePainter(it) }
                        ?: rememberAsyncImagePainter(R.drawable.default_product),
                    contentDescription = product.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = NumberFormat
                        .getCurrencyInstance(Locale("es", "CO"))
                        .format(product.price),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            IconButton(
                onClick = onFavoriteClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme
                        .colorScheme
                        .surface
                        .copy(alpha = 0.7f)
                ),
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
