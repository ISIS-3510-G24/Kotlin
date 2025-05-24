package com.example.unimarket.ui.models

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val errorMessage: String? = null,
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0
)