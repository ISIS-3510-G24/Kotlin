package com.example.unimarket.ui.viewmodels

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.UniMarketDatabase
import com.example.unimarket.data.entities.FindEntity
import com.example.unimarket.ui.models.Find
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OfferDetailViewModel(
    app: Application,
    private val handle: SavedStateHandle
) : AndroidViewModel(app) {

    private val findDao   = UniMarketDatabase.getInstance(app).findDao()
    private val firestore = FirebaseFirestore.getInstance()

    private val _findDetail = MutableStateFlow<Find?>(null)
    val findDetail: StateFlow<Find?> = _findDetail.asStateFlow()

    private val _isLoading  = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isOffline  = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _error      = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val findId: String = requireNotNull(handle["findId"]) { "findId missing" }

    init {
        observeLocal()
        refresh()
    }

    private fun observeLocal() {
        viewModelScope.launch {
            findDao.observeById(findId)
                .flowOn(Dispatchers.IO)
                .collect { ent ->
                    _findDetail.value = ent?.toModel()
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            _isOffline.value = !isOnline()

            if (_isOffline.value) {
                _isLoading.value = false
                return@launch
            }

            try {
                val doc = firestore.collection("finds")
                    .document(findId)
                    .get()
                    .await()

                if (!doc.exists()) {
                    _error.value = "Ítem not found."
                } else {
                    val now = System.currentTimeMillis()

                    val rawImages: List<String> = when {
                        doc.contains("imageUrls") -> (doc.get("imageUrls") as? List<String>) ?: emptyList()
                        doc.contains("images")    -> (doc.get("images")    as? List<String>) ?: emptyList()
                        doc.contains("image")     -> when (val v = doc.get("image")) {
                            is List<*>  -> v.filterIsInstance<String>()
                            is String  -> listOf(v)
                            else       -> emptyList()
                        }
                        else -> emptyList()
                    }

                    Log.d("OfferDetailVM", "rawImages=$rawImages")

                    val ent = FindEntity(
                        id          = doc.id,
                        title       = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        image       = rawImages,
                        labels      = doc.get("labels") as? List<String> ?: emptyList(),
                        major       = doc.getString("major") ?: "",
                        offerCount  = (doc.getLong("offerCount")?.toString() ?: "0"),
                        status      = doc.getString("status") ?: "",
                        fetchedAt   = now
                    )

                    _findDetail.value = ent.toModel()
                    findDao.insert(ent)
                }
            } catch (e: Exception) {
                _error.value = "Error loading: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun isOnline(): Boolean {
        val cm   = getApplication<Application>()
            .getSystemService(ConnectivityManager::class.java)
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun FindEntity.toModel() = Find(
        id          = id,
        title       = title,
        description = description,
        image       = image,
        labels      = labels,
        major       = major,
        userName    = "",
        offerCount  = offerCount.toIntOrNull() ?: 0,
        upvoteCount = 0,
        status      = status
    )
}
