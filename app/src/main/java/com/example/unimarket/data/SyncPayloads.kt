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

data class PublishProductPayload(
    val majorId: String,
    val classId: String,
    val title: String,
    val description: String,
    val price: Double,
    val labels: List<String>,
    val imageUrls: List<String>,
    val status: String,
)

data class PublishWithImagePayload(
    val majorId: String,
    val classId: String,
    val title: String,
    val description: String,
    val price: Double,
    val labels: List<String>,
    val localImageUri: String,
    val remotePath: String,
    val status: String,
)

data class UserReviewPayload(
    val localId: Long,
    val targetUserId: String,
    val reviewerUserId: String,
    val orderId: String,
    val rating: Int,
    val comment: String
)