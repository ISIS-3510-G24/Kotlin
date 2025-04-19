package com.example.unimarket.ui.models

// Data class to represent a class item from Firestore.
// Each document's ID is used as the id and it contains an attribute "name".
data class ClassItem(
    val id: String = "",
    val name: String = ""
)