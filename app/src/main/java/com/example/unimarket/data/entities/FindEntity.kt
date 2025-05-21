package com.example.unimarket.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "finds")
data class FindEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val image: List<String>,
    val labels: List<String>,
    val major: String,
    val offerCount: String,
    val status: String,
    val fetchedAt: Long
)
