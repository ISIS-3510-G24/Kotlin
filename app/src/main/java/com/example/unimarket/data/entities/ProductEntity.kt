package com.example.unimarket.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val title : String,
    val description: String,
    val price: Double,
    val imageUrls: List<String>,
    val labels: List<String>,
    val status: String,
    val fetchedAt: Long
)