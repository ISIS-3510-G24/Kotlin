package com.example.unimarket.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.ui.models.Find
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OfferDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _findDetail = MutableStateFlow<Find?>(null)
    val findDetail: StateFlow<Find?> = _findDetail

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val db = FirebaseFirestore.getInstance()
    private val findId: String = checkNotNull(savedStateHandle["findId"]) { "Missing findId" }

    init {
        fetchFindDetail()
    }

    private fun fetchFindDetail() {
        _isLoading.value = true
        viewModelScope.launch {
            db.collection("finds").document(findId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val find = Find(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            image = listOf(doc.getString("image") ?: ""),
                            labels = doc.get("labels") as? List<String> ?: emptyList(),
                            major = doc.getString("major") ?: "",
                            userName = doc.getString("userName") ?: "",
                            offerCount = doc.getLong("offerCount")?.toInt() ?: 0,
                            upvoteCount = doc.getLong("upvoteCount")?.toInt() ?: 0,
                            status = doc.getString("status") ?: ""
                        )
                        _findDetail.value = find
                    } else {
                        _error.value = "Item not found."
                    }
                    _isLoading.value = false
                }
                .addOnFailureListener { ex ->
                    _error.value = "Error cargando detalle: ${ex.message}"
                    _isLoading.value = false
                }
        }
    }
}
