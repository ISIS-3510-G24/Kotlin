package com.example.unimarket.ui.explore

import com.google.firebase.Timestamp

// This data class represents a product item stored in Firestore
data class Product(
    val id: String = "",
    val classId: String = "",
    val createdAt: Timestamp? = null,
    val description: String = "",
    val imageUrls: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val majorID: String = "",
    val price: Double = 0.0,
    val sellerID: String = "",
    val status: String = "",
    val title: String = "",
    val updatedAt: Timestamp? = null
)
