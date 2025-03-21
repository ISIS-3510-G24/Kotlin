package com.example.unimarket.ui.explore

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.ui.sensor.ShakeDetector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ExploreScreen(
    navController: NavController,
    exploreViewModel: ExploreViewModel = viewModel()
) {
    val productList by exploreViewModel.products.collectAsState()
    val errorMessage by exploreViewModel.errorMessage.collectAsState()
    val isLoading by exploreViewModel.isLoading.collectAsState()
    val userPreferences by exploreViewModel.userPreferences.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Show a tip after 10 seconds
    val tipShown = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!tipShown.value) {
            delay(10000) // Wait 10 seconds
            snackbarHostState.showSnackbar(
                message = "Tip: Shake your phone to refresh products.",
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            tipShown.value = true
        }
    }

    // ShakeDetector for refreshing products
    ShakeDetector {
        exploreViewModel.refreshProducts()
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = "Products refreshed!",
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
        }
    }

    val recommendedProducts = productList.filter { product ->
        product.labels.any { label -> label in userPreferences }
    }
    println("Recommended products: $recommendedProducts")

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate("publish")
            }) {
                Icon(Icons.Default.Add, contentDescription = "Publish Product")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main container for the screen
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Show error message if there is one
                if (errorMessage != null) {
                    item {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Section: Recommended products
                item {
                    Text(
                        text = "Recommended for you",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recommendedProducts) { product ->
                            ProductCard(product)
                        }
                    }
                }

                // Section: All products
                item {
                    Text(
                        text = "All Products",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(productList) { product ->
                            ProductCard(product)
                        }
                    }
                }

                // Extra space at the end
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun ProductCard(product: Product) {
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            // Adjust the width of the Card
            .width(200.dp)
            .padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val painter = rememberAsyncImagePainter(
                model = product.imageUrls.firstOrNull(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Image(
                painter = painter,
                contentDescription = product.title,
                modifier = Modifier
                    .fillMaxSize() // Fill the size of the Card
                    .height(180.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = product.title, style = MaterialTheme.typography.titleMedium)
            Text(text = "$${product.price}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
