package com.example.unimarket.ui.views

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.unimarket.ui.viewmodels.MyUserReviewsViewModel

@Composable
fun MyUserReviewsScreen(
    navController: NavController,
    viewModel: MyUserReviewsViewModel = hiltViewModel()
) {
    val reviews by viewModel.reviews.collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(reviews) { r ->
            ListItem(
                headlineContent    = { Text("⭐".repeat(r.rating)) },
                supportingContent = { Text(r.comment) }
            )
            Divider()
        }
    }
}