package com.example.unimarket.ui.views

import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.ui.models.BottomNavItem
import com.example.unimarket.ui.viewmodels.ProfileViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    rootNavController: NavController,
    bottomItems: List<BottomNavItem>,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val analytics = FirebaseAnalytics.getInstance(context)

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Settings") }) },
    ) { innerPadding ->
        Box(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                uiState.errorMessage != null -> {
                    LaunchedEffect(uiState.errorMessage) {
                        Toast.makeText(context, uiState.errorMessage, Toast.LENGTH_LONG).show()
                    }
                }

                uiState.user != null -> {
                    val u = uiState.user!!
                    Column(Modifier
                        .fillMaxSize()
                        .padding(16.dp)) {
                        // avatar y edit
                        Box(Modifier
                            .size(100.dp)
                            .align(Alignment.CenterHorizontally)) {
                            Image(
                                painter = rememberAsyncImagePainter(u.profilePicture),
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { navController.navigate("edit_profile") },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            u.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text(
                            u.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("${u.ratingAverage} (${u.reviewsCount} reviews)")
                        }

                        Spacer(Modifier.height(16.dp))

                        val menu = listOf(
                            "Wishlist" to "wishlist",
                            "Edit Profile" to "edit_profile",
                            "Validate delivery (Seller)" to "validate_seller",
                            "Receive & validate (Buyer)" to "validate_buyer",
                            "Log Out" to "logout"
                        )
                        LazyColumn {
                            items(menu) { (label, route) ->
                                ListItem(
                                    headlineContent = { Text(label) },
                                    trailingContent = {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            analytics.logEvent(
                                                "profile_option_clicked",
                                                Bundle().apply { putString("option", label) })

                                            if (route == "logout") {
                                                FirebaseAuth.getInstance().signOut()
                                                rootNavController.navigate("login") {
                                                    popUpTo(rootNavController.graph.startDestinationId) { inclusive = true }
                                                }
                                            } else {
                                                navController.navigate(route)
                                            }
                                        }
                                )
                                Divider()
                            }
                            item {
                                Spacer(Modifier.height(24.dp))
                                Button(
                                    onClick = { throw RuntimeException("Forced crash") },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Force Crash") }
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val perf = Firebase.performance
                                        val trace = perf.newTrace("profile_track_performance")
                                        trace.start()
                                        trace.stop()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Track Performance") }
                            }
                        }
                    }
                }
            }
        }
    }
}
