package com.example.unimarket.data

data class WishlistOpPayload(
    val userId: String,
    val productId: String,
    val add: Boolean,
)

data class MarkUnavailablePayload(
    val productId: String,
)

data class CreateOrderPayload(
    val buyerId: String,
    val sellerId: String,
    val hashConfirm: String,
    val price: Double,
    val productId: String,
    val status: String,
)

data class UploadImagePayload(
    val localUri: String,
    val remotePath: String,
)