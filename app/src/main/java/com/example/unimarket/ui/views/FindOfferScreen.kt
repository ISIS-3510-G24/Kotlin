package com.example.unimarket.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.R
import com.example.unimarket.ui.viewmodels.FindItem
import com.example.unimarket.ui.viewmodels.FindOfferViewModel

@Composable
fun FindOfferScreen(
    navController: NavController,
    bottomNavController: NavController,
    viewModel: FindOfferViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val items = uiState.findList
    val currentUserMajor = uiState.userMajor
    val showBanner = uiState.showGreetingBanner
    val isSearching = uiState.isSearchVisible
    val searchText = uiState.searchText

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(top = if (showBanner) 100.dp else 0.dp)
    ) {
        TopBarWithSearch(
            isSearchVisible = isSearching,
            searchText = searchText,
            onSearchClick = viewModel::onSearchClick,
            onTextChange = viewModel::onTextChange,
            onClearSearch = viewModel::onClearSearch
        )

        Spacer(Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                listOf(
                    "ALL REQUESTS", "MATERIALS", "TECHNOLOGY",
                    "SPORTS", "BOOKS", "MUSICAL INSTRUMENTS",
                    "SUITS", "TOYS"
                )
            ) { cat ->
                Text(
                    text = cat,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionTitle("All")
        SectionRow(
            items = items,
            onCardClick = { id -> navController.navigate("findDetail/$id") }
        )

        SectionTitle("From Your Major")
        SectionColumn(
            items = items.filter { it.major == currentUserMajor },
            onClick = { id -> navController.navigate("findDetail/$id") }
        )

        SectionTitle("Selling out")
        SectionColumn(
            items = items.filter { it.status == "active" },
            onClick = { id -> navController.navigate("findDetail/$id") }
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 16.dp)
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SectionRow(
    items: List<FindItem>,
    onCardClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { find ->
            FindCard(
                item = find,
                onClick = { onCardClick(find.id) }
            )
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun SectionColumn(
    items: List<FindItem>,
    onClick: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        items.forEach { find ->
            HorizontalFindCard(
                item = find,
                onClick = { onClick(find.id) }
            )
        }
    }
    Spacer(Modifier.height(16.dp))
}

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
        Button(
            onClick = { /* TODO: New Find */ },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("NEW FIND", color = Color.White)
        }

        if (!isSearchVisible) {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = searchText,
                    onValueChange = onTextChange,
                    placeholder = { Text("Search…") },
                    singleLine = true,
                    modifier = Modifier
                        .width(200.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        )
                )
                IconButton(onClick = onClearSearch) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        }
    }
}

@Composable
fun FindCard(
    item: FindItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(260.dp)
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            val painter =
                if (item.image.isNotBlank()) rememberAsyncImagePainter(item.image)
                else painterResource(R.drawable.default_product)

            Image(
                painter = painter,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Find", color = Color.White)
                }
                Button(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Offer", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun HorizontalFindCard(
    item: FindItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            val painter =
                if (item.image.isNotBlank()) rememberAsyncImagePainter(item.image)
                else painterResource(R.drawable.default_product)

            Image(
                painter = painter,
                contentDescription = item.title,
                modifier = Modifier
                    .size(60.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Go",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}