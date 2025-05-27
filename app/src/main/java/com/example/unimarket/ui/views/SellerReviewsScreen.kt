package com.example.unimarket.ui.views

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.unimarket.data.entities.UserReviewEntity
import com.example.unimarket.ui.viewmodels.SellerReviewsViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerReviewsScreen(
    navController: NavController,
    viewModel: SellerReviewsViewModel = hiltViewModel()
) {
    val reviews    by viewModel.reviews.collectAsState()
    val stats      by viewModel.ratingStats.collectAsState()
    val sellerName by viewModel.sellerName.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Reviews of the Seller") })
        }
    ) { innerPadding ->
        if (reviews.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "This seller does not have reviews",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            val contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 8.dp,
                start = 16.dp,
                end = 16.dp
            )

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(
                            text = sellerName ?: "Loading...",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        stats?.let { s ->
                            val avg = String.format("%.1f", s.average)
                            Text(
                                text = "Average rating: $avg (${s.count} reviews)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                items(reviews, key = { it.localId }) { review ->
                    SellerReviewItem(review)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun SellerReviewItem(r: UserReviewEntity) {
    val date = remember(r.createdAt) {
        Instant.ofEpochMilli(r.createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    ListItem(
        headlineContent = { Text("⭐".repeat(r.rating)) },
        supportingContent = {
            Column {
                Text(r.comment)
                Text(date, style = MaterialTheme.typography.bodySmall)
            }
        }
    )
    HorizontalDivider()
}

