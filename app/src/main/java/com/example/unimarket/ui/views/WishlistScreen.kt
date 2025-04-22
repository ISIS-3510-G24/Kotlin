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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WishlistItem(
    val productId: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val price: Long,
    val available: Boolean
)

class WishlistViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _wishlistItems = MutableStateFlow<List<WishlistItem>>(emptyList())
    val wishlistItems: StateFlow<List<WishlistItem>> = _wishlistItems.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchWishlist()
    }

    fun fetchWishlist() {
        val userId = auth.currentUser?.uid ?: return
        _loading.value = true
        _error.value = null

        firestore.collection("User")
            .document(userId)
            .collection("wishlist")
            .get()
            .addOnSuccessListener { wishDocs ->
                val productIds = wishDocs.documents
                    .mapNotNull { it.getString("productID") }
                    .toSet()

                if (productIds.isEmpty()) {
                    _wishlistItems.value = emptyList()
                    _loading.value = false
                    return@addOnSuccessListener
                }

                firestore.collection("Product")
                    .whereIn(FieldPath.documentId(), productIds.toList())
                    .get()
                    .addOnSuccessListener { prodDocs ->
                        val items = prodDocs.documents.mapNotNull { doc ->
                            if (!doc.exists()) return@mapNotNull null
                            val id = doc.id
                            val title = doc.getString("title") ?: return@mapNotNull null
                            val desc = doc.getString("description") ?: ""
                            val price = doc.getLong("price") ?: 0L
                            val urlList = doc.get("imageUrls") as? List<*>
                            val img = (urlList?.firstOrNull() as? String).orEmpty()
                            val status = doc.getString("status") ?: ""
                            WishlistItem(
                                productId = id,
                                title = title,
                                description = desc,
                                imageUrl = img,
                                price = price,
                                available = status.equals("Available", ignoreCase = true)
                            )
                        }
                        _wishlistItems.value = items
                        _loading.value = false
                    }
                    .addOnFailureListener { ex ->
                        _error.value = "Error loading products: ${ex.localizedMessage}"
                        _loading.value = false
                    }
            }
            .addOnFailureListener { ex ->
                _error.value = "Error loading wishlist: ${ex.localizedMessage}"
                _loading.value = false
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    onBack: () -> Unit,
    viewModel: WishlistViewModel = viewModel()
) {
    val items by viewModel.wishlistItems.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var unavailableTitle by remember { mutableStateOf("") }

    LaunchedEffect(items) {
        val firstUnavailable = items.firstOrNull { !it.available }
        if (firstUnavailable != null) {
            unavailableTitle = firstUnavailable.title
            showDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Wishlist") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.fetchWishlist() }) {
                            Text("Retry")
                        }
                    }
                }
                items.isEmpty() -> {
                    Text(
                        text = "You have no products in your wishlist",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items) { item ->
                            WishlistRow(item) {
                                if (!item.available) {
                                    unavailableTitle = item.title
                                    showDialog = true
                                }
                            }
                        }
                    }
                }
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Not Available Product :(") },
                    text = { Text("The product “$unavailableTitle” is not available anymore.") },
                    confirmButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Accept")
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(item.imageUrl),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
            Spacer(Modifier.width(12.dp))
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "$" + "%,d".format(item.price),
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.available) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
        }
    }
}
