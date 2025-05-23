package com.example.unimarket.ui.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
    val searchText: String = "",
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null
)

class FindOfferViewModel(
    private val appContext: Context
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db   = Firebase.firestore
    private var listener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(FindOfferUiState())
    val uiState: StateFlow<FindOfferUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    private fun isOnline(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isOffline = !isOnline(),
                error = null
            )

            val uid = auth.currentUser?.uid
            if (uid == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }

            try {
                val doc = db.collection("User").document(uid).get().await()
                val major = doc.getString("major")?.uppercase() ?: ""
                _uiState.value = _uiState.value.copy(userMajor = major)
            } catch (_: Exception) {
            }

            if (isOnline()) {
                try {
                    val snap = db.collection("finds").get().await()
                    val items = snap.documents.map { d ->
                        FindItem(
                            id = d.id,
                            title = d.getString("title") ?: "",
                            description = d.getString("description") ?: "",
                            image = d.getString("image") ?: "",
                            status = d.getString("status") ?: "",
                            major = d.getString("major")?.uppercase() ?: "",
                            offerCount = d.getLong("offerCount")?.toInt() ?: 0
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        findList = items
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
                listener?.remove()
                listener = db.collection("finds")
                    .addSnapshotListener { snap, err ->
                        if (err != null || snap == null) return@addSnapshotListener
                        val liveItems = snap.documents.map { d ->
                            FindItem(
                                id = d.id,
                                title = d.getString("title") ?: "",
                                description = d.getString("description") ?: "",
                                image = d.getString("image") ?: "",
                                status = d.getString("status") ?: "",
                                major = d.getString("major")?.uppercase() ?: "",
                                offerCount = d.getLong("offerCount")?.toInt() ?: 0
                            )
                        }
                        _uiState.value = _uiState.value.copy(findList = liveItems)
                    }
            } else {
                listener?.remove()
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
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
            isSearchVisible = false,
            searchText = ""
        )
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
