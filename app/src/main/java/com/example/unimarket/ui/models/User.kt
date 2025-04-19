package com.example.unimarket.ui.models

import com.google.firebase.Timestamp

data class User(
    val bio: String = "",
    val createdAt: Timestamp? = null,
    val displayName: String = "",
    val email: String = "",
    val major: String = "",
    val preferences: List<String> = emptyList(),
    val profilePicture: String = "",
    val ratingAverage: Double = 0.0,
    val reviewsCount: Int = 0,
    val updatedAt: Timestamp? = null
)