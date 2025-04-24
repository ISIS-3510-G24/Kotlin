package com.example.unimarket.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.ui.models.Find
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FindDetailViewModel : ViewModel() {
    private val _findDetail = MutableStateFlow<Find?>(null)
    val findDetail: StateFlow<Find?> = _findDetail

    private val db = FirebaseFirestore.getInstance()

    fun loadFindDetail(findId: String) {
        viewModelScope.launch {
            db.collection("finds").document(findId)
                .get()
                .addOnSuccessListener { document ->
                    document?.let {
                        val find = Find(
                            id = it.getString("id") ?: "",
                            title = it.getString("title") ?: "",
                            description = it.getString("description") ?: "",
                            image = listOf(it.getString("image") ?: ""),
                            labels = it.get("labels") as? List<String> ?: emptyList(),
                            major = it.getString("major") ?: "",
                            userName = it.getString("userName") ?: "",
                            offerCount = it.getLong("offerCount")?.toInt() ?: 0,
                            upvoteCount = it.getLong("upvoteCount")?.toInt() ?: 0,
                            status = it.getString("status") ?: ""
                        )
                        _findDetail.value = find
                    }
                }
        }
    }
}
