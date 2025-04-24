package com.example.unimarket.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class FindItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val image: String = "",
    val status: String = "",
    val major: String = "",
    val offerCount: Int = 0
)

data class FindOfferUiState(
    val findList: List<FindItem> = emptyList(),
    val userMajor: String = "",
    val showGreetingBanner: Boolean = false,
    val isSearchVisible: Boolean = false,
    val searchText: String = ""
)

class FindOfferViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FindOfferUiState())
    val uiState: StateFlow<FindOfferUiState> = _uiState

    private val auth = FirebaseAuth.getInstance()
    private val db   = Firebase.firestore

    init {
        fetchUserMajor()
        fetchFinds()
    }

    private fun fetchUserMajor() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("User")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val major = doc.getString("major") ?: ""
                _uiState.value = _uiState.value.copy(userMajor = major)
            }
    }

    private fun fetchFinds() {
        db.collection("finds")
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents.map { doc ->
                    FindItem(
                        id          = doc.id,
                        title       = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        image       = doc.getString("image") ?: "",
                        status      = doc.getString("status") ?: "",
                        major       = doc.getString("major") ?: "",
                        offerCount  = doc.getLong("offerCount")?.toInt() ?: 0
                    )
                }
                _uiState.value = _uiState.value.copy(findList = items)
            }
    }

    fun onSearchClick() {
        _uiState.value = _uiState.value.copy(
            isSearchVisible = !_uiState.value.isSearchVisible
        )
    }

    fun onTextChange(text: String) {
        _uiState.value = _uiState.value.copy(searchText = text)
    }

    fun onClearSearch() {
        _uiState.value = _uiState.value.copy(
            searchText = "",
            isSearchVisible = false
        )
    }
}