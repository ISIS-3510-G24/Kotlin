package com.example.unimarket.ui.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.UniMarketDatabase
import com.example.unimarket.data.daos.FindDao
import com.example.unimarket.data.entities.FindEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

data class FindItem(
    val id: String,
    val title: String,
    val description: String,
    val image: String,
    val status: String,
    val major: String,
    val offerCount: Int
)

class FindOfferViewModel(
    private val ctx: Context
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore
    private val dao: FindDao = UniMarketDatabase.getInstance(ctx).findDao()
    private var listener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(FindOfferUiState())
    val uiState: StateFlow<FindOfferUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dao.observeAll()
                .flowOn(Dispatchers.IO)
                .collect { entities ->
                    val items = entities.map { ent ->
                        FindItem(
                            id = ent.id,
                            title = ent.title,
                            description = ent.description,
                            image = ent.image.firstOrNull().orEmpty(),
                            status = ent.status,
                            major = ent.major,
                            offerCount = ent.offerCount.toIntOrNull() ?: 0
                        )
                    }
                    _uiState.update { it.copy(findList = items) }
                }
        }
        refreshAll()
    }

    private fun isOnline(): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isOffline = !isOnline(), error = null) }

            auth.currentUser?.uid?.let { uid ->
                runCatching {
                    db.collection("User").document(uid).get().await()
                }.onSuccess { doc ->
                    val maj = doc.getString("major")?.uppercase() ?: ""
                    _uiState.update { it.copy(userMajor = maj) }
                }
            }

            if (isOnline()) {
                runCatching {
                    db.collection("finds").get().await()
                }.onSuccess { snap ->
                    val now = System.currentTimeMillis()
                    val entities = snap.documents.map { d ->
                        FindEntity(
                            id = d.id,
                            title = d.getString("title") ?: "",
                            description = d.getString("description") ?: "",
                            image = d.get("image")?.let { v ->
                                when (v) {
                                    is List<*> -> v.filterIsInstance<String>()
                                    is String -> listOf(v)
                                    else -> emptyList()
                                }
                            } ?: emptyList(),
                            labels = d.get("labels") as? List<String> ?: emptyList(),
                            major = d.getString("major")?.uppercase() ?: "",
                            offerCount = d.getLong("offerCount")?.toString() ?: "0",
                            status = d.getString("status") ?: "",
                            fetchedAt = now
                        )
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        dao.insertAll(entities)
                    }
                }.onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }

                listener?.remove()
                listener = db.collection("finds")
                    .addSnapshotListener { snap, err ->
                        if (err != null || snap == null) return@addSnapshotListener
                        val now2 = System.currentTimeMillis()
                        val liveEntities = snap.documents.map { d ->
                            FindEntity(
                                id = d.id,
                                title = d.getString("title") ?: "",
                                description = d.getString("description") ?: "",
                                image = d.get("image")?.let { v ->
                                    when (v) {
                                        is List<*> -> v.filterIsInstance<String>()
                                        is String -> listOf(v)
                                        else -> emptyList()
                                    }
                                } ?: emptyList(),
                                labels = d.get("labels") as? List<String> ?: emptyList(),
                                major = d.getString("major")?.uppercase() ?: "",
                                offerCount = d.getLong("offerCount")?.toString() ?: "0",
                                status = d.getString("status") ?: "",
                                fetchedAt = now2
                            )
                        }
                        viewModelScope.launch(Dispatchers.IO) {
                            dao.insertAll(liveEntities)
                        }
                    }

            } else {
                listener?.remove()
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onSearchClick() {
        _uiState.update { it.copy(isSearchVisible = !it.isSearchVisible) }
    }

    fun onTextChange(txt: String) {
        _uiState.update { it.copy(searchText = txt) }
    }

    fun onClearSearch() {
        _uiState.update { it.copy(isSearchVisible = false, searchText = "") }
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
