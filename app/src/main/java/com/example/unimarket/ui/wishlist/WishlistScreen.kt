package com.example.unimarket.ui.wishlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material3.ExperimentalMaterial3Api

data class WishlistItem(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val price: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    onBack: () -> Unit = {},
    wishlistItems: List<WishlistItem> = listOf(
        WishlistItem(
            id = "1234567",
            title = "Leather Scalpel",
            description = "These are a must have for Medium: leather, they are in perfect condition. SOLD INDIVIDUALLY",
            imageUrl = "https://stryicarvingtools.com/cdn/shop/files/il_794xN.5161752705_an6w_1080x.jpg?v=1709426801",
            price = "$45000.00"
        )
    )
) {
    var showDialog by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Wishlist") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(wishlistItems) { item ->
                    WishlistRow(item)
                }
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Producto Not Available") },
                    text = {
                        Text("The product with ID ${wishlistItems.first().id} is not available anymore.")
                    },
                    confirmButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Aceptar")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun WishlistRow(item: WishlistItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { /* Navegar al detalle si quieres */ },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter(item.imageUrl),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .padding(8.dp)
            )
            Column(
                Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            Text(
                item.price,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
    }
}
