package com.example.unimarket.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.R
import com.example.unimarket.ui.viewmodels.OfferDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferDetailScreen(
    navController: NavController,
    viewModel: OfferDetailViewModel = viewModel()
) {
    val detail   by viewModel.findDetail.collectAsState()
    val loading  by viewModel.isLoading.collectAsState()
    val offline  by viewModel.isOffline.collectAsState()
    val error    by viewModel.error.collectAsState()
    val snackBar = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let { snackBar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.title ?: "Detalle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBar) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                detail == null -> {
                    Text(
                        text = "No se encontró detalle.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    val currentDetail = detail ?: return@Scaffold

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Banner offline
                        if (offline) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFF4E5))
                                    .padding(8.dp)
                            ) {
                                Text("Sin conexión: datos de caché", color = Color(0xFF795548))
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        // Imagen (primera URL) o placeholder
                        val imageUrl = currentDetail.image.firstOrNull().orEmpty()
                        if (imageUrl.isNotBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(imageUrl),
                                contentDescription = currentDetail.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.default_product),
                                contentDescription = "Placeholder",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(currentDetail.title, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Major: ${currentDetail.major}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "User: ${currentDetail.userName.ifBlank { "Anonymous" }}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (currentDetail.labels.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text("Labels:", fontWeight = FontWeight.Bold)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                currentDetail.labels.forEach { lbl ->
                                    AssistChip(onClick = {}, label = { Text(lbl) })
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text("Description:", fontWeight = FontWeight.Bold)
                        Text(currentDetail.description)

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = { navController.navigate("publishProduct") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Text("Offer this product")
                        }
                    }
                }
            }
        }
    }
}
