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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.unimarket.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun FindOfferScreen(
    onNavigateToProductDetail: (String) -> Unit
) {
    var showBadNewsDialog by remember { mutableStateOf(true) }
    var showGreetingBanner by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Function to trigger the greeting banner based on time of the device
    fun triggerGreetingBannerIfNeeded() {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour in 20..23) {
            showGreetingBanner = true
            coroutineScope.launch {
                delay(10000)
                showGreetingBanner = false
            }
        }
    }

    // Show the bad news AlertDialog if needed
    if (showBadNewsDialog) {
        AlertDialog(
            onDismissRequest = { showBadNewsDialog = false },
            title = { Text("We have bad news :,(") },
            text = {
                Text("The product with ID: 135798642 which was on your wishlist has been offered to another user.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBadNewsDialog = false
                        triggerGreetingBannerIfNeeded()
                    }
                ) {
                    Text("Accept")
                }
            }
        )
    }

    // Main container using Box to overlay the greeting banner at the top
    Box(modifier = Modifier.fillMaxSize()) {
        // Greeting banner placed at the top center
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

        // Main content placed below the banner (or at the top if banner is hidden)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(top = if (showGreetingBanner) 130.dp else 0.dp)
        ) {
            // TopBar with "Find", "Offer" buttons and search icon/field
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
                items(listOf("ALL REQUESTS", "MATERIALS", "TECHNOLOGY", "SPORTS", "BOOKS", "MUSICAL INSTRUMENTS", "SUITS", "TOYS")) { cat ->
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "From your major" section with large cards in LazyRow
            Text(
                text = "From your major",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                // First product
                item {
                    ProductCard(
                        title = "Computer",
                        date = "Lenovo",
                        imageRes = R.drawable.findoffer1,
                        showBuyButton = true,
                        onClick = { onNavigateToProductDetail("computerId") },
                        cardWidth = 260.dp,
                        cardHeight = 280.dp,
                        imageSize = 180.dp
                    )
                }
                // Second product
                item {
                    ProductCard(
                        title = "USB",
                        date = "Type C",
                        imageRes = R.drawable.findoffer2,
                        showBuyButton = true,
                        onClick = { onNavigateToProductDetail("usbId") },
                        cardWidth = 260.dp,
                        cardHeight = 280.dp,
                        imageSize = 180.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Your wishlist" section
            Text(
                text = "Your wishlist",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                HorizontalProductCard(
                    title = "Set pink rulers",
                    subtitle = "Pink reference",
                    imageRes = R.drawable.findoffer3,
                    onClick = { onNavigateToProductDetail("pinkRulersId") }
                )
                HorizontalProductCard(
                    title = "Pink scissors",
                    subtitle = "Any reference",
                    imageRes = R.drawable.findoffer4,
                    onClick = { onNavigateToProductDetail("pinkScissorsId") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Selling out" section
            Text(
                text = "Selling out",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                HorizontalProductCard(
                    title = "Calculator",
                    subtitle = "CASIO",
                    imageRes = R.drawable.findoffer5,
                    onClick = { onNavigateToProductDetail("calculatorCasioId") }
                )
                HorizontalProductCard(
                    title = "Calculator",
                    subtitle = "Sharp",
                    imageRes = R.drawable.findoffer6,
                    onClick = { onNavigateToProductDetail("calculatorSharpId") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Top bar with the "Find", "Offer" buttons and the search icon/field on the right.
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
        // Left section: "Find" and "Offer" buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { /* Logic for 'Find' */ },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("FIND")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { /* Logic for 'Offer' */ },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("OFFER")
            }
        }
        // Right section: search icon or TextField
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
 * Vertical card for large products (image at the top, left-aligned text, and "Buy" button below).
 */
@Composable
fun ProductCard(
    title: String,
    date: String,
    imageRes: Int,
    showBuyButton: Boolean = true,
    onClick: () -> Unit,
    cardWidth: Dp = 150.dp,
    cardHeight: Dp = 180.dp,
    imageSize: Dp = 80.dp
) {
    Card(
        modifier = Modifier.size(width = cardWidth, height = cardHeight),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Box to center the image horizontally
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = "Product Image",
                    modifier = Modifier.size(imageSize)
                )
            }
            // Column for left-aligned text
            Column(
                modifier = Modifier.padding(top = 4.dp),
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
            // "Buy" button (if showBuyButton is true)
            if (showBuyButton) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Buy", textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/**
 * Horizontal card: image on the left, text in the middle, arrow icon on the right.
 */
@Composable
fun HorizontalProductCard(
    title: String,
    subtitle: String,
    imageRes: Int,
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
            // Image on the left
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "Product Image",
                modifier = Modifier
                    .size(60.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Column for text in the middle (title and subtitle)
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
            // Arrow icon on the right
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
