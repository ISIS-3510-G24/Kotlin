package com.example.unimarket.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.unimarket.ui.viewmodels.MyUserReviewsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyUserReviewsScreen(
    navController: NavController,
    viewModel: MyUserReviewsViewModel = hiltViewModel()
) {
    val reviews by viewModel.reviews.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My reviews") })
        }
    ) { padding ->
        if (reviews.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("You don't have reviews", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reviews) { r ->
                    ListItem(
                        headlineContent    = { Text("⭐".repeat(r.rating)) },
                        supportingContent  = { Text(r.comment) }
                    )
                    Divider()
                }
            }
        }
    }
}

