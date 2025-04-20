package com.example.unimarket.ui.views

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidateDeliveryScreen(navController: NavController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    val productToSell = listOf("1234567")
    var product by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showQr by remember { mutableStateOf(false) }
    var hashConfirm by remember { mutableStateOf("") }

    LaunchedEffect(productToSell) {
        val pid = productToSell.firstOrNull() ?: return@LaunchedEffect

        Log.d("ValidateDeliveryScreen", "Fetching product with ID: $pid")

        firestore.collection("Product")
            .document(pid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    Log.d("ValidateDeliveryScreen", "Product found: ${doc.id}")
                    val seller = doc.getString("sellerID").orEmpty()

                    if (seller == userId) {
                        product = doc.data
                        hashConfirm = doc.id
                    } else {
                        Toast.makeText(context, "You are not the seller of this product", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d("ValidateDeliveryScreen", "Product not found for ID: $pid")
                    Toast.makeText(context, "Product not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ValidateDeliveryScreen", "Error fetching product: ${exception.message}")
                Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        product?.let { p ->
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val imageUrl = (p["imageUrls"] as? List<*>)?.firstOrNull() as? String
                val labels = (p["labels"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                imageUrl?.let {
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .height(200.dp)
                            .fillMaxWidth()
                            .background(Color.LightGray, RoundedCornerShape(8.dp))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    p["title"] as? String ?: "No title",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    labels.forEach {
                        Text(
                            text = it,
                            modifier = Modifier
                                .background(Color.Blue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color.Blue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { showQr = true }) {
                    Text("Show delivery QR")
                }
            }

            if (showQr && hashConfirm.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { showQr = false },
                    title = { Text("Scan this QR") },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val qrBitmap = generateQrCodeBitmap(hashConfirm)
                            qrBitmap?.let {
                                Image(
                                    bitmap = it,
                                    contentDescription = "QR Code",
                                    modifier = Modifier.size(200.dp)
                                )
                            } ?: run {
                                Text("Failed to generate QR Code")
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showQr = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("There are no pending orders")
            }
        }
    }
}

fun generateQrCodeBitmap(data: String): ImageBitmap? {
    return if (data.isNotEmpty()) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bmp.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    } else {
        null
    }
}
