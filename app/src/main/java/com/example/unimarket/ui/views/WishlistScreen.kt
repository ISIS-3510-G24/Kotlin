package com.example.unimarket.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

data class WishlistItem(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val price: String,
    val available: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(onBack: () -> Unit) {
    val wishlistIds = listOf("1234567")
    var wishlistItems by remember { mutableStateOf<List<WishlistItem>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val db = Firebase.firestore
        val loaded = wishlistIds.mapNotNull { pid ->
            val doc = db.collection("Product").document(pid).get().await()
            if (!doc.exists()) return@mapNotNull null
            val title = doc.getString("title").orEmpty()
            val desc  = doc.getString("description").orEmpty()
            val priceNum = doc.getLong("price") ?: 0L
            val imgList = doc.get("imageUrls") as? List<*>
            val imgUrl = imgList?.firstOrNull() as? String? ?: ""
            val status = doc.getString("status").orEmpty()
            val available = status.equals("Available", ignoreCase = true)
            WishlistItem(
                id = pid,
                title = title,
                description = desc,
                imageUrl = imgUrl,
                price = "$${"%,d".format(priceNum)}",
                available = available
            )
        }
        wishlistItems = loaded

        if (loaded.firstOrNull()?.available == false) {
            showDialog = true
        }
    }

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
                    WishlistRow(item) {
                    }
                }
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Not Available Product :(") },
                    text = {
                        Text(
                            "The product with ID ${wishlistItems.firstOrNull()?.id ?: ""} " +
                                    "is not available anymore."
                        )
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
private fun WishlistRow(item: WishlistItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
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
                    .weight(1f)
                    .padding(start = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            Text(
                item.price,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.available) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
    }
}