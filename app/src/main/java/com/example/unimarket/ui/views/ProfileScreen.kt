package com.example.unimarket.ui.views

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
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
import com.google.zxing.integration.android.IntentIntegrator

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

    val qrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val scannedHash = IntentIntegrator
            .parseActivityResult(result.resultCode, result.data)
            ?.contents

        scannedHash?.let { hash ->
            viewModel.validateOrder(
                hashConfirm = hash,
                onSuccess = { Toast.makeText(context, "Order validated", Toast.LENGTH_SHORT).show() },
                onError    = { err -> Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show() }
            )
        } ?: Toast.makeText(context, "Operation canceled", Toast.LENGTH_SHORT).show()
    }

    // Launcher to pick image from gallery
    val imagePicker = rememberLauncherForActivityResult (
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadProfilePicture(
                uri = it,
                onSuccess = {
                    Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Settings") }) }
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
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Avatar + tap-to-select new picture
                        Box(
                            Modifier
                                .size(100.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(u.profilePicture),
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        // Launch gallery picker
                                        imagePicker.launch("image/*")
                                    },
                                contentScale = ContentScale.Crop
                            )
                            // Optional: overlay edit icon
                            IconButton(
                                onClick = { imagePicker.launch("image/*") },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit photo")
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
                                            if (route == "validate_buyer") {
                                                val integrator = IntentIntegrator(context as Activity)
                                                    .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                                                qrLauncher.launch(integrator.createScanIntent())
                                            } else {
                                                analytics.logEvent(
                                                    "profile_option_clicked",
                                                    Bundle().apply {
                                                        putString("option", label)
                                                    })
                                                if (route == "logout") {
                                                    FirebaseAuth.getInstance().signOut()
                                                    rootNavController.navigate("login") {
                                                        popUpTo(rootNavController.graph.startDestinationId) {
                                                            inclusive = true
                                                        }
                                                    }
                                                } else {
                                                    navController.navigate(route)
                                                }
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
