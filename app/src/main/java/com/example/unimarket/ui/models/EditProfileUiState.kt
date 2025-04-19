package com.example.unimarket.ui.models

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val bio: String = "",
    val major: String = "",
    val profilePicUrl: String = "",
    val errorMessage: String? = null
)