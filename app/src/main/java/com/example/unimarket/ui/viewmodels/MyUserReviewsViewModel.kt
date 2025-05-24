package com.example.unimarket.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.UniMarketRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyUserReviewsViewModel @Inject constructor(
    private val repo: UniMarketRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {
    private val me: String = auth.currentUser!!.uid

    init {
        viewModelScope.launch {
            repo.refreshUserReviews(me)
        }
    }

    val reviews = repo
        .observeLatestReviewsFor(me, limit = 10)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val ratingStats = repo.observeUserRatingStats(me)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
}