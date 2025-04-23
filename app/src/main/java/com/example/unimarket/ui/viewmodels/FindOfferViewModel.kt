package com.example.unimarket.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Modelo de un ítem de "finds"
data class FindItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val image: String = "",
    val status: String = "",
    val major: String = "",
    val offerCount: Int = 0
)

// Estado UI que expone el ViewModel
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

    /** Obtiene el major del usuario logueado desde la colección "User" */
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

    /** Carga todos los finds desde Firestore */
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

    /** Alterna la visibilidad del campo de búsqueda */
    fun onSearchClick() {
        _uiState.value = _uiState.value.copy(
            isSearchVisible = !_uiState.value.isSearchVisible
        )
    }

    /** Actualiza el texto del buscador */
    fun onTextChange(text: String) {
        _uiState.value = _uiState.value.copy(searchText = text)
    }

    /** Limpia y cierra el buscador */
    fun onClearSearch() {
        _uiState.value = _uiState.value.copy(
            searchText = "",
            isSearchVisible = false
        )
    }
}