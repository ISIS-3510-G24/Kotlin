package com.example.unimarket.ui.findOffer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.unimarket.R

@Composable
fun FindOfferScreen(
    onNavigateToProductDetail: (String) -> Unit
) {
    // State to show/hide search TextField
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        // TopBar with "Find" and "Offer" buttons and a search icon
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
            items(listOf("ALL REQUESTS", "MATERIALS", "TECHNOLOGY", "SPORTS", "BOOKS", "TOYS")) { cat ->
                Text(
                    text = cat,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // "From your major" section with large cards
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

/**
 * Top bar with the "Find," "Offer" buttons, and the search icon (or field) on the right.
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
            // Search icon
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Icon"
                )
            }
        } else {
            // Displays TextField and a button to close the search
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
 * Vertical card for large products with Buy button).
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
 * Horizontal card: image on the left and text next
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

            // Text in the center
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

            // Arrow on the right
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = ">",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
