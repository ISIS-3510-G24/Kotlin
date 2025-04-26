package com.example.unimarket.ui.models

import com.google.firebase.Timestamp

// This data class represents a find item stored in Firestore
data class Find(
    val id: String = "",
    val description: String = "",
    val image: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val major: String = "",
    val offerCount: Int = 0,
    val status: String = "",
    val timestamp: Timestamp? = null,
    val title: String = "",
    val upvoteCount: Int = 0,
    val userId: String = "",
    val userName: String = ""
)
