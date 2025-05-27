package com.example.unimarket.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.ui.models.Product
import com.example.unimarket.ui.viewmodels.ProductDetailViewModel
import com.google.accompanist.flowlayout.FlowRow
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    navController: NavController,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val isLoading    by viewModel.isLoading.collectAsState()
    val product      by viewModel.product.collectAsState()
    val sellerName   by viewModel.sellerName.collectAsState()
    val isFav        by viewModel.isInWishlist.collectAsState()
    val error        by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = product?.title ?: "Product detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleWishlist() }) {
                        if (isFav) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Delete from favorites",
                                tint = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = "Add to favorites"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                error != null -> {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }

                product != null -> {
                    val nonNullProduct: Product = product!!

                    ProductDetailContent(
                        product    = nonNullProduct,
                        sellerName = sellerName ?: "Loading...",
                        onSellerClick = {
                            navController.navigate("sellerReviews/${nonNullProduct.sellerID}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductDetailContent(
    product: Product,
    sellerName: String,
    onSellerClick: () -> Unit
) {
    val formattedPrice = remember(product.price) {
        NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            .format(product.price)
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = product.title,
            style = MaterialTheme.typography.headlineSmall
        )

        if (product.imageUrls.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(product.imageUrls) { url ->
                    Card(
                        modifier = Modifier.size(200.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(url),
                            contentDescription = "Image of ${product.title}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        Text("Price: $formattedPrice", style = MaterialTheme.typography.titleMedium)
        Text("Major ID: ${product.majorID}", style = MaterialTheme.typography.bodyMedium)
        Text("Class ID: ${product.classId}", style = MaterialTheme.typography.bodyMedium)

        if (product.labels.isNotEmpty()) {
            Text("Labels:", style = MaterialTheme.typography.bodyLarge)
            FlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
                product.labels.forEach { label ->
                    Surface(
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Text("Description:", style = MaterialTheme.typography.bodyLarge)
        Text(product.description, style = MaterialTheme.typography.bodyMedium)

        Button(
            onClick = onSellerClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(top = 8.dp)
        ) {
            Text(
                text = "Seller: $sellerName\nTap Here to see seller reviews",
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}
