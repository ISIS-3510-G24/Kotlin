package com.example.unimarket.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.unimarket.ui.viewmodels.FindDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindDetailScreen(
    navController: NavController,
    viewModel: FindDetailViewModel = viewModel()
) {
    val findDetail by viewModel.findDetail.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(findDetail?.title ?: "Find Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (findDetail == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val find = findDetail!!
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    if (find.image.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(LocalContext.current)
                                    .data(find.image)
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = find.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(find.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Major: ${find.major}", style = MaterialTheme.typography.bodyMedium)
                    Text("User: ${find.userName}", style = MaterialTheme.typography.bodyMedium)

                    if (find.labels.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Labels:", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            find.labels.forEach {
                                AssistChip(onClick = {}, label = { Text(it) })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Description:", fontWeight = FontWeight.Bold)
                    Text(find.description)
                }
            }
        }
    }
}
