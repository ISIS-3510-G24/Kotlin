package com.example.unimarket.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val orderId: String,
    val buyerId: String,
    val productId: String,
    val date: Long,
    val total: Double,
    val status: String
)
