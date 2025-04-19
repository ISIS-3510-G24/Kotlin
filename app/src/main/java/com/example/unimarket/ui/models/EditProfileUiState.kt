package com.example.unimarket.ui.models

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val bio: String = "",
    val majorList: List<Major> = emptyList(),
    val selectedMajor: Major? = null,
    val profilePicUrl: String = "",
    val errorMessage: String? = null
)