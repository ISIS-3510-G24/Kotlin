package com.example.unimarket.ui.models

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val errorMessage: String? = null,
    val averageRating: Double? = null,
    val reviewCount: Int = 0
)