package com.example.unimarket.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val orderId: String,
    val buyerId: String,
    val sellerId: String,
    val productId: String,
    val date: Long,
    val price: Double,
    val status: String
)