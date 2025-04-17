package com.example.unimarket.ui.findOffer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

// Data class matching the structure of documents in the "finds" collection
data class FindItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val image: String = "",
    val status: String = "",
    val major: String = "",
    val offerCount: Int = 0
)

@Composable
fun FindOfferScreen(
    onNavigateToProductDetail: (String) -> Unit
) {
    // State for storing Firestore data
    var findList by remember { mutableStateOf(listOf<FindItem>()) }

    LaunchedEffect(Unit) {
        val db = Firebase.firestore
        db.collection("finds")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val items = querySnapshot.documents.map { doc ->
                    FindItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        image = doc.getString("image") ?: "",
                        status = doc.getString("status") ?: "",
                        major = doc.getString("major") ?: "",
                        offerCount = doc.getLong("offerCount")?.toInt() ?: 0
                    )
                }
                findList = items
            }
            .addOnFailureListener {
                // handle error if needed
            }
    }

    // UI states for your existing features
    var showBadNewsDialog by remember { mutableStateOf(true) }
    var showGreetingBanner by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Main container using Box to overlay the greeting banner
    Box(modifier = Modifier.fillMaxSize()) {
        // Greeting banner at the top center if condition is met
        if (showGreetingBanner) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "Tomorrow we will have more products that might interest you, take advantage before they run out. Good night!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.padding(20.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Main content below the banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(top = if (showGreetingBanner) 130.dp else 0.dp)
        ) {
            // TopBar with "New Find" button and search icon/field
            TopBarWithSearch(
                isSearchVisible = isSearchVisible,
                searchText = searchText,
                onSearchClick = { isSearchVisible = !isSearchVisible },
                onTextChange = { searchText = it },
                onClearSearch = {
                    searchText = ""
                    isSearchVisible = false
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Categories in horizontal scroll
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(
                    listOf(
                        "ALL REQUESTS", "MATERIALS", "TECHNOLOGY",
                        "SPORTS", "BOOKS", "MUSICAL INSTRUMENTS", "SUITS", "TOYS"
                    )
                ) { cat ->
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "All" section
            Text(
                text = "All",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            val allItems = findList.take(3)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(allItems) { product ->
                    ProductCard(
                        title = product.title,
                        date = product.description,
                        imageUrl = product.image,
                        showFindButton = true,
                        showOfferButton = true,
                        onClick = { onNavigateToProductDetail(product.id) },
                        cardWidth = 260.dp,
                        imageHeight = 220.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "From Your Major" section
            Text(
                text = "From Your Major",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            val fromYourMajorItems = findList.take(2)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                fromYourMajorItems.forEach { product ->
                    HorizontalProductCard(
                        title = product.title,
                        subtitle = product.description,
                        imageUrl = product.image,
                        onClick = { onNavigateToProductDetail(product.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))


            // "Selling out" section
            Text(
                text = "Selling out",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            val sellingOutItems = findList.take(2)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                sellingOutItems.forEach { product ->
                    HorizontalProductCard(
                        title = product.title,
                        subtitle = product.description,
                        imageUrl = product.image,
                        onClick = { onNavigateToProductDetail(product.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Top bar with "Find", "Offer" and a search icon or field on the right.
 */
@Composable
fun TopBarWithSearch(
    isSearchVisible: Boolean,
    searchText: String,
    onSearchClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // "New Find Button"
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { /* Logic for 'New Find' */ },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("NEW FIND")
            }
        }

        if (!isSearchVisible) {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Icon"
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = searchText,
                    onValueChange = onTextChange,
                    singleLine = true,
                    modifier = Modifier.width(200.dp),
                    placeholder = { Text("Search...") },
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onClearSearch) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Search"
                    )
                }
            }
        }
    }
}

/**
 * Vertical card for large products (using Coil).
 * We let the card wrap its height by using width(...) + wrapContentHeight().
 */
@Composable
fun ProductCard(
    title: String,
    date: String,
    imageUrl: String,
    showFindButton: Boolean = true,
    showOfferButton: Boolean = true,
    onClick: () -> Unit,
    cardWidth: Dp = 260.dp,
    imageHeight: Dp = 200.dp
) {
    Card(
        modifier = Modifier
            .width(cardWidth)
            .wrapContentHeight(), // Let the card grow as needed
        onClick = onClick
    ) {
        // We'll space items by 8dp so the content doesn't get squashed
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1) The image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight),
                contentAlignment = Alignment.Center
            ) {
                val painter = rememberAsyncImagePainter(
                    model = imageUrl,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Image(
                    painter = painter,
                    contentDescription = "Product Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            // 2) The text columns (left-aligned)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Start
                )
            }

            // 3) The row for two side-by-side buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showFindButton) {
                    Button(
                        onClick = onClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Find", textAlign = TextAlign.Center)
                    }
                }
                if (showOfferButton) {
                    Button(
                        onClick = onClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Offer", textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

/**
 * Horizontal card for products (using Coil).
 */
@Composable
fun HorizontalProductCard(
    title: String,
    subtitle: String,
    imageUrl: String,
    onClick: () -> Unit,
    cardHeight: Dp = 80.dp
) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Coil image on the left
            val painter = rememberAsyncImagePainter(
                model = imageUrl,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Image(
                painter = painter,
                contentDescription = "Product Image",
                modifier = Modifier
                    .size(60.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Middle text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            // Right arrow
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Go",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
