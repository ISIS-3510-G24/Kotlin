package com.example.unimarket.ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.unimarket.ui.viewmodels.WriteUserReviewViewModel

@Composable
fun WriteUserReviewScreen(
    targetId: String,
    viewModel: WriteUserReviewViewModel = hiltViewModel(),
    navController: NavController
) {
    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp)) {
        Text("Write a review for $targetId", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        Text("Rating")
        StarRatingBar(
            rating = rating,
            onRatingChange = { newRating -> rating = newRating },
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(Modifier.height(8.dp))

        TextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Comment") },
            modifier = Modifier
                .padding(vertical = 8.dp),
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            viewModel.submitReview(targetId, rating, comment)
            navController.popBackStack()
        }) {
            Text("Submit Review")
        }
    }
}