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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.ui.sensor.ShakeDetector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ExploreViewModel should include a loading state and a refreshProducts() method.
@Composable
fun ExploreScreen(exploreViewModel: ExploreViewModel = viewModel()) {
    val productList by exploreViewModel.products.collectAsState()
    val errorMessage by exploreViewModel.errorMessage.collectAsState()
    val isLoading by exploreViewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Flag to ensure the tip message is shown only once
    var tipShown = remember { mutableStateOf(false) }

    // Show a tip message after 10 seconds if not already shown
    LaunchedEffect(Unit) {
        if (!tipShown.value) {
            delay(10000) // Wait for 10 seconds
            snackbarHostState.showSnackbar(
                message = "Tip: Shake your phone to refresh products.",
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            tipShown.value = true
        }
    }

    // ShakeDetector
    ShakeDetector {
        exploreViewModel.refreshProducts()
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = "Products refreshed!",
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
        }
    }

    // Scaffold that includes a SnackbarHost for showing messages
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                // Show a loading indicator while data is being fetched
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(productList) { product ->
                            ProductCard(product)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(product: Product) {
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val painter = rememberAsyncImagePainter(
                model = product.imageUrls.firstOrNull(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painter,
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = product.title, style = MaterialTheme.typography.titleMedium)
            Text(text = "$${product.price}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
