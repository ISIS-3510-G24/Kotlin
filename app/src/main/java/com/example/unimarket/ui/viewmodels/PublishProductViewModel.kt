package com.example.unimarket.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

data class PublishUiState(
    val isPublishing: Boolean = false,
    val error: String? = null
)

class PublishProductViewModel : ViewModel() {
    private val storage   = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth      = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(PublishUiState())
    val uiState = _uiState

    fun publishOffer(
        findId: String,
        sellerId: String,
        title: String,
        description: String,
        price: Double,
        imageUri: Uri,
        onSuccess: () -> Unit
    ) {
        _uiState.value = PublishUiState(isPublishing = true)

        val ref = storage.reference.child("offers_images/${UUID.randomUUID()}")
        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { url ->
                    val newOffer = mapOf(
                        "findId"      to findId,
                        "sellerID"    to sellerId,
                        "title"       to title,
                        "description" to description,
                        "price"       to price,
                        "image"       to url.toString(),
                        "timestamp"   to Timestamp.now()
                    )
                    firestore.collection("products")
                        .add(newOffer)
                        .addOnSuccessListener { prodRef ->
                            val currentUser = auth.currentUser!!
                            val order = mapOf(
                                "buyerID"     to currentUser.uid,
                                "sellerID"    to sellerId,
                                "productID"   to prodRef.id,
                                "price"       to price,
                                "orderDate"   to Timestamp.now(),
                                "status"      to "Unpaid",
                                "hashConfirm" to UUID.randomUUID().toString().replace("-", "")
                            )
                            firestore.collection("orders")
                                .add(order)
                                .addOnSuccessListener {
                                    _uiState.value = PublishUiState(isPublishing = false)
                                    onSuccess()
                                }
                                .addOnFailureListener { e ->
                                    _uiState.value = PublishUiState(error = e.message)
                                }
                        }
                        .addOnFailureListener { e ->
                            _uiState.value = PublishUiState(error = e.message)
                        }
                }
            }
            .addOnFailureListener { e ->
                _uiState.value = PublishUiState(error = e.message)
            }
    }
}
