package com.example.unimarket.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.UniMarketRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SellerReviewsViewModel @Inject constructor(
    private val repo: UniMarketRepository,
    private val firestore: FirebaseFirestore,
    private val crashlytics: FirebaseCrashlytics,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sellerId: String = checkNotNull(savedStateHandle["sellerId"])

    private val _sellerName = MutableStateFlow<String?>(null)
    val sellerName: StateFlow<String?> = _sellerName.asStateFlow()

    init {
        viewModelScope.launch {
            repo.syncRemoteReviewsFor(sellerId)
        }
        fetchSellerName()
    }

    val reviews = repo
        .observeLatestReviewsFor(sellerId, limit = 5)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val ratingStats = repo
        .observeUserRatingStats(sellerId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private fun fetchSellerName() {
        if (sellerId.isBlank()) {
            _sellerName.value = "Undefined Seller"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val docUser = firestore
                    .collection("User")
                    .document(sellerId)
                    .get()
                    .await()

                if (docUser.exists()) {
                    val name = docUser.getString("displayName").takeUnless { it.isNullOrBlank() }
                    _sellerName.value = name ?: "No available name"
                } else {
                    _sellerName.value = "Seller not found"
                }
            } catch (e: Exception) {
                crashlytics.recordException(e)
                _sellerName.value = "Error loading seller"
            }
        }
    }
}
