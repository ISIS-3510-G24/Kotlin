package com.example.unimarket.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.UniMarketRepository
import com.example.unimarket.di.IoDispatcher
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WriteUserReviewViewModel @Inject constructor(
    private val repo: UniMarketRepository,
    auth: FirebaseAuth,
    @IoDispatcher private val io: CoroutineDispatcher
): ViewModel() {
    private val me = auth.currentUser!!.uid
    fun submitReview(
        orderId: String,
        targetId: String,
        rating: Int,
        comment: String
    ) {
        viewModelScope.launch(io) {
            repo.postUserReview(me, targetId, orderId, rating, comment)
        }
    }
}