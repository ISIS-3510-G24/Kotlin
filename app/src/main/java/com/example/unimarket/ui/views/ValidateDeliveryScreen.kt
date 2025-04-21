package com.example.unimarket.ui.views

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidateDeliveryScreen(
    navController: NavController
) {
    val firestore = FirebaseFirestore.getInstance()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val orderId = backStackEntry?.arguments?.getString("orderId")

    var hashConfirm by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(orderId) {
        if (orderId == null) {
            errorMsg = "No order ID provided"
            loading = false
            return@LaunchedEffect
        }

        loading = true
        errorMsg = null
        try {
            val doc = firestore.collection("orders")
                .document(orderId)
                .get()
                .await()

            if (doc.exists()) {
                hashConfirm = doc.getString("hashConfirm")
                if (hashConfirm.isNullOrBlank()) {
                    errorMsg = "The hashConfirm field is empty"
                }
            } else {
                errorMsg = "Order not found"
            }
        } catch (e: Exception) {
            Log.e("ValidateDelivery", "Error fetching order", e)
            errorMsg = e.localizedMessage ?: "Unknown error"
        } finally {
            loading = false
        }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                loading -> {
                    CircularProgressIndicator()
                }

                errorMsg != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(errorMsg!!, color = Color.Red, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { navController.popBackStack() }) {
                            Text("Back")
                        }
                    }
                }

                hashConfirm != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Scan this code to confirm delivery",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(24.dp))
                        generateQrCodeBitmap(hashConfirm!!)?.let { bmp ->
                            Image(
                                bitmap = bmp,
                                contentDescription = "QR delivery",
                                modifier = Modifier
                                    .size(250.dp)
                                    .background(Color.LightGray, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } ?: Text("Error generating QR", color = Color.Red)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { navController.popBackStack() }) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

fun generateQrCodeBitmap(data: String): ImageBitmap? {
    return try {
        val writer = QRCodeWriter()
        val matrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
        val width = matrix.width
        val height = matrix.height
        val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bmp.asImageBitmap()
    } catch (e: Exception) {
        Log.e("ValidateDelivery", "QR generation failed: ${e.message}")
        null
    }
}
