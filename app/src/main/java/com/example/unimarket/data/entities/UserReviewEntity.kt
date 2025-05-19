package com.example.unimarket.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_reviews")
data class UserReviewEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val targetUserId: String,
    val reviewerUserId: String,
    val rating: Int,
    val comment: String,
    val createdAt: Long,
    val status: String // "PENDING", "SENT", "FAILED"
)