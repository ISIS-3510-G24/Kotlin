package com.example.unimarket.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.R
import com.example.unimarket.ui.viewmodels.FindItem
import com.example.unimarket.ui.viewmodels.FindOfferViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindOfferScreen(
    navController: NavController,
    bottomNavController: NavController
) {
    val context = LocalContext.current
    val viewModel: FindOfferViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return FindOfferViewModel(context) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find & Offer") },
                actions = {
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = {
            if (uiState.isOffline) {
                SnackbarHost(hostState = remember { SnackbarHostState() }) {
                    Snackbar { Text("Offline: showing cache data") }
                }
            }
        },
        floatingActionButton = {
            if (scrollState.value > 0) {
                FloatingActionButton(
                    onClick = { scope.launch { scrollState.animateScrollTo(0) } },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Scroll to top")
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Color(0x88000000)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(scrollState)
                    .padding(top = if (uiState.showGreetingBanner) 100.dp else 0.dp)
            ) {
                TopBarWithSearch(
                    isSearchVisible = uiState.isSearchVisible,
                    searchText      = uiState.searchText,
                    onSearchClick   = viewModel::onSearchClick,
                    onTextChange    = viewModel::onTextChange,
                    onClearSearch   = viewModel::onClearSearch,
                    onNewFindClick  = { bottomNavController.navigate("publishFind") }
                )

                Spacer(Modifier.height(12.dp))

                LazyRow(
                    contentPadding       = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(listOf(
                        "ALL REQUESTS","MATERIALS","TECHNOLOGY",
                        "SPORTS","BOOKS","MUSICAL INSTRUMENTS",
                        "SUITS","TOYS"
                    )) { cat ->
                        Text(
                            text  = cat,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                SectionTitle("All")
                SectionRow(
                    items        = uiState.findList.filter { it.title.contains(uiState.searchText, true) },
                    onFindClick  = { id -> navController.navigate("findDetail/$id") },
                    onOfferClick = { id -> navController.navigate("offerDetail/$id") }
                )

                SectionTitle("From Your Major")
                SectionColumn(
                    items   = uiState.findList.filter { it.major == uiState.userMajor },
                    onClick = { id -> navController.navigate("offerDetail/$id") }
                )

                SectionTitle("Selling out")
                SectionColumn(
                    items   = uiState.findList.filter { it.status == "active" },
                    onClick = { id -> navController.navigate("findDetail/$id") }
                )

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun TopBarWithSearch(
    isSearchVisible: Boolean,
    searchText:      String,
    onSearchClick:   () -> Unit,
    onTextChange:    (String) -> Unit,
    onClearSearch:   () -> Unit,
    onNewFindClick:  () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = onNewFindClick,
            colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("NEW FIND", color = Color.White)
        }

        if (!isSearchVisible) {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value         = searchText,
                    onValueChange = onTextChange,
                    placeholder   = { Text("Search…") },
                    singleLine    = true,
                    modifier      = Modifier
                        .width(200.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        )
                )
                IconButton(onClick = onClearSearch) {
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Clear")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.titleMedium,
        color    = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 16.dp)
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SectionRow(
    items: List<FindItem>,
    onFindClick:  (String) -> Unit,
    onOfferClick: (String) -> Unit
) {
    LazyRow(
        contentPadding       = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { find ->
            FindCard(
                item         = find,
                onFindClick  = { onFindClick(find.id) },
                onOfferClick = { onOfferClick(find.id) }
            )
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
fun FindCard(
    item: FindItem,
    onFindClick:  () -> Unit,
    onOfferClick: () -> Unit
) {
    Card(
        onClick  = onFindClick,
        modifier = Modifier
            .width(260.dp)
            .wrapContentHeight(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(8.dp)) {
            val painter =
                if (item.image.isNotBlank())
                    rememberAsyncImagePainter(item.image)
                else painterResource(R.drawable.default_product)

            Image(
                painter             = painter,
                contentDescription  = item.title,
                modifier            = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale        = ContentScale.Crop
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = item.title,
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onBackground,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis
            )
            Text(
                text      = item.description,
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis,
                modifier  = Modifier.height(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onFindClick,
                    Modifier.weight(1f),
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Find", color = Color.White)
                }
                Button(
                    onClick = onOfferClick,
                    Modifier.weight(1f),
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Offer", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SectionColumn(
    items: List<FindItem>,
    onClick: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier            = Modifier.padding(horizontal = 16.dp)
    ) {
        items.forEach { find ->
            HorizontalFindCard(
                item    = find,
                onClick = { onClick(find.id) }
            )
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
fun HorizontalFindCard(
    item: FindItem,
    onClick: () -> Unit
) {
    Card(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.padding(8.dp)
        ) {
            val painter =
                if (item.image.isNotBlank())
                    rememberAsyncImagePainter(item.image)
                else painterResource(R.drawable.default_product)

            Image(
                painter             = painter,
                contentDescription  = item.title,
                modifier            = Modifier
                    .size(60.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale        = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text      = item.title,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onBackground,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis
                )
                Text(
                    text      = item.description,
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowRight,
                contentDescription = "Go",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
