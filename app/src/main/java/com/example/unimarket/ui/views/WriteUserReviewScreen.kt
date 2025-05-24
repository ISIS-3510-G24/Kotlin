package com.example.unimarket.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.unimarket.ui.viewmodels.WriteUserReviewViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteUserReviewScreen(
    orderId: String,
    targetId: String,
    navController: NavController,
    viewModel: WriteUserReviewViewModel = hiltViewModel()
) {
    val displayName by produceState<String?>(
        initialValue = null,
        key1 = targetId
    ) {
        val doc = Firebase.firestore
            .collection("User")
            .document(targetId)
            .get()
            .await()
        value = doc.getString("displayName") ?: "Unknown"
    }

    var rating  by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text("Write a review for: ${displayName ?: "..."}")
            })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier  = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                elevation = CardDefaults.cardElevation(8.dp),
                shape     = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier           = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StarRatingBar(
                        rating         = rating,
                        onRatingChange = { rating = it },
                        modifier       = Modifier.align(Alignment.CenterHorizontally)
                    )

                    TextField(
                        value         = comment,
                        onValueChange = { comment = it },
                        modifier      = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 200.dp),
                        placeholder   = { Text("Comment") },
                        maxLines      = 5
                    )

                    Button(
                        onClick  = {
                            viewModel.submitReview(orderId, targetId, rating, comment)
                            navController.popBackStack()
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}
